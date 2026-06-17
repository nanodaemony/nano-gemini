package com.naon.grid.service.dto;

import com.naon.grid.base.BaseDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DynamicConfigDto extends BaseDTO {

    private Long id;
    private String namespace;
    private String name;
    private String configKey;
    private String value;
    private String description;
    private Integer status;
}
