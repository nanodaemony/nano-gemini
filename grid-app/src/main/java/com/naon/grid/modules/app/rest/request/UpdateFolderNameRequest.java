package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class UpdateFolderNameRequest {

    @NotBlank(message = "收藏夹名称不能为空")
    @Size(max = 32, message = "收藏夹名称最多32个字符")
    @ApiModelProperty(value = "新名称", required = true)
    private String name;
}
