package com.naon.grid.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指定源语言翻译响应 DTO
 * @author nano
 * @date 2026-06-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslateDirectResponse {

    @ApiModelProperty(value = "记录 ID")
    private Long recordId;

    @ApiModelProperty(value = "源文本")
    private String sourceText;

    @ApiModelProperty(value = "源语言代码")
    private String sourceLanguage;

    @ApiModelProperty(value = "译文")
    private String targetText;

    @ApiModelProperty(value = "目标语言代码")
    private String targetLanguage;
}
