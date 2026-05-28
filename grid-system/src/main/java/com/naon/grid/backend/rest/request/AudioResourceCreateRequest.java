package com.naon.grid.backend.rest.request;

import com.naon.grid.backend.enums.AudioFileFormatEnum;
import com.naon.grid.backend.enums.AudioSourceTypeEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Getter
@Setter
public class AudioResourceCreateRequest implements Serializable {

    @NotBlank(message = "textContent不能为空")
    @ApiModelProperty(value = "音频对应的文字内容", required = true)
    private String textContent;

    @NotNull(message = "sourceType不能为空")
    @ApiModelProperty(value = "来源类型: tts/upload", required = true)
    private AudioSourceTypeEnum sourceType;

    @NotBlank(message = "fileUrl不能为空")
    @ApiModelProperty(value = "音频文件地址", required = true)
    private String fileUrl;

    @ApiModelProperty(value = "文件格式: mp3/wav/m4a")
    private AudioFileFormatEnum fileFormat = AudioFileFormatEnum.MP3;

    @ApiModelProperty(value = "文件大小(字节)")
    private Long fileSize;
}
