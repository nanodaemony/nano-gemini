package com.naon.grid.backend.service.vocabcomparison.dto;

import com.naon.grid.base.BaseDTO;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VocabComparisonGroupDto extends BaseDTO {

    @ApiModelProperty(value = "辨析组ID")
    private Long id;

    @ApiModelProperty(value = "辨析组标识")
    private String groupKey;

    @ApiModelProperty(value = "练习题ID列表JSON")
    private String exerciseQuestionIds;

    @ApiModelProperty(value = "组排序权重")
    private Integer groupOrder;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核, published=已发布")
    private String editStatus;

    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;

    // === 子列表 ===

    @ApiModelProperty(value = "条目列表")
    private List<VocabComparisonItemDto> items;

    @ApiModelProperty(value = "情景对话列表")
    private List<VocabComparisonChatDto> chats;

    // === 列表统计字段 ===

    @ApiModelProperty(value = "条目数量")
    private Integer itemCount;
}
