package com.naon.grid.backend.domain.resource;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseEntity;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "audio_resource")
public class AudioResource implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "主键", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "biz_type", nullable = false, length = 50)
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    @Column(name = "text_content", columnDefinition = "text")
    @ApiModelProperty(value = "音频对应的文字内容")
    private String textContent;

    @NotBlank
    @Column(name = "source_type", nullable = false, length = 50)
    @ApiModelProperty(value = "来源类型")
    private String sourceType;

    @NotBlank
    @Column(name = "file_url", nullable = false, length = 500)
    @ApiModelProperty(value = "音频文件地址")
    private String fileUrl;

    @Column(name = "file_format", length = 20)
    @ApiModelProperty(value = "文件格式")
    private String fileFormat = "mp3";

    @Column(name = "file_size")
    @ApiModelProperty(value = "文件大小(字节)")
    private Long fileSize;

    @Column(name = "tts_record_id")
    @ApiModelProperty(value = "关联的TTS记录ID")
    private Long ttsRecordId;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;
}
