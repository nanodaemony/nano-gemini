package com.naon.grid.backend.domain.culture;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "culture")
public class Culture extends BaseEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "文化点ID", hidden = true)
    private Long id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "pinyin", length = 256)
    private String pinyin;

    @Column(name = "audio_id")
    private Long audioId;

    @Column(name = "translations", columnDefinition = "text")
    private String translations;

    @Column(name = "cover_image_id")
    private Long coverImageId;

    @Column(name = "level", length = 20)
    private String level;

    @Column(name = "project", length = 50)
    private String project;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "one_sentence_intro", length = 1024)
    private String oneSentenceIntro;

    @Column(name = "one_sentence_intro_translations", columnDefinition = "text")
    private String oneSentenceIntroTranslations;

    @Column(name = "one_sentence_intro_audio_id")
    private Long oneSentenceIntroAudioId;

    @Column(name = "one_sentence_intro_image_id")
    private Long oneSentenceIntroImageId;

    @Column(name = "detailed_intro", columnDefinition = "text")
    private String detailedIntro;

    @Column(name = "detailed_intro_translations", columnDefinition = "text")
    private String detailedIntroTranslations;

    @Column(name = "detailed_intro_audio_id")
    private Long detailedIntroAudioId;

    @Column(name = "detailed_intro_image_id")
    private Long detailedIntroImageId;

    @Column(name = "sentence_ids", columnDefinition = "text")
    private String sentenceIds;

    @Column(name = "question_ids", columnDefinition = "text")
    private String questionIds;

    @Column(name = "draft_content", columnDefinition = "text")
    @ApiModelProperty(value = "草稿内容JSON（包含主表和子表）")
    private String draftContent;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();

    @Column(name = "publish_status", length = 20)
    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus = PublishStatusEnum.UNPUBLISHED.getCode();

    @Column(name = "edit_status", length = 20)
    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus = EditStatusEnum.DRAFT.getCode();
}
