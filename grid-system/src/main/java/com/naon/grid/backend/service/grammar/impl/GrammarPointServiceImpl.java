package com.naon.grid.backend.service.grammar.impl;

import com.naon.grid.backend.domain.grammar.GrammarError;
import com.naon.grid.backend.domain.grammar.GrammarMeaning;
import com.naon.grid.backend.domain.grammar.GrammarNotice;
import com.naon.grid.backend.domain.grammar.GrammarPoint;
import com.naon.grid.backend.domain.grammar.GrammarStructure;
import com.naon.grid.backend.repo.grammar.GrammarErrorRepository;
import com.naon.grid.backend.repo.grammar.GrammarMeaningRepository;
import com.naon.grid.backend.repo.grammar.GrammarNoticeRepository;
import com.naon.grid.backend.repo.grammar.GrammarPointRepository;
import com.naon.grid.backend.repo.grammar.GrammarStructureRepository;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammar.GrammarQuestionService;
import com.naon.grid.backend.service.grammar.dto.GrammarErrorDto;
import com.naon.grid.backend.service.grammar.dto.GrammarMeaningDto;
import com.naon.grid.backend.service.grammar.dto.GrammarNoticeDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointQueryCriteria;
import com.naon.grid.backend.service.grammar.dto.GrammarStructureDto;
import com.naon.grid.backend.service.grammar.mapstruct.GrammarPointMapper;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GrammarPointServiceImpl implements GrammarPointService {

    private final GrammarPointRepository grammarPointRepository;
    private final GrammarMeaningRepository grammarMeaningRepository;
    private final GrammarStructureRepository grammarStructureRepository;
    private final GrammarNoticeRepository grammarNoticeRepository;
    private final GrammarErrorRepository grammarErrorRepository;
    private final GrammarPointMapper grammarPointMapper;
    private final ExampleSentenceService exampleSentenceService;
    private final GrammarQuestionService grammarQuestionService;

    @Override
    public PageResult<GrammarPointDto> queryAll(GrammarPointQueryCriteria criteria, Pageable pageable) {
        Page<GrammarPoint> page = grammarPointRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return criteriaBuilder.and(basePredicate, statusPredicate);
        }, pageable);
        PageResult<GrammarPointDto> pageResult = PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
        populateGrammarListStats(pageResult.getContent());
        return pageResult;
    }

    private void populateGrammarListStats(List<GrammarPointDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<Long> ids = dtos.stream().map(GrammarPointDto::getId).collect(Collectors.toList());

        Map<Long, Long> meaningCountMap = grammarMeaningRepository
                .countByGrammarIdInGroupByGrammarId(ids, StatusEnum.ENABLED.getCode())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Map<Long, Long> structureCountMap = grammarStructureRepository
                .countByGrammarIdInGroupByGrammarId(ids, StatusEnum.ENABLED.getCode())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Map<Long, Long> noticeCountMap = grammarNoticeRepository
                .countByGrammarIdInGroupByGrammarId(ids, StatusEnum.ENABLED.getCode())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Map<Long, Long> errorCountMap = grammarErrorRepository
                .countByGrammarIdInGroupByGrammarId(ids, StatusEnum.ENABLED.getCode())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Map<Long, List<Long>> questionIdsMap = grammarQuestionService.findByGrammarIds(ids);

        for (GrammarPointDto dto : dtos) {
            if (dto.getMeaningCount() == null)
                dto.setMeaningCount(meaningCountMap.getOrDefault(dto.getId(), 0L).intValue());
            if (dto.getStructureCount() == null)
                dto.setStructureCount(structureCountMap.getOrDefault(dto.getId(), 0L).intValue());
            if (dto.getNoticeCount() == null)
                dto.setNoticeCount(noticeCountMap.getOrDefault(dto.getId(), 0L).intValue());
            if (dto.getErrorCount() == null)
                dto.setErrorCount(errorCountMap.getOrDefault(dto.getId(), 0L).intValue());
            if (dto.getQuestionIds() == null) {
                dto.setQuestionIds(questionIdsMap.getOrDefault(dto.getId(), Collections.emptyList()));
            }
        }
    }

    private boolean isDraftOrReviewed(GrammarPointDto dto) {
        String editStatus = dto.getEditStatus();
        return EditStatusEnum.DRAFT.getCode().equals(editStatus)
            || EditStatusEnum.REVIEWED.getCode().equals(editStatus);
    }

    private GrammarPointDto toDtoWithDraftOverlay(GrammarPoint entity) {
        GrammarPointDto dto = grammarPointMapper.toDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    private void applyDraftOverlay(GrammarPointDto dto, String draftJson) {
        if (draftJson == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        GrammarPointDto draft;
        try {
            draft = JsonUtils.fromJson(draftJson, GrammarPointDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draft == null) {
            throw new BadRequestException("草稿内容不存在");
        }

        if (draft.getName() != null)         dto.setName(draft.getName());
        if (draft.getHskLevel() != null)     dto.setHskLevel(draft.getHskLevel());
        if (draft.getProject() != null)      dto.setProject(draft.getProject());
        if (draft.getCategory() != null)     dto.setCategory(draft.getCategory());
        if (draft.getSubCategory() != null)  dto.setSubCategory(draft.getSubCategory());

        // 从草稿计算列表统计字段
        if (draft.getMeanings() != null) {
            dto.setMeaningCount(draft.getMeanings().size());
        }
        if (draft.getStructures() != null) {
            dto.setStructureCount(draft.getStructures().size());
        }
        if (draft.getNotices() != null) {
            dto.setNoticeCount(draft.getNotices().size());
        }
        if (draft.getErrors() != null) {
            dto.setErrorCount(draft.getErrors().size());
        }
        if (draft.getQuestionIds() != null) {
            dto.setQuestionIds(draft.getQuestionIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrammarPointDto findById(Long id) {
        if (id == null) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }
        GrammarPoint grammarPoint = grammarPointRepository.findById(id).orElseGet(GrammarPoint::new);
        if (grammarPoint.getId() == null || StatusEnum.DISABLED.getCode().equals(grammarPoint.getStatus())) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }

        if (EditStatusEnum.DRAFT.getCode().equals(grammarPoint.getEditStatus()) ||
            EditStatusEnum.REVIEWED.getCode().equals(grammarPoint.getEditStatus())) {
            if (grammarPoint.getDraftContent() == null) {
                throw new BadRequestException("Draft content not found");
            }
            GrammarPointDto dto = JsonUtils.fromJson(grammarPoint.getDraftContent(), GrammarPointDto.class);
            dto.setId(grammarPoint.getId());
            dto.setStatus(grammarPoint.getStatus());
            dto.setPublishStatus(grammarPoint.getPublishStatus());
            dto.setEditStatus(grammarPoint.getEditStatus());
            dto.setCreateBy(grammarPoint.getCreateBy());
            dto.setUpdateBy(grammarPoint.getUpdateBy());
            dto.setCreateTime(grammarPoint.getCreateTime());
            dto.setUpdateTime(grammarPoint.getUpdateTime());
            dto.setMeanings(sortMeaningsDesc(dto.getMeanings()));
            dto.setStructures(sortStructuresDesc(dto.getStructures()));
            dto.setNotices(sortNoticesDesc(dto.getNotices()));
            dto.setErrors(sortErrorsDesc(dto.getErrors()));
            return dto;
        }

        return toPublishedDetailDto(grammarPoint);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrammarPointDto findPublishedById(Long id) {
        if (id == null) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }
        GrammarPoint grammarPoint = grammarPointRepository.findById(id).orElseGet(GrammarPoint::new);
        if (grammarPoint.getId() == null || StatusEnum.DISABLED.getCode().equals(grammarPoint.getStatus())) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(grammarPoint.getPublishStatus())) {
            throw new BadRequestException("语法点尚未发布");
        }
        return toPublishedDetailDto(grammarPoint);
    }

    @Override
    public PageResult<GrammarPointDto> searchPublished(String keyword, Pageable pageable) {
        Page<GrammarPoint> page = grammarPointRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode()));
            predicates.add(criteriaBuilder.equal(root.get("publishStatus"), PublishStatusEnum.PUBLISHED.getCode()));
            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + keyword.trim() + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        PageResult<GrammarPointDto> pageResult = PageUtil.toPage(page.map(grammarPointMapper::toDto));
        return pageResult;
    }

    private GrammarPointDto toPublishedDetailDto(GrammarPoint grammarPoint) {
        Long id = grammarPoint.getId();
        GrammarPointDto dto = grammarPointMapper.toDto(grammarPoint);
        dto.setMeanings(sortMeaningsDesc(hydrateMeaningSentences(convertToMeaningDtos(
                grammarMeaningRepository.findByGrammarIdAndStatus(id, StatusEnum.ENABLED.getCode())))));
        dto.setStructures(sortStructuresDesc(hydrateStructureSentences(convertToStructureDtos(
                grammarStructureRepository.findByGrammarIdAndStatus(id, StatusEnum.ENABLED.getCode())))));
        dto.setNotices(sortNoticesDesc(hydrateNoticeSentences(convertToNoticeDtos(
                grammarNoticeRepository.findByGrammarIdAndStatus(id, StatusEnum.ENABLED.getCode())))));
        dto.setErrors(sortErrorsDesc(convertToErrorDtos(
                grammarErrorRepository.findByGrammarIdAndStatus(id, StatusEnum.ENABLED.getCode()))));
        dto.setQuestionIds(grammarQuestionService.findByGrammarId(id));
        return dto;
    }

    // ===== Example sentence hydration methods =====

    private List<GrammarMeaningDto> hydrateMeaningSentences(List<GrammarMeaningDto> meanings) {
        if (meanings == null || meanings.isEmpty()) return meanings;
        List<Long> allSentenceIds = new ArrayList<>();
        for (GrammarMeaningDto m : meanings) {
            if (m.getSentences() != null) {
                m.getSentences().forEach(s -> { if (s.getId() != null) allSentenceIds.add(s.getId()); });
            }
        }
        if (allSentenceIds.isEmpty()) return meanings;
        Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByIds(allSentenceIds);
        for (GrammarMeaningDto m : meanings) {
            if (m.getSentences() != null) {
                for (ExampleSentenceDto s : m.getSentences()) {
                    if (s.getId() != null && sentenceMap.containsKey(s.getId())) {
                        ExampleSentenceDto hydrated = sentenceMap.get(s.getId());
                        s.setSentence(hydrated.getSentence());
                        s.setPinyin(hydrated.getPinyin());
                        s.setAudioId(hydrated.getAudioId());
                        s.setTranslations(hydrated.getTranslations());
                        s.setImageId(hydrated.getImageId());
                        s.setOrder(hydrated.getOrder());
                    }
                }
            }
        }
        return meanings;
    }

    private List<GrammarStructureDto> hydrateStructureSentences(List<GrammarStructureDto> structures) {
        if (structures == null || structures.isEmpty()) return structures;
        List<Long> allSentenceIds = new ArrayList<>();
        for (GrammarStructureDto s : structures) {
            if (s.getSentences() != null) {
                s.getSentences().forEach(se -> { if (se.getId() != null) allSentenceIds.add(se.getId()); });
            }
        }
        if (allSentenceIds.isEmpty()) return structures;
        Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByIds(allSentenceIds);
        for (GrammarStructureDto s : structures) {
            if (s.getSentences() != null) {
                for (ExampleSentenceDto se : s.getSentences()) {
                    if (se.getId() != null && sentenceMap.containsKey(se.getId())) {
                        ExampleSentenceDto hydrated = sentenceMap.get(se.getId());
                        se.setSentence(hydrated.getSentence());
                        se.setPinyin(hydrated.getPinyin());
                        se.setAudioId(hydrated.getAudioId());
                        se.setTranslations(hydrated.getTranslations());
                        se.setImageId(hydrated.getImageId());
                        se.setOrder(hydrated.getOrder());
                    }
                }
            }
        }
        return structures;
    }

    private List<GrammarNoticeDto> hydrateNoticeSentences(List<GrammarNoticeDto> notices) {
        if (notices == null || notices.isEmpty()) return notices;
        List<Long> allSentenceIds = new ArrayList<>();
        for (GrammarNoticeDto n : notices) {
            if (n.getSentences() != null) {
                n.getSentences().forEach(s -> { if (s.getId() != null) allSentenceIds.add(s.getId()); });
            }
        }
        if (allSentenceIds.isEmpty()) return notices;
        Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByIds(allSentenceIds);
        for (GrammarNoticeDto n : notices) {
            if (n.getSentences() != null) {
                for (ExampleSentenceDto s : n.getSentences()) {
                    if (s.getId() != null && sentenceMap.containsKey(s.getId())) {
                        ExampleSentenceDto hydrated = sentenceMap.get(s.getId());
                        s.setSentence(hydrated.getSentence());
                        s.setPinyin(hydrated.getPinyin());
                        s.setAudioId(hydrated.getAudioId());
                        s.setTranslations(hydrated.getTranslations());
                        s.setImageId(hydrated.getImageId());
                        s.setOrder(hydrated.getOrder());
                    }
                }
            }
        }
        return notices;
    }

    // ===== Create =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(GrammarPointDto resources) {
        GrammarPoint grammarPoint = new GrammarPoint();
        grammarPoint.setStatus(StatusEnum.ENABLED.getCode());
        grammarPoint.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        grammarPoint.setEditStatus(EditStatusEnum.DRAFT.getCode());
        grammarPoint.setName(resources.getName());
        grammarPoint.setDraftContent(JsonUtils.toJson(resources));
        grammarPoint = grammarPointRepository.save(grammarPoint);
        return grammarPoint.getId();
    }

    // ===== Update =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GrammarPointDto resources) {
        GrammarPoint grammarPoint = grammarPointRepository.findById(id).orElseGet(GrammarPoint::new);
        if (grammarPoint.getId() == null || StatusEnum.DISABLED.getCode().equals(grammarPoint.getStatus())) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }

        if (EditStatusEnum.REVIEWED.getCode().equals(grammarPoint.getEditStatus()) ||
            EditStatusEnum.PUBLISHED.getCode().equals(grammarPoint.getEditStatus())) {
            grammarPoint.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }

        grammarPoint.setDraftContent(JsonUtils.toJson(resources));
        grammarPointRepository.save(grammarPoint);
    }

    // ===== Delete =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        GrammarPoint grammarPoint = grammarPointRepository.findById(id).orElseGet(GrammarPoint::new);
        if (grammarPoint.getId() == null) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }
        grammarPoint.setStatus(StatusEnum.DISABLED.getCode());
        grammarPointRepository.save(grammarPoint);
    }

    // ===== Review =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Long id) {
        GrammarPoint grammarPoint = grammarPointRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id)));

        if (StatusEnum.DISABLED.getCode().equals(grammarPoint.getStatus())) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }

        if (grammarPoint.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }

        if (!EditStatusEnum.DRAFT.getCode().equals(grammarPoint.getEditStatus())) {
            throw new BadRequestException("仅草稿状态可审核");
        }

        grammarPoint.setEditStatus(EditStatusEnum.REVIEWED.getCode());
        grammarPointRepository.save(grammarPoint);
    }

    // ===== Publish =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Long id) {
        GrammarPoint grammarPoint = grammarPointRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id)));

        if (StatusEnum.DISABLED.getCode().equals(grammarPoint.getStatus())) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }

        if (grammarPoint.getDraftContent() == null) {
            throw new BadRequestException("Draft content not found");
        }

        if (!EditStatusEnum.REVIEWED.getCode().equals(grammarPoint.getEditStatus())) {
            throw new BadRequestException("Only reviewed drafts can be published");
        }

        GrammarPointDto draftDto = JsonUtils.fromJson(grammarPoint.getDraftContent(), GrammarPointDto.class);

        // Update main table fields
        grammarPoint.setName(draftDto.getName());
        grammarPoint.setHskLevel(draftDto.getHskLevel());
        grammarPoint.setProject(draftDto.getProject());
        grammarPoint.setCategory(draftDto.getCategory());
        grammarPoint.setSubCategory(draftDto.getSubCategory());

        // Sync child tables
        syncMeanings(grammarPoint.getId(), draftDto.getMeanings());
        syncStructures(grammarPoint.getId(), draftDto.getStructures());
        syncNotices(grammarPoint.getId(), draftDto.getNotices());
        syncErrors(grammarPoint.getId(), draftDto.getErrors());
        syncQuestions(grammarPoint.getId(), draftDto.getQuestionIds());

        // Update status
        grammarPoint.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        grammarPoint.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        grammarPoint.setDraftContent(null);
        grammarPointRepository.save(grammarPoint);
    }

    // ===== Offline =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        GrammarPoint grammarPoint = grammarPointRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id)));

        if (StatusEnum.DISABLED.getCode().equals(grammarPoint.getStatus())) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }

        grammarPoint.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        grammarPointRepository.save(grammarPoint);
    }

    // ===== Sync methods for child tables =====

    private void syncMeanings(Long grammarId, List<GrammarMeaningDto> submittedDtos) {
        List<GrammarMeaningDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<GrammarMeaning> existing = grammarMeaningRepository.findByGrammarIdAndStatus(grammarId, StatusEnum.ENABLED.getCode());
        Map<Long, GrammarMeaning> existingMap = new HashMap<>();
        for (GrammarMeaning m : existing) {
            existingMap.put(m.getId(), m);
        }

        Set<Long> submittedIds = new HashSet<>();
        List<GrammarMeaning> toSave = new ArrayList<>();

        for (GrammarMeaningDto dto : submitted) {
            GrammarMeaning entity;
            if (dto.getId() == null || dto.getId() == 0) {
                entity = new GrammarMeaning();
                entity.setGrammarId(grammarId);
                entity.setStatus(StatusEnum.ENABLED.getCode());
            } else {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("语法意义ID重复: " + dto.getId());
                }
                GrammarMeaning existingEntity = existingMap.get(dto.getId());
                if (existingEntity == null) {
                    throw new BadRequestException("语法意义ID不属于当前语法点: " + dto.getId());
                }
                entity = existingEntity;
            }

            entity.setMeaningContent(dto.getMeaningContent());
            entity.setMeaningContentTranslations(JsonUtils.toTranslationJson(dto.getMeaningContentTranslations()));
            entity.setImageId(dto.getImageId());
            entity.setMeaningOrder(dto.getOrder() != null ? dto.getOrder() : 0);

            // Save sentences and collect IDs
            List<Long> sentenceIds = saveSentencesAndCollectIds(dto.getSentences());
            entity.setMeaningSentenceIds(JsonUtils.toJson(sentenceIds));

            toSave.add(entity);
        }

        // Soft delete removed items
        List<GrammarMeaning> toDelete = new ArrayList<>();
        for (GrammarMeaning m : existing) {
            if (!submittedIds.contains(m.getId())) {
                toDelete.add(m);
            }
        }
        for (GrammarMeaning m : toDelete) {
            m.setStatus(StatusEnum.DISABLED.getCode());
            grammarMeaningRepository.save(m);
        }
        disableChildSentences(toDelete, GrammarMeaning::getMeaningSentenceIds);

        grammarMeaningRepository.saveAll(toSave);
    }

    private void syncStructures(Long grammarId, List<GrammarStructureDto> submittedDtos) {
        List<GrammarStructureDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<GrammarStructure> existing = grammarStructureRepository.findByGrammarIdAndStatus(grammarId, StatusEnum.ENABLED.getCode());
        Map<Long, GrammarStructure> existingMap = new HashMap<>();
        for (GrammarStructure s : existing) {
            existingMap.put(s.getId(), s);
        }

        Set<Long> submittedIds = new HashSet<>();
        List<GrammarStructure> toSave = new ArrayList<>();

        for (GrammarStructureDto dto : submitted) {
            GrammarStructure entity;
            if (dto.getId() == null || dto.getId() == 0) {
                entity = new GrammarStructure();
                entity.setGrammarId(grammarId);
                entity.setStatus(StatusEnum.ENABLED.getCode());
            } else {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("语法结构ID重复: " + dto.getId());
                }
                GrammarStructure existingEntity = existingMap.get(dto.getId());
                if (existingEntity == null) {
                    throw new BadRequestException("语法结构ID不属于当前语法点: " + dto.getId());
                }
                entity = existingEntity;
            }

            entity.setStructureContent(dto.getStructureContent());
            entity.setStructureOrder(dto.getOrder() != null ? dto.getOrder() : 0);

            List<Long> sentenceIds = saveSentencesAndCollectIds(dto.getSentences());
            entity.setStructureSentenceIds(JsonUtils.toJson(sentenceIds));

            toSave.add(entity);
        }

        List<GrammarStructure> toDelete = new ArrayList<>();
        for (GrammarStructure s : existing) {
            if (!submittedIds.contains(s.getId())) {
                toDelete.add(s);
            }
        }
        for (GrammarStructure s : toDelete) {
            s.setStatus(StatusEnum.DISABLED.getCode());
            grammarStructureRepository.save(s);
        }
        disableChildSentences(toDelete, GrammarStructure::getStructureSentenceIds);

        grammarStructureRepository.saveAll(toSave);
    }

    private void syncNotices(Long grammarId, List<GrammarNoticeDto> submittedDtos) {
        List<GrammarNoticeDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<GrammarNotice> existing = grammarNoticeRepository.findByGrammarIdAndStatus(grammarId, StatusEnum.ENABLED.getCode());
        Map<Long, GrammarNotice> existingMap = new HashMap<>();
        for (GrammarNotice n : existing) {
            existingMap.put(n.getId(), n);
        }

        Set<Long> submittedIds = new HashSet<>();
        List<GrammarNotice> toSave = new ArrayList<>();

        for (GrammarNoticeDto dto : submitted) {
            GrammarNotice entity;
            if (dto.getId() == null || dto.getId() == 0) {
                entity = new GrammarNotice();
                entity.setGrammarId(grammarId);
                entity.setStatus(StatusEnum.ENABLED.getCode());
            } else {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("语法注意ID重复: " + dto.getId());
                }
                GrammarNotice existingEntity = existingMap.get(dto.getId());
                if (existingEntity == null) {
                    throw new BadRequestException("语法注意ID不属于当前语法点: " + dto.getId());
                }
                entity = existingEntity;
            }

            entity.setNoticeContent(dto.getNoticeContent());
            entity.setNoticeContentTranslations(JsonUtils.toTranslationJson(dto.getNoticeContentTranslations()));
            entity.setNoticeOrder(dto.getOrder() != null ? dto.getOrder() : 0);

            List<Long> sentenceIds = saveSentencesAndCollectIds(dto.getSentences());
            entity.setNoticeSentenceIds(JsonUtils.toJson(sentenceIds));

            toSave.add(entity);
        }

        List<GrammarNotice> toDelete = new ArrayList<>();
        for (GrammarNotice n : existing) {
            if (!submittedIds.contains(n.getId())) {
                toDelete.add(n);
            }
        }
        for (GrammarNotice n : toDelete) {
            n.setStatus(StatusEnum.DISABLED.getCode());
            grammarNoticeRepository.save(n);
        }
        disableChildSentences(toDelete, GrammarNotice::getNoticeSentenceIds);

        grammarNoticeRepository.saveAll(toSave);
    }

    private void syncErrors(Long grammarId, List<GrammarErrorDto> submittedDtos) {
        List<GrammarErrorDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<GrammarError> existing = grammarErrorRepository.findByGrammarIdAndStatus(grammarId, StatusEnum.ENABLED.getCode());
        Map<Long, GrammarError> existingMap = new HashMap<>();
        for (GrammarError e : existing) {
            existingMap.put(e.getId(), e);
        }

        Set<Long> submittedIds = new HashSet<>();
        List<GrammarError> toSave = new ArrayList<>();

        for (GrammarErrorDto dto : submitted) {
            GrammarError entity;
            if (dto.getId() == null || dto.getId() == 0) {
                entity = new GrammarError();
                entity.setGrammarId(grammarId);
                entity.setStatus(StatusEnum.ENABLED.getCode());
            } else {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("语法偏误ID重复: " + dto.getId());
                }
                GrammarError existingEntity = existingMap.get(dto.getId());
                if (existingEntity == null) {
                    throw new BadRequestException("语法偏误ID不属于当前语法点: " + dto.getId());
                }
                entity = existingEntity;
            }

            entity.setErrorContent(dto.getErrorContent());
            entity.setErrorAnalysis(dto.getErrorAnalysis());
            entity.setErrorAnalysisTranslations(JsonUtils.toTranslationJson(dto.getErrorAnalysisTranslations()));
            entity.setErrorOrder(dto.getOrder() != null ? dto.getOrder() : 0);

            toSave.add(entity);
        }

        List<GrammarError> toDelete = new ArrayList<>();
        for (GrammarError e : existing) {
            if (!submittedIds.contains(e.getId())) {
                toDelete.add(e);
            }
        }
        for (GrammarError e : toDelete) {
            e.setStatus(StatusEnum.DISABLED.getCode());
            grammarErrorRepository.save(e);
        }

        grammarErrorRepository.saveAll(toSave);
    }

    private void syncQuestions(Long grammarId, List<Long> questionIds) {
        grammarQuestionService.syncFromDraft(grammarId, questionIds);
    }

    // ===== Example sentence helpers =====

    private List<Long> saveSentencesAndCollectIds(List<ExampleSentenceDto> sentences) {
        if (sentences == null || sentences.isEmpty()) return Collections.emptyList();
        List<Long> ids = new ArrayList<>();
        for (ExampleSentenceDto s : sentences) {
            if (s != null && s.getSentence() != null && !s.getSentence().trim().isEmpty()) {
                ExampleSentenceDto saved = exampleSentenceService.save(s);
                if (saved != null && saved.getId() != null) {
                    ids.add(saved.getId());
                }
            }
        }
        return ids;
    }

    @FunctionalInterface
    private interface SentenceIdsGetter<T> {
        String getSentenceIds(T entity);
    }

    private <T> void disableChildSentences(List<T> entities, SentenceIdsGetter<T> idsGetter) {
        if (entities == null || entities.isEmpty()) return;
        List<Long> allIds = new ArrayList<>();
        for (T entity : entities) {
            String idsJson = idsGetter.getSentenceIds(entity);
            if (idsJson != null) {
                try {
                    List<Long> ids = JSON.parseArray(idsJson, Long.class);
                    if (ids != null) allIds.addAll(ids);
                } catch (Exception ignored) {}
            }
        }
        if (!allIds.isEmpty()) {
            exampleSentenceService.disableByIds(allIds);
        }
    }

    // ===== Entity ↔ DTO conversion =====

    private List<GrammarMeaningDto> convertToMeaningDtos(List<GrammarMeaning> entities) {
        if (entities == null) return Collections.emptyList();
        List<GrammarMeaningDto> dtos = new ArrayList<>();
        for (GrammarMeaning entity : entities) {
            GrammarMeaningDto dto = new GrammarMeaningDto();
            dto.setId(entity.getId());
            dto.setGrammarId(entity.getGrammarId());
            dto.setMeaningContent(entity.getMeaningContent());
            dto.setMeaningContentTranslations(JsonUtils.parseTranslationList(entity.getMeaningContentTranslations()));
            dto.setImageId(entity.getImageId());
            dto.setOrder(entity.getMeaningOrder());
            dto.setCreateTime(entity.getCreateTime());
            dto.setUpdateTime(entity.getUpdateTime());
            dto.setStatus(entity.getStatus());

            // Parse sentence IDs → ExampleSentenceDto stubs
            String idsJson = entity.getMeaningSentenceIds();
            if (idsJson != null) {
                try {
                    List<Long> ids = JSON.parseArray(idsJson, Long.class);
                    if (ids != null) {
                        dto.setSentences(ids.stream().map(id -> {
                            ExampleSentenceDto s = new ExampleSentenceDto();
                            s.setId(id);
                            return s;
                        }).collect(Collectors.toList()));
                    }
                } catch (Exception ignored) {}
            }

            dtos.add(dto);
        }
        return dtos;
    }

    private List<GrammarStructureDto> convertToStructureDtos(List<GrammarStructure> entities) {
        if (entities == null) return Collections.emptyList();
        List<GrammarStructureDto> dtos = new ArrayList<>();
        for (GrammarStructure entity : entities) {
            GrammarStructureDto dto = new GrammarStructureDto();
            dto.setId(entity.getId());
            dto.setGrammarId(entity.getGrammarId());
            dto.setStructureContent(entity.getStructureContent());
            dto.setOrder(entity.getStructureOrder());
            dto.setCreateTime(entity.getCreateTime());
            dto.setUpdateTime(entity.getUpdateTime());
            dto.setStatus(entity.getStatus());

            String idsJson = entity.getStructureSentenceIds();
            if (idsJson != null) {
                try {
                    List<Long> ids = JSON.parseArray(idsJson, Long.class);
                    if (ids != null) {
                        dto.setSentences(ids.stream().map(id -> {
                            ExampleSentenceDto s = new ExampleSentenceDto();
                            s.setId(id);
                            return s;
                        }).collect(Collectors.toList()));
                    }
                } catch (Exception ignored) {}
            }

            dtos.add(dto);
        }
        return dtos;
    }

    private List<GrammarNoticeDto> convertToNoticeDtos(List<GrammarNotice> entities) {
        if (entities == null) return Collections.emptyList();
        List<GrammarNoticeDto> dtos = new ArrayList<>();
        for (GrammarNotice entity : entities) {
            GrammarNoticeDto dto = new GrammarNoticeDto();
            dto.setId(entity.getId());
            dto.setGrammarId(entity.getGrammarId());
            dto.setNoticeContent(entity.getNoticeContent());
            dto.setNoticeContentTranslations(JsonUtils.parseTranslationList(entity.getNoticeContentTranslations()));
            dto.setOrder(entity.getNoticeOrder());
            dto.setCreateTime(entity.getCreateTime());
            dto.setUpdateTime(entity.getUpdateTime());
            dto.setStatus(entity.getStatus());

            String idsJson = entity.getNoticeSentenceIds();
            if (idsJson != null) {
                try {
                    List<Long> ids = JSON.parseArray(idsJson, Long.class);
                    if (ids != null) {
                        dto.setSentences(ids.stream().map(id -> {
                            ExampleSentenceDto s = new ExampleSentenceDto();
                            s.setId(id);
                            return s;
                        }).collect(Collectors.toList()));
                    }
                } catch (Exception ignored) {}
            }

            dtos.add(dto);
        }
        return dtos;
    }

    private List<GrammarErrorDto> convertToErrorDtos(List<GrammarError> entities) {
        if (entities == null) return Collections.emptyList();
        List<GrammarErrorDto> dtos = new ArrayList<>();
        for (GrammarError entity : entities) {
            GrammarErrorDto dto = new GrammarErrorDto();
            dto.setId(entity.getId());
            dto.setGrammarId(entity.getGrammarId());
            dto.setErrorContent(entity.getErrorContent());
            dto.setErrorAnalysis(entity.getErrorAnalysis());
            dto.setErrorAnalysisTranslations(JsonUtils.parseTranslationList(entity.getErrorAnalysisTranslations()));
            dto.setOrder(entity.getErrorOrder());
            dto.setCreateTime(entity.getCreateTime());
            dto.setUpdateTime(entity.getUpdateTime());
            dto.setStatus(entity.getStatus());
            dtos.add(dto);
        }
        return dtos;
    }

    // ===== Sort helpers =====

    private List<GrammarMeaningDto> sortMeaningsDesc(List<GrammarMeaningDto> list) {
        if (list == null || list.isEmpty()) return list;
        list.sort(Comparator.comparing(GrammarMeaningDto::getOrder, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    private List<GrammarStructureDto> sortStructuresDesc(List<GrammarStructureDto> list) {
        if (list == null || list.isEmpty()) return list;
        list.sort(Comparator.comparing(GrammarStructureDto::getOrder, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    private List<GrammarNoticeDto> sortNoticesDesc(List<GrammarNoticeDto> list) {
        if (list == null || list.isEmpty()) return list;
        list.sort(Comparator.comparing(GrammarNoticeDto::getOrder, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    private List<GrammarErrorDto> sortErrorsDesc(List<GrammarErrorDto> list) {
        if (list == null || list.isEmpty()) return list;
        list.sort(Comparator.comparing(GrammarErrorDto::getOrder, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }
}
