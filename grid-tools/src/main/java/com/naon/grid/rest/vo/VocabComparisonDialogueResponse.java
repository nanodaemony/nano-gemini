package com.naon.grid.rest.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "词汇辨析对话生成响应")
public class VocabComparisonDialogueResponse {
    @ApiModelProperty(value = "情景对话列表")
    private List<VocabChatBaseVO> chats;
}
