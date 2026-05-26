package com.naon.grid.backend.domain.vocabulary;

import com.naon.grid.enums.StatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "vocab_word")
public class VocabWord implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "词汇唯一ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(name = "word", nullable = false, length = 50)
    @ApiModelProperty(value = "词汇")
    private String word;

    @Column(name = "word_traditional", length = 50)
    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @NotBlank
    @Column(name = "pinyin", nullable = false, length = 100)
    @ApiModelProperty(value = "标准拼音（含声调）")
    private String pinyin;

    @Column(name = "audio_id")
    @ApiModelProperty(value = "词汇读音音频资源ID")
    private Long audioId;

    @Column(name = "hsk_level", length = 20)
    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();
}
