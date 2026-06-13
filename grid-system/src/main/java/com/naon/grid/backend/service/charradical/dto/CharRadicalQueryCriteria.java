package com.naon.grid.backend.service.charradical.dto;

import com.naon.grid.annotation.Query;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class CharRadicalQueryCriteria implements Serializable {

    @ApiModelProperty(value = "部首名称模糊查询")
    @Query(blurry = "radical")
    private String blurry;

    @ApiModelProperty(value = "发布状态: unpublished / published")
    @Query
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft / reviewed / published")
    @Query
    private String editStatus;
}
