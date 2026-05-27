package com.naon.grid.backend.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
public class AudioResourceDto implements Serializable {

    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "音频对应的文字内容")
    private String textContent;

    @ApiModelProperty(value = "来源类型: tts/upload")
    private String sourceType;

    @ApiModelProperty(value = "音频文件地址")
    private String fileUrl;

    @ApiModelProperty(value = "文件格式: mp3/wav/m4a")
    private String fileFormat;

    @ApiModelProperty(value = "文件大小(字节)")
    private Long fileSize;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;
}
