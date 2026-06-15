package com.naon.grid.backend.domain.grammar;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "grammar_point")
public class GrammarPoint extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "语法点ID", hidden = true)
    private Long id;

    @NotBlank
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "hsk_level", length = 20)
    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @Column(name = "project", length = 20)
    @ApiModelProperty(value = "项目")
    private String project;

    @Column(name = "category", length = 50)
    @ApiModelProperty(value = "类别")
    private String category;

    @Column(name = "sub_category", length = 50)
    @ApiModelProperty(value = "细目")
    private String subCategory;

    @Column(name = "draft_content", columnDefinition = "text")
    @ApiModelProperty(value = "草稿内容JSON（包含主表和子表）")
    private String draftContent;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();

    @Column(name = "publish_status", length = 20)
    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus = PublishStatusEnum.UNPUBLISHED.getCode();

    @Column(name = "edit_status", length = 20)
    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus = EditStatusEnum.DRAFT.getCode();
}
