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
@Table(name = "char_word")
public class CharWord implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "组词唯一ID", hidden = true)
    private Integer id;

    @NotNull
    @Column(name = "char_id", nullable = false)
    private Integer charId;

    @NotBlank
    @Column(name = "word_item", nullable = false, length = 50)
    private String wordItem;

    @Column(name = "level", length = 20)
    private String level;

    @Column(name = "pinyin", length = 100)
    private String pinyin;

    @Column(name = "part_of_speech", length = 50)
    private String partOfSpeech;

    @Column(name = "word_item_translations", columnDefinition = "text")
    private String wordItemTranslations;

    @Column(name = "example_sentence", columnDefinition = "text")
    private String exampleSentence;

    @Column(name = "example_pinyin", length = 500)
    private String examplePinyin;

    @Column(name = "example_translations", columnDefinition = "text")
    private String exampleTranslations;

    @Column(name = "example_image", length = 255)
    private String exampleImage;

    @Column(name = "create_time", insertable = false, updatable = false)
    private Timestamp createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private Timestamp updateTime;
}
