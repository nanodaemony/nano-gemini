package com.naon.grid.domain.common;

import lombok.Data;

/**
 * 练习题选项
 */
@Data
public class QuestionOption {

    /**
     * 选项标识，如 "A", "B", "C", "D"
     */
    private String option;

    /**
     * 选项文案
     */
    private String optionText;

    /**
     * 选项图片ID
     */
    private String optionImageId;

}
