package com.naon.grid.backend.rest.controller;

import com.naon.grid.backend.rest.request.VocabWordCreateRequest;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VocabWordControllerTest {

    @Test
    void updatePassesNestedChildIdsToServiceDto() {
        VocabWordService vocabWordService = mock(VocabWordService.class);
        VocabWordController controller = new VocabWordController(vocabWordService);

        VocabWordCreateRequest request = new VocabWordCreateRequest();
        request.setWord("学习");

        VocabWordCreateRequest.VocabExampleRequest example = new VocabWordCreateRequest.VocabExampleRequest();
        example.setId(301);
        example.setSentence("我学习中文。");

        VocabWordCreateRequest.VocabStructureRequest structure = new VocabWordCreateRequest.VocabStructureRequest();
        structure.setId(201);
        structure.setPattern("学习 + 语言");
        structure.setExamples(Collections.singletonList(example));

        VocabWordCreateRequest.VocabSenseRequest sense = new VocabWordCreateRequest.VocabSenseRequest();
        sense.setId(101);
        sense.setChineseDef("获取知识");
        sense.setStructures(Collections.singletonList(structure));

        VocabWordCreateRequest.VocabExerciseRequest exercise = new VocabWordCreateRequest.VocabExerciseRequest();
        exercise.setId(401);
        exercise.setQuestionText("选择正确释义");

        request.setSenses(Collections.singletonList(sense));
        request.setExercises(Collections.singletonList(exercise));

        controller.update(1, request);

        ArgumentCaptor<VocabWordDto> captor = ArgumentCaptor.forClass(VocabWordDto.class);
        verify(vocabWordService).update(eq(1), captor.capture());
        VocabWordDto dto = captor.getValue();

        assertEquals(Integer.valueOf(101), dto.getSenses().get(0).getId());
        assertEquals(Integer.valueOf(201), dto.getSenses().get(0).getStructures().get(0).getId());
        assertEquals(Integer.valueOf(301), dto.getSenses().get(0).getStructures().get(0).getExamples().get(0).getId());
        assertEquals(Integer.valueOf(401), dto.getExercises().get(0).getId());
    }
}
