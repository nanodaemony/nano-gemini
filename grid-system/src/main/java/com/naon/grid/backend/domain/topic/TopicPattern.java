package com.naon.grid.backend.domain.topic;

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
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "topic_pattern")
public class TopicPattern implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "句式ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_id", nullable = false)
    @ApiModelProperty(value = "所属话题ID")
    private Long topicId;

    @Column(name = "pattern", nullable = false, length = 512)
    @ApiModelProperty(value = "句式文本（如"（某人）+希望……"）")
    private String pattern;

    @Column(name = "image_id")
    @ApiModelProperty(value = "句式示意图资源ID")
    private Long imageId;

    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "组内排序权重（大的在前）")
    private Integer patternOrder = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private Timestamp updateTime;

    @Column(name = "status")
    private Integer status = StatusEnum.ENABLED.getCode();
}
