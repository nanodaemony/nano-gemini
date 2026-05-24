package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.naon.grid.annotation.Query;
import java.io.Serializable;

@Data
public class VocabWordQueryCriteria implements Serializable {

    @ApiModelProperty(value = "词汇模糊查询")
    @Query(blurry = "word")
    private String blurry;
}
