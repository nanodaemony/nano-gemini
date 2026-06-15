package com.naon.grid.backend.domain.grammarcomparison;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "grammar_comparison_group")
public class GrammarComparisonGroup extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "语法辨析组ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_key", nullable = false, length = 64)
    @ApiModelProperty(value = "辨析组标识（如'会vs能'），查询时直接返回此字段作为对比头")
    private String groupKey;

    @Column(name = "exercise_question_ids", length = 256)
    @ApiModelProperty(value = "练习题ID列表JSON")
    private String exerciseQuestionIds;

    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "组排序权重（大在前）")
    private Integer groupOrder = 0;

    @Column(name = "draft_content", columnDefinition = "text")
    @ApiModelProperty(value = "草稿内容JSON")
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
