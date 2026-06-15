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
@Table(name = "grammar_comparison_item")
public class GrammarComparisonItem implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "语法辨析条目ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    @ApiModelProperty(value = "所属辨析组ID")
    private Long groupId;

    @Column(name = "grammar_id", nullable = false)
    @ApiModelProperty(value = "语法点ID")
    private Long grammarId;

    @Column(name = "grammar_name", nullable = false, length = 50)
    @ApiModelProperty(value = "语法点名称（冗余，方便查询和显示）")
    private String grammarName;

    @Column(name = "usage_comparison", length = 2048)
    @ApiModelProperty(value = "用法对比：该语法点与其他语法点的差异说明")
    private String usageComparison;

    @Column(name = "usage_comparison_translations", columnDefinition = "text")
    @ApiModelProperty(value = "用法对比外文翻译（JSON数组）")
    private String usageComparisonTranslations;

    @Column(name = "example_sentences", columnDefinition = "text")
    @ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
    private String exampleSentences;

    @Column(name = "usage_sentence_id")
    @ApiModelProperty(value = "用法例句ID（关联example_sentence表）")
    private Long usageSentenceId;

    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "组内排序权重（大在前）")
    private Integer itemOrder = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private Timestamp updateTime;

    @Column(name = "status")
    private Integer status = StatusEnum.ENABLED.getCode();
}
