package com.naon.grid.backend.service.culture.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
public class CultureKeywordDto implements Serializable {

    private Long id;
    private Long cultureId;
    private String keyword;
    private String keywordDescription;
    private String keywordTranslations;
    private String keywordDescriptionTranslations;
    private Long audioId;
    private Long imageId;
    private Integer order;
    private Timestamp createTime;
    private Timestamp updateTime;
}
