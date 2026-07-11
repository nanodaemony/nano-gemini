# 自动简繁转换集成设计

## 背景

当前项目中汉字 (`char_character.traditional`) 和词汇 (`vocab_word.word_traditional`) 的繁体字段完全依赖管理员手动输入，容易遗漏或出错。

集成 OpenCC (Open Chinese Convert) 的 `s2t`（简体→繁体）能力，在管理员创建/更新时**自动补全**繁体字段，减少人工操作。

## 目标

- 管理员只需输入简体，系统自动生成繁体填入 `traditional` / `wordTraditional`
- 只在管理员未手动填写时自动补全（保留手动覆盖能力）
- 不改变现有的草稿→审核→发布工作流

## 数据流

```
管理员输入 → CreateRequest (character / word + 可选的 traditional / wordTraditional)
                ↓
          Wrapper.toDto()  透传
                ↓
          Service.create() / update()
                ↓
          检查: traditional 为空?
                ↓ 是
          OpenCcUtils.toTraditional(character/word) → 自动填充
                ↓
          存入 draft_content (JSON) → 后续工作流不变
```

## 改动范围

### 1. 新增依赖 — `grid-tools/pom.xml`

```xml
<dependency>
    <groupId>com.github.houbb</groupId>
    <artifactId>opencc4j</artifactId>
    <version>1.8.0</version>
</dependency>
```

### 2. 新增工具类 — `grid-tools/src/main/java/com/naon/grid/utils/OpenCcUtils.java`

```java
package com.naon.grid.utils;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

/**
 * OpenCC 简繁转换工具类
 */
public class OpenCcUtils {

    private OpenCcUtils() {}

    /**
     * 简体转繁体（s2t 标准模式），输入为空时返回 null
     */
    public static String toTraditional(String simplified) {
        if (simplified == null || simplified.isBlank()) {
            return null;
        }
        return ZhConverterUtil.toTraditional(simplified.trim());
    }
}
```

### 3. 改动服务层

**CharCharacterServiceImpl** — 在创建/更新草稿方法中，保存 `draftContent` 之前：

```java
// 自动补全繁体字（仅在未手动填写时）
if (dto.getTraditional() == null || dto.getTraditional().isBlank()) {
    dto.setTraditional(OpenCcUtils.toTraditional(dto.getCharacter()));
}
```

**VocabWordServiceImpl** — 同理：

```java
// 自动补全繁体词汇（仅在未手动填写时）
if (dto.getWordTraditional() == null || dto.getWordTraditional().isBlank()) {
    dto.setWordTraditional(OpenCcUtils.toTraditional(dto.getWord()));
}
```

### 4. 不变的部分

- 数据库表结构不变
- Request/DTO/VO 结构不变
- 草稿→审核→发布工作流不变
- 管理员手动填入的值不会被覆盖（只在为空时转换）
- App 端展示不受影响
- 不做 s2tw/s2hk 等其他转换模式

## 模块归属

`OpenCcUtils` 放在 `grid-tools`，与现有 TTS、翻译、图像等工具类并列。`grid-system` 已依赖 `grid-tools`，可直接调用。
