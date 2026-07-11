# OpenCC 自动简繁转换集成 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在管理员创建/更新汉字和词汇时，若未手动填写繁体字段，自动通过 OpenCC s2t 转换补全。

**Architecture:** 在 `grid-tools` 新增 `OpenCcUtils` 工具类封装 opencc4j 的 `s2t` 转换；在 `grid-system` 的两个 Service 的 `create()`/`update()` 方法中，序列化草稿前检查并自动补全繁体字段。

**Tech Stack:** Java 8, Maven, opencc4j 1.8.0, Spring Boot 2.7.18

## Global Constraints

- Java 8 兼容 — 不使用 Java 9+ API
- 只在 `traditional`/`wordTraditional` 为空时自动补全，不覆盖手动填写值
- 不改变数据库表结构、Request/DTO/VO 结构、草稿工作流
- 使用 s2t 标准模式，不做 s2tw/s2hk

---

### Task 1: 添加 opencc4j 依赖

**Files:**
- Modify: `grid-tools/pom.xml`

**Interfaces:**
- Produces: `com.github.houbb.opencc4j.util.ZhConverterUtil` 在 grid-tools classpath 中可用

- [ ] **Step 1: 在 `grid-tools/pom.xml` 的 `<dependencies>` 末尾添加 opencc4j 依赖**

在 `</dependencies>` 闭合标签之前（最后一行为 spring-boot-starter-mail 依赖之后）插入：

```xml
        <!-- OpenCC 简繁转换 -->
        <dependency>
            <groupId>com.github.houbb</groupId>
            <artifactId>opencc4j</artifactId>
            <version>1.8.0</version>
        </dependency>
```

插入位置：第 89 行 `</dependencies>` 之前。

- [ ] **Step 2: 验证依赖可下载**

```bash
cd grid-tools && mvn dependency:resolve -DincludeArtifactIds=opencc4j
```

Expected: BUILD SUCCESS，能看到 `com.github.houbb:opencc4j:jar:1.8.0` 被 resolve。

- [ ] **Step 3: 提交**

```bash
git add grid-tools/pom.xml
git commit -m "build: add opencc4j dependency for simplified-to-traditional conversion"
```

---

### Task 2: 创建 OpenCcUtils 工具类

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/utils/OpenCcUtils.java`

**Interfaces:**
- Produces: `OpenCcUtils.toTraditional(String simplified): String` — 输入简体，返回繁体；输入为 null 或空白时返回 null

- [ ] **Step 1: 创建工具类文件**

```java
package com.naon.grid.utils;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

/**
 * OpenCC 简繁转换工具类
 *
 * @author nano
 * @date 2026-07-11
 */
public class OpenCcUtils {

    private OpenCcUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 简体转繁体（s2t 标准模式）。
     * 输入为 null 或空白时返回 null。
     *
     * @param simplified 简体中文字符串
     * @return 繁体中文字符串，输入为空时返回 null
     */
    public static String toTraditional(String simplified) {
        if (simplified == null || simplified.trim().isEmpty()) {
            return null;
        }
        return ZhConverterUtil.toTraditional(simplified.trim());
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl grid-tools -am
```

Expected: BUILD SUCCESS，`OpenCcUtils.class` 编译生成。

- [ ] **Step 3: 提交**

```bash
git add grid-tools/src/main/java/com/naon/grid/utils/OpenCcUtils.java
git commit -m "feat: add OpenCcUtils for simplified-to-traditional Chinese conversion"
```

---

### Task 3: 汉字服务层集成自动转换

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

**Interfaces:**
- Consumes: `OpenCcUtils.toTraditional(String): String` (from Task 2)
- Produces: `CharCharacterServiceImpl.create()` 和 `update()` 在保存草稿前自动补全 `traditional`

- [ ] **Step 1: 添加 import**

在文件头部 import 区域（第 22 行附近，`import com.naon.grid.utils.JsonUtils;` 之后）添加：

```java
import com.naon.grid.utils.OpenCcUtils;
```

- [ ] **Step 2: 在 `create()` 方法中添加自动补全逻辑**

在 `create()` 方法（第 286–295 行）中，在 `charCharacter.setDraftContent(JsonUtils.toJson(resources));` 这一行之前（即第 292 行之前）插入：

```java
        // 自动补全繁体字（仅在未手动填写时）
        if (resources.getTraditional() == null || resources.getTraditional().trim().isEmpty()) {
            resources.setTraditional(OpenCcUtils.toTraditional(resources.getCharacter()));
        }
```

- [ ] **Step 3: 在 `update()` 方法中添加自动补全逻辑**

在 `update()` 方法（第 299–323 行）中，在 `charCharacter.setDraftContent(JsonUtils.toJson(resources));` 这一行之前（即第 322 行之前）插入与 Step 2 相同的代码块：

```java
        // 自动补全繁体字（仅在未手动填写时）
        if (resources.getTraditional() == null || resources.getTraditional().trim().isEmpty()) {
            resources.setTraditional(OpenCcUtils.toTraditional(resources.getCharacter()));
        }
```

- [ ] **Step 4: 编译验证**

```bash
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS。

- [ ] **Step 5: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java
git commit -m "feat: auto-fill traditional character field via OpenCC in CharCharacterService"
```

---

### Task 4: 词汇服务层集成自动转换

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`

**Interfaces:**
- Consumes: `OpenCcUtils.toTraditional(String): String` (from Task 2)
- Produces: `VocabWordServiceImpl.create()` 和 `update()` 在保存草稿前自动补全 `wordTraditional`

- [ ] **Step 1: 添加 import**

在文件头部 import 区域（第 13 行附近，`import com.naon.grid.utils.JsonUtils;` 之后）添加：

```java
import com.naon.grid.utils.OpenCcUtils;
```

- [ ] **Step 2: 在 `create()` 方法中添加自动补全逻辑**

在 `create()` 方法（第 254–263 行）中，在 `vocabWord.setDraftContent(JsonUtils.toJson(resources));` 这一行之前（即第 260 行之前）插入：

```java
        // 自动补全繁体词汇（仅在未手动填写时）
        if (resources.getWordTraditional() == null || resources.getWordTraditional().trim().isEmpty()) {
            resources.setWordTraditional(OpenCcUtils.toTraditional(resources.getWord()));
        }
```

- [ ] **Step 3: 在 `update()` 方法中添加自动补全逻辑**

在 `update()` 方法（第 267–289 行）中，在 `vocabWord.setDraftContent(JsonUtils.toJson(resources));` 这一行之前（即第 288 行之前）插入与 Step 2 相同的代码块：

```java
        // 自动补全繁体词汇（仅在未手动填写时）
        if (resources.getWordTraditional() == null || resources.getWordTraditional().trim().isEmpty()) {
            resources.setWordTraditional(OpenCcUtils.toTraditional(resources.getWord()));
        }
```

- [ ] **Step 4: 编译验证**

```bash
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS。

- [ ] **Step 5: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java
git commit -m "feat: auto-fill word_traditional field via OpenCC in VocabWordService"
```

---

### Task 5: 全量构建验证

**Files:** (none — build-only task)

- [ ] **Step 1: 全量构建**

```bash
mvn clean package -DskipTests
```

Expected: BUILD SUCCESS，所有模块编译打包通过。

- [ ] **Step 2: 启动应用验证（可选）**

```bash
cd grid-bootstrap && mvn spring-boot:run
```

然后可以通过 Swagger (http://localhost:8000/doc.html) 测试：
1. POST 创建汉字，只填 `character="爱"`，不填 `traditional`，查看返回的草稿中 `traditional` 是否自动填充为 `愛`
2. POST 创建词汇，只填 `word="学习"`，不填 `wordTraditional`，查看返回的草稿中 `wordTraditional` 是否自动填充为 `學習`
3. 手动填写 `traditional`/`wordTraditional` 后更新，确认不被覆盖
