package com.naon.grid.backend.domain.topic;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "topic")
public class Topic extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "话题ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 128)
    @ApiModelProperty(value = "话题名称（如"希望"）")
    private String name;

    @Column(name = "pinyin", length = 256)
    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @Column(name = "audio_id")
    @ApiModelProperty(value = "音频资源ID")
    private Long audioId;

    @Column(name = "cover_image_id")
    @ApiModelProperty(value = "封面图片资源ID")
    private Long coverImageId;

    @Column(name = "translations", columnDefinition = "text")
    @ApiModelProperty(value = "话题多语言翻译（JSON）")
    private String translations;

    @Column(name = "draft_content", columnDefinition = "text")
    @ApiModelProperty(value = "草稿内容JSON")
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
