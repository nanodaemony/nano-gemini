package com.naon.grid.rest.request;

import lombok.Data;

@Data
public class DynamicConfigQueryRequest {
    private String namespace;
    private String name;
    private String configKey;
}
