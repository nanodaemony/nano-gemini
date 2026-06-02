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
package com.naon.grid.enums;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 大模型厂商枚举
 * 支持的大模型服务提供商
 * @author nano
 * @date 2026-06-02
 */
@ApiModel(description = "大模型厂商枚举")
public enum ChatProviderEnum {

    @ApiModelProperty(value = "阿里云百炼", notes = "阿里云的大模型服务，支持 qwen-plus/qwen-max/qwen-turbo 等模型")
    ALIYUN,

    @ApiModelProperty(value = "DeepSeek", notes = "深度求索的大模型服务，支持 deepseek-chat 等模型")
    DEEPSEEK
}
