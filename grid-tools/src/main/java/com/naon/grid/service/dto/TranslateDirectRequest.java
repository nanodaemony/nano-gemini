package com.naon.grid.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 指定源语言翻译请求 DTO
 * @author nano
 * @date 2026-06-14
 */
@Data
public class TranslateDirectRequest {

    @NotBlank(message = "源文本不能为空")
    @ApiModelProperty(value = "源文本", required = true)
    private String sourceText;

    @ApiModelProperty(value = "源语言代码，默认 zh", example = "en")
    private String sourceLanguage = "zh";

    @NotBlank(message = "目标语言不能为空")
    @ApiModelProperty(value = "目标语言代码", required = true, example = "ja")
    private String targetLanguage;

    @ApiModelProperty(value = "模型名称")
    private String model = "qwen-plus";
}
