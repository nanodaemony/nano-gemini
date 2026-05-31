package com.naon.grid.backend.domain.vocabulary;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@Table(name = "vocab_outline_record")
public class VocabOutlineRecord implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "主键ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "word", nullable = false, length = 50)
    @ApiModelProperty(value = "词汇文本")
    private String word;

    @Column(name = "search_count", nullable = false)
    @ApiModelProperty(value = "未搜到次数")
    private Integer searchCount = 1;

    @Column(name = "status")
    @ApiModelProperty(value = "处理状态, 0:未处理 1:已处理")
    private Integer status = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;
}
