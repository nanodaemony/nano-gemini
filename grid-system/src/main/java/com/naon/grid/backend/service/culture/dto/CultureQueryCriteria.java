package com.naon.grid.backend.service.culture.dto;

import com.naon.grid.annotation.Query;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CultureQueryCriteria {

    @Query(blurry = "name")
    private String blurry;

    @Query
    private String publishStatus;

    @Query
    private String editStatus;

    @Query
    private String level;

    @Query
    private String project;

    @Query
    private String category;
}
