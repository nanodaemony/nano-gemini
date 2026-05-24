package com.naon.grid.backend.service.character.dto;

import com.naon.grid.base.BaseDTO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CharCharacterDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "等级")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔画")
    private String stroke;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "说明翻译")
    private String descTranslations;

    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationDto> discriminations;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordDto> words;
}
