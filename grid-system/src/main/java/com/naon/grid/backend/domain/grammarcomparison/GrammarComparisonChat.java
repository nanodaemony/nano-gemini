package com.naon.grid.backend.domain.grammarcomparison;

import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "grammar_comparison_chat")
public class GrammarComparisonChat implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "对话ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    @ApiModelProperty(value = "所属语法辨析组ID")
    private Long groupId;

    @Column(name = "role", nullable = false, length = 20)
    @ApiModelProperty(value = "角色: teacher=老师, student=学生")
    private String role;

    @Column(name = "content", nullable = false, length = 1024)
    @ApiModelProperty(value = "中文对话内容")
    private String content;

    @Column(name = "example_sentence_id")
    @ApiModelProperty(value = "对话例句内容（文案、翻译、音频等，对应表example_sentence）")
    private Long exampleSentenceId;

    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "排序权重（大在前）")
    private Integer chatOrder = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private Timestamp updateTime;

    @Column(name = "status")
    private Integer status = StatusEnum.ENABLED.getCode();
}
