package com.naon.grid.backend.domain.vocabulary;

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
@Table(name = "vocab_exercise")
public class VocabExercise implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "练习题目唯一ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "word_id", nullable = false)
    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @NotBlank
    @Column(name = "question_type", nullable = false, length = 20)
    @ApiModelProperty(value = "题目类型")
    private String questionType;

    @NotBlank
    @Column(name = "question_text", nullable = false, columnDefinition = "text")
    @ApiModelProperty(value = "练习题干描述")
    private String questionText;

    @Column(name = "options", columnDefinition = "json")
    @ApiModelProperty(value = "选项列表")
    private String options;

    @Column(name = "answers", columnDefinition = "json")
    @ApiModelProperty(value = "答案列表")
    private String answers;

    @NotNull
    @Column(name = "exercise_order", nullable = false)
    @ApiModelProperty(value = "练习题目排序权重")
    private Integer exerciseOrder = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;
}
