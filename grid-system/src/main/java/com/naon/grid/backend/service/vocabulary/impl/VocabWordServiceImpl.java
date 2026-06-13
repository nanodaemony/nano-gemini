package com.naon.grid.backend.service.vocabulary.impl;

import com.naon.grid.backend.domain.common.ExampleSentence;
import com.naon.grid.backend.domain.vocabulary.*;
import com.naon.grid.backend.repo.common.ExampleSentenceRepository;
import com.naon.grid.backend.repo.vocabulary.*;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.vocabulary.dto.*;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.SentenceBizTypeEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.enums.VocabRelationTypeEnum;
import lombok.RequiredArgsConstructor;
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.mapstruct.VocabWordMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabWordServiceImpl implements VocabWordService {

    private final VocabWordRepository vocabWordRepository;
    private final VocabSenseRepository vocabSenseRepository;
    private final VocabStructureRepository vocabStructureRepository;
    private final VocabRelationRepository vocabRelationRepository;
    private final VocabWordMapper vocabWordMapper;
    private final ExampleSentenceService exampleSentenceService;
    private final ExampleSentenceRepository exampleSentenceRepository;

    private static final String DEF_IMAGE_SENTENCE_BIZ = SentenceBizTypeEnum.VOCAB_SENSE_DEF_IMAGE_SENTENCE.getCode();
    private static final String STRUCTURE_SENTENCE_BIZ = SentenceBizTypeEnum.VOCAB_SENSE_STRUCTURE_SENTENCE.getCode();

    @Override
    public PageResult<VocabWordDto> queryAll(VocabWordQueryCriteria criteria, Pageable pageable) {
        Page<VocabWord> page = vocabWordRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return criteriaBuilder.and(basePredicate, statusPredicate);
        }, pageable);
        return PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
    }

    /**
     * 主表实体 → DTO；若处于 draft/reviewed，将 draftContent 中的业务字段覆盖回 DTO。
     * 仅供 {@link #queryAll} 调用。
     */
    private VocabWordDto toDtoWithDraftOverlay(VocabWord entity) {
        VocabWordDto dto = vocabWordMapper.toDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    /**
     * 把 draftContent JSON 中"列表页需要的业务字段"覆盖到 DTO 上。
     *
     * 覆盖范围应与 {@link com.naon.grid.backend.rest.vo.VocabWordBaseVO} 暴露的业务字段保持一致。
     * BaseVO 新增业务字段时，请同步在此方法添加覆盖。
     *
     * 永远不覆盖：id、status、publishStatus、editStatus、createBy、updateBy、
     * createTime、updateTime —— 这些字段以主表为准。
     *
     * 不读取：senses —— 列表页不返回子表。
     *
     * @throws BadRequestException 草稿数据缺失或解析失败
     */
    private void applyDraftOverlay(VocabWordDto dto, String draftJson) {
        if (draftJson == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        VocabWordDto draft;
        try {
            draft = JsonUtils.fromJson(draftJson, VocabWordDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draft == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        if (draft.getWord() != null)            dto.setWord(draft.getWord());
        if (draft.getWordTraditional() != null) dto.setWordTraditional(draft.getWordTraditional());
        if (draft.getPinyin() != null)          dto.setPinyin(draft.getPinyin());
        if (draft.getAudioId() != null)         dto.setAudioId(draft.getAudioId());
        if (draft.getHskLevel() != null)        dto.setHskLevel(draft.getHskLevel());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VocabWordDto findById(Integer id) {
        VocabWord vocabWord = vocabWordRepository.findById(id).orElseGet(VocabWord::new);
        if (vocabWord.getId() == null || StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
            throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
        }

        if (EditStatusEnum.DRAFT.getCode().equals(vocabWord.getEditStatus()) ||
            EditStatusEnum.REVIEWED.getCode().equals(vocabWord.getEditStatus())) {
            VocabWordDto dto = JsonUtils.fromJson(vocabWord.getDraftContent(), VocabWordDto.class);
            dto.setId(vocabWord.getId());
            dto.setStatus(vocabWord.getStatus());
            dto.setPublishStatus(vocabWord.getPublishStatus());
            dto.setEditStatus(vocabWord.getEditStatus());
            dto.setCreateTime(vocabWord.getCreateTime());
            dto.setUpdateTime(vocabWord.getUpdateTime());
            dto.setCreateBy(vocabWord.getCreateBy());
            dto.setUpdateBy(vocabWord.getUpdateBy());
            return dto;
        }

        VocabWordDto vocabWordDto = vocabWordMapper.toDto(vocabWord);

        List<VocabSenseDto> senseDtos = new ArrayList<>();
        List<VocabSense> senses = vocabSenseRepository.findByWordIdAndStatus(id, StatusEnum.ENABLED.getCode());
        for (VocabSense sense : senses) {
            VocabSenseDto senseDto = convertToSenseDto(sense);
            senseDtos.add(senseDto);
        }
        vocabWordDto.setSenses(senseDtos);

        return vocabWordDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VocabWordDto findPublishedById(Integer id) {
        VocabWord vocabWord = vocabWordRepository.findById(id).orElseGet(VocabWord::new);
        if (vocabWord.getId() == null
            || StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())
            || !PublishStatusEnum.PUBLISHED.getCode().equals(vocabWord.getPublishStatus())) {
            throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
        }
        VocabWordDto vocabWordDto = vocabWordMapper.toDto(vocabWord);

        List<VocabSenseDto> senseDtos = new ArrayList<>();
        List<VocabSense> senses = vocabSenseRepository.findByWordIdAndStatus(id, StatusEnum.ENABLED.getCode());
        for (VocabSense sense : senses) {
            VocabSenseDto senseDto = convertToSenseDto(sense);
            senseDtos.add(senseDto);
        }
        vocabWordDto.setSenses(senseDtos);

        return vocabWordDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer create(VocabWordDto resources) {
        VocabWord vocabWord = new VocabWord();
        vocabWord.setStatus(StatusEnum.ENABLED.getCode());
        vocabWord.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        vocabWord.setEditStatus(EditStatusEnum.DRAFT.getCode());
        vocabWord.setWord(resources.getWord());
        vocabWord.setDraftContent(JsonUtils.toJson(resources));
        vocabWord = vocabWordRepository.save(vocabWord);
        return vocabWord.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Integer id, VocabWordDto resources) {
        VocabWord vocabWord = vocabWordRepository.findById(id).orElseGet(VocabWord::new);
        if (vocabWord.getId() == null || StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
            throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
        }
        if (EditStatusEnum.REVIEWED.getCode().equals(vocabWord.getEditStatus()) ||
            EditStatusEnum.PUBLISHED.getCode().equals(vocabWord.getEditStatus())) {
            vocabWord.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }
        vocabWord.setDraftContent(JsonUtils.toJson(resources));
        vocabWordRepository.save(vocabWord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        VocabWord vocabWord = vocabWordRepository.findById(id).orElseGet(VocabWord::new);
        if (vocabWord.getId() == null) {
            throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
        }

        // 仅更新状态，不改动子表
        vocabWord.setStatus(StatusEnum.DISABLED.getCode());
        vocabWordRepository.save(vocabWord);
    }

    private void syncSenses(Integer wordId, String word, List<VocabSenseDto> submittedDtos) {
        List<VocabSenseDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<VocabSense> existing = vocabSenseRepository.findByWordIdAndStatus(wordId, StatusEnum.ENABLED.getCode());
        Map<Integer, VocabSense> existingMap = new HashMap<>();
        for (VocabSense sense : existing) {
            existingMap.put(sense.getId(), sense);
        }

        Set<Integer> submittedIds = new HashSet<>();

        for (VocabSenseDto dto : submitted) {
            if (dto.getId() != null) {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("义项ID重复: " + dto.getId());
                }
            }
        }

        for (VocabSenseDto dto : submitted) {
            if (dto.getId() == null) {
                VocabSense sense = convertToSenseEntity(dto, wordId);
                sense = vocabSenseRepository.save(sense);
                syncStructures(wordId, sense.getId(), dto.getStructures());
                syncRelations(wordId, sense.getId(), word, dto);
                syncDefImageSentence(sense.getId(), dto.getDefImageSentence());
            } else {
                VocabSense sense = existingMap.get(dto.getId());
                if (sense == null) {
                    throw new BadRequestException("义项ID不属于当前词汇: " + dto.getId());
                }
                updateSense(sense, dto);
                vocabSenseRepository.save(sense);
                syncStructures(wordId, sense.getId(), dto.getStructures());
                syncRelations(wordId, sense.getId(), word, dto);
                syncDefImageSentence(sense.getId(), dto.getDefImageSentence());
            }
        }

        // 软删除被移除的义项及其关联数据
        for (VocabSense sense : existing) {
            if (!submittedIds.contains(sense.getId())) {
                // 删除关联词汇
                disableRelationsBySenseId(sense.getId());
                // 删除释义图片例句
                exampleSentenceService.disableByBizIds(DEF_IMAGE_SENTENCE_BIZ,
                        Collections.singletonList(sense.getId().longValue()));
                // 删除结构及其例句
                List<VocabStructure> structures = vocabStructureRepository.findBySenseId(sense.getId());
                for (VocabStructure s : structures) {
                    exampleSentenceService.disableByBizIds(STRUCTURE_SENTENCE_BIZ,
                            Collections.singletonList(s.getId().longValue()));
                    s.setStatus(StatusEnum.DISABLED.getCode());
                    vocabStructureRepository.save(s);
                }
                sense.setStatus(StatusEnum.DISABLED.getCode());
                vocabSenseRepository.save(sense);
            }
        }
    }

    private void syncStructures(Integer wordId, Integer senseId, List<VocabStructureDto> submittedDtos) {
        List<VocabStructureDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<VocabStructure> existing = vocabStructureRepository.findBySenseIdAndStatus(senseId, StatusEnum.ENABLED.getCode());
        Map<Integer, VocabStructure> existingMap = new HashMap<>();
        for (VocabStructure structure : existing) {
            existingMap.put(structure.getId(), structure);
        }

        Set<Integer> submittedIds = new HashSet<>();

        for (VocabStructureDto dto : submitted) {
            if (dto.getId() != null) {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("搭配ID重复: " + dto.getId());
                }
            }
        }

        for (VocabStructureDto dto : submitted) {
            Integer structureId;
            if (dto.getId() == null) {
                VocabStructure structure = convertToStructureEntity(dto, wordId, senseId);
                structure = vocabStructureRepository.save(structure);
                structureId = structure.getId();
            } else {
                VocabStructure structure = existingMap.get(dto.getId());
                if (structure == null) {
                    throw new BadRequestException("搭配ID不属于当前义项: " + dto.getId());
                }
                updateStructure(structure, dto);
                vocabStructureRepository.save(structure);
                structureId = structure.getId();
            }
            // 同步结构例句
            syncStructureSentences(structureId.longValue(), dto.getStructureSentences());
        }

        // 软删除被移除的结构及其例句
        for (VocabStructure structure : existing) {
            if (!submittedIds.contains(structure.getId())) {
                exampleSentenceService.disableByBizIds(STRUCTURE_SENTENCE_BIZ,
                        Collections.singletonList(structure.getId().longValue()));
                structure.setStatus(StatusEnum.DISABLED.getCode());
                vocabStructureRepository.save(structure);
            }
        }
    }

    private void syncStructureSentences(Long structureId, List<ExampleSentenceDto> sentenceDtos) {
        // 先软删除该结构旧例句
        exampleSentenceService.disableByBizIds(STRUCTURE_SENTENCE_BIZ, Collections.singletonList(structureId));

        if (sentenceDtos == null || sentenceDtos.isEmpty()) return;

        List<ExampleSentence> toSave = new ArrayList<>();
        for (ExampleSentenceDto dto : sentenceDtos) {
            ExampleSentence entity;
            if (dto.getId() != null && dto.getId() > 0) {
                entity = exampleSentenceRepository.findById(dto.getId()).orElse(null);
                if (entity == null) continue;
            } else {
                entity = new ExampleSentence();
            }
            entity.setBizType(STRUCTURE_SENTENCE_BIZ);
            entity.setBizId(structureId);
            entity.setSentence(dto.getSentence());
            entity.setPinyin(dto.getPinyin());
            entity.setAudioId(dto.getAudioId());
            entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
            entity.setImageId(dto.getImageId());
            entity.setSentenceOrder(dto.getOrder() != null ? dto.getOrder() : 0);
            entity.setStatus(StatusEnum.ENABLED.getCode());
            toSave.add(entity);
        }
        if (!toSave.isEmpty()) {
            exampleSentenceRepository.saveAll(toSave);
        }
    }

    private void syncDefImageSentence(Integer senseId, ExampleSentenceDto dto) {
        exampleSentenceService.syncOne(DEF_IMAGE_SENTENCE_BIZ, senseId.longValue(), dto);
    }

    private void syncRelations(Integer wordId, Integer senseId, String word, VocabSenseDto dto) {
        // 先软删除该义项的所有旧关联
        disableRelationsBySenseId(senseId);

        // 批量新增
        List<VocabRelation> toSave = new ArrayList<>();
        toSave.addAll(buildRelations(wordId, senseId, word, VocabRelationTypeEnum.SYNONYMS_WORDS.getCode(), dto.getSynonymWords()));
        toSave.addAll(buildRelations(wordId, senseId, word, VocabRelationTypeEnum.ANTONYMS_WORDS.getCode(), dto.getAntonymWords()));
        toSave.addAll(buildRelations(wordId, senseId, word, VocabRelationTypeEnum.SEQUENTIAL_WORDS.getCode(), dto.getSequentialWords()));
        toSave.addAll(buildRelations(wordId, senseId, word, VocabRelationTypeEnum.REVERSE_SEQUENTIAL_WORDS.getCode(), dto.getReverseSequentialWords()));
        toSave.addAll(buildRelations(wordId, senseId, word, VocabRelationTypeEnum.JUMBLED_WORDS.getCode(), dto.getJumbledWords()));

        if (!toSave.isEmpty()) {
            vocabRelationRepository.saveAll(toSave);
        }
    }

    private List<VocabRelation> buildRelations(Integer wordId, Integer senseId, String word, String type, List<VocabRelationDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        List<VocabRelation> list = new ArrayList<>();
        for (VocabRelationDto r : dtos) {
            VocabRelation entity = new VocabRelation();
            entity.setWordId(wordId);
            entity.setSenseId(senseId);
            entity.setWord(word);
            entity.setRelationType(type);
            entity.setRelationWordId(r.getRelationWordId() != null ? r.getRelationWordId() : 0L);
            entity.setRelationSenseId(r.getRelationSenseId() != null ? r.getRelationSenseId() : 0L);
            entity.setRelationWord(r.getRelationWord());
            entity.setRelationOrder(r.getOrder() != null ? r.getOrder() : 0);
            entity.setStatus(StatusEnum.ENABLED.getCode());
            list.add(entity);
        }
        return list;
    }

    private void disableRelationsBySenseId(Integer senseId) {
        List<VocabRelation> existing = vocabRelationRepository.findBySenseIdAndStatus(senseId, StatusEnum.ENABLED.getCode());
        for (VocabRelation r : existing) {
            r.setStatus(StatusEnum.DISABLED.getCode());
            vocabRelationRepository.save(r);
        }
    }

    private VocabSenseDto convertToSenseDto(VocabSense sense) {
        VocabSenseDto dto = new VocabSenseDto();
        dto.setId(sense.getId());
        dto.setWordId(sense.getWordId());
        dto.setPartOfSpeech(sense.getPartOfSpeech());
        dto.setChineseDef(sense.getChineseDef());
        dto.setDefAudioId(sense.getDefAudioId());
        dto.setDefImageId(sense.getDefImageId());
        dto.setDefTranslations(JsonUtils.parseTranslationList(sense.getDefTranslations()));
        dto.setSenseOrder(sense.getSenseOrder());
        dto.setCreateTime(sense.getCreateTime());
        dto.setUpdateTime(sense.getUpdateTime());
        dto.setStatus(sense.getStatus());

        // 加载释义图片例句
        dto.setDefImageSentence(exampleSentenceService.findOne(
                DEF_IMAGE_SENTENCE_BIZ, sense.getId().longValue()));

        // 加载关联词汇（按类型分组）
        List<VocabRelation> relations = vocabRelationRepository.findBySenseIdAndStatus(
                sense.getId(), StatusEnum.ENABLED.getCode());
        Map<String, List<VocabRelationDto>> grouped = new HashMap<>();
        for (VocabRelation r : relations) {
            grouped.computeIfAbsent(r.getRelationType(), k -> new ArrayList<>())
                   .add(toVocabRelationDto(r));
        }
        dto.setSynonymWords(grouped.getOrDefault(VocabRelationTypeEnum.SYNONYMS_WORDS.getCode(), Collections.emptyList()));
        dto.setAntonymWords(grouped.getOrDefault(VocabRelationTypeEnum.ANTONYMS_WORDS.getCode(), Collections.emptyList()));
        dto.setSequentialWords(grouped.getOrDefault(VocabRelationTypeEnum.SEQUENTIAL_WORDS.getCode(), Collections.emptyList()));
        dto.setReverseSequentialWords(grouped.getOrDefault(VocabRelationTypeEnum.REVERSE_SEQUENTIAL_WORDS.getCode(), Collections.emptyList()));
        dto.setJumbledWords(grouped.getOrDefault(VocabRelationTypeEnum.JUMBLED_WORDS.getCode(), Collections.emptyList()));

        // 加载结构（含例句批量加载）
        List<VocabStructure> structures = vocabStructureRepository.findBySenseIdAndStatus(
                sense.getId(), StatusEnum.ENABLED.getCode());
        dto.setStructures(batchConvertStructureDto(structures));

        return dto;
    }

    private List<VocabStructureDto> batchConvertStructureDto(List<VocabStructure> structures) {
        if (structures == null || structures.isEmpty()) return Collections.emptyList();

        List<VocabStructureDto> dtos = new ArrayList<>();
        for (VocabStructure s : structures) {
            VocabStructureDto d = new VocabStructureDto();
            d.setId(s.getId());
            d.setWordId(s.getWordId());
            d.setSenseId(s.getSenseId());
            d.setPattern(s.getPattern());
            d.setPatternDef(s.getPatternDef());
            d.setPatternDefTranslations(JsonUtils.parseTranslationList(s.getPatternDefTranslations()));
            d.setStructureOrder(s.getStructureOrder());
            d.setCreateTime(s.getCreateTime());
            d.setUpdateTime(s.getUpdateTime());
            d.setStatus(s.getStatus());
            dtos.add(d);
        }

        // 批量加载结构例句
        List<Long> structureIds = dtos.stream().map(s -> s.getId().longValue()).collect(Collectors.toList());
        List<ExampleSentence> allSentences = exampleSentenceRepository
                .findByBizTypeAndBizIdInAndStatus(STRUCTURE_SENTENCE_BIZ, structureIds, StatusEnum.ENABLED.getCode());

        Map<Long, List<ExampleSentenceDto>> sentenceMap = new HashMap<>();
        for (ExampleSentence s : allSentences) {
            sentenceMap.computeIfAbsent(s.getBizId(), k -> new ArrayList<>())
                       .add(toExampleSentenceDto(s));
        }

        for (VocabStructureDto dto : dtos) {
            dto.setStructureSentences(sentenceMap.getOrDefault(dto.getId().longValue(), Collections.emptyList()));
        }

        return dtos;
    }

    private VocabRelationDto toVocabRelationDto(VocabRelation entity) {
        VocabRelationDto dto = new VocabRelationDto();
        dto.setId(entity.getId());
        dto.setWordId(entity.getWordId());
        dto.setSenseId(entity.getSenseId());
        dto.setWord(entity.getWord());
        dto.setRelationType(entity.getRelationType());
        dto.setRelationWordId(entity.getRelationWordId());
        dto.setRelationSenseId(entity.getRelationSenseId());
        dto.setRelationWord(entity.getRelationWord());
        dto.setOrder(entity.getRelationOrder());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    private ExampleSentenceDto toExampleSentenceDto(ExampleSentence entity) {
        if (entity == null) return null;
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(entity.getId());
        dto.setBizType(entity.getBizType());
        dto.setBizId(entity.getBizId());
        dto.setSentence(entity.getSentence());
        dto.setPinyin(entity.getPinyin());
        dto.setAudioId(entity.getAudioId());
        dto.setTranslations(JsonUtils.parseTranslationList(entity.getTranslations()));
        dto.setImageId(entity.getImageId());
        dto.setOrder(entity.getSentenceOrder());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    private VocabSense convertToSenseEntity(VocabSenseDto dto, Integer wordId) {
        VocabSense entity = new VocabSense();
        entity.setWordId(wordId);
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setChineseDef(dto.getChineseDef());
        entity.setDefAudioId(dto.getDefAudioId());
        entity.setDefImageId(dto.getDefImageId());
        entity.setDefTranslations(JsonUtils.toTranslationJson(dto.getDefTranslations()));
        entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }

    private VocabStructure convertToStructureEntity(VocabStructureDto dto, Integer wordId, Integer senseId) {
        VocabStructure entity = new VocabStructure();
        entity.setWordId(wordId);
        entity.setSenseId(senseId);
        entity.setPattern(dto.getPattern());
        entity.setPatternDef(dto.getPatternDef());
        entity.setPatternDefTranslations(JsonUtils.toTranslationJson(dto.getPatternDefTranslations()));
        entity.setStructureOrder(dto.getStructureOrder() != null ? dto.getStructureOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }

    private void updateSense(VocabSense entity, VocabSenseDto dto) {
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setChineseDef(dto.getChineseDef());
        entity.setDefAudioId(dto.getDefAudioId());
        entity.setDefImageId(dto.getDefImageId());
        entity.setDefTranslations(JsonUtils.toTranslationJson(dto.getDefTranslations()));
        entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
    }

    private void updateStructure(VocabStructure entity, VocabStructureDto dto) {
        entity.setPattern(dto.getPattern());
        entity.setPatternDef(dto.getPatternDef());
        entity.setPatternDefTranslations(JsonUtils.toTranslationJson(dto.getPatternDefTranslations()));
        entity.setStructureOrder(dto.getStructureOrder() != null ? dto.getStructureOrder() : 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Integer id) {
        VocabWord vocabWord = vocabWordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id)));

        if (StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
            throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
        }

        if (vocabWord.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }

        if (!EditStatusEnum.DRAFT.getCode().equals(vocabWord.getEditStatus())) {
            throw new BadRequestException("仅草稿状态可审核");
        }

        vocabWord.setEditStatus(EditStatusEnum.REVIEWED.getCode());
        vocabWordRepository.save(vocabWord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Integer id) {
        VocabWord vocabWord = vocabWordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id)));

        if (StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
            throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
        }

        if (vocabWord.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }

        if (!EditStatusEnum.REVIEWED.getCode().equals(vocabWord.getEditStatus())) {
            throw new BadRequestException("仅已审核状态可发布");
        }

        // 解析草稿
        VocabWordDto draftDto = JsonUtils.fromJson(vocabWord.getDraftContent(), VocabWordDto.class);

        // 更新主表（不更新word字段）
        vocabWord.setWordTraditional(draftDto.getWordTraditional());
        vocabWord.setPinyin(draftDto.getPinyin());
        vocabWord.setAudioId(draftDto.getAudioId());
        vocabWord.setHskLevel(draftDto.getHskLevel());

        // 更新子表
        syncSenses(id, draftDto.getWord(), draftDto.getSenses());

        // 更新状态
        vocabWord.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        vocabWord.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        vocabWord.setDraftContent(null);
        vocabWordRepository.save(vocabWord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Integer id) {
        VocabWord vocabWord = vocabWordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id)));

        if (StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
            throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
        }

        // 仅更新状态，不改动子表
        vocabWord.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        vocabWordRepository.save(vocabWord);
    }
}
