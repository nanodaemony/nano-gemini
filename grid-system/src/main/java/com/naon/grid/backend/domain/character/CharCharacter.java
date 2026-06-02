package com.naon.grid.backend.domain.character;

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
@Table(name = "char_character")
public class CharCharacter extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "汉字唯一ID", hidden = true)
    private Integer id;

    @Column(name = "sequence_no")
    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @Column(name = "`character`", length = 10)
    private String character;

    @Column(name = "level", length = 20)
    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @Column(name = "pinyin", length = 100)
    private String pinyin;

    @Column(name = "audio_id")
    private Long audioId;

    @Column(name = "traditional", length = 10)
    private String traditional;

    @Column(name = "radical", length = 10)
    private String radical;

    @Column(name = "stroke", length = 4096)
    private String stroke;

    @Column(name = "char_desc", length = 1024)
    private String charDesc;

    @Column(name = "desc_translations", columnDefinition = "text")
    private String descTranslations;

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
