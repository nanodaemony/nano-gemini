package com.naon.grid.backend.domain.vocabulary;

import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "vocab_book")
public class VocabBook implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "词汇书ID", hidden = true)
    private Long id;

    @Column(name = "type", nullable = false, length = 32)
    @ApiModelProperty(value = "词汇书类型")
    private String type;

    @Column(name = "name", nullable = false, length = 32)
    @ApiModelProperty(value = "词汇书名称")
    private String name;

    @Column(name = "sub_name", nullable = false, length = 32)
    @ApiModelProperty(value = "词汇书子名称")
    private String subName;

    @Column(name = "cover_image", nullable = false, length = 512)
    @ApiModelProperty(value = "词汇书封面图")
    private String coverImage;

    @Column(name = "`desc`", length = 1024)
    @ApiModelProperty(value = "词汇书描述")
    private String desc;

    @Column(name = "hsk_level", length = 32)
    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @Column(name = "word_ids", columnDefinition = "text")
    @ApiModelProperty(value = "词汇ID列表JSON")
    private String wordIds;

    @Column(name = "`order`")
    @ApiModelProperty(value = "排序(值大的排前面)")
    private Integer order;

    @Column(name = "create_time")
    @ApiModelProperty(value = "创建时间", hidden = true)
    private Timestamp createTime;

    @Column(name = "update_time")
    @ApiModelProperty(value = "更新时间", hidden = true)
    private Timestamp updateTime;

    @Column(name = "status")
    @ApiModelProperty(value = "有效状态：1-有效，0-无效", hidden = true)
    private Integer status = StatusEnum.ENABLED.getCode();
}
