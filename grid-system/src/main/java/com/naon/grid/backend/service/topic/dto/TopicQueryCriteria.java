package com.naon.grid.backend.service.topic.dto;

import com.naon.grid.annotation.Query;
import lombok.Data;

@Data
public class TopicQueryCriteria {

    @Query(blurry = "name")
    private String blurry;

    @Query
    private String publishStatus;

    @Query
    private String editStatus;
}
