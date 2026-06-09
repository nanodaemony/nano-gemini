package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.naon.grid.backend.enums.AudioFileFormatEnum;
import com.naon.grid.backend.enums.AudioSourceTypeEnum;
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

    @ApiModelProperty(value = "业务类型（参考 SentenceBizTypeEnum）")
    private String bizType;

    @ApiModelProperty(value = "业务数据ID")
    private Long bizId;

    @ApiModelProperty(value = "例句中文文案")
    private String sentence;

    @ApiModelProperty(value = "例句音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "例句拼音")
    private String pinyin;

    @ApiModelProperty(value = "例句外文翻译列表")
    private List<TextTranslationVO> translations;

    @ApiModelProperty(value = "例句图片ID")
    private Integer imageId;

    @ApiModelProperty(value = "例句排序权重（大在前）")
    private Integer ordinal = 0;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

}
