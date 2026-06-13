package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
public class VocabWordBaseVO implements Serializable {

    @ApiModelProperty(value = "词汇唯一ID")
    private Integer id;

    @ApiModelProperty(value = "词汇", required = true)
    private String word;

    @ApiModelProperty(value = "词汇读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "义项数量")
    private int senseCount;

    @ApiModelProperty(value = "结构数量")
    private int structureCount;

    @ApiModelProperty(value = "翻译状态: generated=已生成, not_generated=未生成")
    private String translationStatus;

    @ApiModelProperty(value = "拼音状态: generated=已生成, not_generated=未生成")
    private String pinyinStatus;

    @ApiModelProperty(value = "音频状态: generated=已生成, not_generated=未生成")
    private String audioStatus;

    @ApiModelProperty(value = "配图状态: generated=已生成, not_generated=未生成")
    private String imageStatus;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;
}
