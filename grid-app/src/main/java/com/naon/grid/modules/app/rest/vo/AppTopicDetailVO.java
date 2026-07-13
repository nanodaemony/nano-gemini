package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class AppTopicDetailVO {

    @ApiModelProperty(value = "话题ID")
    private Long id;

    @ApiModelProperty(value = "话题名称")
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "翻译")
    private TextTranslationVO translation;

    @ApiModelProperty(value = "封面图")
    private AppTopicBaseVO.ImageVO coverImage;

    @ApiModelProperty(value = "音频")
    private AppTopicBaseVO.AudioVO audio;

    @ApiModelProperty(value = "句式列表")
    private List<AppTopicPatternVO> patterns;
}
