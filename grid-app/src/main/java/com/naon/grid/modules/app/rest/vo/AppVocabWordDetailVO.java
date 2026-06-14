package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppVocabWordDetailVO implements Serializable {

    @ApiModelProperty(value = "词汇唯一ID")
    private Integer id;

    @ApiModelProperty(value = "词汇")
    private String word;

    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "词汇读音音频")
    private AudioVO audio;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "义项列表")
    private List<VocabSenseVO> senses;

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
    public static class VocabSenseVO implements Serializable {
        @ApiModelProperty(value = "义项ID")
        private Integer id;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "中文释义")
        private String chineseDef;

        @ApiModelProperty(value = "释义音频")
        private AudioVO defAudio;

        @ApiModelProperty(value = "释义图片")
        private ImageVO defImage;

        @ApiModelProperty(value = "中文释义外文翻译（按语言筛选后的单条）")
        private TextTranslationVO translation;

        @ApiModelProperty(value = "释义图片例句")
        private VocabExampleVO defImageSentence;

        @ApiModelProperty(value = "近义词列表")
        private List<SynonymVO> synonymWords;

        @ApiModelProperty(value = "反义词列表")
        private List<AntonymVO> antonymWords;

        @ApiModelProperty(value = "正序关联词汇")
        private List<RelatedWordVO> sequentialWords;

        @ApiModelProperty(value = "逆序关联词汇")
        private List<RelatedWordVO> reverseSequentialWords;

        @ApiModelProperty(value = "乱序关联词汇")
        private List<RelatedWordVO> jumbledWords;

        @ApiModelProperty(value = "义项排序")
        private Integer order;

        @ApiModelProperty(value = "搭配列表")
        private List<VocabStructureVO> structures;
    }

    @Getter
    @Setter
    public static class SynonymVO implements Serializable {
        @ApiModelProperty(value = "近义词内容")
        private String content;
    }

    @Getter
    @Setter
    public static class AntonymVO implements Serializable {
        @ApiModelProperty(value = "反义词内容")
        private String content;
    }

    @Getter
    @Setter
    public static class RelatedWordVO implements Serializable {
        @ApiModelProperty(value = "关联词汇内容")
        private String content;
    }

    @Getter
    @Setter
    public static class VocabStructureVO implements Serializable {

        @ApiModelProperty(value = "搭配文案")
        private String pattern;

        @ApiModelProperty(value = "搭配释义")
        private String patternDef;

        @ApiModelProperty(value = "搭配释义外文翻译（按语言筛选后的单条）")
        private TextTranslationVO patternDefTranslation;

        @ApiModelProperty(value = "搭配排序")
        private Integer order;

        @ApiModelProperty(value = "例句列表")
        private List<VocabExampleVO> examples;
    }

    @Getter
    @Setter
    public static class VocabExampleVO implements Serializable {

        @ApiModelProperty(value = "例句中文文案")
        private String sentence;

        @ApiModelProperty(value = "例句音频")
        private AudioVO audio;

        @ApiModelProperty(value = "例句拼音")
        private String pinyin;

        @ApiModelProperty(value = "例句外文翻译")
        private TextTranslationVO translation;

        @ApiModelProperty(value = "例句图片")
        private ImageVO image;

        @ApiModelProperty(value = "例句排序")
        private Integer order;
    }

}
