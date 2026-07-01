package com.naon.grid.backend.service.dto;

import com.naon.grid.annotation.Query;
import lombok.Data;
import java.io.Serializable;

@Data
public class OrganizationQueryCriteria implements Serializable {

    @Query
    private String auditStatus;

    @Query(blurry = "name,nameEn,contactName,contactEmail")
    private String blurry;
}
