package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppDailyVocabularyShareImageRequest implements Serializable {

    @ApiModelProperty(value = "分享图资源ID", required = true)
    private Long imageId;
}
