package com.naon.grid.backend.rest.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CultureQueryRequest {

    private String blurry;
    private String publishStatus;
    private String editStatus;
    private String level;
    private String project;
    private String category;
}
