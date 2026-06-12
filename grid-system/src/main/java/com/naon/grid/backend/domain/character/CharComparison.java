package com.naon.grid.backend.domain.character;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "char_comparison")
public class CharComparison implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "辨析记录ID", hidden = true)
    private Integer id;

    @NotNull
    @Column(name = "char_id", nullable = false)
    private Integer charId;

    @NotBlank
    @Column(name = "comparison_char", nullable = false, length = 10)
    private String comparisonChar;

    @Column(name = "comparison_pinyin", length = 100)
    private String comparisonPinyin;

    @Column(name = "comparison_char_translations", columnDefinition = "text")
    private String comparisonCharTranslations;

    @Column(name = "comparison_desc_translations", columnDefinition = "text")
    private String comparisonDescTranslations;

    @NotNull
    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
    private Integer comparisonOrder = 0;

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
