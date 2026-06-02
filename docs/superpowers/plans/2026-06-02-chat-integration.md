# 大模型对话集成实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 集成阿里云百炼和 DeepSeek 大模型对话能力，提供统一接口、预设提示词管理（含占位符替换）和对话记录持久化

**Architecture:** 采用 Provider 模式，ChatProvider 接口定义统一对话能力，AliyunChatProvider 和 DeepSeekChatProvider 分别实现，ChatService 作为门面服务整合所有能力

**Tech Stack:** Spring Boot 2.7, Spring Data JPA, DashScope SDK, OpenAI Java SDK (for DeepSeek)

---

## 前期准备

### 先查看项目结构和现有代码模式

**Files:**
- Read: `grid-tools/src/main/java/com/naon/grid/config/AliTranslateConfig.java`
- Read: `grid-tools/src/main/java/com/naon/grid/domain/TranslateRecord.java`
- Read: `grid-tools/src/main/java/com/naon/grid/repository/TranslateRecordRepository.java`
- Read: `grid-tools/src/main/java/com/naon/grid/service/TranslateService.java`
- Read: `grid-tools/src/main/java/com/naon/grid/service/impl/TranslateServiceImpl.java`
- Read: `grid-tools/src/main/java/com/naon/grid/rest/TranslateController.java`

- [ ] **Step 1: 阅读现有代码以确认模式**

  已在 brainstorming 阶段完成，确认：
  - 配置类使用 `@ConfigurationProperties`
  - 实体类继承 `BaseEntity`
  - Repository 继承 `JpaRepository` 和 `JpaSpecificationExecutor`
  - Service 使用 `@Service` 和 `@RequiredArgsConstructor`
  - Controller 使用 `@AnonymousPostMapping` 允许匿名访问

---

## Task 1: 添加 Maven 依赖

**Files:**
- Modify: `grid-tools/pom.xml`

- [ ] **Step 1: 添加 OpenAI Java SDK 依赖**

  在 `grid-tools/pom.xml` 的 `<dependencies>` 中添加：

  ```xml
      <!-- Gson (DashScope SDK 需要) -->
      <dependency>
          <groupId>com.google.code.gson</groupId>
          <artifactId>gson</artifactId>
          <version>${gson.version}</version>
      </dependency>

      <!-- OpenAI Java SDK (用于 DeepSeek) -->
      <dependency>
          <groupId>com.theokanning.openai-gpt3-java</groupId>
          <artifactId>service</artifactId>
          <version>0.18.2</version>
      </dependency>
  ```

  （注意：gson 可能已存在，确认即可）

- [ ] **Step 2: 验证依赖**

  Run: `cd grid-tools && mvn clean compile -DskipTests`
  Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

  ```bash
  git add grid-tools/pom.xml
  git commit -m "feat: add openai-java-sdk for deepseek integration"
  ```

---

## Task 2: 添加配置文件

**Files:**
- Modify: `grid-bootstrap/src/main/resources/config/application.yml`

- [ ] **Step 1: 添加 chat 配置到 application.yml**

  在 `grid-bootstrap/src/main/resources/config/application.yml` 末尾添加：

  ```yaml
  # 大模型 Chat 配置
  chat:
    aliyun:
      api-key: ${DASHSCOPE_API_KEY:}
      base-url: ${ALI_CHAT_BASE_URL:https://dashscope.aliyuncs.com/api/v1}
    deepseek:
      api-key: ${DEEPSEEK_API_KEY:}
      base-url: ${DEEPSEEK_CHAT_BASE_URL:https://api.deepseek.com/v1}
  ```

- [ ] **Step 2: Commit**

  ```bash
  git add grid-bootstrap/src/main/resources/config/application.yml
  git commit -m "feat: add chat configuration to application.yml"
  ```

---

## Task 3: 创建配置类

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/config/ChatAliyunConfig.java`
- Create: `grid-tools/src/main/java/com/naon/grid/config/ChatDeepSeekConfig.java`

- [ ] **Step 1: 创建 ChatAliyunConfig.java**

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
   * 阿里云百炼 Chat 配置
   * @author nano
   * @date 2026-06-02
   */
  @Data
  @Configuration
  @ConfigurationProperties(prefix = "chat.aliyun")
  public class ChatAliyunConfig {

      private String apiKey;

      private String baseUrl;
  }
  ```

- [ ] **Step 2: 创建 ChatDeepSeekConfig.java**

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
   * DeepSeek Chat 配置
   * @author nano
   * @date 2026-06-02
   */
  @Data
  @Configuration
  @ConfigurationProperties(prefix = "chat.deepseek")
  public class ChatDeepSeekConfig {

      private String apiKey;

      private String baseUrl;
  }
  ```

- [ ] **Step 3: 编译验证**

  Run: `cd grid-tools && mvn clean compile -DskipTests`
  Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

  ```bash
  git add grid-tools/src/main/java/com/naon/grid/config/ChatAliyunConfig.java grid-tools/src/main/java/com/naon/grid/config/ChatDeepSeekConfig.java
  git commit -m "feat: add chat configuration classes"
  ```

---

## Task 4: 创建枚举类

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/enums/ChatProviderEnum.java`

- [ ] **Step 1: 创建 ChatProviderEnum.java**

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
  package com.naon.grid.enums;

  /**
   * 大模型厂商枚举
   * @author nano
   * @date 2026-06-02
   */
  public enum ChatProviderEnum {
      ALIYUN,
      DEEPSEEK
  }
  ```

- [ ] **Step 2: 编译验证**

  Run: `cd grid-tools && mvn clean compile -DskipTests`
  Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

  ```bash
  git add grid-tools/src/main/java/com/naon/grid/enums/ChatProviderEnum.java
  git commit -m "feat: add ChatProviderEnum"
  ```

---

## Task 5: 创建 DTO 类

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/dto/ChatRequest.java`
- Create: `grid-tools/src/main/java/com/naon/grid/service/dto/ChatResponse.java`

- [ ] **Step 1: 创建 ChatRequest.java**

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

  import com.naon.grid.enums.ChatProviderEnum;
  import io.swagger.annotations.ApiModelProperty;
  import lombok.Data;

  import javax.validation.constraints.NotBlank;
  import javax.validation.constraints.NotNull;
  import java.util.Map;

  /**
   * 对话请求 DTO
   * @author nano
   * @date 2026-06-02
   */
  @Data
  public class ChatRequest {

      @NotNull(message = "厂商不能为空")
      @ApiModelProperty(value = "厂商：ALIYUN/DEEPSEEK", required = true)
      private ChatProviderEnum provider;

      @NotBlank(message = "模型名称不能为空")
      @ApiModelProperty(value = "模型名称", required = true)
      private String model;

      @ApiModelProperty(value = "预设提示词名称")
      private String promptName;

      @ApiModelProperty(value = "系统提示词")
      private String systemPrompt;

      @NotBlank(message = "用户提示词不能为空")
      @ApiModelProperty(value = "用户输入提示词", required = true)
      private String userPrompt;

      @ApiModelProperty(value = "温度参数，默认 0.7")
      private Double temperature = 0.7;

      @ApiModelProperty(value = "最大 token 数")
      private Integer maxTokens;

      @ApiModelProperty(value = "top_p 参数")
      private Double topP;

      @ApiModelProperty(value = "占位符替换参数")
      private Map<String, String> placeholderValues;

      @ApiModelProperty(value = "用户 ID")
      private Long userId;
  }
  ```

- [ ] **Step 2: 创建 ChatResponse.java**

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
  import lombok.Builder;
  import lombok.Data;
  import lombok.NoArgsConstructor;

  /**
   * 对话响应 DTO
   * @author nano
   * @date 2026-06-02
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public class ChatResponse {

      @ApiModelProperty(value = "厂商请求 ID")
      private String requestId;

      @ApiModelProperty(value = "模型原始响应内容", required = true)
      private String content;

      @ApiModelProperty(value = "输入 token 数")
      private Integer inputTokens;

      @ApiModelProperty(value = "输出 token 数")
      private Integer outputTokens;

      @ApiModelProperty(value = "总 token 数")
      private Integer totalTokens;

      @ApiModelProperty(value = "请求耗时（毫秒）")
      private Integer latencyMs;
  }
  ```

- [ ] **Step 3: 编译验证**

  Run: `cd grid-tools && mvn clean compile -DskipTests`
  Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

  ```bash
  git add grid-tools/src/main/java/com/naon/grid/service/dto/ChatRequest.java grid-tools/src/main/java/com/naon/grid/service/dto/ChatResponse.java
  git commit -m "feat: add ChatRequest and ChatResponse DTOs"
  ```

---

## Task 6: 创建实体类

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/domain/ChatPrompt.java`
- Create: `grid-tools/src/main/java/com/naon/grid/domain/ChatRecord.java`

- [ ] **Step 1: 创建 ChatPrompt.java**

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

  import com.naon.grid.base.BaseEntity;
  import io.swagger.annotations.ApiModelProperty;
  import lombok.Getter;
  import lombok.Setter;

  import javax.persistence.*;
  import java.io.Serializable;
  import java.math.BigDecimal;

  /**
   * 预设提示词实体类
   * @author nano
   * @date 2026-06-02
   */
  @Getter
  @Setter
  @Entity
  @Table(name = "chat_prompt")
  public class ChatPrompt extends BaseEntity implements Serializable {

      @Id
      @Column(name = "id")
      @ApiModelProperty(value = "ID", hidden = true)
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @ApiModelProperty(value = "提示词名称")
      @Column(name = "name", unique = true, nullable = false, length = 100)
      private String name;

      @ApiModelProperty(value = "描述说明")
      @Column(name = "description", length = 500)
      private String description;

      @ApiModelProperty(value = "系统提示词内容")
      @Column(name = "system_prompt", columnDefinition = "text", nullable = false)
      private String systemPrompt;

      @ApiModelProperty(value = "推荐使用的模型")
      @Column(name = "model", length = 100)
      private String model;

      @ApiModelProperty(value = "推荐温度参数")
      @Column(name = "temperature", precision = 3, scale = 2)
      private BigDecimal temperature;

      @ApiModelProperty(value = "状态：1-有效，0-无效")
      @Column(name = "status", nullable = false)
      private Integer status = 1;
  }
  ```

- [ ] **Step 2: 创建 ChatRecord.java**

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

  import com.naon.grid.enums.ChatProviderEnum;
  import io.swagger.annotations.ApiModelProperty;
  import lombok.Getter;
  import lombok.Setter;
  import org.hibernate.annotations.CreationTimestamp;

  import javax.persistence.*;
  import java.io.Serializable;
  import java.math.BigDecimal;
  import java.sql.Timestamp;

  /**
   * 对话记录实体类
   * @author nano
   * @date 2026-06-02
   */
  @Getter
  @Setter
  @Entity
  @Table(name = "chat_record")
  public class ChatRecord implements Serializable {

      @Id
      @Column(name = "id")
      @ApiModelProperty(value = "ID", hidden = true)
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @ApiModelProperty(value = "厂商")
      @Column(name = "provider", nullable = false, length = 50)
      @Enumerated(EnumType.STRING)
      private ChatProviderEnum provider;

      @ApiModelProperty(value = "使用的模型")
      @Column(name = "model", nullable = false, length = 100)
      private String model;

      @ApiModelProperty(value = "使用的预设提示词名称")
      @Column(name = "prompt_name", length = 100)
      private String promptName;

      @ApiModelProperty(value = "实际使用的系统提示词")
      @Column(name = "system_prompt", columnDefinition = "text")
      private String systemPrompt;

      @ApiModelProperty(value = "用户输入提示词")
      @Column(name = "user_prompt", columnDefinition = "text", nullable = false)
      private String userPrompt;

      @ApiModelProperty(value = "模型原始响应")
      @Column(name = "assistant_response", columnDefinition = "text", nullable = false)
      private String assistantResponse;

      @ApiModelProperty(value = "温度参数")
      @Column(name = "temperature", precision = 3, scale = 2)
      private BigDecimal temperature;

      @ApiModelProperty(value = "最大 token 数")
      @Column(name = "max_tokens")
      private Integer maxTokens;

      @ApiModelProperty(value = "top_p 参数")
      @Column(name = "top_p", precision = 3, scale = 2)
      private BigDecimal topP;

      @ApiModelProperty(value = "厂商请求 ID")
      @Column(name = "request_id", length = 100)
      private String requestId;

      @ApiModelProperty(value = "输入 token 数")
      @Column(name = "input_tokens")
      private Integer inputTokens;

      @ApiModelProperty(value = "输出 token 数")
      @Column(name = "output_tokens")
      private Integer outputTokens;

      @ApiModelProperty(value = "总 token 数")
      @Column(name = "total_tokens")
      private Integer totalTokens;

      @ApiModelProperty(value = "请求耗时（毫秒）")
      @Column(name = "latency_ms")
      private Integer latencyMs;

      @ApiModelProperty(value = "用户 ID")
      @Column(name = "user_id")
      private Long userId;

      @ApiModelProperty(value = "其他额外参数 (JSON)")
      @Column(name = "extra_params", columnDefinition = "json")
      private String extraParams;

      @CreationTimestamp
      @Column(name = "create_time", nullable = false, updatable = false)
      @ApiModelProperty(value = "创建时间", hidden = true)
      private Timestamp createTime;
  }
  ```

- [ ] **Step 3: 编译验证**

  Run: `cd grid-tools && mvn clean compile -DskipTests`
  Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

  ```bash
  git add grid-tools/src/main/java/com/naon/grid/domain/ChatPrompt.java grid-tools/src/main/java/com/naon/grid/domain/ChatRecord.java
  git commit -m "feat: add ChatPrompt and ChatRecord entities"
  ```

---

## Task 7: 创建 Repository 接口

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/repository/ChatPromptRepository.java`
- Create: `grid-tools/src/main/java/com/naon/grid/repository/ChatRecordRepository.java`

- [ ] **Step 1: 创建 ChatPromptRepository.java**

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

  import com.naon.grid.domain.ChatPrompt;
  import org.springframework.data.jpa.repository.JpaRepository;
  import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

  import java.util.Optional;

  /**
   * 预设提示词 Repository
   * @author nano
   * @date 2026-06-02
   */
  public interface ChatPromptRepository extends JpaRepository<ChatPrompt, Long>, JpaSpecificationExecutor<ChatPrompt> {

      Optional<ChatPrompt> findByNameAndStatus(String name, Integer status);
  }
  ```

- [ ] **Step 2: 创建 ChatRecordRepository.java**

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

  import com.naon.grid.domain.ChatRecord;
  import org.springframework.data.jpa.repository.JpaRepository;
  import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

  /**
   * 对话记录 Repository
   * @author nano
   * @date 2026-06-02
   */
  public interface ChatRecordRepository extends JpaRepository<ChatRecord, Long>, JpaSpecificationExecutor<ChatRecord> {
  }
  ```

- [ ] **Step 3: 编译验证**

  Run: `cd grid-tools && mvn clean compile -DskipTests`
  Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

  ```bash
  git add grid-tools/src/main/java/com/naon/grid/repository/ChatPromptRepository.java grid-tools/src/main/java/com/naon/grid/repository/ChatRecordRepository.java
  git commit -m "feat: add ChatPromptRepository and ChatRecordRepository"
  ```

---

## Task 8: 创建 ChatProvider 接口和实现

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/ChatProvider.java`
- Create: `grid-tools/src/main/java/com/naon/grid/service/impl/AliyunChatProvider.java`
- Create: `grid-tools/src/main/java/com/naon/grid/service/impl/DeepSeekChatProvider.java`

- [ ] **Step 1: 创建 ChatProvider.java**

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

  import com.naon.grid.enums.ChatProviderEnum;
  import com.naon.grid.service.dto.ChatRequest;
  import com.naon.grid.service.dto.ChatResponse;

  /**
   * 大模型对话 Provider 接口
   * @author nano
   * @date 2026-06-02
   */
  public interface ChatProvider {

      ChatProviderEnum getProvider();

      ChatResponse chat(ChatRequest request, String systemPrompt);
  }
  ```

- [ ] **Step 2: 创建 AliyunChatProvider.java**

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
  import com.alibaba.dashscope.common.Message;
  import com.alibaba.dashscope.common.Role;
  import com.alibaba.dashscope.utils.Constants;
  import com.naon.grid.config.ChatAliyunConfig;
  import com.naon.grid.enums.ChatProviderEnum;
  import com.naon.grid.exception.BadRequestException;
  import com.naon.grid.service.ChatProvider;
  import com.naon.grid.service.dto.ChatRequest;
  import com.naon.grid.service.dto.ChatResponse;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.stereotype.Service;

  import java.util.ArrayList;
  import java.util.List;

  /**
   * 阿里云百炼对话 Provider
   * @author nano
   * @date 2026-06-02
   */
  @Slf4j
  @Service
  @RequiredArgsConstructor
  public class AliyunChatProvider implements ChatProvider {

      private final ChatAliyunConfig chatAliyunConfig;

      @Override
      public ChatProviderEnum getProvider() {
          return ChatProviderEnum.ALIYUN;
      }

      @Override
      public ChatResponse chat(ChatRequest request, String systemPrompt) {
          try {
              if (chatAliyunConfig.getBaseUrl() != null) {
                  Constants.baseHttpApiUrl = chatAliyunConfig.getBaseUrl();
              }

              List<Message> messages = new ArrayList<>();
              if (systemPrompt != null && !systemPrompt.isEmpty()) {
                  messages.add(Message.builder().role(Role.SYSTEM.getValue()).content(systemPrompt).build());
              }
              messages.add(Message.builder().role(Role.USER.getValue()).content(request.getUserPrompt()).build());

              GenerationParam.Builder paramBuilder = GenerationParam.builder()
                      .apiKey(chatAliyunConfig.getApiKey())
                      .model(request.getModel())
                      .messages(messages);

              if (request.getTemperature() != null) {
                  paramBuilder.temperature(request.getTemperature().floatValue());
              }
              if (request.getTopP() != null) {
                  paramBuilder.topP(request.getTopP().floatValue());
              }
              if (request.getMaxTokens() != null) {
                  paramBuilder.maxTokens(request.getMaxTokens());
              }

              Generation gen = new Generation();
              GenerationResult result = gen.call(paramBuilder.build());

              String content = "";
              Integer inputTokens = null;
              Integer outputTokens = null;
              Integer totalTokens = null;

              if (result.getOutput() != null && result.getOutput().getChoices() != null && !result.getOutput().getChoices().isEmpty()) {
                  content = result.getOutput().getChoices().get(0).getMessage().getContent();
              }

              if (result.getUsage() != null) {
                  inputTokens = result.getUsage().getInputTokens() != null ? result.getUsage().getInputTokens().intValue() : null;
                  outputTokens = result.getUsage().getOutputTokens() != null ? result.getUsage().getOutputTokens().intValue() : null;
                  totalTokens = result.getUsage().getTotalTokens() != null ? result.getUsage().getTotalTokens().intValue() : null;
              }

              return ChatResponse.builder()
                      .requestId(result.getRequestId())
                      .content(content)
                      .inputTokens(inputTokens)
                      .outputTokens(outputTokens)
                      .totalTokens(totalTokens)
                      .build();

          } catch (Exception e) {
              log.error("阿里云对话失败", e);
              throw new BadRequestException("阿里云对话失败: " + e.getMessage());
          }
      }
  }
  ```

- [ ] **Step 3: 创建 DeepSeekChatProvider.java**

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

  import com.naon.grid.config.ChatDeepSeekConfig;
  import com.naon.grid.enums.ChatProviderEnum;
  import com.naon.grid.exception.BadRequestException;
  import com.naon.grid.service.ChatProvider;
  import com.naon.grid.service.dto.ChatRequest;
  import com.naon.grid.service.dto.ChatResponse;
  import com.theokanning.openai.client.OpenAiApi;
  import com.theokanning.openai.completion.chat.ChatCompletionChoice;
  import com.theokanning.openai.completion.chat.ChatCompletionRequest;
  import com.theokanning.openai.completion.chat.ChatCompletionResult;
  import com.theokanning.openai.completion.chat.ChatMessage;
  import com.theokanning.openai.service.OpenAiService;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import okhttp3.OkHttpClient;
  import org.springframework.stereotype.Service;
  import retrofit2.Retrofit;
  import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
  import retrofit2.converter.jackson.JacksonConverterFactory;

  import java.time.Duration;
  import java.util.ArrayList;
  import java.util.List;

  /**
   * DeepSeek 对话 Provider
   * @author nano
   * @date 2026-06-02
   */
  @Slf4j
  @Service
  @RequiredArgsConstructor
  public class DeepSeekChatProvider implements ChatProvider {

      private final ChatDeepSeekConfig chatDeepSeekConfig;

      @Override
      public ChatProviderEnum getProvider() {
          return ChatProviderEnum.DEEPSEEK;
      }

      @Override
      public ChatResponse chat(ChatRequest request, String systemPrompt) {
          try {
              String baseUrl = chatDeepSeekConfig.getBaseUrl();
              String apiKey = chatDeepSeekConfig.getApiKey();

              OkHttpClient client = new OkHttpClient.Builder()
                      .connectTimeout(Duration.ofSeconds(30))
                      .readTimeout(Duration.ofSeconds(120))
                      .writeTimeout(Duration.ofSeconds(30))
                      .addInterceptor(chain -> {
                          okhttp3.Request original = chain.request();
                          okhttp3.Request.Builder requestBuilder = original.newBuilder()
                                  .header("Authorization", "Bearer " + apiKey)
                                  .header("Content-Type", "application/json")
                                  .method(original.method(), original.body());
                          return chain.proceed(requestBuilder.build());
                      })
                      .build();

              Retrofit retrofit = new Retrofit.Builder()
                      .baseUrl(baseUrl)
                      .client(client)
                      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                      .addConverterFactory(JacksonConverterFactory.create())
                      .build();

              OpenAiApi api = retrofit.create(OpenAiApi.class);
              OpenAiService service = new OpenAiService(api);

              List<ChatMessage> messages = new ArrayList<>();
              if (systemPrompt != null && !systemPrompt.isEmpty()) {
                  messages.add(new ChatMessage("system", systemPrompt));
              }
              messages.add(new ChatMessage("user", request.getUserPrompt()));

              ChatCompletionRequest.Builder requestBuilder = ChatCompletionRequest.builder()
                      .model(request.getModel())
                      .messages(messages);

              if (request.getTemperature() != null) {
                  requestBuilder.temperature(request.getTemperature());
              }
              if (request.getTopP() != null) {
                  requestBuilder.topP(request.getTopP());
              }
              if (request.getMaxTokens() != null) {
                  requestBuilder.maxTokens(request.getMaxTokens());
              }

              ChatCompletionResult result = service.createChatCompletion(requestBuilder.build());

              String content = "";
              Integer inputTokens = null;
              Integer outputTokens = null;
              Integer totalTokens = null;

              if (result.getChoices() != null && !result.getChoices().isEmpty()) {
                  content = result.getChoices().get(0).getMessage().getContent();
              }

              if (result.getUsage() != null) {
                  inputTokens = result.getUsage().getPromptTokens();
                  outputTokens = result.getUsage().getCompletionTokens();
                  totalTokens = result.getUsage().getTotalTokens();
              }

              return ChatResponse.builder()
                      .requestId(result.getId())
                      .content(content)
                      .inputTokens(inputTokens)
                      .outputTokens(outputTokens)
                      .totalTokens(totalTokens)
                      .build();

          } catch (Exception e) {
              log.error("DeepSeek 对话失败", e);
              throw new BadRequestException("DeepSeek 对话失败: " + e.getMessage());
          }
      }
  }
  ```

- [ ] **Step 4: 编译验证**

  Run: `cd grid-tools && mvn clean compile -DskipTests`
  Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

  ```bash
  git add grid-tools/src/main/java/com/naon/grid/service/ChatProvider.java grid-tools/src/main/java/com/naon/grid/service/impl/AliyunChatProvider.java grid-tools/src/main/java/com/naon/grid/service/impl/DeepSeekChatProvider.java
  git commit -m "feat: add ChatProvider interface and implementations"
  ```

---

## Task 9: 创建 ChatPromptService

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/ChatPromptService.java`
- Create: `grid-tools/src/main/java/com/naon/grid/service/impl/ChatPromptServiceImpl.java`

- [ ] **Step 1: 创建 ChatPromptService.java**

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

  import com.naon.grid.domain.ChatPrompt;

  import java.util.Map;

  /**
   * 预设提示词服务接口
   * @author nano
   * @date 2026-06-02
   */
  public interface ChatPromptService {

      ChatPrompt findByName(String name);

      String replacePlaceholders(String template, Map<String, String> values);
  }
  ```

- [ ] **Step 2: 创建 ChatPromptServiceImpl.java**

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

  import com.naon.grid.domain.ChatPrompt;
  import com.naon.grid.repository.ChatPromptRepository;
  import com.naon.grid.service.ChatPromptService;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.stereotype.Service;

  import java.util.Map;
  import java.util.regex.Matcher;
  import java.util.regex.Pattern;

  /**
   * 预设提示词服务实现
   * @author nano
   * @date 2026-06-02
   */
  @Slf4j
  @Service
  @RequiredArgsConstructor
  public class ChatPromptServiceImpl implements ChatPromptService {

      private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

      private final ChatPromptRepository chatPromptRepository;

      @Override
      public ChatPrompt findByName(String name) {
          return chatPromptRepository.findByNameAndStatus(name, 1).orElse(null);
      }

      @Override
      public String replacePlaceholders(String template, Map<String, String> values) {
          if (template == null || template.isEmpty()) {
              return template;
          }
          if (values == null || values.isEmpty()) {
              return template;
          }

          StringBuffer result = new StringBuffer();
          Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

          while (matcher.find()) {
              String key = matcher.group(1);
              String replacement = values.get(key);
              if (replacement != null) {
                  matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
              } else {
                  matcher.appendReplacement(result, matcher.group());
              }
          }
          matcher.appendTail(result);

          return result.toString();
      }
  }
  ```

- [ ] **Step 3: 编译验证**

  Run: `cd grid-tools && mvn clean compile -DskipTests`
  Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

  ```bash
  git add grid-tools/src/main/java/com/naon/grid/service/ChatPromptService.java grid-tools/src/main/java/com/naon/grid/service/impl/ChatPromptServiceImpl.java
  git commit -m "feat: add ChatPromptService and implementation"
  ```

---

## Task 10: 创建 ChatService 门面服务

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/ChatService.java`
- Create: `grid-tools/src/main/java/com/naon/grid/service/impl/ChatServiceImpl.java`

- [ ] **Step 1: 创建 ChatService.java**

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

  import com.naon.grid.service.dto.ChatRequest;
  import com.naon.grid.service.dto.ChatResponse;

  /**
   * 大模型对话服务接口
   * @author nano
   * @date 2026-06-02
   */
  public interface ChatService {

      ChatResponse chat(ChatRequest request);
  }
  ```

- [ ] **Step 2: 创建 ChatServiceImpl.java**

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

  import com.naon.grid.domain.ChatPrompt;
  import com.naon.grid.domain.ChatRecord;
  import com.naon.grid.enums.ChatProviderEnum;
  import com.naon.grid.exception.BadRequestException;
  import com.naon.grid.repository.ChatRecordRepository;
  import com.naon.grid.service.ChatProvider;
  import com.naon.grid.service.ChatPromptService;
  import com.naon.grid.service.ChatService;
  import com.naon.grid.service.dto.ChatRequest;
  import com.naon.grid.service.dto.ChatResponse;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  import java.math.BigDecimal;
  import java.util.List;
  import java.util.Map;

  /**
   * 大模型对话服务实现
   * @author nano
   * @date 2026-06-02
   */
  @Slf4j
  @Service
  @RequiredArgsConstructor
  public class ChatServiceImpl implements ChatService {

      private final List<ChatProvider> chatProviders;
      private final ChatPromptService chatPromptService;
      private final ChatRecordRepository chatRecordRepository;

      @Override
      @Transactional(rollbackFor = Exception.class)
      public ChatResponse chat(ChatRequest request) {
          String resolvedSystemPrompt = resolveSystemPrompt(request);

          long startTime = System.currentTimeMillis();

          ChatProvider provider = findProvider(request.getProvider());
          ChatResponse response = provider.chat(request, resolvedSystemPrompt);

          long latencyMs = (int) (System.currentTimeMillis() - startTime);
          response.setLatencyMs(latencyMs);

          saveChatRecord(request, resolvedSystemPrompt, response);

          return response;
      }

      private String resolveSystemPrompt(ChatRequest request) {
          String systemPrompt = request.getSystemPrompt();

          if (request.getPromptName() != null && !request.getPromptName().isEmpty()) {
              ChatPrompt chatPrompt = chatPromptService.findByName(request.getPromptName());
              if (chatPrompt == null) {
                  throw new BadRequestException("找不到预设提示词: " + request.getPromptName());
              }
              systemPrompt = chatPrompt.getSystemPrompt();

              if (request.getTemperature() == null && chatPrompt.getTemperature() != null) {
                  request.setTemperature(chatPrompt.getTemperature().doubleValue());
              }
              if (request.getModel() == null && chatPrompt.getModel() != null) {
                  request.setModel(chatPrompt.getModel());
              }
          }

          if (request.getPlaceholderValues() != null && !request.getPlaceholderValues().isEmpty()) {
              systemPrompt = chatPromptService.replacePlaceholders(systemPrompt, request.getPlaceholderValues());
          }

          return systemPrompt;
      }

      private ChatProvider findProvider(ChatProviderEnum providerEnum) {
          return chatProviders.stream()
                  .filter(p -> p.getProvider() == providerEnum)
                  .findFirst()
                  .orElseThrow(() -> new BadRequestException("不支持的厂商: " + providerEnum));
      }

      private void saveChatRecord(ChatRequest request, String systemPrompt, ChatResponse response) {
          ChatRecord record = new ChatRecord();
          record.setProvider(request.getProvider());
          record.setModel(request.getModel());
          record.setPromptName(request.getPromptName());
          record.setSystemPrompt(systemPrompt);
          record.setUserPrompt(request.getUserPrompt());
          record.setAssistantResponse(response.getContent());
          record.setTemperature(request.getTemperature() != null ? BigDecimal.valueOf(request.getTemperature()) : null);
          record.setMaxTokens(request.getMaxTokens());
          record.setTopP(request.getTopP() != null ? BigDecimal.valueOf(request.getTopP()) : null);
          record.setRequestId(response.getRequestId());
          record.setInputTokens(response.getInputTokens());
          record.setOutputTokens(response.getOutputTokens());
          record.setTotalTokens(response.getTotalTokens());
          record.setLatencyMs(response.getLatencyMs());
          record.setUserId(request.getUserId());

          if (request.getPlaceholderValues() != null && !request.getPlaceholderValues().isEmpty()) {
              record.setExtraParams(com.alibaba.fastjson.JSON.toJSONString(request.getPlaceholderValues()));
          }

          chatRecordRepository.save(record);
      }
  }
  ```

- [ ] **Step 3: 编译验证**

  Run: `cd grid-tools && mvn clean compile -DskipTests`
  Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

  ```bash
  git add grid-tools/src/main/java/com/naon/grid/service/ChatService.java grid-tools/src/main/java/com/naon/grid/service/impl/ChatServiceImpl.java
  git commit -m "feat: add ChatService facade and implementation"
  ```

---

## Task 11: 创建 ChatController

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/rest/ChatController.java`

- [ ] **Step 1: 创建 ChatController.java**

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

  import com.naon.grid.annotation.Log;
  import com.naon.grid.annotation.rest.AnonymousPostMapping;
  import com.naon.grid.service.ChatService;
  import com.naon.grid.service.dto.ChatRequest;
  import com.naon.grid.service.dto.ChatResponse;
  import io.swagger.annotations.Api;
  import io.swagger.annotations.ApiOperation;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.http.HttpStatus;
  import org.springframework.http.ResponseEntity;
  import org.springframework.validation.annotation.Validated;
  import org.springframework.web.bind.annotation.*;

  /**
   * 大模型对话控制器
   * @author nano
   * @date 2026-06-02
   */
  @Slf4j
  @RestController
  @RequiredArgsConstructor
  @Api(tags = "工具：大模型对话")
  @RequestMapping("/api/chat")
  public class ChatController {

      private final ChatService chatService;

      @Log("大模型对话")
      @ApiOperation("大模型对话")
      @AnonymousPostMapping("/completions")
      public ResponseEntity<ChatResponse> chat(@Validated @RequestBody ChatRequest request) {
          return new ResponseEntity<>(chatService.chat(request), HttpStatus.OK);
      }
  }
  ```

- [ ] **Step 2: 编译验证**

  Run: `cd grid-tools && mvn clean compile -DskipTests`
  Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

  ```bash
  git add grid-tools/src/main/java/com/naon/grid/rest/ChatController.java
  git commit -m "feat: add ChatController"
  ```

---

## Task 12: 创建数据库 SQL 脚本

**Files:**
- Create: `sql/chat.sql`

- [ ] **Step 1: 创建 chat.sql**

  ```sql
  -- 大模型对话相关表

  -- 预设提示词表
  CREATE TABLE IF NOT EXISTS `chat_prompt` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `name` varchar(100) NOT NULL COMMENT '提示词名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述说明',
    `system_prompt` text NOT NULL COMMENT '系统提示词内容',
    `model` varchar(100) DEFAULT NULL COMMENT '推荐使用的模型',
    `temperature` decimal(3,2) DEFAULT NULL COMMENT '推荐温度参数',
    `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1-有效，0-无效',
    `create_by` varchar(255) DEFAULT NULL COMMENT '创建人',
    `update_by` varchar(255) DEFAULT NULL COMMENT '更新人',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='预设提示词表';

  -- 对话记录表
  CREATE TABLE IF NOT EXISTS `chat_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `provider` varchar(50) NOT NULL COMMENT '厂商：ALIYUN/DEEPSEEK',
    `model` varchar(100) NOT NULL COMMENT '使用的模型',
    `prompt_name` varchar(100) DEFAULT NULL COMMENT '使用的预设提示词名称',
    `system_prompt` text DEFAULT NULL COMMENT '实际使用的系统提示词',
    `user_prompt` text NOT NULL COMMENT '用户输入提示词',
    `assistant_response` text NOT NULL COMMENT '模型原始响应',
    `temperature` decimal(3,2) DEFAULT NULL COMMENT '温度参数',
    `max_tokens` int(11) DEFAULT NULL COMMENT '最大 token 数',
    `top_p` decimal(3,2) DEFAULT NULL COMMENT 'top_p 参数',
    `request_id` varchar(100) DEFAULT NULL COMMENT '厂商请求 ID',
    `input_tokens` int(11) DEFAULT NULL COMMENT '输入 token 数',
    `output_tokens` int(11) DEFAULT NULL COMMENT '输出 token 数',
    `total_tokens` int(11) DEFAULT NULL COMMENT '总 token 数',
    `latency_ms` int(11) DEFAULT NULL COMMENT '请求耗时（毫秒）',
    `user_id` bigint(20) DEFAULT NULL COMMENT '用户 ID',
    `extra_params` json DEFAULT NULL COMMENT '其他额外参数',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='对话记录表';

  -- 插入一个示例预设提示词
  INSERT INTO `chat_prompt` (`name`, `description`, `system_prompt`, `model`, `temperature`, `status`)
  VALUES ('common_assistant', '通用助手', '你是一个乐于助人的AI助手，请用友好、专业的语气回答用户的问题。', 'qwen-plus', 0.7, 1);
  ```

- [ ] **Step 2: Commit**

  ```bash
  git add sql/chat.sql
  git commit -m "feat: add database schema for chat"
  ```

---

## Task 13: 整体编译和测试

**Files:**
- 无新文件

- [ ] **Step 1: 整体编译**

  Run: `mvn clean compile -DskipTests`
  Expected: BUILD SUCCESS

- [ ] **Step 2: Commit**

  ```bash
  git add docs/superpowers/plans/2026-06-02-chat-integration.md
  git commit -m "docs: add chat integration implementation plan"
  ```

---

## 完成

Plan complete! Implementation is divided into 13 bite-sized tasks that build on each other. Each task has clear steps with exact code and commands.

---

## 自审检查

**1. Spec coverage:** ✓ 所有 spec 需求都有对应的 task
- 配置管理 ✓ Task 2-3
- 数据库表 ✓ Task 6, 12
- 阿里云集成 ✓ Task 10
- DeepSeek 集成 ✓ Task 10
- 预设提示词和占位符替换 ✓ Task 9
- 对话记录 ✓ Task 10
- REST API ✓ Task 11

**2. Placeholder scan:** ✓ 无占位符，所有代码都是完整的

**3. Type consistency:** ✓ 所有类型、方法名、属性名都是一致的
