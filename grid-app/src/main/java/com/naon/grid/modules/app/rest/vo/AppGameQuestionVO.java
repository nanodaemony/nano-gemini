package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 用户端游戏题目 VO（单语言筛选后）。
 */
@Getter
@Setter
public class AppGameQuestionVO implements Serializable {

    @ApiModelProperty(value = "游戏类型: radical / comparison / word_formation")
    private String gameType;

    @ApiModelProperty(value = "题号 1-10")
    private Integer questionIndex;

    @ApiModelProperty(value = "题干（展示用的汉字或句子语境）")
    private String stem;

    @ApiModelProperty(value = "目标汉字")
    private String character;

    @ApiModelProperty(value = "目标汉字拼音")
    private String pinyin;

    @ApiModelProperty(value = "选项列表（4个，已打乱）")
    private List<GameOptionVO> options;

    @ApiModelProperty(value = "正确答案 key")
    private String correctKey;

    @ApiModelProperty(value = "解析信息（单语言）")
    private GameExplanationVO explanation;

    // --- inner classes ---

    @Getter
    @Setter
    public static class GameOptionVO implements Serializable {

        @ApiModelProperty(value = "选项标识: A/B/C/D")
        private String key;

        @ApiModelProperty(value = "选项文字")
        private String text;

        @ApiModelProperty(value = "是否正确答案")
        private Boolean isCorrect;
    }

    @Getter
    @Setter
    public static class GameExplanationVO implements Serializable {

        // --- 部首游戏 ---
        @ApiModelProperty(value = "部首")
        private String radical;

        @ApiModelProperty(value = "部首名称")
        private String radicalName;

        @ApiModelProperty(value = "部首含义（单语言）")
        private TextTranslationVO radicalMeaning;

        // --- 形近字辨析游戏 ---
        @ApiModelProperty(value = "对比字")
        private String comparisonChar;

        @ApiModelProperty(value = "对比字拼音")
        private String comparisonPinyin;

        @ApiModelProperty(value = "对比说明（单语言）")
        private TextTranslationVO comparisonDesc;

        // --- 组词游戏 ---
        @ApiModelProperty(value = "正确组词")
        private String correctWord;

        @ApiModelProperty(value = "正确组词拼音")
        private String correctWordPinyin;

        @ApiModelProperty(value = "正确组词词性")
        private String correctWordPos;

        @ApiModelProperty(value = "正确组词释义（单语言）")
        private TextTranslationVO correctWordMeaning;
    }
}
