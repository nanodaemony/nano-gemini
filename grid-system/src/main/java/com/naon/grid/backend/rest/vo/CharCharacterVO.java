package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class CharCharacterVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字", required = true)
    private String character;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @ApiModelProperty(value = "拼音", required = true)
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
    private List<TextTranslationVO> descTranslations;

    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationVO> discriminations;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordVO> words;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "修改人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @Getter
    @Setter
    public static class CharDiscriminationVO implements Serializable {
        @ApiModelProperty(value = "辨析唯一ID")
        private Integer id;

        @ApiModelProperty(value = "汉字ID", required = true)
        private Integer charId;

        @ApiModelProperty(value = "辨析汉字", required = true)
        private String discrimChar;

        @ApiModelProperty(value = "辨析拼音")
        private String discrimPinyin;

        @ApiModelProperty(value = "辨析汉字翻译")
        private List<TextTranslationVO> discrimCharTranslations;

        @ApiModelProperty(value = "对比翻译")
        private List<TextTranslationVO> comparisonTranslations;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }

    @Getter
    @Setter
    public static class CharWordVO implements Serializable {
        @ApiModelProperty(value = "组词唯一ID")
        private Integer id;

        @ApiModelProperty(value = "汉字ID", required = true)
        private Integer charId;

        @ApiModelProperty(value = "组词", required = true)
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

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }
}
