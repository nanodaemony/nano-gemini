package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CharCharacterCreateRequest implements Serializable {

    @NotBlank
    @ApiModelProperty(value = "汉字", required = true)
    private String character;

    @ApiModelProperty(value = "HSK等级，参考枚举：HskLevelEnum")
    private String hskLevel;

    @ApiModelProperty(value = "拼音", required = true)
    private String pinyin;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "音频资源ID")
    private long audioId;

    @ApiModelProperty(value = "部首ID")
    private String radicalId;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "部件组合")
    private String componentCombination;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "说明翻译")
    private List<TextTranslationRequest> charDescTranslations;

    @ApiModelProperty(value = "笔画")
    private String stroke;

    @Valid
    @ApiModelProperty(value = "汉字辨析列表")
    private List<CharComparisonRequest> comparisons;

    @Valid
    @ApiModelProperty(value = "汉字组词列表")
    private List<CharWordRequest> words;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @Getter
    @Setter
    public static class CharComparisonRequest implements Serializable {

        @ApiModelProperty(value = "辨析ID, 新增时不传, 更新时传")
        private Integer id;

        @NotBlank
        @ApiModelProperty(value = "辨析汉字", required = true)
        private String comparisonChar;

        @ApiModelProperty(value = "辨析拼音")
        private String comparisonPinyin;

        @ApiModelProperty(value = "辨析汉字翻译")
        private List<TextTranslationRequest> comparisonCharTranslations;

        @ApiModelProperty(value = "对比辨析说明外文翻译")
        private List<TextTranslationRequest> comparisonDescTranslations;

        @ApiModelProperty(value = "辨析排序权重（值大的排前面，不传默认 0）")
        private int order;
    }

    @Getter
    @Setter
    public static class CharWordRequest implements Serializable {

        @NotBlank
        @ApiModelProperty(value = "组词ID, 新增时不传 更新时传", required = true)
        private Integer id;

        @NotBlank
        @ApiModelProperty(value = "组词", required = true)
        private String wordItem;

        @ApiModelProperty(value = "HSK等级")
        private String hskLevel;

        @ApiModelProperty(value = "组词拼音")
        private String pinyin;

        @ApiModelProperty(value = "组词词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "组词的翻译")
        private List<TextTranslationRequest> wordItemTranslations;

        @ApiModelProperty(value = "组词例句列表")
        private List<ExampleSentenceRequest> sentenceContents;

        @ApiModelProperty(value = "组词排序权重（值大的排前面，不传默认 0）")
        private Integer order;
    }

}
