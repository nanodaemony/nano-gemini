package com.naon.grid.backend.service.common.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class ExampleSentenceDto implements Serializable {

    @ApiModelProperty(value = "例句ID")
    private Long id;

    @ApiModelProperty(value = "例句中文文案")
    private String sentence;

    @ApiModelProperty(value = "例句拼音")
    private String pinyin;

    @ApiModelProperty(value = "例句音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "例句外文翻译")
    private List<TextTranslation> translations;

    @ApiModelProperty(value = "例句图片ID")
    private Long imageId;

    @ApiModelProperty(value = "排序权重")
    private Integer order;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "有效状态")
    private Integer status;
}