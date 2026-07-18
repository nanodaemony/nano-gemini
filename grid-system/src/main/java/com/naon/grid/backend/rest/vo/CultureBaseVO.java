package com.naon.grid.backend.rest.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class CultureBaseVO implements Serializable {

    private Long id;
    private String name;
    private String pinyin;
    private Long audioId;
    private Long coverImageId;
    private String level;
    private String project;
    private String category;
    private String publishStatus;
    private String editStatus;
    private Integer keywordCount;
    private Integer sentenceCount;
    private Integer questionCount;
    private String createBy;
    private String updateBy;
    private Timestamp createTime;
    private Timestamp updateTime;
    private List<TextTranslationVO> translations;
}
