package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VocabStructureDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "自增ID, 结构搭配ID")
    private Integer id;

    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @ApiModelProperty(value = "所属义项ID")
    private Integer senseId;

    @ApiModelProperty(value = "结构搭配文案")
    private String pattern;

    @ApiModelProperty(value = "搭配排序权重")
    private Integer structureOrder;

    @ApiModelProperty(value = "例句列表")
    private List<VocabExampleDto> examples;
}
