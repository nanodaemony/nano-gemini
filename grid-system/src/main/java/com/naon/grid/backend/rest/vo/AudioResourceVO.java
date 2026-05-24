package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
public class AudioResourceVO implements Serializable {

    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "业务类型")
    private String bizType;

    @ApiModelProperty(value = "音频对应的文字内容")
    private String textContent;

    @ApiModelProperty(value = "来源类型")
    private String sourceType;

    @ApiModelProperty(value = "音频文件地址")
    private String fileUrl;

    @ApiModelProperty(value = "文件格式")
    private String fileFormat;

    @ApiModelProperty(value = "文件大小(字节)")
    private Long fileSize;

    @ApiModelProperty(value = "关联的TTS记录ID")
    private Long ttsRecordId;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "修改人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;
}
