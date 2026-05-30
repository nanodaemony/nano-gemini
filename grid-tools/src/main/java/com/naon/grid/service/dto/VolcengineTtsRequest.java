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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 火山引擎 TTS 语音合成请求 DTO
 * <p>
 * 精简版，针对中文教学网站场景设计。
 * 用于批量生成词汇、句子的标准普通话音频资源。
 * </p>
 * <p>
 * 调用火山引擎 HTTP Chunked 接口：POST https://openspeech.bytedance.com/api/v3/tts/unidirectional
 * </p>
 *
 * @author nano
 * @date 2026-05-20
 */
@Data
@ApiModel(value = "火山引擎TTS语音合成请求",
        description = "中文教学场景专用——合成词汇/句子的标准普通话音频")
public class VolcengineTtsRequest {

    @ApiModelProperty(value = "合成文本", required = true,
            notes = "需要转为语音的中文文本。支持词汇、短语、句子等。\n" +
                    "示例：\"你好，请问你叫什么名字？\" 或 \"中国\"\n" +
                    "如果文本中包含英文单词（如 \"iPhone\"），受 explicitLanguage 参数影响：\n" +
                    "- 不传 explicitLanguage：正常中英混读\n" +
                    "- 传 \"zh-cn\"：英文部分可能被按中文发音处理")
    private String text;

    @ApiModelProperty(value = "音色标识（发音人）", required = true,
            example = "zh_female_shuangkuaisisi_moon_bigtts",
            notes = "指定发音人/音色。中文教学推荐选择发音清晰标准的音色。\n" +
                    "火山引擎提供丰富的音色选择：\n" +
                    "- 推荐女生音色：zh_female_shuangkuaisisi_moon_bigtts（爽快思思）等\n" +
                    "- 推荐男生音色：zh_male_bvlazysheep、zh_male_m191_uranus_bigtts 等\n" +
                    "- 完整音色列表详见火山引擎官方文档《发音人列表》\n" +
                    "建议：选定几个音色试听，确定一个最满意的固定使用，保持教学音频的一致性。")
    private String speaker;

    @ApiModelProperty(value = "API Resource Id（模型版本）", required = true,
            example = "seed-tts-2.0",
            notes = "火山引擎 TTS 的资源标识，决定模型版本和计费方式。\n" +
                    "中文教学推荐使用：\n" +
                    "- seed-tts-2.0：豆包语音合成模型 2.0（推荐，音质更好，对应「语音合成2.0字符版」计费）\n" +
                    "- seed-tts-1.0：豆包语音合成模型 1.0（对应「语音合成1.0字符版」计费）\n" +
                    "选定后固定即可，不需要每次调整。")
    private String apiResourceId;

    @ApiModelProperty(value = "语速", example = "0",
            notes = "控制朗读速度，取值范围 [-50, 100]，默认 0（正常语速）。\n" +
                    "教学场景推荐值：\n" +
                    "- 0（正常语速）：适合中高级学习者\n" +
                    "- -10 ~ -20（偏慢）：⭐ 推荐，适合初级学习者，每个字更清晰\n" +
                    "- -30 ~ -50（很慢）：适合非常基础的字词跟读\n" +
                    "- 正值（加快）：一般不建议在教学场景使用\n" +
                    "换算参考：-50 = 0.5 倍速，+100 = 2.0 倍速")
    private Integer speechRate = 0;

    @ApiModelProperty(value = "明确语种", example = "zh-cn",
            notes = "指定朗读语种。中文教学推荐设为 \"zh-cn\"。\n" +
                    "取值说明：\n" +
                    "- 不传：正常中英混读（如果文本有英文单词会尝试用英文发音）\n" +
                    "- \"zh-cn\"：（⭐ 推荐）中文为主，支持中英混——英文单词也会尝试用中文发音风格处理\n" +
                    "- \"en\"：仅英文\n" +
                    "如果你的教学文本里偶尔夹带英文（如 \"Apple\"、\"iPhone\"），设为 zh-cn 可避免语种切换的生硬感。")
    private String explicitLanguage;

    @ApiModelProperty(value = "语音指令（控制发音风格）",
            notes = "通过自然语言指令控制发音风格，让合成效果更贴合教学场景。\n" +
                    "🌰 常用示例：\n" +
                    "- [\"字正腔圆，发音清晰标准\"]：⭐ 推荐，标准教学风格\n" +
                    "- [\"请用教学语速，每个字读清楚\"]：适合初级词汇\n" +
                    "- [\"语气亲切自然\"]：适合对话类句子\n" +
                    "- [\"用朗读课文的语气\"]：适合课文/段落\n\n" +
                    "⚠️ 说明：\n" +
                    "- 列表只第一个值有效\n" +
                    "- 仅适用于 seed-tts-2.0（TTS2.0）系列模型\n" +
                    "- 该指令文本不参与计费\n" +
                    "- 建议先试听默认效果，不满意再用此参数微调")
    private List<String> contextTexts;

    @ApiModelProperty(value = "句尾静音时长", example = "500",
            notes = "在合成的音频末尾增加静音，单位毫秒，范围 0~30000。\n" +
                    "教学场景推荐值：\n" +
                    "- 0（默认）：不增加静音，适合连续播放\n" +
                    "- 300~500：⭐ 推荐，适合词汇跟读，每个词后留一点反应时间\n" +
                    "- 1000~2000：适合句子听写，留够书写时间\n\n" +
                    "注意：只在整段文本的末尾增加静音，不会在每句话之间都加。")
    private Integer silenceDuration;
}
