package com.naon.grid.backend.service.grammar.dto;

import com.naon.grid.annotation.Query;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class GrammarPointQueryCriteria implements Serializable {

    @ApiModelProperty(value = "语法点名称模糊查询")
    @Query(blurry = "name")
    private String blurry;

    @ApiModelProperty(value = "发布状态")
    @Query
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    @Query
    private String editStatus;

    @ApiModelProperty(value = "HSK等级")
    @Query
    private String hskLevel;

    @ApiModelProperty(value = "类别")
    @Query
    private String category;
}
