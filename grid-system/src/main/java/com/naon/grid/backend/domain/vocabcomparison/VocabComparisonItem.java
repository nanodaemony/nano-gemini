package com.naon.grid.backend.domain.vocabcomparison;

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
@Table(name = "vocab_comparison_item")
public class VocabComparisonItem implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "词汇辨析条目ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    @ApiModelProperty(value = "所属辨析组ID")
    private Long groupId;

    @Column(name = "word_id", nullable = false)
    @ApiModelProperty(value = "词汇ID")
    private Long wordId;

    @Column(name = "word", nullable = false, length = 50)
    @ApiModelProperty(value = "词汇词头（冗余字段）")
    private String word;

    @Column(name = "part_of_speech", length = 50)
    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @Column(name = "usage_comparison", length = 512)
    @ApiModelProperty(value = "用法对比")
    private String usageComparison;

    @Column(name = "usage_comparison_translations", columnDefinition = "text")
    @ApiModelProperty(value = "用法对比外文翻译")
    private String usageComparisonTranslations;

    @Column(name = "common_usage", length = 512)
    @ApiModelProperty(value = "通用用法")
    private String commonUsage;

    @Column(name = "common_usage_translations", columnDefinition = "text")
    @ApiModelProperty(value = "通用用法外文翻译")
    private String commonUsageTranslations;

    @Column(name = "example_sentences", columnDefinition = "text")
    @ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
    private String exampleSentences;

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
