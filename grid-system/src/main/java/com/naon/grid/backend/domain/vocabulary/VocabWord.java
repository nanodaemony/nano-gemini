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

@Entity
@Getter
@Setter
@Table(name = "vocab_word")
public class VocabWord extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "词汇唯一ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(name = "word", nullable = false, length = 50)
    @ApiModelProperty(value = "词汇")
    private String word;

    @Column(name = "word_traditional", length = 50)
    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @Column(name = "pinyin", length = 100)
    @ApiModelProperty(value = "标准拼音（含声调）")
    private String pinyin;

    @Column(name = "audio_id")
    @ApiModelProperty(value = "词汇读音音频资源ID")
    private Long audioId;

    @Column(name = "hsk_level", length = 20)
    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String hskLevel;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();

    @Column(name = "publish_status", length = 20)
    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus = PublishStatusEnum.UNPUBLISHED.getCode();

    @Column(name = "edit_status", length = 20)
    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus = EditStatusEnum.DRAFT.getCode();

    @Column(name = "draft_content", columnDefinition = "json")
    @ApiModelProperty(value = "草稿内容JSON（包含主表和子表）")
    private String draftContent;
}
