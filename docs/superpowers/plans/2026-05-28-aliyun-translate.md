# 阿里云百炼翻译集成实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps using checkbox (`- [ ]`) syntax for tracking.

**Goal:** 集成阿里云百炼通用翻译服务，实现中文到其他语言的翻译，保存翻译记录，提供管理员 API。

**Architecture:** 遵循项目现有代码结构，在 grid-tools 模块中新增配置类、实体类、Repository、Service、Controller，复用已有的 DashScope SDK 依赖。

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, DashScope SDK 2.22.17

---

## File Structure

| 文件 | 操作 | 说明 |
|------|------|------|
| `grid-tools/src/main/java/com/naon/grid/config/AliTranslateConfig.java` | 新建 | 翻译配置类 |
| `grid-tools/src/main/java/com/naon/grid/domain/TranslateRecord.java` | 新建 | 翻译记录实体 |
| `grid-tools/src/main/java/com/naon/grid/repository/TranslateRecordRepository.java` | 新建 | Repository 接口 |
| `grid-tools/src/main/java/com/naon/grid/service/dto/TranslateRequest.java` | 新建 | 请求 DTO |
| `grid-tools/src/main/java/com/naon/grid/service/dto/TranslateResponse.java` | 新建 | 响应 DTO |
| `grid-tools/src/main/java/com/naon/grid/service/TranslateService.java` | 新建 | Service 接口 |
| `grid-tools/src/main/java/com/naon/grid/service/impl/TranslateServiceImpl.java` | 新建 | Service 实现 |
| `grid-tools/src/main/java/com/naon/grid/rest/TranslateController.java` | 新建 | REST 控制器 |
| `grid-system/src/main/resources/config/application.yml` | 修改 | 添加翻译配置 |

---

### Task 1: 创建配置类 AliTranslateConfig

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/config/AliTranslateConfig.java`

- [ ] **Step 1: 创建配置类**

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
 * 阿里云百炼翻译配置
 * @author nano
 * @date 2026-05-28
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ali.translate")
public class AliTranslateConfig {

    /**
     * 访问密钥 Key
     */
    private String apiKey;
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-tools/src/main/java/com/naon/grid/config/AliTranslateConfig.java
git commit -m "feat: add AliTranslateConfig for aliyun translate"
```

---

### Task 2: 创建实体类 TranslateRecord

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/domain/TranslateRecord.java`

- [ ] **Step 1: 创建实体类**

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
 * 翻译记录实体类
 * @author nano
 * @date 2026-05-28
 */
@Getter
@Setter
@Entity
@Table(name = "translate_record")
public class TranslateRecord extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ApiModelProperty(value = "源文本（中文）")
    @Column(columnDefinition = "text")
    private String sourceText;

    @ApiModelProperty(value = "目标文本（译文）")
    @Column(columnDefinition = "text")
    private String targetText;

    @ApiModelProperty(value = "目标语言代码")
    private String targetLanguage;

    @ApiModelProperty(value = "使用的模型")
    private String model;

    @ApiModelProperty(value = "阿里云请求 ID")
    private String requestId;
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-tools/src/main/java/com/naon/grid/domain/TranslateRecord.java
git commit -m "feat: add TranslateRecord entity"
```

---

### Task 3: 创建 Repository 接口

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/repository/TranslateRecordRepository.java`

- [ ] **Step 1: 创建 Repository**

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

import com.naon.grid.domain.TranslateRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 翻译记录 Repository
 * @author nano
 * @date 2026-05-28
 */
public interface TranslateRecordRepository extends JpaRepository<TranslateRecord, Long>, JpaSpecificationExecutor<TranslateRecord> {
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-tools/src/main/java/com/naon/grid/repository/TranslateRecordRepository.java
git commit -m "feat: add TranslateRecordRepository"
```

---

### Task 4: 创建 DTO 类

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/dto/TranslateRequest.java`
- Create: `grid-tools/src/main/java/com/naon/grid/service/dto/TranslateResponse.java`

- [ ] **Step 1: 创建 TranslateRequest**

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

import javax.validation.constraints.NotBlank;

/**
 * 翻译请求 DTO
 * @author nano
 * @date 2026-05-28
 */
@Data
public class TranslateRequest {

    @NotBlank(message = "源文本不能为空")
    @ApiModelProperty(value = "源文本（中文）", required = true)
    private String sourceText;

    @NotBlank(message = "目标语言不能为空")
    @ApiModelProperty(value = "目标语言代码", required = true)
    private String targetLanguage;

    @ApiModelProperty(value = "模型名称")
    private String model = "qwen-plus";
}
```

- [ ] **Step 2: 创建 TranslateResponse**

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
 * 翻译响应 DTO
 * @author nano
 * @date 2026-05-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslateResponse {

    @ApiModelProperty(value = "记录 ID")
    private Long recordId;

    @ApiModelProperty(value = "源文本")
    private String sourceText;

    @ApiModelProperty(value = "译文")
    private String targetText;

    @ApiModelProperty(value = "目标语言")
    private String targetLanguage;
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/dto/TranslateRequest.java
git add grid-tools/src/main/java/com/naon/grid/service/dto/TranslateResponse.java
git commit -m "feat: add TranslateRequest and TranslateResponse DTOs"
```

---

### Task 5: 创建 Service 接口和实现

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/TranslateService.java`
- Create: `grid-tools/src/main/java/com/naon/grid/service/impl/TranslateServiceImpl.java`

- [ ] **Step 1: 创建 TranslateService 接口**

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

import com.naon.grid.service.dto.TranslateRequest;
import com.naon.grid.service.dto.TranslateResponse;

/**
 * 翻译服务接口
 * @author nano
 * @date 2026-05-28
 */
public interface TranslateService {

    /**
     * 执行翻译
     * @param request 翻译请求
     * @return 翻译响应
     */
    TranslateResponse translate(TranslateRequest request);
}
```

- [ ] **Step 2: 创建 TranslateServiceImpl 实现类**

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

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.naon.grid.config.AliTranslateConfig;
import com.naon.grid.domain.TranslateRecord;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.repository.TranslateRecordRepository;
import com.naon.grid.service.TranslateService;
import com.naon.grid.service.dto.TranslateRequest;
import com.naon.grid.service.dto.TranslateResponse;
import com.naon.grid.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * 翻译服务实现
 * @author nano
 * @date 2026-05-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateServiceImpl implements TranslateService {

    private final AliTranslateConfig aliTranslateConfig;
    private final TranslateRecordRepository translateRecordRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TranslateResponse translate(TranslateRequest request) {
        // 验证必选参数
        if (StringUtils.isBlank(request.getSourceText())) {
            throw new BadRequestException("源文本不能为空");
        }
        if (StringUtils.isBlank(request.getTargetLanguage())) {
            throw new BadRequestException("目标语言不能为空");
        }

        // 验证文本长度（不超过500字）
        if (request.getSourceText().length() > 500) {
            throw new BadRequestException("源文本长度不能超过500字");
        }

        // 设置 DashScope 配置
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";

        String targetText = null;
        String requestId = null;

        try {
            // 构建翻译提示词
            String prompt = buildTranslatePrompt(request.getSourceText(), request.getTargetLanguage());

            // 构建请求参数
            Generation gen = new Generation();
            GenerationParam param = GenerationParam.builder()
                    .apiKey(aliTranslateConfig.getApiKey())
                    .model(request.getModel())
                    .prompt(prompt)
                    .build();

            // 调用 API
            GenerationResult result = gen.call(param);

            // 获取翻译结果
            targetText = result.getOutput().getText();
            requestId = result.getRequestId();

            log.info("翻译成功，sourceText: {}, targetLanguage: {}, requestId: {}",
                    request.getSourceText(), request.getTargetLanguage(), requestId);

            // 保存记录
            TranslateRecord record = new TranslateRecord();
            record.setSourceText(request.getSourceText());
            record.setTargetText(targetText);
            record.setTargetLanguage(request.getTargetLanguage());
            record.setModel(request.getModel());
            record.setRequestId(requestId);
            translateRecordRepository.save(record);

            return new TranslateResponse(record.getId(), request.getSourceText(), targetText, request.getTargetLanguage());

        } catch (Exception e) {
            log.error("翻译失败: {}", e.getMessage(), e);
            throw new BadRequestException("翻译失败: " + e.getMessage());
        }
    }

    /**
     * 构建翻译提示词
     */
    private String buildTranslatePrompt(String text, String targetLanguage) {
        // 常用语言映射到完整名称
        String languageName = getLanguageFullName(targetLanguage);
        return String.format("请将以下中文文本翻译成%s，只返回译文，不要添加任何解释：\n\n%s", languageName, text);
    }

    /**
     * 获取语言完整名称
     */
    private String getLanguageFullName(String languageCode) {
        switch (languageCode.toLowerCase()) {
            case "en": return "英语";
            case "ja": return "日语";
            case "ko": return "韩语";
            case "fr": return "法语";
            case "de": return "德语";
            case "es": return "西班牙语";
            case "ru": return "俄语";
            default: return languageCode;
        }
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/TranslateService.java
git add grid-tools/src/main/java/com/naon/grid/service/impl/TranslateServiceImpl.java
git commit -m "feat: add TranslateService and implementation"
```

---

### Task 6: 创建 Controller

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/rest/TranslateController.java`

- [ ] **Step 1: 创建 TranslateController**

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
import com.naon.grid.annotation.Log;
import com.naon.grid.service.TranslateService;
import com.naon.grid.service.dto.TranslateRequest;
import com.naon.grid.service.dto.TranslateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 翻译控制器
 * @author nano
 * @date 2026-05-28
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "翻译管理")
@RequestMapping("/api/translate")
public class TranslateController {

    private final TranslateService translateService;

    @Log("执行翻译")
    @ApiOperation("执行翻译")
    @PostMapping
    public ResponseEntity<TranslateResponse> translate(@Validated @RequestBody TranslateRequest request) {
        return new ResponseEntity<>(translateService.translate(request), HttpStatus.OK);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-tools/src/main/java/com/naon/grid/rest/TranslateController.java
git commit -m "feat: add TranslateController"
```

---

### Task 7: 更新 application.yml 配置

**Files:**
- Modify: `grid-system/src/main/resources/config/application.yml`

- [ ] **Step 1: 读取当前配置文件**

- [ ] **Step 2: 添加翻译配置（在 ali: 配置块下）**

```yaml
# 阿里云百炼翻译配置
  translate:
    api-key: ${DASHSCOPE_API_KEY:}
```

- [ ] **Step 3: 提交**

```bash
git add grid-system/src/main/resources/config/application.yml
git commit -m "feat: add ali.translate configuration"
```

---

### Task 8: 创建数据库迁移 SQL

**Files:**
- Create: `docs/sql/translate_record.sql`

- [ ] **Step 1: 创建建表 SQL**

```sql
-- 翻译记录表
CREATE TABLE IF NOT EXISTS `translate_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `source_text` text COMMENT '源文本（中文）',
  `target_text` text COMMENT '目标文本（译文）',
  `target_language` varchar(50) DEFAULT NULL COMMENT '目标语言代码',
  `model` varchar(100) DEFAULT NULL COMMENT '使用的模型',
  `request_id` varchar(100) DEFAULT NULL COMMENT '阿里云请求 ID',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='翻译记录表';
```

- [ ] **Step 2: 提交**

```bash
mkdir -p docs/sql
git add docs/sql/translate_record.sql
git commit -m "feat: add translate_record table SQL"
```

---

## Plan Self-Review

**1. Spec coverage:**
- ✅ 配置类 - Task 1
- ✅ 实体类 - Task 2
- ✅ Repository - Task 3
- ✅ DTOs - Task 4
- ✅ Service - Task 5
- ✅ Controller - Task 6
- ✅ 配置更新 - Task 7
- ✅ 数据库 SQL - Task 8

**2. Placeholder scan:** No placeholders found.

**3. Type consistency:** All type names, method names, and file paths are consistent.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-28-aliyun-translate.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
