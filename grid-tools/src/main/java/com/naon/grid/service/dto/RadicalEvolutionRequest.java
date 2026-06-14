package com.naon.grid.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 部首演化图生成请求 DTO
 * @author nano
 * @date 2026-06-14
 */
@Data
public class RadicalEvolutionRequest {

    @NotBlank(message = "部首不能为空")
    @ApiModelProperty(value = "部首", required = true, example = "人")
    private String radical;

    @NotBlank(message = "演化解说不能为空")
    @ApiModelProperty(value = "部首演化历史文案", required = true,
            example = "「人」是象形字，古文字模拟侧立的人形。「人」部的字多与跟人和人的活动相关，如称谓、德行、行为等。")
    private String evolutionDesc;
}
