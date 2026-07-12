package com.naon.grid.modules.system.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "ai_content_marker")
public class AiContentMarker implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    @ApiModelProperty(value = "实体表名")
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    @ApiModelProperty(value = "实体记录ID")
    private Long entityId;

    @Column(name = "field_name", nullable = false, length = 255)
    @ApiModelProperty(value = "Java字段名(驼峰)")
    private String fieldName;

    @Column(name = "ai_generated", nullable = false)
    @ApiModelProperty(value = "1=AI生成 0=人工")
    private Integer aiGenerated = 1;
}
