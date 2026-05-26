package com.naon.grid.backend.service.vocabulary.impl;

import com.naon.grid.backend.domain.vocabulary.VocabExample;
import com.naon.grid.backend.domain.vocabulary.VocabExercise;
import com.naon.grid.backend.domain.vocabulary.VocabSense;
import com.naon.grid.backend.domain.vocabulary.VocabStructure;
import com.naon.grid.backend.domain.vocabulary.VocabWord;
import com.naon.grid.backend.repo.vocabulary.VocabExampleRepository;
import com.naon.grid.backend.repo.vocabulary.VocabExerciseRepository;
import com.naon.grid.backend.repo.vocabulary.VocabSenseRepository;
import com.naon.grid.backend.repo.vocabulary.VocabStructureRepository;
import com.naon.grid.backend.repo.vocabulary.VocabWordRepository;
import com.naon.grid.backend.service.vocabulary.dto.VocabExampleDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabExerciseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabStructureDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.mapstruct.VocabWordMapper;
import com.naon.grid.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VocabWordServiceImplTest {

    @Mock
    private VocabWordRepository vocabWordRepository;
    @Mock
    private VocabSenseRepository vocabSenseRepository;
    @Mock
    private VocabStructureRepository vocabStructureRepository;
    @Mock
    private VocabExampleRepository vocabExampleRepository;
    @Mock
    private VocabExerciseRepository vocabExerciseRepository;
    @Mock
    private VocabWordMapper vocabWordMapper;

    private VocabWordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VocabWordServiceImpl(
                vocabWordRepository,
                vocabSenseRepository,
                vocabStructureRepository,
                vocabExampleRepository,
                vocabExerciseRepository,
                vocabWordMapper
        );
    }

    @Test
    void updateDiffsAllVocabularyChildren() {
        VocabWord word = new VocabWord();
        word.setId(1);
        word.setWord("旧词");
        lenient().when(vocabWordRepository.findById(1)).thenReturn(Optional.of(word));

        VocabSense existingSense = sense(10, 1, "旧义项");
        VocabSense deletedSense = sense(11, 1, "删除义项");
        lenient().when(vocabSenseRepository.findByWordId(1)).thenReturn(Arrays.asList(existingSense, deletedSense));

        VocabStructure existingStructure = structure(20, 1, 10, "旧结构");
        VocabStructure deletedStructure = structure(21, 1, 10, "删除结构");
        lenient().when(vocabStructureRepository.findBySenseId(10)).thenReturn(Arrays.asList(existingStructure, deletedStructure));

        VocabExample existingExample = example(30, 1, 10, 20, "旧例句");
        VocabExample deletedExample = example(31, 1, 10, 20, "删除例句");
        lenient().when(vocabExampleRepository.findByStructureId(20)).thenReturn(Arrays.asList(existingExample, deletedExample));
        lenient().when(vocabExampleRepository.findBySenseId(11)).thenReturn(Collections.singletonList(example(32, 1, 11, 22, "义项删除例句")));
        lenient().when(vocabStructureRepository.findBySenseId(11)).thenReturn(Collections.singletonList(structure(22, 1, 11, "义项删除结构")));

        VocabExercise existingExercise = exercise(40, 1, "旧题目");
        VocabExercise deletedExercise = exercise(41, 1, "删除题目");
        lenient().when(vocabExerciseRepository.findByWordId(1)).thenReturn(Arrays.asList(existingExercise, deletedExercise));

        lenient().when(vocabSenseRepository.save(argThat(sense -> sense.getId() == null && "新增义项".equals(sense.getChineseDef()))))
                .thenAnswer(invocation -> {
                    VocabSense saved = invocation.getArgument(0);
                    saved.setId(12);
                    return saved;
                });
        lenient().when(vocabStructureRepository.save(argThat(structure -> structure.getId() == null && Integer.valueOf(12).equals(structure.getSenseId()))))
                .thenAnswer(invocation -> {
                    VocabStructure saved = invocation.getArgument(0);
                    saved.setId(23);
                    return saved;
                });

        VocabWordDto dto = new VocabWordDto();
        dto.setWord("新词");
        dto.setSenses(Arrays.asList(
                senseDto(10, "新义项", Collections.singletonList(
                        structureDto(20, "新结构", Collections.singletonList(exampleDto(30, "新例句")))
                )),
                senseDto(null, "新增义项", Collections.singletonList(
                        structureDto(null, "新增结构", Collections.singletonList(exampleDto(null, "新增例句")))
                ))
        ));
        dto.setExercises(Arrays.asList(
                exerciseDto(40, "新题目"),
                exerciseDto(null, "新增题目")
        ));

        service.update(1, dto);

        verify(vocabWordRepository).save(argThat(saved -> "新词".equals(saved.getWord())));
        verify(vocabSenseRepository).save(argThat(saved -> Integer.valueOf(10).equals(saved.getId()) && "新义项".equals(saved.getChineseDef())));
        verify(vocabSenseRepository).save(argThat(saved -> "新增义项".equals(saved.getChineseDef())));
        verify(vocabSenseRepository).deleteAll(argThat((Iterable<VocabSense> iterable) -> {
            List<VocabSense> list = new ArrayList<>();
            iterable.forEach(list::add);
            return containsSenseId(list, 11);
        }));

        verify(vocabStructureRepository).save(argThat(saved -> Integer.valueOf(20).equals(saved.getId()) && "新结构".equals(saved.getPattern())));
        verify(vocabStructureRepository).save(argThat(saved -> "新增结构".equals(saved.getPattern())));
        verify(vocabStructureRepository).deleteAll(argThat((Iterable<VocabStructure> iterable) -> {
            List<VocabStructure> list = new ArrayList<>();
            iterable.forEach(list::add);
            return containsStructureId(list, 21);
        }));

        verify(vocabExampleRepository).saveAll(argThat((Iterable<VocabExample> iterable) -> {
            List<VocabExample> list = new ArrayList<>();
            iterable.forEach(list::add);
            return list.stream().anyMatch(example -> Integer.valueOf(30).equals(example.getId()) && "新例句".equals(example.getSentence()));
        }));
        verify(vocabExampleRepository).saveAll(argThat((Iterable<VocabExample> iterable) -> {
            List<VocabExample> list = new ArrayList<>();
            iterable.forEach(list::add);
            return list.stream().anyMatch(example -> "新增例句".equals(example.getSentence()));
        }));
        verify(vocabExampleRepository).deleteAll(argThat((Iterable<VocabExample> iterable) -> {
            List<VocabExample> list = new ArrayList<>();
            iterable.forEach(list::add);
            return containsExampleId(list, 31);
        }));
        verify(vocabExampleRepository).deleteAll(argThat((Iterable<VocabExample> iterable) -> {
            List<VocabExample> list = new ArrayList<>();
            iterable.forEach(list::add);
            return containsExampleId(list, 32);
        }));

        verify(vocabExerciseRepository).saveAll(argThat((Iterable<VocabExercise> iterable) -> {
            List<VocabExercise> list = new ArrayList<>();
            iterable.forEach(list::add);
            return list.stream().anyMatch(exercise -> Integer.valueOf(40).equals(exercise.getId()) && "新题目".equals(exercise.getQuestionText()));
        }));
        verify(vocabExerciseRepository).saveAll(argThat((Iterable<VocabExercise> iterable) -> {
            List<VocabExercise> list = new ArrayList<>();
            iterable.forEach(list::add);
            return list.stream().anyMatch(exercise -> "新增题目".equals(exercise.getQuestionText()));
        }));
        verify(vocabExerciseRepository).deleteAll(argThat((Iterable<VocabExercise> iterable) -> {
            List<VocabExercise> list = new ArrayList<>();
            iterable.forEach(list::add);
            return containsExerciseId(list, 41);
        }));
    }

    @Test
    void updateRejectsDuplicateSenseId() {
        VocabWord word = new VocabWord();
        word.setId(1);
        lenient().when(vocabWordRepository.findById(1)).thenReturn(Optional.of(word));
        lenient().when(vocabSenseRepository.findByWordId(1)).thenReturn(Collections.singletonList(sense(10, 1, "旧义项")));

        VocabWordDto dto = new VocabWordDto();
        dto.setSenses(Arrays.asList(senseDto(10, "义项1", null), senseDto(10, "义项2", null)));
        dto.setExercises(Collections.emptyList());

        assertThrows(BadRequestException.class, () -> service.update(1, dto));
        verify(vocabSenseRepository, never()).save(any(VocabSense.class));
    }

    @Test
    void updateRejectsStructureIdOutsideCurrentSense() {
        VocabWord word = new VocabWord();
        word.setId(1);
        lenient().when(vocabWordRepository.findById(1)).thenReturn(Optional.of(word));
        lenient().when(vocabSenseRepository.findByWordId(1)).thenReturn(Collections.singletonList(sense(10, 1, "旧义项")));
        lenient().when(vocabStructureRepository.findBySenseId(10)).thenReturn(Collections.singletonList(structure(20, 1, 10, "旧结构")));

        VocabWordDto dto = new VocabWordDto();
        dto.setSenses(Collections.singletonList(
                senseDto(10, "新义项", Collections.singletonList(structureDto(99, "错误结构", null)))
        ));
        dto.setExercises(Collections.emptyList());

        assertThrows(BadRequestException.class, () -> service.update(1, dto));
        verify(vocabStructureRepository, never()).save(any(VocabStructure.class));
    }

    private VocabSense sense(Integer id, Integer wordId, String chineseDef) {
        VocabSense sense = new VocabSense();
        sense.setId(id);
        sense.setWordId(wordId);
        sense.setChineseDef(chineseDef);
        return sense;
    }

    private VocabStructure structure(Integer id, Integer wordId, Integer senseId, String pattern) {
        VocabStructure structure = new VocabStructure();
        structure.setId(id);
        structure.setWordId(wordId);
        structure.setSenseId(senseId);
        structure.setPattern(pattern);
        return structure;
    }

    private VocabExample example(Integer id, Integer wordId, Integer senseId, Integer structureId, String sentence) {
        VocabExample example = new VocabExample();
        example.setId(id);
        example.setWordId(wordId);
        example.setSenseId(senseId);
        example.setStructureId(structureId);
        example.setSentence(sentence);
        return example;
    }

    private VocabExercise exercise(Integer id, Integer wordId, String questionText) {
        VocabExercise exercise = new VocabExercise();
        exercise.setId(id);
        exercise.setWordId(wordId);
        exercise.setQuestionText(questionText);
        return exercise;
    }

    private VocabSenseDto senseDto(Integer id, String chineseDef, List<VocabStructureDto> structures) {
        VocabSenseDto dto = new VocabSenseDto();
        dto.setId(id);
        dto.setChineseDef(chineseDef);
        dto.setStructures(structures);
        return dto;
    }

    private VocabStructureDto structureDto(Integer id, String pattern, List<VocabExampleDto> examples) {
        VocabStructureDto dto = new VocabStructureDto();
        dto.setId(id);
        dto.setPattern(pattern);
        dto.setExamples(examples);
        return dto;
    }

    private VocabExampleDto exampleDto(Integer id, String sentence) {
        VocabExampleDto dto = new VocabExampleDto();
        dto.setId(id);
        dto.setSentence(sentence);
        return dto;
    }

    private VocabExerciseDto exerciseDto(Integer id, String questionText) {
        VocabExerciseDto dto = new VocabExerciseDto();
        dto.setId(id);
        dto.setQuestionText(questionText);
        return dto;
    }

    private boolean containsSenseId(List<VocabSense> list, Integer id) {
        return list.stream().anyMatch(item -> id.equals(item.getId()));
    }

    private boolean containsStructureId(List<VocabStructure> list, Integer id) {
        return list.stream().anyMatch(item -> id.equals(item.getId()));
    }

    private boolean containsExampleId(List<VocabExample> list, Integer id) {
        return list.stream().anyMatch(item -> id.equals(item.getId()));
    }

    private boolean containsExerciseId(List<VocabExercise> list, Integer id) {
        return list.stream().anyMatch(item -> id.equals(item.getId()));
    }
}
