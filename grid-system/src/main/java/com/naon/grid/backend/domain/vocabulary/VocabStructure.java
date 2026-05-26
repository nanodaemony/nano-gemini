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

    @NotNull
    @Column(name = "structure_order", nullable = false)
    @ApiModelProperty(value = "搭配排序权重")
    private Integer structureOrder = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();
}
