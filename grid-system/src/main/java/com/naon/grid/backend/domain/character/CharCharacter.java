package com.naon.grid.backend.domain.character;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "char_character")
public class CharCharacter implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "汉字唯一ID", hidden = true)
    private Integer id;

    @Column(name = "sequence_no")
    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @NotBlank
    @Column(name = "`character`", nullable = false, length = 10)
    private String character;

    @Column(name = "level", length = 20)
    private String level;

    @NotBlank
    @Column(name = "pinyin", nullable = false, length = 100)
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

    @Column(name = "create_time", insertable = false, updatable = false)
    private Timestamp createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private Timestamp updateTime;
}
