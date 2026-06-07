package com.naon.grid.backend.domain.vocabulary;

import com.naon.grid.enums.StatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "vocab_sense")
public class VocabSense implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "自增ID, 义项ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "word_id", nullable = false)
    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @Column(name = "part_of_speech", length = 50)
    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @Column(name = "chinese_def", columnDefinition = "text")
    @ApiModelProperty(value = "中文释义")
    private String chineseDef;

    @Column(name = "def_audio_id")
    @ApiModelProperty(value = "中文释义音频资源ID")
    private Long defAudioId;

    @Column(name = "def_image")
    @ApiModelProperty(value = "中文释义图片资源ID")
    private Long defImage;

    @Column(name = "translations", columnDefinition = "json")
    @ApiModelProperty(value = "外文翻译列表")
    private String translations;

    @Column(name = "synonyms", columnDefinition = "text")
    @ApiModelProperty(value = "近义词列表")
    private String synonyms;

    @Column(name = "antonyms", columnDefinition = "text")
    @ApiModelProperty(value = "反义词列表")
    private String antonyms;

    @Column(name = "related_forward", columnDefinition = "text")
    @ApiModelProperty(value = "正序关联词汇")
    private String relatedForward;

    @Column(name = "related_backward", columnDefinition = "text")
    @ApiModelProperty(value = "逆序关联词汇")
    private String relatedBackward;

    @Column(name = "related_other", columnDefinition = "text")
    @ApiModelProperty(value = "其他关联词汇")
    private String relatedOther;

    @NotNull
    @Column(name = "sense_order", nullable = false)
    @ApiModelProperty(value = "义项排序权重")
    private Integer senseOrder = 0;

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
