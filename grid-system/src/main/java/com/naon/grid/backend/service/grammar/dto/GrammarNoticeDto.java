package com.naon.grid.backend.service.grammar.dto;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class GrammarNoticeDto implements Serializable {

    @ApiModelProperty(value = "语法注意ID")
    private Long id;

    @ApiModelProperty(value = "语法点ID")
    private Long grammarId;

    @ApiModelProperty(value = "注意内容")
    private String noticeContent;

    @ApiModelProperty(value = "注意内容外文翻译")
    private List<TextTranslation> noticeContentTranslations;

    @ApiModelProperty(value = "例句列表")
    private List<ExampleSentenceDto> sentences;

    @ApiModelProperty(value = "排序权重（值大的排前面）")
    private Integer order;

    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "有效状态")
    private Integer status;
}
