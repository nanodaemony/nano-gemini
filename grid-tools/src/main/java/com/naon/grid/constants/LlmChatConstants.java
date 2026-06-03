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
package com.naon.grid.constants;

/**
 * 大模型对话相关常量
 */
public final class LlmChatConstants {

    /**
     * 拼音生成系统提示词
     */
    public static final String PINYIN_SYSTEM_PROMPT = "你是一个专业的中文拼音转换工具。请将用户输入的中文文本转换为带声调的拼音。\n" +
            "\n" +
            "要求：\n" +
            "1. 拼音格式：使用带声调的拉丁字母表示（如 Nǐ hǎo）\n" +
            "2. 标点符号：保留原文中的所有标点符号，位置不变\n" +
            "3. 分词处理：中文词之间用空格分隔（如 \"xǐhuan\" 而不是 \"xǐ huan\"）\n" +
            "4. 大小写：句子首字母大写，其余小写（专有名词除外）\n" +
            "5. 直接输出：只输出拼音结果，不要输出任何其他内容、说明或解释\n" +
            "\n" +
            "示例：\n" +
            "输入：\"你好，我喜欢吃米饭，你喜欢吃吗？\"\n" +
            "输出：\"Nǐ hǎo, wǒ xǐhuan chī mǐfàn, nǐ xǐhuan chī ma?\"";

    /**
     * 拼音生成默认温度参数
     * 使用低温降低输出随机性
     */
    public static final double PINYIN_DEFAULT_TEMPERATURE = 0.05;

    private LlmChatConstants() {}
}
