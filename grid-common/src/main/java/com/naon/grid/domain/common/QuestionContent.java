package com.naon.grid.domain.common;

import lombok.Data;

/**
 * 题目内容材料信息
 */
@Data
public class QuestionContent {

    /**
     * 内容文案
     */
    private String contentText;

    /**
     * 内容图片ID
     */
    private String contentImageId;

}
