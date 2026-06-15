package com.naon.grid.backend.service.grammar.dto;

import com.naon.grid.base.BaseDTO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class GrammarPointDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "语法点ID")
    private Long id;

    @ApiModelProperty(value = "语法点名称")
    private String name;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "项目")
    private String project;

    @ApiModelProperty(value = "类别")
    private String category;

    @ApiModelProperty(value = "细目")
    private String subCategory;

    @ApiModelProperty(value = "语法意义列表")
    private List<GrammarMeaningDto> meanings;

    @ApiModelProperty(value = "语法结构列表")
    private List<GrammarStructureDto> structures;

    @ApiModelProperty(value = "语法注意列表")
    private List<GrammarNoticeDto> notices;

    @ApiModelProperty(value = "语法偏误列表")
    private List<GrammarErrorDto> errors;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;

    // ===== 列表统计字段 =====
    @ApiModelProperty(value = "意义个数")
    private Integer meaningCount;

    @ApiModelProperty(value = "结构个数")
    private Integer structureCount;

    @ApiModelProperty(value = "注意个数")
    private Integer noticeCount;

    @ApiModelProperty(value = "偏误个数")
    private Integer errorCount;
}
