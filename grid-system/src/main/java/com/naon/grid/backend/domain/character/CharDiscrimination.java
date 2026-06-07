package com.naon.grid.backend.domain.character;

import com.naon.grid.enums.StatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @NotNull
    @Column(name = "discrimination_order", nullable = false)
    @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
    private Integer discriminationOrder = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();
}
