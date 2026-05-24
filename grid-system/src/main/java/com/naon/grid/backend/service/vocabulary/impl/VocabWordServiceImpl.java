package com.naon.grid.backend.service.vocabulary.impl;

import com.naon.grid.backend.domain.vocabulary.*;
import com.naon.grid.backend.repo.vocabulary.*;
import com.naon.grid.backend.service.vocabulary.dto.*;
import lombok.RequiredArgsConstructor;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.mapstruct.VocabWordMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Page<VocabWord> page = vocabWordRepository.findAll((root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root, criteria, criteriaBuilder), pageable);
        return PageUtil.toPage(page.map(vocabWordMapper::toDto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VocabWordDto findById(Integer id) {
        VocabWord vocabWord = vocabWordRepository.findById(id).orElseGet(VocabWord::new);
        if (vocabWord.getId() == null) {
            throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
        }
        VocabWordDto vocabWordDto = vocabWordMapper.toDto(vocabWord);

        List<VocabSenseDto> senseDtos = new ArrayList<>();
        List<VocabSense> senses = vocabSenseRepository.findByWordId(id);
        for (VocabSense sense : senses) {
            VocabSenseDto senseDto = convertToSenseDto(sense);
            senseDtos.add(senseDto);
        }
        vocabWordDto.setSenses(senseDtos);

        List<VocabExerciseDto> exerciseDtos = new ArrayList<>();
        List<VocabExercise> exercises = vocabExerciseRepository.findByWordId(id);
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
        VocabWord vocabWord = vocabWordMapper.toEntity(resources);
        vocabWord = vocabWordRepository.save(vocabWord);

        if (resources.getSenses() != null) {
            for (VocabSenseDto senseDto : resources.getSenses()) {
                VocabSense sense = convertToSenseEntity(senseDto, vocabWord.getId());
                sense = vocabSenseRepository.save(sense);

                if (senseDto.getStructures() != null) {
                    for (VocabStructureDto structureDto : senseDto.getStructures()) {
                        VocabStructure structure = convertToStructureEntity(structureDto, vocabWord.getId(), sense.getId());
                        structure = vocabStructureRepository.save(structure);

                        if (structureDto.getExamples() != null) {
                            for (VocabExampleDto exampleDto : structureDto.getExamples()) {
                                VocabExample example = convertToExampleEntity(exampleDto, vocabWord.getId(), sense.getId(), structure.getId());
                                vocabExampleRepository.save(example);
                            }
                        }
                    }
                }

            }
        }

        if (resources.getExercises() != null) {
            for (VocabExerciseDto exerciseDto : resources.getExercises()) {
                VocabExercise exercise = convertToExerciseEntity(exerciseDto, vocabWord.getId());
                vocabExerciseRepository.save(exercise);
            }
        }

        return vocabWord.getId();
    }

    private VocabSenseDto convertToSenseDto(VocabSense sense) {
        VocabSenseDto dto = new VocabSenseDto();
        dto.setId(sense.getId());
        dto.setWordId(sense.getWordId());
        dto.setPartOfSpeech(sense.getPartOfSpeech());
        dto.setChineseDef(sense.getChineseDef());
        dto.setDefAudioId(sense.getDefAudioId());
        dto.setTranslations(sense.getTranslations());
        dto.setSynonyms(sense.getSynonyms());
        dto.setAntonyms(sense.getAntonyms());
        dto.setRelatedForward(sense.getRelatedForward());
        dto.setRelatedBackward(sense.getRelatedBackward());
        dto.setSenseOrder(sense.getSenseOrder());
        dto.setCreateTime(sense.getCreateTime());
        dto.setUpdateTime(sense.getUpdateTime());

        List<VocabStructureDto> structureDtos = new ArrayList<>();
        List<VocabStructure> structures = vocabStructureRepository.findBySenseId(sense.getId());
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

        List<VocabExampleDto> exampleDtos = new ArrayList<>();
        List<VocabExample> examples = vocabExampleRepository.findByStructureId(structure.getId());
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
        dto.setTranslations(example.getTranslations());
        dto.setExampleOrder(example.getExampleOrder());
        dto.setCreateTime(example.getCreateTime());
        dto.setUpdateTime(example.getUpdateTime());
        return dto;
    }

    private VocabExerciseDto convertToExerciseDto(VocabExercise exercise) {
        VocabExerciseDto dto = new VocabExerciseDto();
        dto.setId(exercise.getId());
        dto.setWordId(exercise.getWordId());
        dto.setQuestionType(exercise.getQuestionType());
        dto.setQuestionText(exercise.getQuestionText());
        dto.setOptions(exercise.getOptions());
        dto.setAnswers(exercise.getAnswers());
        dto.setExerciseOrder(exercise.getExerciseOrder());
        dto.setCreateTime(exercise.getCreateTime());
        dto.setUpdateTime(exercise.getUpdateTime());
        return dto;
    }

    private VocabSense convertToSenseEntity(VocabSenseDto dto, Integer wordId) {
        VocabSense entity = new VocabSense();
        entity.setWordId(wordId);
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setChineseDef(dto.getChineseDef());
        entity.setDefAudioId(dto.getDefAudioId());
        entity.setTranslations(dto.getTranslations());
        entity.setSynonyms(dto.getSynonyms());
        entity.setAntonyms(dto.getAntonyms());
        entity.setRelatedForward(dto.getRelatedForward());
        entity.setRelatedBackward(dto.getRelatedBackward());
        entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
        return entity;
    }

    private VocabStructure convertToStructureEntity(VocabStructureDto dto, Integer wordId, Integer senseId) {
        VocabStructure entity = new VocabStructure();
        entity.setWordId(wordId);
        entity.setSenseId(senseId);
        entity.setPattern(dto.getPattern());
        entity.setStructureOrder(dto.getStructureOrder() != null ? dto.getStructureOrder() : 0);
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
        entity.setTranslations(dto.getTranslations());
        entity.setExampleOrder(dto.getExampleOrder() != null ? dto.getExampleOrder() : 0);
        return entity;
    }

    private VocabExercise convertToExerciseEntity(VocabExerciseDto dto, Integer wordId) {
        VocabExercise entity = new VocabExercise();
        entity.setWordId(wordId);
        entity.setQuestionType(dto.getQuestionType());
        entity.setQuestionText(dto.getQuestionText());
        entity.setOptions(dto.getOptions());
        entity.setAnswers(dto.getAnswers());
        entity.setExerciseOrder(dto.getExerciseOrder() != null ? dto.getExerciseOrder() : 0);
        return entity;
    }
}
