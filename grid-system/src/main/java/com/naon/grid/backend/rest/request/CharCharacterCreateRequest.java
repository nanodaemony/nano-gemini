package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CharCharacterCreateRequest implements Serializable {

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "等级")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔画")
    private String stroke;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "说明翻译")
    private String descTranslations;

    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationRequest> discriminations;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordRequest> words;

    @Getter
    @Setter
    public static class CharDiscriminationRequest implements Serializable {
        @ApiModelProperty(value = "辨析唯一ID")
        private Integer id;

        @ApiModelProperty(value = "辨析汉字")
        private String discrimChar;

        @ApiModelProperty(value = "辨析拼音")
        private String discrimPinyin;

        @ApiModelProperty(value = "辨析汉字翻译")
        private String discrimCharTranslations;

        @ApiModelProperty(value = "对比翻译")
        private String comparisonTranslations;
    }

    @Getter
    @Setter
    public static class CharWordRequest implements Serializable {
        @ApiModelProperty(value = "组词唯一ID")
        private Integer id;

        @ApiModelProperty(value = "组词")
        private String wordItem;

        @ApiModelProperty(value = "等级")
        private String level;

        @ApiModelProperty(value = "拼音")
        private String pinyin;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "组词翻译")
        private String wordItemTranslations;

        @ApiModelProperty(value = "例句")
        private String exampleSentence;

        @ApiModelProperty(value = "例句拼音")
        private String examplePinyin;

        @ApiModelProperty(value = "例句翻译")
        private String exampleTranslations;

        @ApiModelProperty(value = "例句图片")
        private String exampleImage;
    }
}
