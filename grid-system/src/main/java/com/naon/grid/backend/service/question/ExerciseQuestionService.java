package com.naon.grid.backend.service.question;

import com.naon.grid.backend.service.question.dto.ExerciseQuestionDto;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface ExerciseQuestionService {

    PageResult<ExerciseQuestionDto> queryAll(ExerciseQuestionQueryCriteria criteria, Pageable pageable);

    ExerciseQuestionDto findById(Long id);

    Long create(ExerciseQuestionDto dto);

    void update(Long id, ExerciseQuestionDto dto);

    void delete(Long id);

    void reviewDraft(Long id);

    void publishDraft(Long id);

    void offline(Long id);
}
