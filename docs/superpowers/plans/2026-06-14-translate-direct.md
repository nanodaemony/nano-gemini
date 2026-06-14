# 指定源语言翻译功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 `POST /api/translate/direct` 端点，支持指定源语言和目标语言进行翻译

**Architecture:** 新建独立 DTO 和 Controller 端点，Service 层新增方法并抽取公共 DashScope 调用逻辑，避免与现有中文→X 翻译逻辑耦合。数据库新增 `source_language` 字段记录翻译方向。

**Tech Stack:** Spring Boot 2.7.18, DashScope SDK, JPA, MySQL

---

### Task 1: 创建 `TranslateDirectRequest` DTO

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/dto/TranslateDirectRequest.java`

- [ ] **Step 1: 创建文件**

```java
package com.naon.grid.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 指定源语言翻译请求 DTO
 * @author nano
 * @date 2026-06-14
 */
@Data
public class TranslateDirectRequest {

    @NotBlank(message = "源文本不能为空")
    @ApiModelProperty(value = "源文本", required = true)
    private String sourceText;

    @ApiModelProperty(value = "源语言代码，默认 zh", example = "en")
    private String sourceLanguage = "zh";

    @NotBlank(message = "目标语言不能为空")
    @ApiModelProperty(value = "目标语言代码", required = true, example = "ja")
    private String targetLanguage;

    @ApiModelProperty(value = "模型名称")
    private String model = "qwen-plus";
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/dto/TranslateDirectRequest.java
git commit -m "feat: add TranslateDirectRequest DTO for direct translation"
```

---

### Task 2: 创建 `TranslateDirectResponse` DTO

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/dto/TranslateDirectResponse.java`

- [ ] **Step 1: 创建文件**

```java
package com.naon.grid.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指定源语言翻译响应 DTO
 * @author nano
 * @date 2026-06-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslateDirectResponse {

    @ApiModelProperty(value = "记录 ID")
    private Long recordId;

    @ApiModelProperty(value = "源文本")
    private String sourceText;

    @ApiModelProperty(value = "源语言代码")
    private String sourceLanguage;

    @ApiModelProperty(value = "译文")
    private String targetText;

    @ApiModelProperty(value = "目标语言代码")
    private String targetLanguage;
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/dto/TranslateDirectResponse.java
git commit -m "feat: add TranslateDirectResponse DTO for direct translation"
```

---

### Task 3: 修改 `TranslateRecord` 实体 — 新增 `sourceLanguage` 字段

**Files:**
- Modify: `grid-tools/src/main/java/com/naon/grid/domain/TranslateRecord.java`

- [ ] **Step 1: 在 `sourceText` 字段之后添加 `sourceLanguage` 字段**

```java
@ApiModelProperty(value = "源语言代码")
@Column(length = 50)
private String sourceLanguage;
```

紧接在 `private String sourceText;` 之后（第 44 行之后）。

- [ ] **Step 2: 提交**

```bash
git add grid-tools/src/main/java/com/naon/grid/domain/TranslateRecord.java
git commit -m "feat: add sourceLanguage field to TranslateRecord entity"
```

---

### Task 4: 修改 `TranslateService` 接口 — 新增 `translateDirect` 方法

**Files:**
- Modify: `grid-tools/src/main/java/com/naon/grid/service/TranslateService.java`

- [ ] **Step 1: 添加新方法**

```java
/**
 * 执行指定源语言和目标语言的翻译
 * @param request 翻译请求
 * @return 翻译响应
 */
TranslateDirectResponse translateDirect(TranslateDirectRequest request);
```

在现有 `translate(TranslateRequest request)` 方法之后添加。

需要新增 import：
```java
import com.naon.grid.service.dto.TranslateDirectRequest;
import com.naon.grid.service.dto.TranslateDirectResponse;
```

- [ ] **Step 2: 提交**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/TranslateService.java
git commit -m "feat: add translateDirect method to TranslateService interface"
```

---

### Task 5: 修改 `TranslateServiceImpl` — 实现 `translateDirect` + 抽取公共方法

**Files:**
- Modify: `grid-tools/src/main/java/com/naon/grid/service/impl/TranslateServiceImpl.java`

- [ ] **Step 1: 新增 import**

在文件顶部 import 区域添加：
```java
import com.naon.grid.service.dto.TranslateDirectRequest;
import com.naon.grid.service.dto.TranslateDirectResponse;
```

- [ ] **Step 2: 抽取公共 `callDashScope` 方法**

在 `translate()` 方法之前或之后添加：

```java
/**
 * 调用 DashScope API
 * @param model 模型名称
 * @param prompt 提示词
 * @return API 响应结果
 */
private GenerationResult callDashScope(String model, String prompt) {
    Generation gen = new Generation();
    GenerationParam param = GenerationParam.builder()
            .apiKey(aliTranslateConfig.getApiKey())
            .model(model)
            .prompt(prompt)
            .build();
    return gen.call(param);
}
```

- [ ] **Step 3: 修改现有 `translate()` 方法，复用 `callDashScope`**

将原有 API 调用代码从：
```java
Generation gen = new Generation();
GenerationParam param = GenerationParam.builder()
        .apiKey(aliTranslateConfig.getApiKey())
        .model(request.getModel())
        .prompt(prompt)
        .build();
GenerationResult result = gen.call(param);
```

改为：
```java
GenerationResult result = callDashScope(request.getModel(), prompt);
```

并在构建 `TranslateRecord` 时添加 `sourceLanguage = "zh"`：
```java
record.setSourceLanguage("zh");
```

- [ ] **Step 4: 实现 `translateDirect` 方法**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public TranslateDirectResponse translateDirect(TranslateDirectRequest request) {
    // 验证参数
    if (StringUtils.isBlank(request.getSourceText())) {
        throw new BadRequestException("源文本不能为空");
    }
    if (StringUtils.isBlank(request.getTargetLanguage())) {
        throw new BadRequestException("目标语言不能为空");
    }
    if (request.getSourceText().length() > 500) {
        throw new BadRequestException("源文本长度不能超过500字");
    }

    // 验证语言代码
    LanguageCodeEnum sourceEnum = LanguageCodeEnum.fromCode(request.getSourceLanguage());
    if (sourceEnum == null) {
        throw new BadRequestException("不支持的源语言代码: " + request.getSourceLanguage());
    }
    LanguageCodeEnum targetEnum = LanguageCodeEnum.fromCode(request.getTargetLanguage());
    if (targetEnum == null) {
        throw new BadRequestException("不支持的目标语言代码: " + request.getTargetLanguage());
    }

    Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";

    try {
        // 构建动态 prompt — 使用英文名称 codeName
        String prompt = String.format("请将以下%s翻译成%s，只返回译文，不要添加任何解释：\n\n%s",
                sourceEnum.getCodeName(), targetEnum.getCodeName(), request.getSourceText());

        // 调用 DashScope API（复用公共方法）
        GenerationResult result = callDashScope(request.getModel(), prompt);
        log.info("翻译结果，result: {}", JSONUtil.toJsonStr(result));

        // 提取翻译结果
        String targetText = null;
        if (result.getOutput() != null && result.getOutput().getChoices() != null
                && !result.getOutput().getChoices().isEmpty()) {
            targetText = result.getOutput().getChoices().get(0).getMessage().getContent();
        }
        String requestId = result.getRequestId();

        log.info("翻译成功，sourceText: {}, sourceLanguage: {}, targetLanguage: {}, requestId: {}",
                request.getSourceText(), request.getSourceLanguage(), request.getTargetLanguage(), requestId);

        // 保存记录
        TranslateRecord record = new TranslateRecord();
        record.setSourceText(request.getSourceText());
        record.setSourceLanguage(request.getSourceLanguage());
        record.setTargetText(targetText);
        record.setTargetLanguage(request.getTargetLanguage());
        record.setModel(request.getModel());
        record.setRequestId(requestId);
        translateRecordRepository.save(record);

        return new TranslateDirectResponse(record.getId(), request.getSourceText(),
                request.getSourceLanguage(), targetText, request.getTargetLanguage());

    } catch (Exception e) {
        log.error("翻译失败: {}", e.getMessage(), e);
        throw new BadRequestException("翻译失败: " + e.getMessage());
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/impl/TranslateServiceImpl.java
git commit -m "feat: implement translateDirect and extract common callDashScope method"
```

---

### Task 6: 修改 `TranslateController` — 新增 `/direct` 端点

**Files:**
- Modify: `grid-tools/src/main/java/com/naon/grid/rest/TranslateController.java`

- [ ] **Step 1: 新增 import**

```java
import com.naon.grid.service.dto.TranslateDirectRequest;
import com.naon.grid.service.dto.TranslateDirectResponse;
```

- [ ] **Step 2: 新增端点方法**

在现有 `translate()` 方法之后添加：

```java
@Log("指定源语言翻译")
@ApiOperation("指定源语言翻译")
@AnonymousPostMapping("/direct")
public ResponseEntity<TranslateDirectResponse> translateDirect(@Validated @RequestBody TranslateDirectRequest request) {
    return new ResponseEntity<>(translateService.translateDirect(request), HttpStatus.OK);
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-tools/src/main/java/com/naon/grid/rest/TranslateController.java
git commit -m "feat: add POST /api/translate/direct endpoint"
```

---

### Task 7: 数据库迁移 — 新增 `source_language` 列

- [ ] **Step 1: 执行 SQL**

```sql
ALTER TABLE translate_record 
ADD COLUMN source_language VARCHAR(50) NULL COMMENT '源语言代码' AFTER source_text;
```

（执行方式：通过 MySQL 客户端或项目中的数据库迁移脚本执行）

- [ ] **Step 2: 验证**

```sql
DESC translate_record;
-- 应看到 source_language 列在 source_text 之后
```

---

### Task 8: 编译验证

- [ ] **Step 1: 编译项目**

```bash
mvn clean compile -pl grid-tools -am -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 2: 如有测试则运行**

```bash
mvn test -pl grid-tools -am
```

Expected: `BUILD SUCCESS`（全部测试通过）

- [ ] **Step 3: 最终提交（将所有未提交的改动一起提交）**

```bash
git add -A
git commit -m "feat: implement direct translation with source language support"
```

---

### Task 9: 最终检查清单

- [ ] 现有 `POST /api/translate` 完全不受影响
- [ ] 新 `POST /api/translate/direct` 正常工作
- [ ] `sourceLanguage` 默认值为 `zh`，不传时等同于中文→X
- [ ] prompt 动态构建，使用英文语言名称
- [ ] 翻译记录完整保存 sourceLanguage
- [ ] 现有记录 sourceLanguage = NULL，向下兼容
- [ ] 编译通过，无测试失败
