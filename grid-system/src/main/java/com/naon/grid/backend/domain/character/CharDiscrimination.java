package com.naon.grid.backend.domain.character;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "char_discrimination")
public class CharDiscrimination implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "辨析唯一ID", hidden = true)
    private Integer id;

    @NotNull
    @Column(name = "char_id", nullable = false)
    private Integer charId;

    @NotBlank
    @Column(name = "discrim_char", nullable = false, length = 10)
    private String discrimChar;

    @Column(name = "discrim_pinyin", length = 100)
    private String discrimPinyin;

    @Column(name = "discrim_char_translations", columnDefinition = "text")
    private String discrimCharTranslations;

    @Column(name = "comparison_translations", columnDefinition = "text")
    private String comparisonTranslations;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;
}
