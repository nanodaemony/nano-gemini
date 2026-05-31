package com.naon.grid.backend.service.vocabulary.dto;

import com.naon.grid.annotation.Query;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class VocabOutlineRecordQueryCriteria implements Serializable {

    @ApiModelProperty(value = "处理状态")
    @Query
    private Integer status;
}
