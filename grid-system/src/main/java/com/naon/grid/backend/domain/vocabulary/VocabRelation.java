package com.naon.grid.backend.domain.vocabulary;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.naon.grid.enums.StatusEnum;
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
@Table(name = "vocab_relation")
public class VocabRelation implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "自增ID, 关联关系ID", hidden = true)
    private Long id;

    @NotNull
    @Column(name = "word_id", nullable = false)
    @ApiModelProperty(value = "词汇ID")
    private Integer wordId;

    @NotNull
    @Column(name = "sense_id", nullable = false)
    @ApiModelProperty(value = "义项ID")
    private Integer senseId;

    @NotBlank
    @Column(name = "word", nullable = false, length = 32)
    @ApiModelProperty(value = "当前词汇")
    private String word;

    @Column(name = "relation_type", length = 32)
    @ApiModelProperty(value = "关联类型，参考枚举：VocabRelationTypeEnum")
    private String relationType;

    @NotNull
    @Column(name = "relation_word_id", nullable = false)
    @ApiModelProperty(value = "关联词汇ID")
    private Long relationWordId;

    @NotNull
    @Column(name = "relation_sense_id", nullable = false)
    @ApiModelProperty(value = "关联义项ID")
    private Long relationSenseId;

    @NotBlank
    @Column(name = "relation_word", nullable = false, length = 32)
    @ApiModelProperty(value = "关联词汇")
    private String relationWord;

    @NotNull
    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "排序权重（大在前）")
    private Integer relationOrder = 0;

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
