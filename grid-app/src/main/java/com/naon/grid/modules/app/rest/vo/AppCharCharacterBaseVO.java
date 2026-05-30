package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 用户端汉字基础VO（不包含审计字段）
 */
@Getter
@Setter
public class AppCharCharacterBaseVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔顺")
    private String stroke;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "汉字说明的多语种翻译")
    private List<TextTranslationVO> descTranslations;
}
