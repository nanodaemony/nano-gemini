package com.naon.grid.backend.service.culture.impl;

import com.naon.grid.backend.domain.culture.Culture;
import com.naon.grid.backend.domain.culture.CultureKeyword;
import com.naon.grid.backend.repo.culture.CultureKeywordRepository;
import com.naon.grid.backend.repo.culture.CultureRepository;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.culture.CultureService;
import com.naon.grid.backend.service.culture.dto.CultureDto;
import com.naon.grid.backend.service.culture.dto.CultureKeywordDto;
import com.naon.grid.backend.service.culture.dto.CultureQueryCriteria;
import com.naon.grid.backend.service.culture.mapstruct.CultureMapper;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CultureServiceImpl implements CultureService {

    private final CultureRepository cultureRepository;
    private final CultureKeywordRepository cultureKeywordRepository;
    private final CultureMapper cultureMapper;
    private final ExampleSentenceService exampleSentenceService;

    @Override
    public PageResult<CultureDto> queryAll(CultureQueryCriteria criteria, Pageable pageable) {
        Page<Culture> page = cultureRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return criteriaBuilder.and(basePredicate, statusPredicate);
        }, pageable);
        PageResult<CultureDto> pageResult = PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
        populateCultureListStats(pageResult.getContent());
        return pageResult;
    }

    private CultureDto toDtoWithDraftOverlay(Culture entity) {
        CultureDto dto = cultureMapper.toDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    private void applyDraftOverlay(CultureDto dto, String draftJson) {
        if (draftJson == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        CultureDto draft;
        try {
            draft = JsonUtils.fromJson(draftJson, CultureDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draft == null) {
            throw new BadRequestException("草稿内容不存在");
        }

        if (draft.getName() != null)             dto.setName(draft.getName());
        if (draft.getPinyin() != null)           dto.setPinyin(draft.getPinyin());
        if (draft.getLevel() != null)            dto.setLevel(draft.getLevel());
        if (draft.getProject() != null)          dto.setProject(draft.getProject());
        if (draft.getCategory() != null)         dto.setCategory(draft.getCategory());

        if (draft.getKeywords() != null) {
            dto.setKeywordCount(draft.getKeywords().size());
        }
        if (draft.getSentenceIds() != null) {
            dto.setSentenceCount(draft.getSentenceIds().size());
            dto.setSentenceIds(draft.getSentenceIds());
        }
        if (draft.getQuestionIds() != null) {
            dto.setQuestionCount(draft.getQuestionIds().size());
            dto.setQuestionIds(draft.getQuestionIds());
        }
    }

    private void populateCultureListStats(List<CultureDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<Long> ids = dtos.stream().map(CultureDto::getId).collect(Collectors.toList());

        Map<Long, Long> keywordCountMap = cultureKeywordRepository
                .countByCultureIdInGroupByCultureId(ids, StatusEnum.ENABLED.getCode())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        for (CultureDto dto : dtos) {
            if (dto.getKeywordCount() == null) {
                dto.setKeywordCount(keywordCountMap.getOrDefault(dto.getId(), 0L).intValue());
            }
            if (dto.getSentenceCount() == null && dto.getSentenceIds() != null) {
                dto.setSentenceCount(dto.getSentenceIds().size());
            }
            if (dto.getQuestionCount() == null && dto.getQuestionIds() != null) {
                dto.setQuestionCount(dto.getQuestionIds().size());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CultureDto findById(Long id) {
        if (id == null) {
            throw new EntityNotFoundException(Culture.class, "id", String.valueOf(id));
        }
        Culture culture = cultureRepository.findById(id).orElseGet(Culture::new);
        if (culture.getId() == null || StatusEnum.DISABLED.getCode().equals(culture.getStatus())) {
            throw new EntityNotFoundException(Culture.class, "id", String.valueOf(id));
        }

        if (EditStatusEnum.DRAFT.getCode().equals(culture.getEditStatus()) ||
            EditStatusEnum.REVIEWED.getCode().equals(culture.getEditStatus())) {
            if (culture.getDraftContent() == null) {
                throw new BadRequestException("Draft content not found");
            }
            CultureDto dto = JsonUtils.fromJson(culture.getDraftContent(), CultureDto.class);
            dto.setId(culture.getId());
            dto.setStatus(culture.getStatus());
            dto.setPublishStatus(culture.getPublishStatus());
            dto.setEditStatus(culture.getEditStatus());
            dto.setCreateBy(culture.getCreateBy());
            dto.setUpdateBy(culture.getUpdateBy());
            dto.setCreateTime(culture.getCreateTime());
            dto.setUpdateTime(culture.getUpdateTime());
            if (dto.getKeywords() != null) {
                dto.getKeywords().sort(Comparator.comparing(CultureKeywordDto::getOrder, Comparator.reverseOrder()));
            }
            return dto;
        }

        return toPublishedDetailDto(culture);
    }

    private CultureDto toPublishedDetailDto(Culture culture) {
        Long id = culture.getId();
        CultureDto dto = cultureMapper.toDto(culture);

        List<CultureKeyword> keywords = cultureKeywordRepository.findByCultureIdAndStatus(id, StatusEnum.ENABLED.getCode());
        dto.setKeywords(convertToKeywordDtos(keywords).stream()
                .sorted(Comparator.comparing(CultureKeywordDto::getOrder, Comparator.reverseOrder()))
                .collect(Collectors.toList()));

        if (dto.getSentenceIds() != null && !dto.getSentenceIds().isEmpty()) {
            Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByIds(dto.getSentenceIds());
            List<ExampleSentenceDto> sentences = new ArrayList<>();
            for (Long sid : dto.getSentenceIds()) {
                ExampleSentenceDto s = sentenceMap.get(sid);
                if (s != null) sentences.add(s);
            }
            dto.setSentences(sentences);
            dto.setSentenceCount(sentences.size());
        }

        if (dto.getQuestionIds() != null && !dto.getQuestionIds().isEmpty()) {
            dto.setQuestionCount(dto.getQuestionIds().size());
        }

        return dto;
    }

    private List<CultureKeywordDto> convertToKeywordDtos(List<CultureKeyword> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(e -> {
            CultureKeywordDto dto = new CultureKeywordDto();
            dto.setId(e.getId());
            dto.setCultureId(e.getCultureId());
            dto.setKeyword(e.getKeyword());
            dto.setKeywordDescription(e.getKeywordDescription());
            dto.setKeywordTranslations(e.getKeywordTranslations());
            dto.setKeywordDescriptionTranslations(e.getKeywordDescriptionTranslations());
            dto.setAudioId(e.getAudioId());
            dto.setImageId(e.getImageId());
            dto.setOrder(e.getOrder());
            dto.setCreateTime(e.getCreateTime());
            dto.setUpdateTime(e.getUpdateTime());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CultureDto findPublishedById(Long id) {
        if (id == null) {
            throw new EntityNotFoundException(Culture.class, "id", String.valueOf(id));
        }
        Culture culture = cultureRepository.findById(id).orElseGet(Culture::new);
        if (culture.getId() == null || StatusEnum.DISABLED.getCode().equals(culture.getStatus())) {
            throw new EntityNotFoundException(Culture.class, "id", String.valueOf(id));
        }
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(culture.getPublishStatus())) {
            throw new BadRequestException("文化点尚未发布");
        }
        return toPublishedDetailDto(culture);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CultureDto dto) {
        Culture culture = new Culture();
        culture.setName(dto.getName());
        culture.setDraftContent(JsonUtils.toJson(dto));
        culture.setStatus(StatusEnum.ENABLED.getCode());
        culture.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        culture.setEditStatus(EditStatusEnum.DRAFT.getCode());
        culture = cultureRepository.save(culture);
        return culture.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CultureDto dto) {
        Culture culture = cultureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Culture.class, "id", String.valueOf(id)));

        if (StatusEnum.DISABLED.getCode().equals(culture.getStatus())) {
            throw new BadRequestException("已删除的文化点不能修改");
        }

        if (dto.getName() != null) {
            culture.setName(dto.getName());
        }
        culture.setDraftContent(JsonUtils.toJson(dto));

        if (EditStatusEnum.REVIEWED.getCode().equals(culture.getEditStatus()) ||
            EditStatusEnum.PUBLISHED.getCode().equals(culture.getEditStatus())) {
            culture.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }

        cultureRepository.save(culture);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Long id) {
        Culture culture = cultureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Culture.class, "id", String.valueOf(id)));
        if (!EditStatusEnum.DRAFT.getCode().equals(culture.getEditStatus())) {
            throw new BadRequestException("只有草稿状态的文化点可以审核");
        }
        if (culture.getDraftContent() == null) {
            throw new BadRequestException("草稿内容为空，无法审核");
        }
        culture.setEditStatus(EditStatusEnum.REVIEWED.getCode());
        cultureRepository.save(culture);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Long id) {
        Culture culture = cultureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Culture.class, "id", String.valueOf(id)));
        if (!EditStatusEnum.REVIEWED.getCode().equals(culture.getEditStatus())) {
            throw new BadRequestException("只有已审核状态的文化点可以发布");
        }
        if (culture.getDraftContent() == null) {
            throw new BadRequestException("草稿内容为空，无法发布");
        }

        CultureDto draft = JsonUtils.fromJson(culture.getDraftContent(), CultureDto.class);

        // 回写主表字段
        culture.setName(draft.getName());
        culture.setPinyin(draft.getPinyin());
        culture.setAudioId(draft.getAudioId());
        culture.setTranslations(draft.getTranslations());
        culture.setCoverImageId(draft.getCoverImageId());
        culture.setLevel(draft.getLevel());
        culture.setProject(draft.getProject());
        culture.setCategory(draft.getCategory());
        culture.setOneSentenceIntro(draft.getOneSentenceIntro());
        culture.setOneSentenceIntroTranslations(draft.getOneSentenceIntroTranslations());
        culture.setOneSentenceIntroAudioId(draft.getOneSentenceIntroAudioId());
        culture.setOneSentenceIntroImageId(draft.getOneSentenceIntroImageId());
        culture.setDetailedIntro(draft.getDetailedIntro());
        culture.setDetailedIntroTranslations(draft.getDetailedIntroTranslations());
        culture.setDetailedIntroAudioId(draft.getDetailedIntroAudioId());
        culture.setDetailedIntroImageId(draft.getDetailedIntroImageId());
        culture.setSentenceIds(draft.getSentenceIds() != null ? JsonUtils.toJson(draft.getSentenceIds()) : null);
        culture.setQuestionIds(draft.getQuestionIds() != null ? JsonUtils.toJson(draft.getQuestionIds()) : null);

        // 同步子表 culture_keyword
        syncKeywords(id, draft.getKeywords());

        culture.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        culture.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        culture.setDraftContent(null);
        cultureRepository.save(culture);
    }

    private void syncKeywords(Long cultureId, List<CultureKeywordDto> draftKeywords) {
        List<CultureKeyword> existingKeywords = cultureKeywordRepository
                .findByCultureIdAndStatus(cultureId, StatusEnum.ENABLED.getCode());
        Map<Long, CultureKeyword> existingMap = existingKeywords.stream()
                .collect(Collectors.toMap(CultureKeyword::getId, k -> k));
        Set<Long> submittedIds = new java.util.HashSet<>();

        if (draftKeywords != null) {
            for (CultureKeywordDto dto : draftKeywords) {
                if (dto.getId() != null && dto.getId() > 0 && existingMap.containsKey(dto.getId())) {
                    // Update existing
                    CultureKeyword existing = existingMap.get(dto.getId());
                    existing.setKeyword(dto.getKeyword());
                    existing.setKeywordDescription(dto.getKeywordDescription());
                    existing.setKeywordTranslations(dto.getKeywordTranslations());
                    existing.setKeywordDescriptionTranslations(dto.getKeywordDescriptionTranslations());
                    existing.setAudioId(dto.getAudioId());
                    existing.setImageId(dto.getImageId());
                    existing.setOrder(dto.getOrder() != null ? dto.getOrder() : 0);
                    cultureKeywordRepository.save(existing);
                    submittedIds.add(dto.getId());
                } else {
                    // Insert new
                    CultureKeyword newKeyword = new CultureKeyword();
                    newKeyword.setCultureId(cultureId);
                    newKeyword.setKeyword(dto.getKeyword());
                    newKeyword.setKeywordDescription(dto.getKeywordDescription());
                    newKeyword.setKeywordTranslations(dto.getKeywordTranslations());
                    newKeyword.setKeywordDescriptionTranslations(dto.getKeywordDescriptionTranslations());
                    newKeyword.setAudioId(dto.getAudioId());
                    newKeyword.setImageId(dto.getImageId());
                    newKeyword.setOrder(dto.getOrder() != null ? dto.getOrder() : 0);
                    newKeyword.setStatus(StatusEnum.ENABLED.getCode());
                    cultureKeywordRepository.save(newKeyword);
                }
            }
        }

        // Soft delete keywords not in draft
        for (CultureKeyword existing : existingKeywords) {
            if (!submittedIds.contains(existing.getId())) {
                cultureKeywordRepository.softDeleteById(existing.getId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        Culture culture = cultureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Culture.class, "id", String.valueOf(id)));
        culture.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        cultureRepository.save(culture);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Culture culture = cultureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Culture.class, "id", String.valueOf(id)));
        culture.setStatus(StatusEnum.DISABLED.getCode());
        cultureRepository.save(culture);
    }

    @Override
    public List<CultureDto> searchPublished(String blurry) {
        List<Culture> entities = cultureRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode()));
            predicates.add(criteriaBuilder.equal(root.get("publishStatus"), PublishStatusEnum.PUBLISHED.getCode()));
            if (blurry != null && !blurry.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + blurry.trim() + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        return entities.stream().map(cultureMapper::toDto).collect(Collectors.toList());
    }
}
