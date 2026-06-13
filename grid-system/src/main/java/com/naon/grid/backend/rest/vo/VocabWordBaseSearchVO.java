package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VocabWordBaseSearchVO implements Serializable {

    @ApiModelProperty(value = "词汇ID")
    private Integer id;

    @ApiModelProperty(value = "词汇词头")
    private String word;

    @ApiModelProperty(value = "对应义项列表")
    private List<VocabSenseSearchItemVO> senses;

    @Getter
    @Setter
    public static class VocabSenseSearchItemVO implements Serializable {

        @ApiModelProperty(value = "义项ID")
        private Integer id;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "中文释义")
        private String chineseDef;

        @ApiModelProperty(value = "中文释义外文翻译")
        private List<TextTranslationVO> defTranslations;
    }
}
