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
package com.naon.grid.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云百炼 文生图（Qwen-Image）配置
 * @author nano
 * @date 2026-05-30
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ali.image")
public class AliYunImageConfig {

    /**
     * 访问密钥 Key
     */
    private String apiKey;

    /**
     * API 请求地址，默认北京地域
     */
    private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
}
