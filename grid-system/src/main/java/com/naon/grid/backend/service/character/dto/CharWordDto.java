package com.naon.grid.backend.service.character.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class CharWordDto implements Serializable {

    @ApiModelProperty(value = "组词唯一ID")
    private Integer id;

    @ApiModelProperty(value = "汉字ID")
    private Integer charId;

    @ApiModelProperty(value = "组词")
    private String wordItem;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @ApiModelProperty(value = "组词翻译")
    private List<TextTranslation> wordItemTranslations;

    @ApiModelProperty(value = "例句")
    private String exampleSentence;

    @ApiModelProperty(value = "例句拼音")
    private String examplePinyin;

    @ApiModelProperty(value = "例句翻译")
    private List<TextTranslation> exampleTranslations;

    @ApiModelProperty(value = "例句图片")
    private String exampleImage;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
