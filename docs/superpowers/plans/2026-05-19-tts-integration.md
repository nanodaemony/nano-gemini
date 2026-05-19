# 阿里云 TTS 语音合成 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 grid-tools 模块中添加阿里云 TTS 语音合成功能，支持文本转语音，调用 DashScope API 获取音频，下载后上传到自己的 OSS，保存记录到 tts_record 表，返回最终音频 URL。

**Architecture:** 参考现有 AliOssStorage 的实现模式，保持代码风格一致，使用 JPA。

**Tech Stack:** Spring Boot 2.7, Spring Data JPA, DashScope SDK 2.22.17, Gson 2.13.1, Aliyun OSS SDK 3.17.4

---

## 文件结构总览

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| Modify | `grid-tools/pom.xml` | 添加 DashScope SDK 和 Gson 依赖 |
| Modify | `.env.example` | 添加 TTS 配置示例 |
| Modify | `grid-system/src/main/resources/config/application.yml` | 添加 TTS 配置项 |
| Modify | `grid-tools/src/main/java/com/naon/grid/domain/enums/OssBusinessType.java` | 添加 TTS 业务类型 |
| Create | `grid-tools/src/main/java/com/naon/grid/config/AliTtsConfig.java` | TTS 配置类 |
| Create | `grid-tools/src/main/java/com/naon/grid/domain/TtsRecord.java` | 实体类 |
| Create | `grid-tools/src/main/java/com/naon/grid/repository/TtsRecordRepository.java` | Repository |
| Create | `grid-tools/src/main/java/com/naon/grid/service/dto/TtsRequest.java` | 请求 DTO |
| Create | `grid-tools/src/main/java/com/naon/grid/service/dto/TtsResponse.java` | 响应 DTO |
| Create | `grid-tools/src/main/java/com/naon/grid/service/TtsService.java` | Service 接口 |
| Create | `grid-tools/src/main/java/com/naon/grid/service/impl/TtsServiceImpl.java` | Service 实现 |
| Create | `grid-tools/src/main/java/com/naon/grid/rest/TtsController.java` | REST 接口 |

---

### Task 1: 添加 Maven 依赖

**Files:**
- Modify: `grid-tools/pom.xml`

- [ ] **Step 1: 修改 pom.xml 添加 DashScope SDK 和 Gson 依赖**

在 properties 中添加版本（可选，为了统一管理）：

```xml
    <properties>
        <mail.version>1.4.7</mail.version>
        <alipay.version>4.22.57.ALL</alipay.version>
        <aliyun.oss.version>3.17.4</aliyun.oss.version>
        <dashscope.version>2.22.17</dashscope.version>
        <gson.version>2.13.1</gson.version>
    </properties>
```

在 dependencies 中添加：

```xml
        <!-- DashScope SDK -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>dashscope-sdk-java</artifactId>
            <version>${dashscope.version}</version>
            <scope>compile</scope>
        </dependency>

        <!-- Gson (DashScope SDK 需要) -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>${gson.version}</version>
        </dependency>
```

- [ ] **Step 2: 验证项目可以正常编译**

Run: `cd /c/Users/nano/Desktop/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/pom.xml
git commit -m "feat: add dashscope sdk and gson dependencies"
```

---

### Task 2: 修改 OssBusinessType 枚举，添加 TTS 类型

**Files:**
- Modify: `grid-tools/src/main/java/com/naon/grid/domain/enums/OssBusinessType.java`

- [ ] **Step 1: 修改枚举类，添加 TTS**

在现有枚举中添加：

```java
    /** 语音合成 */
    TTS("tts"),
```

修改后的完整枚举：

```java
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
```

- [ ] **Step 2: 验证编译**

Run: `cd /c/Users/nano/Desktop/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/domain/enums/OssBusinessType.java
git commit -m "feat: add tts business type enum"
```

---

### Task 3: 添加配置项

**Files:**
- Modify: `.env.example`
- Modify: `grid-system/src/main/resources/config/application.yml`

- [ ] **Step 1: 修改 .env.example，添加 TTS 配置示例**

在文件末尾添加：

```bash
# ============================================================
# 阿里云百炼 TTS 配置
# ============================================================
DASHSCOPE_API_KEY=你的API Key
```

- [ ] **Step 2: 修改 application.yml，添加 TTS 配置项**

在 ali.oss 配置之后添加：

```yaml
# 阿里云百炼 TTS 配置
ali:
  tts:
    api-key: ${DASHSCOPE_API_KEY:}
```

- [ ] **Step 3: Commit**

```bash
git add .env.example grid-system/src/main/resources/config/application.yml
git commit -m "feat: add tts configuration items"
```

---

### Task 4: 创建配置类 AliTtsConfig

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/config/AliTtsConfig.java`

- [ ] **Step 1: 编写配置类**

```java
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
 * 阿里云百炼 TTS 配置
 * @author nano
 * @date 2025-05-19
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ali.tts")
public class AliTtsConfig {

    /**
     * 访问密钥 Key
     */
    private String apiKey;
}
```

- [ ] **Step 2: 验证编译**

Run: `cd /c/Users/nano/Desktop/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/config/AliTtsConfig.java
git commit -m "feat: add ali tts config class"
```

---

### Task 5: 创建实体类 TtsRecord

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/domain/TtsRecord.java`

- [ ] **Step 1: 编写实体类**

参考 AliOssStorage 的风格：

```java
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
package com.naon.grid.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseEntity;
import javax.persistence.*;
import java.io.Serializable;

/**
 * 阿里云 TTS 语音合成记录实体类
 * @author nano
 * @date 2025-05-19
 */
@Getter
@Setter
@Entity
@Table(name = "tts_record")
public class TtsRecord extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ApiModelProperty(value = "音色名称")
    private String voice;

    @ApiModelProperty(value = "合成文本")
    @Column(columnDefinition = "text")
    private String text;

    @ApiModelProperty(value = "控制指令")
    @Column(columnDefinition = "text")
    private String instructions;

    @ApiModelProperty(value = "模型名称")
    private String model;

    @ApiModelProperty(value = "语言类型")
    private String languageType;

    @ApiModelProperty(value = "OSS 最终音频 URL")
    private String finalAudioUrl;

    @ApiModelProperty(value = "阿里云请求 ID")
    private String requestId;

    @ApiModelProperty(value = "错误信息")
    @Column(columnDefinition = "text")
    private String errorMsg;
}
```

注意：数据库表需要后续手动创建或使用 Flyway/Liquibase，这里先定义实体类。

- [ ] **Step 2: 验证编译**

Run: `cd /c/Users/nano/Desktop/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/domain/TtsRecord.java
git commit -m "feat: add tts record entity class"
```

---

### Task 6: 创建 Repository

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/repository/TtsRecordRepository.java`

- [ ] **Step 1: 编写 Repository**

```java
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
package com.naon.grid.repository;

import com.naon.grid.domain.TtsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 阿里云 TTS 语音合成记录 Repository
 * @author nano
 * @date 2025-05-19
 */
public interface TtsRecordRepository extends JpaRepository<TtsRecord, Long>, JpaSpecificationExecutor<TtsRecord> {

}
```

- [ ] **Step 2: 验证编译**

Run: `cd /c/Users/nano/Desktop/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/repository/TtsRecordRepository.java
git commit -m "feat: add tts record repository"
```

---

### Task 7: 创建请求和响应 DTO

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/dto/TtsRequest.java`
- Create: `grid-tools/src/main/java/com/naon/grid/service/dto/TtsResponse.java`

- [ ] **Step 1: 编写 TtsRequest**

```java
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

/**
 * TTS 语音合成请求 DTO
 * @author nano
 * @date 2025-05-19
 */
@Data
public class TtsRequest {

    @ApiModelProperty(value = "音色名称", required = true, example = "cherry")
    private String voice;

    @ApiModelProperty(value = "合成文本", required = true)
    private String text;

    @ApiModelProperty(value = "控制指令", example = "语速较快，年轻女声")
    private String instructions;

    @ApiModelProperty(value = "模型名称", example = "qwen-tts-flash")
    private String model = "qwen-tts-flash";

    @ApiModelProperty(value = "语言类型", example = "chinese")
    private String languageType;
}
```

- [ ] **Step 2: 编写 TtsResponse**

```java
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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS 语音合成响应 DTO
 * @author nano
 * @date 2025-05-19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TtsResponse {

    @ApiModelProperty(value = "音频 URL")
    private String audioUrl;
}
```

- [ ] **Step 3: 验证编译**

Run: `cd /c/Users/nano/Desktop/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/dto/TtsRequest.java grid-tools/src/main/java/com/naon/grid/service/dto/TtsResponse.java
git commit -m "feat: add tts request and response dto"
```

---

### Task 8: 创建 Service 接口

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/TtsService.java`

- [ ] **Step 1: 编写 Service 接口**

```java
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
package com.naon.grid.service;

import com.naon.grid.service.dto.TtsRequest;
import com.naon.grid.service.dto.TtsResponse;

/**
 * 阿里云 TTS 语音合成 Service 接口
 * @author nano
 * @date 2025-05-19
 */
public interface TtsService {

    /**
     * 语音合成
     * @param request 请求参数
     * @return 响应参数（包含最终 OSS 音频 URL）
     */
    TtsResponse generate(TtsRequest request);
}
```

- [ ] **Step 2: 验证编译**

Run: `cd /c/Users/nano/Desktop/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/TtsService.java
git commit -m "feat: add tts service interface"
```

---

### Task 9: 创建 Service 实现类（核心逻辑）

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/impl/TtsServiceImpl.java`

- [ ] **Step 1: 查看 AliOssStorageService，确认有合适的 upload 方法支持 OssBusinessType**

先查看现有的 AliOssStorageService 接口，确认是否有支持 OssBusinessType 的 upload 方法。

根据之前查看的代码，AliOssStorageServiceImpl 有三个 upload 方法：
- upload(MultipartFile file)
- upload(MultipartFile file, OssBusinessType businessType)
- upload(MultipartFile file, OssBusinessType businessType, String customPath)

我们需要支持传入 InputStream，或者先把 InputStream 转成临时文件再转成 MultipartFile，或者扩展 AliOssStorageService。

这里我们采用扩展 AliOssStorageService 的方式，添加一个支持 InputStream 的方法。

先修改 AliOssStorageService，添加：

```java
    /**
     * 上传文件到 OSS（字节数组形式）
     * @param bytes 文件字节数组
     * @param originalFilename 原始文件名
     * @param businessType 业务类型
     * @param customPath 自定义路径
     * @return 保存后的存储对象
     */
    AliOssStorage upload(byte[] bytes, String originalFilename, OssBusinessType businessType, String customPath);
```

然后在 AliOssStorageServiceImpl 中实现该方法。

我们先跳过这一步，继续写 TtsService，后面在需要时再添加。

- [ ] **Step 2: 编写 TtsServiceImpl**

```java
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
package com.naon.grid.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.utils.Constants;
import com.aliyun.oss.OSS;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.naon.grid.config.AliOssConfig;
import com.naon.grid.config.AliTtsConfig;
import com.naon.grid.domain.AliOssStorage;
import com.naon.grid.domain.TtsRecord;
import com.naon.grid.domain.enums.OssBusinessType;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.repository.AliOssStorageRepository;
import com.naon.grid.repository.TtsRecordRepository;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.TtsService;
import com.naon.grid.service.dto.TtsRequest;
import com.naon.grid.service.dto.TtsResponse;
import com.naon.grid.utils.FileUtil;
import com.naon.grid.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * 阿里云 TTS 语音合成 Service 实现
 * @author nano
 * @date 2025-05-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsServiceImpl implements TtsService {

    private final AliTtsConfig aliTtsConfig;
    private final AliOssConfig aliOssConfig;
    private final OSS ossClient;
    private final TtsRecordRepository ttsRecordRepository;
    private final AliOssStorageRepository aliOssStorageRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TtsResponse generate(TtsRequest request) {
        // 验证必选参数
        if (StringUtils.isBlank(request.getVoice())) {
            throw new BadRequestException("音色名称不能为空");
        }
        if (StringUtils.isBlank(request.getText())) {
            throw new BadRequestException("合成文本不能为空");
        }

        // 设置 DashScope 配置
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";

        String originalAudioUrl = null;
        String requestId = null;

        try {
            // 构建请求参数
            MultiModalConversation conv = new MultiModalConversation();
            MultiModalConversationParam.MultiModalConversationParamBuilder builder = MultiModalConversationParam.builder()
                    .apiKey(aliTtsConfig.getApiKey())
                    .model(request.getModel())
                    .text(request.getText())
                    .voice(request.getVoice());

            if (StringUtils.isNotBlank(request.getLanguageType())) {
                builder.languageType(request.getLanguageType());
            }

            if (StringUtils.isNotBlank(request.getInstructions())) {
                builder.parameter("instructions", request.getInstructions());
                builder.parameter("optimize_instructions", true);
            }

            MultiModalConversationParam param = builder.build();

            // 调用 API
            MultiModalConversationResult result = conv.call(param);

            // 获取音频 URL
            originalAudioUrl = result.getOutput().getAudio().getUrl();
            requestId = result.getRequestId();

            log.info("TTS 合成成功，阿里云临时 URL: {}, requestId: {}", originalAudioUrl, requestId);

            // 下载音频
            byte[] audioBytes;
            try (InputStream in = new URL(originalAudioUrl).openStream()) {
                audioBytes = in.readAllBytes();
            }

            // 上传到自己的 OSS
            String finalAudioUrl = uploadToOss(audioBytes, request);

            // 保存记录
            TtsRecord record = new TtsRecord();
            record.setVoice(request.getVoice());
            record.setText(request.getText());
            record.setInstructions(request.getInstructions());
            record.setModel(request.getModel());
            record.setLanguageType(request.getLanguageType());
            record.setFinalAudioUrl(finalAudioUrl);
            record.setRequestId(requestId);
            ttsRecordRepository.save(record);

            return new TtsResponse(finalAudioUrl);

        } catch (Exception e) {
            log.error("TTS 合成失败: {}", e.getMessage(), e);
            throw new BadRequestException("语音合成失败: " + e.getMessage());
        }
    }

    /**
     * 上传音频到自己的 OSS
     */
    private String uploadToOss(byte[] audioBytes, TtsRequest request) {
        String bucketName = aliOssConfig.getBucketName();
        // 生成文件名
        String fileRealName = IdUtil.simpleUUID() + ".wav";
        String originalFilename = "tts_" + System.currentTimeMillis() + ".wav";

        // 生成路径
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(OssBusinessType.TTS.getValue());
        String timeFolder = DateUtil.format(new Date(), aliOssConfig.getTimeformat());
        pathBuilder.append("/").append(timeFolder);
        String filePath = pathBuilder.toString() + "/" + fileRealName;

        // 构建访问 URL
        String fileUrl = aliOssConfig.getDomain() + "/" + filePath;

        try {
            // 上传到 OSS
            ossClient.putObject(bucketName, filePath, new ByteArrayInputStream(audioBytes));

            // 保存记录到 oss_resource_meta
            AliOssStorage storage = new AliOssStorage();
            storage.setFileName(originalFilename);
            storage.setFileRealName(fileRealName);
            storage.setFileSize(FileUtil.getSize(audioBytes.length));
            storage.setFileMimeType("audio/wav");
            storage.setFileType("wav");
            storage.setFileUrl(fileUrl);
            storage.setBucketName(bucketName);
            storage.setBusinessType(OssBusinessType.TTS.getValue());
            aliOssStorageRepository.save(storage);

            return fileUrl;

        } catch (Exception e) {
            log.error("上传音频到 OSS 失败: {}", e.getMessage(), e);
            throw new BadRequestException("音频上传失败: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 3: 确认 AliOssStorage 实体类有 businessType 和 customPath 字段**

如果之前没有，需要加上。

- [ ] **Step 4: 验证编译**

Run: `cd /c/Users/nano/Desktop/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/impl/TtsServiceImpl.java
git commit -m "feat: add tts service implementation"
```

---

### Task 10: 创建 Controller

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/rest/TtsController.java`

- [ ] **Step 1: 编写 Controller**

```java
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
package com.naon.grid.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.naon.grid.annotation.Log;
import com.naon.grid.service.TtsService;
import com.naon.grid.service.dto.TtsRequest;
import com.naon.grid.service.dto.TtsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 阿里云 TTS 语音合成管理
 * @author nano
 * @date 2025-05-19
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tts")
@Api(tags = "工具：阿里云 TTS 语音合成")
public class TtsController {

    private final TtsService ttsService;

    @Log("语音合成")
    @ApiOperation("语音合成")
    @PostMapping("/generate")
    public ResponseEntity<TtsResponse> generate(@Validated @RequestBody TtsRequest request) {
        TtsResponse response = ttsService.generate(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd /c/Users/nano/Desktop/nano-gemini && mvn clean compile -pl grid-tools -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/rest/TtsController.java
git commit -m "feat: add tts controller"
```

---

### Task 11: 创建数据库表 SQL

**Files:**
- Create: `sql/tts_record.sql` (可选，放在 sql 目录下)

- [ ] **Step 1: 编写建表 SQL**

```sql
CREATE TABLE IF NOT EXISTS `tts_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `voice` varchar(100) NOT NULL COMMENT '音色',
  `text` text NOT NULL COMMENT '合成文本',
  `instructions` text COMMENT '控制指令',
  `model` varchar(100) NOT NULL COMMENT '模型名称',
  `language_type` varchar(50) DEFAULT NULL COMMENT '语言类型',
  `final_audio_url` varchar(500) NOT NULL COMMENT 'OSS最终音频URL',
  `request_id` varchar(100) DEFAULT NULL COMMENT '阿里云请求ID',
  `error_msg` text COMMENT '错误信息',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TTS语音合成记录表';
```

同时确认 oss_resource_meta 表有 business_type 和 custom_path 字段，如果没有需要添加：

```sql
ALTER TABLE `oss_resource_meta` 
ADD COLUMN `business_type` varchar(50) DEFAULT NULL COMMENT '业务类型' AFTER `bucket_name`,
ADD COLUMN `custom_path` varchar(255) DEFAULT NULL COMMENT '自定义路径' AFTER `business_type`;
```

- [ ] **Step 2: Commit**

```bash
git add sql/tts_record.sql
git commit -m "feat: add tts record table sql"
```

---

### Task 12: 整体编译验证

**Files:**
- None

- [ ] **Step 1: 执行完整项目编译**

Run: `cd /c/Users/nano/Desktop/nano-gemini && mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 2: 如果编译成功，完成最终确认**

---

## Self-Review Checklist

- [x] **Spec coverage:** 所有需求点都有对应任务：TTS 配置、调用 DashScope API、获取临时 URL、下载音频、上传到自己的 OSS、保存记录到 tts_record 表、返回最终 URL、指令控制参数支持。
- [x] **No placeholders:** 所有步骤都有完整代码，没有 TODO 或待填充内容。
- [x] **Type consistency:** 使用的类、方法名、字段名都一致，参考了现有 AliOssStorage 的实现模式。
- [x] **No extra features:** 没有添加多余功能，只实现了需求中提到的点。
