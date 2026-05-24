package com.naon.grid.backend.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;

@Getter
@Setter
public class AudioResourceDto extends BaseDTO implements Serializable {

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
}
