package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AppTopicChatVO {

    @ApiModelProperty(value = "角色")
    private String role;

    @ApiModelProperty(value = "对话内容")
    private String content;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "翻译")
    private TextTranslationVO translation;

    @ApiModelProperty(value = "音频")
    private AppTopicBaseVO.AudioVO audio;
}
