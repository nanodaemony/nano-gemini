package com.naon.grid.backend.service.question.dto;

import com.naon.grid.base.BaseDTO;
import com.naon.grid.domain.common.QuestionContent;
import com.naon.grid.domain.common.QuestionOption;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExerciseQuestionDto extends BaseDTO {

    private Long id;
    private Long parentId;
    private String questionType;
    private String stem;
    private QuestionContent content;
    private List<QuestionOption> options;
    private List<String> answer;
    private String explanation;
    private Long audioId;
    private String audioText;
    private Integer sort;

    private String editStatus;
    private String publishStatus;
    private Integer status;

    // 子题列表（详情时需要）
    private List<ExerciseQuestionDto> children;

    // 列表统计
    private Integer childCount;
}
