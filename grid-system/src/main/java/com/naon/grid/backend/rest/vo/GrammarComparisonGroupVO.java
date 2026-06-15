package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class GrammarComparisonGroupVO {

    @ApiModelProperty(value = "辨析组ID")
    private Long id;

    @ApiModelProperty(value = "辨析组标识")
    private String groupKey;

    @ApiModelProperty(value = "练习题ID列表JSON")
    private String exerciseQuestionIds;

    @ApiModelProperty(value = "组排序权重")
    private Integer groupOrder;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "条目列表")
    private List<GrammarComparisonItemVO> items;

    @ApiModelProperty(value = "情景对话列表")
    private List<GrammarComparisonChatVO> chats;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
