package com.naon.grid.backend.domain.culture;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "culture_keyword")
public class CultureKeyword implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "关键词ID", hidden = true)
    private Long id;

    @Column(name = "culture_id", nullable = false)
    private Long cultureId;

    @Column(name = "keyword", nullable = false, length = 128)
    private String keyword;

    @Column(name = "keyword_description", columnDefinition = "text")
    private String keywordDescription;

    @Column(name = "keyword_translations", columnDefinition = "text")
    private String keywordTranslations;

    @Column(name = "keyword_description_translations", columnDefinition = "text")
    private String keywordDescriptionTranslations;

    @Column(name = "audio_id")
    private Long audioId;

    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "排序权重（值大的排前面）")
    private Integer order = 0;

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
