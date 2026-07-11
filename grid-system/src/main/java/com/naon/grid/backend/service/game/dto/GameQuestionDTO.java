package com.naon.grid.backend.service.game.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 游戏题目 DTO（多语言原始数据）。
 * <p>
 * 包含所有可用语言的翻译列表，由 AppGameWrapper 在 app 层筛选为单语言 VO。
 */
@Getter
@Setter
public class GameQuestionDTO implements Serializable {

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

    @ApiModelProperty(value = "选项列表（4个，顺序已打乱）")
    private List<GameOptionDTO> options;

    @ApiModelProperty(value = "正确答案的 key: A/B/C/D")
    private String correctKey;

    @ApiModelProperty(value = "解析信息（多语言原始列表）")
    private GameExplanationDTO explanation;

    // --- inner classes ---

    @Getter
    @Setter
    public static class GameOptionDTO implements Serializable {

        @ApiModelProperty(value = "选项标识: A/B/C/D")
        private String key;

        @ApiModelProperty(value = "选项文字")
        private String text;

        @ApiModelProperty(value = "是否正确答案")
        private Boolean isCorrect;
    }

    @Getter
    @Setter
    public static class GameExplanationDTO implements Serializable {

        // --- 部首游戏 ---

        @ApiModelProperty(value = "部首（部首游戏）")
        private String radical;

        @ApiModelProperty(value = "部首名称（部首游戏）")
        private String radicalName;

        @ApiModelProperty(value = "部首含义多语言（部首游戏）")
        private List<TextTranslation> radicalMeaning;

        // --- 形近字辨析游戏 ---

        @ApiModelProperty(value = "对比字（形近字游戏）")
        private String comparisonChar;

        @ApiModelProperty(value = "对比字拼音（形近字游戏）")
        private String comparisonPinyin;

        @ApiModelProperty(value = "对比说明多语言（形近字游戏）")
        private List<TextTranslation> comparisonDesc;

        // --- 组词游戏 ---

        @ApiModelProperty(value = "正确组词（组词游戏）")
        private String correctWord;

        @ApiModelProperty(value = "正确组词拼音（组词游戏）")
        private String correctWordPinyin;

        @ApiModelProperty(value = "正确组词词性（组词游戏）")
        private String correctWordPos;

        @ApiModelProperty(value = "正确组词释义多语言（组词游戏）")
        private List<TextTranslation> correctWordMeaning;
    }
}
