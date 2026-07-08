package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class LearningHistoryItemVO implements Serializable {

    @ApiModelProperty("业务类型：CHARACTER/VOCABULARY/RADICAL/GRAMMAR/GRAMMAR_COMPARISON/VOCAB_COMPARISON")
    private String bizType;

    @ApiModelProperty("内容ID")
    private Long contentId;

    @ApiModelProperty("内容展示名称")
    private String contentName;

    @ApiModelProperty(value = "学习时间", example = "2026-07-08 10:30:00")
    private String learnedAt;
}
