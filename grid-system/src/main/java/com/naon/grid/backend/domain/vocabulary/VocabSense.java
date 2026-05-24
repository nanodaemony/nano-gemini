package com.naon.grid.backend.domain.vocabulary;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseEntity;
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

    @NotNull
    @Column(name = "sense_order", nullable = false)
    @ApiModelProperty(value = "义项排序权重")
    private Integer senseOrder = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;
}
