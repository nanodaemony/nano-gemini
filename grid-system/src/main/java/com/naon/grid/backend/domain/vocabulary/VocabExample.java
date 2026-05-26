package com.naon.grid.backend.domain.vocabulary;

import com.naon.grid.enums.StatusEnum;
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
@Table(name = "vocab_example")
public class VocabExample implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "例句唯一ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "word_id", nullable = false)
    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @NotNull
    @Column(name = "sense_id", nullable = false)
    @ApiModelProperty(value = "所属义项ID")
    private Integer senseId;

    @NotNull
    @Column(name = "structure_id", nullable = false)
    @ApiModelProperty(value = "所属结构搭配ID")
    private Integer structureId;

    @NotBlank
    @Column(name = "sentence", nullable = false, columnDefinition = "text")
    @ApiModelProperty(value = "例句中文文案")
    private String sentence;

    @Column(name = "audio_id")
    @ApiModelProperty(value = "例句音频资源ID")
    private Long audioId;

    @Column(name = "pinyin", length = 500)
    @ApiModelProperty(value = "例句拼音")
    private String pinyin;

    @Column(name = "translations", columnDefinition = "json")
    @ApiModelProperty(value = "例句外文翻译列表")
    private String translations;

    @NotNull
    @Column(name = "example_order", nullable = false)
    @ApiModelProperty(value = "例句排序权重")
    private Integer exampleOrder = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();
}
