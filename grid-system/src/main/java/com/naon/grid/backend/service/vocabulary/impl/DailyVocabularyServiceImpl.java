package com.naon.grid.backend.service.vocabulary.impl;

import com.naon.grid.backend.domain.vocabulary.DailyVocabulary;
import com.naon.grid.backend.repo.vocabulary.DailyVocabularyRepository;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.vocabulary.DailyVocabularyService;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyDto;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyQueryCriteria;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import com.naon.grid.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyVocabularyServiceImpl implements DailyVocabularyService {

    private static final String CACHE_KEY_TODAY_MAIN = "daily_vocabulary:today:main";
    private static final String CACHE_KEY_TODAY_BACKUPS = "daily_vocabulary:today:backups";

    private final DailyVocabularyRepository dailyVocabularyRepository;
    private final ExampleSentenceService exampleSentenceService;
    private final RedisUtils redisUtils;

    // ==================== 查询 ====================

    @Override
    public PageResult<DailyVocabularyDto> queryAll(DailyVocabularyQueryCriteria criteria, Pageable pageable) {
        Page<DailyVocabulary> page = dailyVocabularyRepository.findAll((root, cq, cb) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, cb);
            Predicate statusPredicate = cb.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return cb.and(basePredicate, statusPredicate);
        }, pageable);
        PageResult<DailyVocabularyDto> result = PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyVocabularyDto findById(Integer id) {
        DailyVocabulary entity = getEntity(id);
        if (isDraftOrReviewed(entity)) {
            return buildDtoFromDraft(entity);
        }
        return buildDtoFromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyVocabularyDto findPublishedById(Integer id) {
        DailyVocabulary entity = getEntity(id);
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(entity.getPublishStatus())) {
            throw new EntityNotFoundException(DailyVocabulary.class, "id", String.valueOf(id));
        }
        return buildDtoFromEntity(entity);
    }

    // ==================== 写入 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer create(DailyVocabularyDto dto) {
        DailyVocabulary entity = new DailyVocabulary();
        entity.setStatus(StatusEnum.ENABLED.getCode());
        entity.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        entity.setEditStatus(EditStatusEnum.DRAFT.getCode());
        entity.setPhrase(dto.getPhrase());
        if (dto.getPhraseType() != null) {
            entity.setPhraseType(dto.getPhraseType());
        }
        entity.setDraftContent(JsonUtils.toJson(dto));
        entity = dailyVocabularyRepository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Integer id, DailyVocabularyDto dto) {
        DailyVocabulary entity = getEntity(id);

        // 已发布不允许改词目
        String newPhrase = dto.getPhrase();
        if (newPhrase != null && !newPhrase.equals(entity.getPhrase())) {
            if (PublishStatusEnum.PUBLISHED.getCode().equals(entity.getPublishStatus())) {
                throw new BadRequestException("已发布的每日一词不允许修改词目");
            }
            entity.setPhrase(newPhrase);
        }

        // 回退到草稿
        if (EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus()) ||
                EditStatusEnum.PUBLISHED.getCode().equals(entity.getEditStatus())) {
            entity.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }

        entity.setDraftContent(JsonUtils.toJson(dto));
        dailyVocabularyRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        DailyVocabulary entity = getEntity(id);
        entity.setStatus(StatusEnum.DISABLED.getCode());
        dailyVocabularyRepository.save(entity);
        evictTodayCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Integer id) {
        DailyVocabulary entity = getEntity(id);
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅草稿状态可审核");
        }
        entity.setEditStatus(EditStatusEnum.REVIEWED.getCode());
        dailyVocabularyRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Integer id) {
        DailyVocabulary entity = getEntity(id);
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅已审核状态可发布");
        }

        DailyVocabularyDto draft = JsonUtils.fromJson(entity.getDraftContent(), DailyVocabularyDto.class);
        if (draft == null) {
            throw new BadRequestException("草稿数据解析失败");
        }

        // 写回主表业务字段
        entity.setPhrase(draft.getPhrase());
        entity.setPhraseType(draft.getPhraseType());
        entity.setPinyin(draft.getPinyin());
        entity.setPhraseTranslations(JsonUtils.toTranslationJson(draft.getPhraseTranslations()));
        entity.setAudioId(draft.getAudioId());
        entity.setImageId(draft.getImageId());
        entity.setPlainExplanation(draft.getPlainExplanation());
        entity.setExplanationTranslations(JsonUtils.toTranslationJson(draft.getExplanationTranslations()));
        entity.setOriginStory(draft.getOriginStory());
        entity.setExampleSentenceId(draft.getExampleSentenceId());
        entity.setDisplayDate(draft.getDisplayDate());
        entity.setSortOrder(draft.getSortOrder() != null ? draft.getSortOrder() : 0);
        entity.setRelatedWordId(draft.getRelatedWordId());

        entity.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        entity.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        entity.setDraftContent(null);
        dailyVocabularyRepository.save(entity);

        // 清缓存
        if (entity.getDisplayDate() != null && entity.getDisplayDate().equals(LocalDate.now())) {
            evictTodayCache();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Integer id) {
        DailyVocabulary entity = getEntity(id);
        entity.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        dailyVocabularyRepository.save(entity);
        evictTodayCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void schedule(Integer id, LocalDate date) {
        DailyVocabulary entity = getEntity(id);
        entity.setDisplayDate(date);
        dailyVocabularyRepository.save(entity);
        if (date != null && date.equals(LocalDate.now())) {
            evictTodayCache();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSchedule(List<Integer> ids, List<LocalDate> dates) {
        if (ids.size() != dates.size()) {
            throw new BadRequestException("ID 列表和日期列表长度不一致");
        }
        boolean todayAffected = false;
        for (int i = 0; i < ids.size(); i++) {
            DailyVocabulary entity = getEntity(ids.get(i));
            entity.setDisplayDate(dates.get(i));
            dailyVocabularyRepository.save(entity);
            if (dates.get(i) != null && dates.get(i).equals(LocalDate.now())) {
                todayAffected = true;
            }
        }
        if (todayAffected) {
            evictTodayCache();
        }
    }

    // ==================== 今日内容 ====================

    @Override
    public DailyVocabularyDto getTodayMain() {
        // 尝试缓存
        DailyVocabularyDto cached = redisUtils.get(CACHE_KEY_TODAY_MAIN, DailyVocabularyDto.class);
        if (cached != null) {
            return cached;
        }

        List<DailyVocabulary> list = dailyVocabularyRepository
                .findByDisplayDateAndPublishStatusAndStatusOrderBySortOrderAsc(
                        LocalDate.now(), PublishStatusEnum.PUBLISHED.getCode(), StatusEnum.ENABLED.getCode());

        if (list.isEmpty()) {
            return null;
        }

        DailyVocabularyDto dto = buildDtoFromEntity(list.get(0));
        redisUtils.set(CACHE_KEY_TODAY_MAIN, dto, secondsUntilMidnight());
        return dto;
    }

    @Override
    public List<DailyVocabularyDto> getTodayBackups() {
        // 尝试缓存
        List<DailyVocabularyDto> cached = redisUtils.getList(CACHE_KEY_TODAY_BACKUPS, DailyVocabularyDto.class);
        if (cached != null) {
            return cached;
        }

        List<DailyVocabulary> list = dailyVocabularyRepository
                .findByDisplayDateAndPublishStatusAndStatusOrderBySortOrderAsc(
                        LocalDate.now(), PublishStatusEnum.PUBLISHED.getCode(), StatusEnum.ENABLED.getCode());

        if (list.size() <= 1) {
            return Collections.emptyList();
        }

        List<DailyVocabularyDto> backups = list.subList(1, list.size()).stream()
                .map(this::buildDtoFromEntity)
                .collect(Collectors.toList());
        redisUtils.set(CACHE_KEY_TODAY_BACKUPS, backups, secondsUntilMidnight());
        return backups;
    }

    // ==================== 历史 & 日历 ====================

    @Override
    public PageResult<DailyVocabularyDto> queryHistory(DailyVocabularyQueryCriteria criteria, Pageable pageable) {
        Page<DailyVocabulary> page = dailyVocabularyRepository.findAll((root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 仅已发布有效
            predicates.add(cb.equal(root.get("status"), StatusEnum.ENABLED.getCode()));
            predicates.add(cb.equal(root.get("publishStatus"), PublishStatusEnum.PUBLISHED.getCode()));

            // 可选过滤
            if (criteria.getPhraseType() != null) {
                predicates.add(cb.equal(root.get("phraseType"), criteria.getPhraseType()));
            }
            if (criteria.getBlurry() != null) {
                predicates.add(cb.like(root.get("phrase"), "%" + criteria.getBlurry() + "%"));
            }
            if (criteria.getDisplayDateStart() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("displayDate"), criteria.getDisplayDateStart()));
            }
            if (criteria.getDisplayDateEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("displayDate"), criteria.getDisplayDateEnd()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        return PageUtil.toPage(page.map(this::buildDtoFromEntity));
    }

    @Override
    public List<LocalDate> getCalendarDates(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        List<java.sql.Date> sqlDates = dailyVocabularyRepository.findDistinctDisplayDatesByMonth(start, end);
        return sqlDates.stream()
                .map(java.sql.Date::toLocalDate)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveShareImage(Integer id, Long imageId) {
        DailyVocabulary entity = getEntity(id);
        entity.setImageId(imageId);
        dailyVocabularyRepository.save(entity);
    }

    // ==================== 私有辅助 ====================

    private DailyVocabulary getEntity(Integer id) {
        DailyVocabulary entity = dailyVocabularyRepository.findById(id).orElse(null);
        if (entity == null || StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(DailyVocabulary.class, "id", String.valueOf(id));
        }
        return entity;
    }

    private boolean isDraftOrReviewed(DailyVocabulary entity) {
        return EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus());
    }

    /** 从草稿 JSON 构建 DTO，ID/状态/审计以主表为准 */
    private DailyVocabularyDto buildDtoFromDraft(DailyVocabulary entity) {
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        DailyVocabularyDto draft = JsonUtils.fromJson(entity.getDraftContent(), DailyVocabularyDto.class);
        if (draft == null) {
            throw new BadRequestException("草稿数据解析失败");
        }
        draft.setId(entity.getId());
        draft.setStatus(entity.getStatus());
        draft.setPublishStatus(entity.getPublishStatus());
        draft.setEditStatus(entity.getEditStatus());
        draft.setCreateBy(entity.getCreateBy());
        draft.setUpdateBy(entity.getUpdateBy());
        draft.setCreateTime(entity.getCreateTime());
        draft.setUpdateTime(entity.getUpdateTime());
        return draft;
    }

    /** 列表页草稿覆盖：仅覆盖业务字段 */
    private DailyVocabularyDto toDtoWithDraftOverlay(DailyVocabulary entity) {
        DailyVocabularyDto dto = entityToBaseDto(entity);
        if (isDraftOrReviewed(entity)) {
            DailyVocabularyDto draft = JsonUtils.fromJson(entity.getDraftContent(), DailyVocabularyDto.class);
            if (draft != null) {
                if (draft.getPhrase() != null) dto.setPhrase(draft.getPhrase());
                if (draft.getPhraseType() != null) dto.setPhraseType(draft.getPhraseType());
                if (draft.getPinyin() != null) dto.setPinyin(draft.getPinyin());
                if (draft.getDisplayDate() != null) dto.setDisplayDate(draft.getDisplayDate());
                if (draft.getSortOrder() != null) dto.setSortOrder(draft.getSortOrder());
            }
        }
        return dto;
    }

    /** 从实表字段构建完整 DTO（已发布详情） */
    private DailyVocabularyDto buildDtoFromEntity(DailyVocabulary entity) {
        DailyVocabularyDto dto = entityToBaseDto(entity);
        // 加载例句
        if (entity.getExampleSentenceId() != null) {
            try {
                ExampleSentenceDto esDto = exampleSentenceService.findById(entity.getExampleSentenceId());
                dto.setExampleSentence(esDto);
            } catch (Exception e) {
                log.warn("例句加载失败, exampleSentenceId={}", entity.getExampleSentenceId(), e);
            }
        }
        return dto;
    }

    /** 实体→DTO 基础字段映射 */
    private DailyVocabularyDto entityToBaseDto(DailyVocabulary entity) {
        DailyVocabularyDto dto = new DailyVocabularyDto();
        dto.setId(entity.getId());
        dto.setPhrase(entity.getPhrase());
        dto.setPhraseType(entity.getPhraseType());
        dto.setPinyin(entity.getPinyin());
        dto.setPhraseTranslations(JsonUtils.parseTranslationList(entity.getPhraseTranslations()));
        dto.setAudioId(entity.getAudioId());
        dto.setImageId(entity.getImageId());
        dto.setPlainExplanation(entity.getPlainExplanation());
        dto.setExplanationTranslations(JsonUtils.parseTranslationList(entity.getExplanationTranslations()));
        dto.setOriginStory(entity.getOriginStory());
        dto.setExampleSentenceId(entity.getExampleSentenceId());
        dto.setDisplayDate(entity.getDisplayDate());
        dto.setSortOrder(entity.getSortOrder());
        dto.setRelatedWordId(entity.getRelatedWordId());
        dto.setStatus(entity.getStatus());
        dto.setPublishStatus(entity.getPublishStatus());
        dto.setEditStatus(entity.getEditStatus());
        dto.setCreateBy(entity.getCreateBy());
        dto.setUpdateBy(entity.getUpdateBy());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }

    /** 计算到次日 00:00 的秒数 */
    private long secondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.plusDays(1).truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        return ChronoUnit.SECONDS.between(now, midnight);
    }

    /** 清除今日缓存 */
    private void evictTodayCache() {
        redisUtils.del(CACHE_KEY_TODAY_MAIN, CACHE_KEY_TODAY_BACKUPS);
    }
}
