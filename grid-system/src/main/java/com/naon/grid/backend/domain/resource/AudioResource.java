package com.naon.grid.backend.domain.resource;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseEntity;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "audio_resource")
public class AudioResource extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "主键", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "text_content", columnDefinition = "text")
    @ApiModelProperty(value = "音频对应的文字内容")
    private String textContent;

    @NotBlank
    @Column(name = "source_type", nullable = false, length = 50)
    @ApiModelProperty(value = "来源类型: tts/upload")
    private String sourceType;

    @NotBlank
    @Column(name = "file_url", nullable = false, length = 500)
    @ApiModelProperty(value = "音频文件地址")
    private String fileUrl;

    @Column(name = "file_format", length = 20)
    @ApiModelProperty(value = "文件格式: mp3/wav/m4a")
    private String fileFormat = "mp3";

    @Column(name = "file_size")
    @ApiModelProperty(value = "文件大小(字节)")
    private Long fileSize;

    @Column(name = "status")
    @ApiModelProperty(value = "有效状态, 1:有效 0:无效")
    private Integer status = 1;
}
