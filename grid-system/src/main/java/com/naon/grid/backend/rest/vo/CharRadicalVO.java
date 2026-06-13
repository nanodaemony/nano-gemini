package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class CharRadicalVO {

    @ApiModelProperty(value = "部首ID")
    private Long id;

    @ApiModelProperty(value = "部首名称")
    private String radical;

    @ApiModelProperty(value = "笔画数")
    private Integer strokeNum;

    @ApiModelProperty(value = "演化解说")
    private String evolutionDesc;

    @ApiModelProperty(value = "演化解说外文翻译")
    private List<TextTranslationVO> evolutionDescTranslations;

    @ApiModelProperty(value = "演化解说图片（路径或资源ID）")
    private String evolutionImageId;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
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
