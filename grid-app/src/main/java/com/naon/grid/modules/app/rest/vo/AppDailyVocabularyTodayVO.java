package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppDailyVocabularyTodayVO implements Serializable {

    @ApiModelProperty(value = "今日主推")
    private AppDailyVocabularyDetailVO main;

    @ApiModelProperty(value = "备选池")
    private List<AppDailyVocabularyDetailVO> backups;
}
