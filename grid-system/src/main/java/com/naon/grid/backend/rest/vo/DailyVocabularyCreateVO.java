package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class DailyVocabularyCreateVO implements Serializable {

    @ApiModelProperty(value = "新增的每日一词ID")
    private Integer id;
}
