package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

/**
 * 例句信息VO
 */
@Getter
@Setter
public class ExampleSentenceVO implements Serializable {

    @ApiModelProperty(value = "例句ID")
    private Long id;

    @ApiModelProperty(value = "例句中文文案")
    private String sentence;

    @ApiModelProperty(value = "例句拼音")
    private String pinyin;

    @ApiModelProperty(value = "例句音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "例句外文翻译")
    private List<TextTranslationVO> translations;

    @ApiModelProperty(value = "例句图片ID")
    private Long imageId;

    @ApiModelProperty(value = "排序权重")
    private Integer order;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;

    @ApiModelProperty(value = "已人工审核的AI字段名列表（是 aiGeneratedFields 的子集）")
    private List<String> aiReviewedFields;
}