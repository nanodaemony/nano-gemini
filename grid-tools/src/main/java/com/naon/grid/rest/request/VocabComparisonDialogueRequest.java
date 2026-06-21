package com.naon.grid.rest.request;

import com.naon.grid.enums.ChatProviderEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@ApiModel(description = "词汇辨析对话生成请求")
public class VocabComparisonDialogueRequest {
    @NotEmpty(message = "词汇列表不能为空")
    @Size(max = 5, message = "词汇数量不能超过5个")
    @ApiModelProperty(value = "词汇列表", required = true)
    private List<VocabWordInfo> words;

    @ApiModelProperty(value = "大模型厂商", example = "ALIYUN")
    private ChatProviderEnum provider;

    @ApiModelProperty(value = "模型名称", example = "qwen-plus")
    private String model;

    @Data
    @ApiModel(description = "词汇信息")
    public static class VocabWordInfo {
        @NotBlank(message = "词汇词头不能为空")
        @ApiModelProperty(value = "词汇词头", required = true)
        private String word;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "用法对比")
        private String usageComparison;
    }
}
