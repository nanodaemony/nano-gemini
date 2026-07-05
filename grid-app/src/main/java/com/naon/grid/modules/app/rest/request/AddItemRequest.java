package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class AddItemRequest {

    @ApiModelProperty(value = "收藏夹ID（不传则使用默认收藏夹）")
    private Long folderId;

    @NotBlank(message = "业务类型不能为空")
    @ApiModelProperty(value = "业务类型：CHARACTER/VOCABULARY/RADICAL/GRAMMAR/GRAMMAR_COMPARISON/VOCAB_COMPARISON", required = true)
    private String bizType;

    @ApiModelProperty(value = "收藏内容ID（与contentText至少提供一个）")
    private Long contentId;

    @ApiModelProperty(value = "收藏内容文本（用于好词好句等无结构化ID的内容）")
    private String contentText;
}
