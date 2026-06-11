package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 关联词汇请求
 */
@Data
public class VocabRelationVO implements Serializable {

    @ApiModelProperty(value = "关联ID")
    private long relationId;

    @ApiModelProperty(value = "关联类型：近义词(SYNONYMS_WORDS)、反义词(ANTONYMS)、正序词(SEQUENTIAL_WORDS)、逆序词(REVERSE_SEQUENTIAL_WORDS)、乱序词(JUMBLED_WORDS)")
    private String relationType;

    @ApiModelProperty(value = "关联词汇ID")
    private long relationWordId;

    @ApiModelProperty(value = "关联义项ID(一期为空)")
    private long relationSenseId;

    @ApiModelProperty(value = "关联词汇")
    private String relationWord;

    @ApiModelProperty(value = "排序权重")
    private int order;

}
