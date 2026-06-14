package com.naon.grid.backend.service.vocabulary.dto;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VocabSenseDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "自增ID, 义项ID")
    private Integer id;

    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @ApiModelProperty(value = "中文释义")
    private String chineseDef;

    @ApiModelProperty(value = "中文释义音频资源ID")
    private Long defAudioId;

    @ApiModelProperty(value = "中文释义图片资源ID")
    private Long defImageId;

    @ApiModelProperty(value = "释义图片例句ID（对应 example_sentence.id）")
    private Long defImageSentenceId;

    @ApiModelProperty(value = "中文释义外文翻译")
    private List<TextTranslation> defTranslations;

    @ApiModelProperty(value = "中文释义图片例句")
    private ExampleSentenceDto defImageSentence;

    @ApiModelProperty(value = "近义词列表")
    private List<VocabRelationDto> synonymWords;

    @ApiModelProperty(value = "反义词列表")
    private List<VocabRelationDto> antonymWords;

    @ApiModelProperty(value = "正序关联词汇")
    private List<VocabRelationDto> sequentialWords;

    @ApiModelProperty(value = "逆序关联词汇")
    private List<VocabRelationDto> reverseSequentialWords;

    @ApiModelProperty(value = "乱序关联词汇")
    private List<VocabRelationDto> jumbledWords;

    @ApiModelProperty(value = "义项排序权重")
    private Integer senseOrder;

    @ApiModelProperty(value = "搭配列表")
    private List<VocabStructureDto> structures;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
