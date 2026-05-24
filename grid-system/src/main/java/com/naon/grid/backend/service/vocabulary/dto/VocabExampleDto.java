package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;

@Getter
@Setter
public class VocabExampleDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "例句唯一ID")
    private Integer id;

    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @ApiModelProperty(value = "所属义项ID")
    private Integer senseId;

    @ApiModelProperty(value = "所属结构搭配ID")
    private Integer structureId;

    @ApiModelProperty(value = "例句中文文案")
    private String sentence;

    @ApiModelProperty(value = "例句音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "例句拼音")
    private String pinyin;

    @ApiModelProperty(value = "例句外文翻译列表")
    private String translations;

    @ApiModelProperty(value = "例句排序权重")
    private Integer exampleOrder;
}
