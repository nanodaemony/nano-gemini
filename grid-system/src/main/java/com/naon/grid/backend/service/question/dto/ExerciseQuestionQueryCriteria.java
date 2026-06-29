package com.naon.grid.backend.service.question.dto;

import com.naon.grid.annotation.Query;
import lombok.Data;

import java.io.Serializable;

@Data
public class ExerciseQuestionQueryCriteria implements Serializable {

    @Query(blurry = "stem")
    private String blurry;

    @Query
    private String questionType;

    @Query
    private String publishStatus;

    @Query
    private String editStatus;
}
