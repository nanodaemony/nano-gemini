package com.naon.grid.backend.domain.charradical;

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
@Table(name = "char_radical")
public class CharRadical extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "部首ID", hidden = true)
    private Long id;

    @Column(name = "radical", nullable = false, length = 10)
    @ApiModelProperty(value = "部首")
    private String radical;

    @Column(name = "radical_name", length = 32)
    @ApiModelProperty(value = "部首名称")
    private String radicalName;

    @Column(name = "stroke_num")
    @ApiModelProperty(value = "笔画数")
    private Integer strokeNum;

    @Column(name = "relation_id")
    @ApiModelProperty(value = "关联部首ID")
    private Long relationId;

    @Column(name = "evolution_desc", length = 2048)
    @ApiModelProperty(value = "演化解说")
    private String evolutionDesc;

    @Column(name = "evolution_desc_translations", columnDefinition = "text")
    @ApiModelProperty(value = "演化解说外文翻译（JSON多语言）")
    private String evolutionDescTranslations;

    @Column(name = "evolution_image_id", length = 255)
    @ApiModelProperty(value = "演化解说图片（路径或资源ID）")
    private String evolutionImageId;

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
