package com.naon.grid.backend.domain.grammar;

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
@Table(name = "grammar_structure")
public class GrammarStructure implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "语法结构ID", hidden = true)
    private Long id;

    @NotNull
    @Column(name = "grammar_id", nullable = false)
    private Long grammarId;

    @NotBlank
    @Column(name = "structure_content", nullable = false, length = 1024)
    private String structureContent;

    @Column(name = "structure_sentence_ids", length = 128)
    private String structureSentenceIds;

    @NotNull
    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "排序权重（值大的排前面）")
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
