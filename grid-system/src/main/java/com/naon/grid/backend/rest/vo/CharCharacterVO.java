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

    @ApiModelProperty(value = "汉字ID")
    private Integer id;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "HSK等级")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "读音音频ID")
    private long audioId;

    @ApiModelProperty(value = "部首ID")
    private long radicalId;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "部件组合")
    private String componentCombination;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "汉字说明翻译")
    private List<TextTranslationVO> charDescTranslations;

    @ApiModelProperty(value = "笔画")
    private String stroke;

    @ApiModelProperty(value = "辨析列表")
    private List<CharComparisonVO> comparisons;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordVO> words;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

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
    public static class CharComparisonVO implements Serializable {

        @ApiModelProperty(value = "辨析ID")
        private int id;

        @ApiModelProperty(value = "辨析汉字")
        private String comparisonChar;

        @ApiModelProperty(value = "辨析拼音")
        private String comparisonPinyin;

        @ApiModelProperty(value = "辨析汉字翻译")
        private List<TextTranslationVO> comparisonCharTranslations;

        @ApiModelProperty(value = "对比辨析说明外文翻译")
        private List<TextTranslationVO> comparisonDescTranslations;

        @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
        private Integer order;

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

        @ApiModelProperty(value = "组词ID")
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

        @ApiModelProperty(value = "组词例句")
        private ExampleSentenceVO wordItemSentence;

        @ApiModelProperty(value = "组词排序权重（值大的排前面）")
        private Integer order;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }
}
