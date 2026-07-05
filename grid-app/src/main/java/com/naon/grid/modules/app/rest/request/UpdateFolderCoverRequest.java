package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateFolderCoverRequest {

    @ApiModelProperty(value = "封面图资源ID")
    private Long coverImageId;
}
