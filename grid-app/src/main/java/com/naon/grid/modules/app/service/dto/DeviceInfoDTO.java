package com.naon.grid.modules.app.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DeviceInfoDTO {
    @ApiModelProperty(value = "操作系统")
    private String os;
    @ApiModelProperty(value = "系统版本")
    private String version;
    @ApiModelProperty(value = "设备型号")
    private String model;
    @ApiModelProperty(value = "应用版本")
    private String appVersion;
}
