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

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 配置
 * @author Zheng Jie
 * @date 2025-05-19
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ali.oss")
public class AliOssConfig {

    /**
     * OSS Endpoint
     */
    private String endpoint;

    /**
     * 访问密钥 ID
     */
    private String accessKeyId;

    /**
     * 访问密钥 Secret
     */
    private String accessKeySecret;

    /**
     * 默认存储桶名称
     */
    private String bucketName;

    /**
     * OSS 访问域名
     */
    private String domain;

    /**
     * 文件存储文件夹格式
     */
    private String timeformat = "yyyy-MM";

    /**
     * 创建 OSS 客户端
     * @return OSS 客户端实例
     */
    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
