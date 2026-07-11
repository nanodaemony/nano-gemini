package com.naon.grid.backend.domain.vocabulary;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "daily_vocabulary")
public class DailyVocabulary extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "每日一词唯一ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(name = "phrase", nullable = false, length = 100)
    @ApiModelProperty(value = "词目")
    private String phrase;

    @Column(name = "phrase_type", length = 20)
    @ApiModelProperty(value = "类型枚举")
    private String phraseType;

    @Column(name = "pinyin", length = 200)
    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @Column(name = "phrase_translations", columnDefinition = "text")
    @ApiModelProperty(value = "词目翻译列表JSON")
    private String phraseTranslations;

    @Column(name = "audio_id")
    @ApiModelProperty(value = "发音音频ID")
    private Long audioId;

    @Column(name = "image_id")
    @ApiModelProperty(value = "AI配图ID")
    private Long imageId;

    @Column(name = "plain_explanation", length = 1024)
    @ApiModelProperty(value = "通俗中文讲解")
    private String plainExplanation;

    @Column(name = "explanation_translations", columnDefinition = "text")
    @ApiModelProperty(value = "讲解翻译列表JSON")
    private String explanationTranslations;

    @Column(name = "origin_story", columnDefinition = "text")
    @ApiModelProperty(value = "出处/典故/背景故事")
    private String originStory;

    @Column(name = "example_sentence_id")
    @ApiModelProperty(value = "例句ID")
    private Long exampleSentenceId;

    @Column(name = "display_date")
    @ApiModelProperty(value = "计划展示日期")
    private LocalDate displayDate;

    @Column(name = "`order`")
    @ApiModelProperty(value = "同日期排序权重")
    private Integer order;

    @Column(name = "related_word_id")
    @ApiModelProperty(value = "关联词汇ID")
    private Long relatedWordId;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();

    @Column(name = "publish_status", length = 20)
    @ApiModelProperty(value = "发布状态")
    private String publishStatus = PublishStatusEnum.UNPUBLISHED.getCode();

    @Column(name = "edit_status", length = 20)
    @ApiModelProperty(value = "编辑状态")
    private String editStatus = EditStatusEnum.DRAFT.getCode();

    @Column(name = "draft_content", columnDefinition = "text")
    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;
}
