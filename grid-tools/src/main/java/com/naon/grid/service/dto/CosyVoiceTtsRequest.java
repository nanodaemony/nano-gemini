/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * CosyVoice 非实时语音合成请求 DTO
 * @author nano
 * @date 2025-05-29
 */
@Data
public class CosyVoiceTtsRequest {

    @ApiModelProperty(value = "音色名称", required = true, example = "longanyang",
            notes = "取值范围：系统音色参见音色列表，声音复刻/设计音色参见 CosyVoice 声音复刻/设计 API")
    private String voice;

    @ApiModelProperty(value = "待合成文本", required = true, example = "我家的后面有一个很大的花园。",
            notes = "支持 SSML 和 LaTeX 格式输入。使用 SSML 时需同时将 enableSsml 设置为 true")
    private String text;

    @ApiModelProperty(value = "语音合成模型", example = "cosyvoice-v3-flash",
            notes = "取值范围：cosyvoice-v3.5-plus、cosyvoice-v3.5-flash、cosyvoice-v3-plus、cosyvoice-v3-flash、cosyvoice-v2")
    private String model = "cosyvoice-v3-flash";

    @ApiModelProperty(value = "音频编码格式", example = "mp3",
            notes = "默认值：mp3。取值范围：mp3、pcm、wav、opus")
    private String format = "mp3";

    @ApiModelProperty(value = "音频采样率（Hz）", example = "22050",
            notes = "默认值：22050。取值范围：8000、16000、22050、24000、44100、48000")
    private Integer sampleRate;

    @ApiModelProperty(value = "音量", example = "50",
            notes = "默认值：50。取值范围：[0, 100]")
    private Integer volume;

    @ApiModelProperty(value = "语速", example = "1.0",
            notes = "默认值：1.0。取值范围：[0.5, 2.0]")
    private Float rate;

    @ApiModelProperty(value = "音调", example = "1.0",
            notes = "默认值：1.0。取值范围：[0.5, 2.0]")
    private Float pitch;

    @ApiModelProperty(value = "是否开启 SSML 功能", example = "false",
            notes = "默认值：false。设置为 true 时，text 参数需传入 SSML 格式文本")
    private Boolean enableSsml;

    @ApiModelProperty(value = "是否开启字级别时间戳", example = "false",
            notes = "默认值：false。仅适用于 cosyvoice-v3-flash、cosyvoice-v3-plus 和 cosyvoice-v2 模型的复刻音色及部分系统音色")
    private Boolean wordTimestampEnabled;

    @ApiModelProperty(value = "随机数种子", example = "0",
            notes = "默认值：0。取值范围：[0, 65535]。相同参数下使用相同 seed 可复现相同合成结果")
    private Integer seed;

    @ApiModelProperty(value = "目标语言", example = "[\"zh\"]",
            notes = "用于提升合成效果，当前版本仅处理第一个元素。取值范围：zh、en、fr、de、ja、ko、ru、pt、th、id、vi")
    private List<String> languageHints;

    @ApiModelProperty(value = "控制指令", example = "请用非常开心的语气说话。",
            notes = "用于控制方言、情感或角色等合成效果")
    private String instruction;

    @ApiModelProperty(value = "音频码率（kbps）", example = "32",
            notes = "默认值：32。取值范围：[6, 510]。仅在 format 为 opus 时生效")
    private Integer bitRate;

    @ApiModelProperty(value = "是否添加 AIGC 隐性标识", example = "false",
            notes = "默认值：false。仅 cosyvoice-v3-flash、cosyvoice-v3-plus、cosyvoice-v2 支持")
    private Boolean enableAigcTag;

    @ApiModelProperty(value = "AIGC 隐性标识中的 ContentPropagator 字段",
            notes = "默认值：阿里云 UID。仅在 enableAigcTag 为 true 时生效")
    private String aigcPropagator;

    @ApiModelProperty(value = "AIGC 隐性标识中的 PropagateID 字段",
            notes = "默认值：本次请求 Request ID。仅在 enableAigcTag 为 true 时生效")
    private String aigcPropagateId;

    @ApiModelProperty(value = "文本热修复配置",
            notes = "用于自定义指定词语的发音或文本替换。cosyvoice-v2 不支持。示例：{\"pronunciation\":[{\"天气\":\"tian1 qi4\"}],\"replace\":[{\"今天\":\"金天\"}]}")
    private Map<String, Object> hotFix;

    @ApiModelProperty(value = "是否启用 Markdown 过滤", example = "false",
            notes = "默认值：false。仅 cosyvoice-v3-flash 复刻音色支持。启用后自动过滤输入文本中的 Markdown 标记符号")
    private Boolean enableMarkdownFilter;
}
