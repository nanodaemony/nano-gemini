package com.naon.grid.backend.service.vocabulary.impl;

import com.naon.grid.backend.domain.vocabulary.*;
import com.naon.grid.backend.repo.vocabulary.*;
import com.naon.grid.backend.service.vocabulary.dto.*;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
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

@Service
@RequiredArgsConstructor
public class VocabWordServiceImpl implements VocabWordService {

    private final VocabWordRepository vocabWordRepository;
    private final VocabSenseRepository vocabSenseRepository;
    private final VocabStructureRepository vocabStructureRepository;
    private final VocabExampleRepository vocabExampleRepository;
    private final VocabExerciseRepository vocabExerciseRepository;
    private final VocabWordMapper vocabWordMapper;

    @Override
    public PageResult<VocabWordDto> queryAll(VocabWordQueryCriteria criteria, Pageable pageable) {
        Page<VocabWord> page = vocabWordRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return criteriaBuilder.and(basePredicate, statusPredicate);
        }, pageable);
        return PageUtil.toPage(page.map(vocabWordMapper::toDto));
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

        List<VocabExerciseDto> exerciseDtos = new ArrayList<>();
        List<VocabExercise> exercises = vocabExerciseRepository.findByWordIdAndStatus(id, StatusEnum.ENABLED.getCode());
        for (VocabExercise exercise : exercises) {
            VocabExerciseDto exerciseDto = convertToExerciseDto(exercise);
            exerciseDtos.add(exerciseDto);
        }
        vocabWordDto.setExercises(exerciseDtos);

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

        List<VocabExerciseDto> exerciseDtos = new ArrayList<>();
        List<VocabExercise> exercises = vocabExerciseRepository.findByWordIdAndStatus(id, StatusEnum.ENABLED.getCode());
        for (VocabExercise exercise : exercises) {
            VocabExerciseDto exerciseDto = convertToExerciseDto(exercise);
            exerciseDtos.add(exerciseDto);
        }
        vocabWordDto.setExercises(exerciseDtos);

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

    private void saveChildren(VocabWordDto resources, Integer wordId) {
        if (resources.getSenses() != null) {
            for (VocabSenseDto senseDto : resources.getSenses()) {
                VocabSense sense = convertToSenseEntity(senseDto, wordId);
                sense = vocabSenseRepository.save(sense);

                if (senseDto.getStructures() != null) {
                    for (VocabStructureDto structureDto : senseDto.getStructures()) {
                        VocabStructure structure = convertToStructureEntity(structureDto, wordId, sense.getId());
                        structure = vocabStructureRepository.save(structure);

                        if (structureDto.getExamples() != null) {
                            for (VocabExampleDto exampleDto : structureDto.getExamples()) {
                                VocabExample example = convertToExampleEntity(exampleDto, wordId, sense.getId(), structure.getId());
                                vocabExampleRepository.save(example);
                            }
                        }
                    }
                }

            }
        }

        if (resources.getExercises() != null) {
            for (VocabExerciseDto exerciseDto : resources.getExercises()) {
                VocabExercise exercise = convertToExerciseEntity(exerciseDto, wordId);
                vocabExerciseRepository.save(exercise);
            }
        }
    }

    private void deleteChildren(Integer wordId) {
        List<VocabExample> examples = vocabExampleRepository.findByWordId(wordId);
        for (VocabExample e : examples) {
            e.setStatus(StatusEnum.DISABLED.getCode());
            vocabExampleRepository.save(e);
        }
        List<VocabStructure> structures = vocabStructureRepository.findByWordId(wordId);
        for (VocabStructure s : structures) {
            s.setStatus(StatusEnum.DISABLED.getCode());
            vocabStructureRepository.save(s);
        }
        List<VocabSense> senses = vocabSenseRepository.findByWordId(wordId);
        for (VocabSense s : senses) {
            s.setStatus(StatusEnum.DISABLED.getCode());
            vocabSenseRepository.save(s);
        }
        List<VocabExercise> exercises = vocabExerciseRepository.findByWordId(wordId);
        for (VocabExercise e : exercises) {
            e.setStatus(StatusEnum.DISABLED.getCode());
            vocabExerciseRepository.save(e);
        }
    }

    private void syncSenses(Integer wordId, List<VocabSenseDto> submittedDtos) {
        List<VocabSenseDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<VocabSense> existing = vocabSenseRepository.findByWordIdAndStatus(wordId, StatusEnum.ENABLED.getCode());
        Map<Integer, VocabSense> existingMap = new HashMap<>();
        for (VocabSense sense : existing) {
            existingMap.put(sense.getId(), sense);
        }

        Set<Integer> submittedIds = new HashSet<>();

        // First, check all IDs for duplicates
        for (VocabSenseDto dto : submitted) {
            if (dto.getId() != null) {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("义项ID重复: " + dto.getId());
                }
            }
        }

        // Now process each DTO in order
        for (VocabSenseDto dto : submitted) {
            if (dto.getId() == null) {
                VocabSense sense = convertToSenseEntity(dto, wordId);
                sense = vocabSenseRepository.save(sense);
                syncStructures(wordId, sense.getId(), dto.getStructures());
            } else {
                VocabSense sense = existingMap.get(dto.getId());
                if (sense == null) {
                    throw new BadRequestException("义项ID不属于当前词汇: " + dto.getId());
                }
                updateSense(sense, dto);
                vocabSenseRepository.save(sense);
                syncStructures(wordId, sense.getId(), dto.getStructures());
            }
        }

        List<VocabSense> toDelete = new ArrayList<>();
        for (VocabSense sense : existing) {
            if (!submittedIds.contains(sense.getId())) {
                List<VocabExample> examples = vocabExampleRepository.findBySenseId(sense.getId());
                for (VocabExample e : examples) {
                    e.setStatus(StatusEnum.DISABLED.getCode());
                    vocabExampleRepository.save(e);
                }
                List<VocabStructure> structures = vocabStructureRepository.findBySenseId(sense.getId());
                for (VocabStructure s : structures) {
                    s.setStatus(StatusEnum.DISABLED.getCode());
                    vocabStructureRepository.save(s);
                }
                toDelete.add(sense);
            }
        }
        for (VocabSense s : toDelete) {
            s.setStatus(StatusEnum.DISABLED.getCode());
            vocabSenseRepository.save(s);
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

        // First, check all IDs for duplicates
        for (VocabStructureDto dto : submitted) {
            if (dto.getId() != null) {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("搭配ID重复: " + dto.getId());
                }
            }
        }

        // Now process each DTO in order
        for (VocabStructureDto dto : submitted) {
            if (dto.getId() == null) {
                VocabStructure structure = convertToStructureEntity(dto, wordId, senseId);
                structure = vocabStructureRepository.save(structure);
                syncExamples(wordId, senseId, structure.getId(), dto.getExamples());
            } else {
                VocabStructure structure = existingMap.get(dto.getId());
                if (structure == null) {
                    throw new BadRequestException("搭配ID不属于当前义项: " + dto.getId());
                }
                updateStructure(structure, dto);
                vocabStructureRepository.save(structure);
                syncExamples(wordId, senseId, structure.getId(), dto.getExamples());
            }
        }

        List<VocabStructure> toDelete = new ArrayList<>();
        for (VocabStructure structure : existing) {
            if (!submittedIds.contains(structure.getId())) {
                List<VocabExample> examples = vocabExampleRepository.findByStructureId(structure.getId());
                for (VocabExample e : examples) {
                    e.setStatus(StatusEnum.DISABLED.getCode());
                    vocabExampleRepository.save(e);
                }
                toDelete.add(structure);
            }
        }
        for (VocabStructure s : toDelete) {
            s.setStatus(StatusEnum.DISABLED.getCode());
            vocabStructureRepository.save(s);
        }
    }

    private void syncExamples(Integer wordId, Integer senseId, Integer structureId, List<VocabExampleDto> submittedDtos) {
        List<VocabExampleDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<VocabExample> existing = vocabExampleRepository.findByStructureIdAndStatus(structureId, StatusEnum.ENABLED.getCode());
        Map<Integer, VocabExample> existingMap = new HashMap<>();
        for (VocabExample example : existing) {
            existingMap.put(example.getId(), example);
        }

        Set<Integer> submittedIds = new HashSet<>();
        List<VocabExample> toSave = new ArrayList<>();

        // First, check all IDs for duplicates
        for (VocabExampleDto dto : submitted) {
            if (dto.getId() != null) {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("例句ID重复: " + dto.getId());
                }
            }
        }

        // Now process each DTO in order
        for (VocabExampleDto dto : submitted) {
            if (dto.getId() == null) {
                toSave.add(convertToExampleEntity(dto, wordId, senseId, structureId));
            } else {
                VocabExample example = existingMap.get(dto.getId());
                if (example == null) {
                    throw new BadRequestException("例句ID不属于当前搭配: " + dto.getId());
                }
                updateExample(example, dto);
                toSave.add(example);
            }
        }

        List<VocabExample> toDelete = new ArrayList<>();
        for (VocabExample example : existing) {
            if (!submittedIds.contains(example.getId())) {
                toDelete.add(example);
            }
        }
        for (VocabExample e : toDelete) {
            e.setStatus(StatusEnum.DISABLED.getCode());
            vocabExampleRepository.save(e);
        }
        vocabExampleRepository.saveAll(toSave);
    }

    private void syncExercises(Integer wordId, List<VocabExerciseDto> submittedDtos) {
        List<VocabExerciseDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<VocabExercise> existing = vocabExerciseRepository.findByWordIdAndStatus(wordId, StatusEnum.ENABLED.getCode());
        Map<Integer, VocabExercise> existingMap = new HashMap<>();
        for (VocabExercise exercise : existing) {
            existingMap.put(exercise.getId(), exercise);
        }

        Set<Integer> submittedIds = new HashSet<>();
        List<VocabExercise> toSave = new ArrayList<>();

        // First, check all IDs for duplicates
        for (VocabExerciseDto dto : submitted) {
            if (dto.getId() != null) {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("练习题ID重复: " + dto.getId());
                }
            }
        }

        // Now process each DTO in order
        for (VocabExerciseDto dto : submitted) {
            if (dto.getId() == null) {
                toSave.add(convertToExerciseEntity(dto, wordId));
            } else {
                VocabExercise exercise = existingMap.get(dto.getId());
                if (exercise == null) {
                    throw new BadRequestException("练习题ID不属于当前词汇: " + dto.getId());
                }
                updateExercise(exercise, dto);
                toSave.add(exercise);
            }
        }

        List<VocabExercise> toDelete = new ArrayList<>();
        for (VocabExercise exercise : existing) {
            if (!submittedIds.contains(exercise.getId())) {
                toDelete.add(exercise);
            }
        }
        for (VocabExercise e : toDelete) {
            e.setStatus(StatusEnum.DISABLED.getCode());
            vocabExerciseRepository.save(e);
        }
        vocabExerciseRepository.saveAll(toSave);
    }

    private VocabSenseDto convertToSenseDto(VocabSense sense) {
        VocabSenseDto dto = new VocabSenseDto();
        dto.setId(sense.getId());
        dto.setWordId(sense.getWordId());
        dto.setPartOfSpeech(sense.getPartOfSpeech());
        dto.setChineseDef(sense.getChineseDef());
        dto.setDefAudioId(sense.getDefAudioId());
        dto.setTranslations(JsonUtils.parseTranslationList(sense.getTranslations()));
        dto.setSynonyms(JsonUtils.parseStringList(sense.getSynonyms()));
        dto.setAntonyms(JsonUtils.parseStringList(sense.getAntonyms()));
        dto.setRelatedForward(JsonUtils.parseStringList(sense.getRelatedForward()));
        dto.setRelatedBackward(JsonUtils.parseStringList(sense.getRelatedBackward()));
        dto.setSenseOrder(sense.getSenseOrder());
        dto.setCreateTime(sense.getCreateTime());
        dto.setUpdateTime(sense.getUpdateTime());
        dto.setStatus(sense.getStatus());

        List<VocabStructureDto> structureDtos = new ArrayList<>();
        List<VocabStructure> structures = vocabStructureRepository.findBySenseIdAndStatus(sense.getId(), StatusEnum.ENABLED.getCode());
        for (VocabStructure structure : structures) {
            VocabStructureDto structureDto = convertToStructureDto(structure);
            structureDtos.add(structureDto);
        }
        dto.setStructures(structureDtos);

        return dto;
    }

    private VocabStructureDto convertToStructureDto(VocabStructure structure) {
        VocabStructureDto dto = new VocabStructureDto();
        dto.setId(structure.getId());
        dto.setWordId(structure.getWordId());
        dto.setSenseId(structure.getSenseId());
        dto.setPattern(structure.getPattern());
        dto.setStructureOrder(structure.getStructureOrder());
        dto.setCreateTime(structure.getCreateTime());
        dto.setUpdateTime(structure.getUpdateTime());
        dto.setStatus(structure.getStatus());

        List<VocabExampleDto> exampleDtos = new ArrayList<>();
        List<VocabExample> examples = vocabExampleRepository.findByStructureIdAndStatus(structure.getId(), StatusEnum.ENABLED.getCode());
        for (VocabExample example : examples) {
            VocabExampleDto exampleDto = convertToExampleDto(example);
            exampleDtos.add(exampleDto);
        }
        dto.setExamples(exampleDtos);

        return dto;
    }

    private VocabExampleDto convertToExampleDto(VocabExample example) {
        VocabExampleDto dto = new VocabExampleDto();
        dto.setId(example.getId());
        dto.setWordId(example.getWordId());
        dto.setSenseId(example.getSenseId());
        dto.setStructureId(example.getStructureId());
        dto.setSentence(example.getSentence());
        dto.setAudioId(example.getAudioId());
        dto.setPinyin(example.getPinyin());
        dto.setTranslations(JsonUtils.parseTranslationList(example.getTranslations()));
        dto.setExampleOrder(example.getExampleOrder());
        dto.setCreateTime(example.getCreateTime());
        dto.setUpdateTime(example.getUpdateTime());
        dto.setStatus(example.getStatus());
        return dto;
    }

    private VocabExerciseDto convertToExerciseDto(VocabExercise exercise) {
        VocabExerciseDto dto = new VocabExerciseDto();
        dto.setId(exercise.getId());
        dto.setWordId(exercise.getWordId());
        dto.setQuestionType(exercise.getQuestionType());
        dto.setQuestionText(exercise.getQuestionText());
        dto.setOptions(JsonUtils.parseExerciseOptionList(exercise.getOptions()));
        dto.setAnswers(JsonUtils.parseStringList(exercise.getAnswers()));
        dto.setExerciseOrder(exercise.getExerciseOrder());
        dto.setCreateTime(exercise.getCreateTime());
        dto.setUpdateTime(exercise.getUpdateTime());
        dto.setStatus(exercise.getStatus());
        return dto;
    }

    private void updateSense(VocabSense entity, VocabSenseDto dto) {
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setChineseDef(dto.getChineseDef());
        entity.setDefAudioId(dto.getDefAudioId());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setSynonyms(JsonUtils.toStringListJson(dto.getSynonyms()));
        entity.setAntonyms(JsonUtils.toStringListJson(dto.getAntonyms()));
        entity.setRelatedForward(JsonUtils.toStringListJson(dto.getRelatedForward()));
        entity.setRelatedBackward(JsonUtils.toStringListJson(dto.getRelatedBackward()));
        entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
    }

    private void updateStructure(VocabStructure entity, VocabStructureDto dto) {
        entity.setPattern(dto.getPattern());
        entity.setStructureOrder(dto.getStructureOrder() != null ? dto.getStructureOrder() : 0);
    }

    private void updateExample(VocabExample entity, VocabExampleDto dto) {
        entity.setSentence(dto.getSentence());
        entity.setAudioId(dto.getAudioId());
        entity.setPinyin(dto.getPinyin());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setExampleOrder(dto.getExampleOrder() != null ? dto.getExampleOrder() : 0);
    }

    private void updateExercise(VocabExercise entity, VocabExerciseDto dto) {
        entity.setQuestionType(dto.getQuestionType());
        entity.setQuestionText(dto.getQuestionText());
        entity.setOptions(JsonUtils.toExerciseOptionListJson(dto.getOptions()));
        entity.setAnswers(JsonUtils.toStringListJson(dto.getAnswers()));
        entity.setExerciseOrder(dto.getExerciseOrder() != null ? dto.getExerciseOrder() : 0);
    }

    private VocabSense convertToSenseEntity(VocabSenseDto dto, Integer wordId) {
        VocabSense entity = new VocabSense();
        entity.setWordId(wordId);
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setChineseDef(dto.getChineseDef());
        entity.setDefAudioId(dto.getDefAudioId());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setSynonyms(JsonUtils.toStringListJson(dto.getSynonyms()));
        entity.setAntonyms(JsonUtils.toStringListJson(dto.getAntonyms()));
        entity.setRelatedForward(JsonUtils.toStringListJson(dto.getRelatedForward()));
        entity.setRelatedBackward(JsonUtils.toStringListJson(dto.getRelatedBackward()));
        entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }

    private VocabStructure convertToStructureEntity(VocabStructureDto dto, Integer wordId, Integer senseId) {
        VocabStructure entity = new VocabStructure();
        entity.setWordId(wordId);
        entity.setSenseId(senseId);
        entity.setPattern(dto.getPattern());
        entity.setStructureOrder(dto.getStructureOrder() != null ? dto.getStructureOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }

    private VocabExample convertToExampleEntity(VocabExampleDto dto, Integer wordId, Integer senseId, Integer structureId) {
        VocabExample entity = new VocabExample();
        entity.setWordId(wordId);
        entity.setSenseId(senseId);
        entity.setStructureId(structureId);
        entity.setSentence(dto.getSentence());
        entity.setAudioId(dto.getAudioId());
        entity.setPinyin(dto.getPinyin());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setExampleOrder(dto.getExampleOrder() != null ? dto.getExampleOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }

    private VocabExercise convertToExerciseEntity(VocabExerciseDto dto, Integer wordId) {
        VocabExercise entity = new VocabExercise();
        entity.setWordId(wordId);
        entity.setQuestionType(dto.getQuestionType());
        entity.setQuestionText(dto.getQuestionText());
        entity.setOptions(JsonUtils.toExerciseOptionListJson(dto.getOptions()));
        entity.setAnswers(JsonUtils.toStringListJson(dto.getAnswers()));
        entity.setExerciseOrder(dto.getExerciseOrder() != null ? dto.getExerciseOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
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
        syncSenses(id, draftDto.getSenses());
        syncExercises(id, draftDto.getExercises());

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

    private List<VocabSenseDto> convertToSenseDtos(List<VocabSense> senses) {
        List<VocabSenseDto> dtos = new ArrayList<>();
        for (VocabSense sense : senses) {
            dtos.add(convertToSenseDto(sense));
        }
        return dtos;
    }

    private List<VocabExerciseDto> convertToExerciseDtos(List<VocabExercise> exercises) {
        List<VocabExerciseDto> dtos = new ArrayList<>();
        for (VocabExercise exercise : exercises) {
            dtos.add(convertToExerciseDto(exercise));
        }
        return dtos;
    }
}
