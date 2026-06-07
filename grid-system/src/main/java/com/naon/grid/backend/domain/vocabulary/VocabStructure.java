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
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "vocab_structure")
public class VocabStructure implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "自增ID, 结构搭配ID", hidden = true)
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

    @NotBlank
    @Column(name = "pattern", nullable = false, length = 255)
    @ApiModelProperty(value = "结构搭配文案")
    private String pattern;

    @Column(name = "pattern_def", length = 512)
    @ApiModelProperty(value = "结构搭配释义")
    private String patternDef;

    @Column(name = "pattern_def_translations", length = 1024)
    @ApiModelProperty(value = "结构搭配释义外文翻译（JSON）")
    private String patternDefTranslations;

    @NotNull
    @Column(name = "structure_order", nullable = false)
    @ApiModelProperty(value = "搭配排序权重")
    private Integer structureOrder = 0;

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
