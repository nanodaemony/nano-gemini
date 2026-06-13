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

    @ApiModelProperty(value = "汉字ID")
    private Integer id;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源")
    private AudioVO audio;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "部件组合")
    private String componentCombination;

    @ApiModelProperty(value = "汉字说明翻译")
    private TextTranslationVO descTranslation;

    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationVO> discriminations;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordVO> words;

    @Getter
    @Setter
    public static class CharDiscriminationVO implements Serializable {

        @ApiModelProperty(value = "辨析汉字")
        private String comparisonChar;

        @ApiModelProperty(value = "辨析拼音")
        private String comparisonPinyin;

        @ApiModelProperty(value = "辨析汉字翻译")
        private TextTranslationVO comparisonCharTranslation;

        @ApiModelProperty(value = "对比辨析说明翻译")
        private TextTranslationVO comparisonDescTranslation;

        @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
        private Integer order;
    }

    @Getter
    @Setter
    public static class AudioVO implements Serializable {

        @ApiModelProperty(value = "音频文件地址")
        private String audioUrl;
    }

    @Getter
    @Setter
    public static class ImageVO implements Serializable {

        @ApiModelProperty(value = "图片文件地址")
        private String imageUrl;
    }

    @Getter
    @Setter
    public static class CharWordVO implements Serializable {

        @ApiModelProperty(value = "组词")
        private String wordItem;

        @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
        private String hskLevel;

        @ApiModelProperty(value = "拼音")
        private String pinyin;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "组词翻译")
        private TextTranslationVO wordItemTranslation;

        @ApiModelProperty(value = "例句")
        private String exampleSentence;

        @ApiModelProperty(value = "例句拼音")
        private String examplePinyin;

        @ApiModelProperty(value = "例句翻译")
        private TextTranslationVO exampleTranslation;

        @ApiModelProperty(value = "例句图片")
        private ImageVO exampleImage;
    }
}
