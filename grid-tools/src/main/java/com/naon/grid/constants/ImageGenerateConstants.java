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
 * 文生图提示词常量
 * @author nano
 * @date 2026-06-14
 */
public class ImageGenerateConstants {

    /**
     * 部首演化图提示词模板
     * <p>
     * 占位符：
     * %s — 象形特征描述（如 模拟侧立的人形）
     * %s — 部首（如 人、木、水）
     */
    public static final String RADICAL_EVOLUTION_PROMPT = "你是一位专业的汉字书法与古文字学者插图师。" +
            "请生成一幅展示该汉字/部首从古至今演化过程的科普信息图。" +
            "画面采用从左至右的水平时间轴构图，展示该字在三个关键历史阶段的书体形态，" +
            "并配以考古学风格的背景衬托。" +
            "\n\n" +
            "画面分为三个区块，从左至右依次展示：" +
            "\n\n" +
            "【阶段1：甲骨文】古文字形态。展示该字在商周时期的甲骨文写法，" +
            "刻写在龟甲兽骨质感背景上，线条纤细刚劲，" +
            "保留鲜明的象形特征：%s。配文标签：甲骨文 · 商周" +
            "\n\n" +
            "【阶段2：小篆】秦代统一文字后的标准书体。展示该字的小篆写法，" +
            "线条圆转匀称、结构规整对称，书写于竹简质感背景上。" +
            "配文标签：小篆 · 秦" +
            "\n\n" +
            "【阶段3：楷书】现代标准书体。展示「%s」的楷书形态，" +
            "笔画清晰规整、结构方正，书写于宣纸质感背景上。" +
            "配文标签：楷书 · 现代" +
            "\n\n" +
            "整体风格：教育科普信息图，画面干净清晰，每个阶段独立展示并配有标注。" +
            "色彩以古朴的暖色调为主（赭石、墨色、宣纸白），" +
            "适当使用阴影和纹理增强历史感。" +
            "画面底部添加时间轴箭头从左至右指示演化方向。文字标签保持清晰可读。" +
            "\n\n" +
            "避免：现代数码字体、照片、写实人物、3D渲染、过于鲜艳的颜色、" +
            "霓虹色、卡通、动漫、构图杂乱、字形扭曲、笔画错误";
}
