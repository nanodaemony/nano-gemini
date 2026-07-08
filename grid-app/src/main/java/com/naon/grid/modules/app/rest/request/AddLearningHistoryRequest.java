package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class AddLearningHistoryRequest {

    @NotBlank(message = "业务类型不能为空")
    @ApiModelProperty(value = "业务类型：CHARACTER/VOCABULARY/RADICAL/GRAMMAR/GRAMMAR_COMPARISON/VOCAB_COMPARISON", required = true)
    private String bizType;

    @NotNull(message = "内容ID不能为空")
    @ApiModelProperty(value = "学习内容ID", required = true)
    private Long contentId;
}
