package com.naon.grid.rest.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class DynamicConfigCreateRequest {
    @NotBlank(message = "namespace 不能为空")
    private String namespace;

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotBlank(message = "configKey 不能为空")
    private String configKey;

    private String value;
    private String description;
}
