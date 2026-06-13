package com.naon.grid.backend.domain.character;

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
@Table(name = "char_stroke")
public class CharStroke implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "汉字笔顺ID", hidden = true)
    private Long id;

    @Column(name = "`character`", nullable = false, length = 32)
    @ApiModelProperty(value = "汉字")
    private String character;

    @Column(name = "stroke", columnDefinition = "text")
    @ApiModelProperty(value = "汉字笔顺JSON（hanzi-writer格式）")
    private String stroke;

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
