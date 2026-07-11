package com.naon.grid.backend.service.vocabulary.dto;

import com.naon.grid.annotation.Query;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class DailyVocabularyQueryCriteria implements Serializable {

    @ApiModelProperty(value = "词目模糊查询")
    @Query(blurry = "phrase")
    private String blurry;

    @ApiModelProperty(value = "类型")
    @Query
    private String phraseType;

    @ApiModelProperty(value = "发布状态")
    @Query
    private String publishStatus;

    @ApiModelProperty(value = "展示日期起始")
    @Query(propName = "displayDate", type = Query.Type.GREATER_THAN_OR_EQUAL)
    private LocalDate displayDateStart;

    @ApiModelProperty(value = "展示日期截止")
    @Query(propName = "displayDate", type = Query.Type.LESS_THAN_OR_EQUAL)
    private LocalDate displayDateEnd;

    @ApiModelProperty(value = "仅查已发布（App端使用）")
    private Boolean publishedOnly;
}
