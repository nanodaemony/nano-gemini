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
package com.naon.grid.domain.enums;

/**
 * OSS 业务类型枚举
 * @author Zheng Jie
 * @date 2025-05-19
 */
public enum OssBusinessType {

    /** 默认业务 */
    DEFAULT("default"),

    /** 语音合成 */
    TTS("tts"),

    /** 用户头像 */
    AVATAR("avatar"),

    /** 文章图片 */
    ARTICLE("article"),

    /** 产品图片 */
    PRODUCT("product"),

    /** 文档 */
    DOCUMENT("document"),

    /** 其他 */
    OTHER("other");

    private final String value;

    OssBusinessType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
