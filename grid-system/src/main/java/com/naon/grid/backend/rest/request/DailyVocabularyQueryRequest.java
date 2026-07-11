package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
public class DailyVocabularyQueryRequest implements Serializable {

    @ApiModelProperty(value = "词目模糊搜索")
    private String blurry;

    @ApiModelProperty(value = "类型")
    private String phraseType;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "展示日期起")
    private LocalDate displayDateStart;

    @ApiModelProperty(value = "展示日期止")
    private LocalDate displayDateEnd;
}
