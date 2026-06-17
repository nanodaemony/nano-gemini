package com.naon.grid.service.dto;

import com.naon.grid.annotation.Query;
import lombok.Data;
import java.io.Serializable;

@Data
public class DynamicConfigQueryCriteria implements Serializable {

    @Query(type = Query.Type.INNER_LIKE)
    private String name;

    @Query(type = Query.Type.INNER_LIKE)
    private String configKey;

    @Query
    private String namespace;
}
