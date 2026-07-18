package com.naon.grid.backend.service.culture.dto;

import com.naon.grid.base.BaseDTO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CultureDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "文化点ID")
    private Long id;

    @ApiModelProperty(value = "文化点名称")
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "名称音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "名称多语言翻译JSON")
    private String translations;

    @ApiModelProperty(value = "封面图片资源ID")
    private Long coverImageId;

    @ApiModelProperty(value = "等级")
    private String level;

    @ApiModelProperty(value = "一级项目")
    private String project;

    @ApiModelProperty(value = "二级项目")
    private String category;

    @ApiModelProperty(value = "一句话介绍")
    private String oneSentenceIntro;

    @ApiModelProperty(value = "一句话介绍翻译JSON")
    private String oneSentenceIntroTranslations;

    @ApiModelProperty(value = "一句话介绍音频ID")
    private Long oneSentenceIntroAudioId;

    @ApiModelProperty(value = "一句话介绍图片ID")
    private Long oneSentenceIntroImageId;

    @ApiModelProperty(value = "详细介绍")
    private String detailedIntro;

    @ApiModelProperty(value = "详细介绍翻译JSON")
    private String detailedIntroTranslations;

    @ApiModelProperty(value = "详细介绍音频ID")
    private Long detailedIntroAudioId;

    @ApiModelProperty(value = "详细介绍图片ID")
    private Long detailedIntroImageId;

    @ApiModelProperty(value = "学一学例句ID列表")
    private List<Long> sentenceIds;

    @ApiModelProperty(value = "练一练习题ID列表")
    private List<Long> questionIds;

    @ApiModelProperty(value = "关键词列表")
    private List<CultureKeywordDto> keywords;

    @ApiModelProperty(value = "学一学例句详情列表（已发布时加载）")
    private List<ExampleSentenceDto> sentences;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;

    // --- 列表统计字段 ---
    @ApiModelProperty(value = "关键词个数")
    private Integer keywordCount;

    @ApiModelProperty(value = "学一学例句个数")
    private Integer sentenceCount;

    @ApiModelProperty(value = "练一练习题个数")
    private Integer questionCount;
}
