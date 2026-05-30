package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 用户端汉字详情VO（不包含审计字段）
 */
@Getter
@Setter
public class AppCharCharacterDetailVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔顺")
    private String stroke;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "汉字说明的多语种翻译")
    private List<TextTranslationVO> descTranslations;

    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationVO> discriminations;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordVO> words;

    @Getter
    @Setter
    public static class CharDiscriminationVO implements Serializable {

        @ApiModelProperty(value = "辨析唯一ID")
        private Integer id;

        @ApiModelProperty(value = "汉字ID")
        private Integer charId;

        @ApiModelProperty(value = "辨析汉字")
        private String discrimChar;

        @ApiModelProperty(value = "辨析拼音")
        private String discrimPinyin;

        @ApiModelProperty(value = "辨析汉字翻译")
        private List<TextTranslationVO> discrimCharTranslations;

        @ApiModelProperty(value = "对比翻译")
        private List<TextTranslationVO> comparisonTranslations;
    }

    @Getter
    @Setter
    public static class CharWordVO implements Serializable {

        @ApiModelProperty(value = "组词唯一ID")
        private Integer id;

        @ApiModelProperty(value = "汉字ID")
        private Integer charId;

        @ApiModelProperty(value = "组词")
        private String wordItem;

        @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
        private String level;

        @ApiModelProperty(value = "拼音")
        private String pinyin;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "组词翻译")
        private List<TextTranslationVO> wordItemTranslations;

        @ApiModelProperty(value = "例句")
        private String exampleSentence;

        @ApiModelProperty(value = "例句拼音")
        private String examplePinyin;

        @ApiModelProperty(value = "例句翻译")
        private List<TextTranslationVO> exampleTranslations;

        @ApiModelProperty(value = "例句图片")
        private String exampleImage;
    }
}
