package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UpdateAvatarRequest {

    @NotNull(message = "图片ID不能为空")
    private Long ossImageId;
}
