# 汉字笔顺模块 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增汉字笔顺独立表和查询接口，从 `char_character` 中移除冗余的 `stroke` 字段。

**Architecture:** 在 `grid-system` 模块的 `character` 包下新增 `CharStroke` Entity/Repo/Service，在 `CharCharacterController` 新增 `/api/character/stroke/{character}` 查询接口。笔顺数据通过 Python 脚本从 hanzi-writer JSON 文件导出为 SQL 批量导入。

**Tech Stack:** Python 3（导出脚本）、Spring Boot 2.7 + JPA（后端）、Fastjson2（JSON 解析）、MySQL

---

### Task 1: Python 数据导出脚本

**Files:**
- Create: `scripts/export_stroke_data.py`

- [ ] **Step 1: Write the export script**

```python
#!/usr/bin/env python3
"""
将 hanzi-writer-data 的 JSON 笔顺文件导出为 SQL INSERT 语句。
用法: python scripts/export_stroke_data.py
"""

import json
import os
import glob

INPUT_DIR = r"C:/Users/nano/Desktop/hanzi-writer-data/data"
OUTPUT_FILE = r"C:/Users/nano/Desktop/nano-gemini/sql/char_stroke_data.sql"

def main():
    json_files = sorted(glob.glob(os.path.join(INPUT_DIR, "*.json")))
    print(f"找到 {len(json_files)} 个 JSON 文件")

    values = []
    for filepath in json_files:
        char_name = os.path.splitext(os.path.basename(filepath))[0]
        with open(filepath, "r", encoding="utf-8") as f:
            stroke_data = f.read()
        # 转义单引号
        stroke_escaped = stroke_data.replace("'", "''")
        values.append(f"('{char_name}', '{stroke_escaped}', 1)")

    sql = f"INSERT INTO `char_stroke` (`character`, `stroke`, `status`) VALUES\n"
    sql += ",\n".join(values) + ";\n"

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write(sql)

    print(f"已导出 {len(values)} 条记录到 {OUTPUT_FILE}")

if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 创建 scripts 目录并运行脚本**

```bash
mkdir -p "C:/Users/nano/Desktop/nano-gemini/scripts"
cd "C:/Users/nano/Desktop/nano-gemini"
python scripts/export_stroke_data.py
```

预期输出:
```
找到 9574 个 JSON 文件
已导出 9574 条记录到 C:/Users/nano/Desktop/nano-gemini/sql/char_stroke_data.sql
```

- [ ] **Step 3: 验证输出文件**

```bash
wc -l "C:/Users/nano/Desktop/nano-gemini/sql/char_stroke_data.sql"
head -c 500 "C:/Users/nano/Desktop/nano-gemini/sql/char_stroke_data.sql"
```

预期：文件非空，以 `INSERT INTO` 开头，包含有效的 SQL 语法。

---

### Task 2: SQL DDL 变更

**Files:**
- Modify: `sql/biz_character.sql`

- [ ] **Step 1: 在 biz_character.sql 末尾追加 char_stroke 建表语句**

打开 `sql/biz_character.sql`，在文件末尾（部首表之后）追加：

```sql

-- 汉字笔顺表
-- 注：存储 hanzi-writer 笔顺动画数据（JSON 格式），由脚本导入，为静态只读数据。
CREATE TABLE `char_stroke` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '汉字笔顺ID',
    `character` VARCHAR(32) NOT NULL COMMENT '汉字',
    `stroke` TEXT DEFAULT NULL COMMENT '汉字笔顺JSON（hanzi-writer格式，含strokes/medians/radStrokes）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效，0-无效',
    PRIMARY KEY (`id`),
    INDEX `idx_character` (`character`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='汉字笔顺表';
```

- [ ] **Step 2: 从 char_character 建表语句中移除 stroke 行**

在 `biz_character.sql` 中找到 `char_character` 的 CREATE TABLE，删除以下行：

```sql
  `stroke` varchar(4096) NULL DEFAULT NULL COMMENT '笔顺',
```

---

### Task 3: 创建 CharStroke Entity

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharStroke.java`

- [ ] **Step 1: 编写 Entity 类**

```java
package com.naon.grid.backend.domain.character;

import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "char_stroke")
public class CharStroke implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "汉字笔顺ID", hidden = true)
    private Long id;

    @Column(name = "`character`", nullable = false, length = 32)
    @ApiModelProperty(value = "汉字")
    private String character;

    @Column(name = "stroke", columnDefinition = "text")
    @ApiModelProperty(value = "汉字笔顺JSON（hanzi-writer格式）")
    private String stroke;

    @Column(name = "create_time")
    @ApiModelProperty(value = "创建时间", hidden = true)
    private Timestamp createTime;

    @Column(name = "update_time")
    @ApiModelProperty(value = "更新时间", hidden = true)
    private Timestamp updateTime;

    @Column(name = "status")
    @ApiModelProperty(value = "有效状态：1-有效，0-无效", hidden = true)
    private Integer status = StatusEnum.ENABLED.getCode();
}
```

- [ ] **Step 2: 创建 CharStrokeRepository**

```java
package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharStroke;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CharStrokeRepository extends JpaRepository<CharStroke, Long> {

    Optional<CharStroke> findByCharacterAndStatus(String character, Integer status);
}
```

---

### Task 4: 创建 CharStrokeVO

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharStrokeVO.java`

- [ ] **Step 1: 编写 VO 类**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CharStrokeVO implements Serializable {

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "笔顺SVG路径数据")
    private List<String> strokes;

    @ApiModelProperty(value = "笔顺坐标参考线数据（每个元素为笔画的坐标点列表）")
    private List<List<List<Integer>>> medians;

    @ApiModelProperty(value = "部首笔画索引（部分汉字有此数据）")
    private List<Integer> radStrokes;
}
```

---

### Task 5: 创建 CharStrokeWrapper

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharStrokeWrapper.java`

- [ ] **Step 1: 编写 Wrapper 类**

使用项目已有的 Fastjson2（`com.alibaba.fastjson2.JSON`、`JSONObject`、`JSONArray`）：

```java
package com.naon.grid.backend.rest.wrapper;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.naon.grid.backend.rest.vo.CharStrokeVO;

import java.util.Collections;
import java.util.List;

public class CharStrokeWrapper {

    private CharStrokeWrapper() {
    }

    /**
     * 将笔顺JSON字符串转换为VO
     *
     * @param character  汉字
     * @param strokeJson hanzi-writer格式的笔顺JSON字符串（含strokes/medians/radStrokes）
     * @return CharStrokeVO，strokeJson为null时返回仅含character的空VO
     */
    @SuppressWarnings("unchecked")
    public static CharStrokeVO toStrokeVO(String character, String strokeJson) {
        CharStrokeVO vo = new CharStrokeVO();
        vo.setCharacter(character);
        if (strokeJson == null) {
            return vo;
        }
        JSONObject obj = JSON.parseObject(strokeJson);
        if (obj == null) {
            return vo;
        }

        // strokes: SVG路径字符串列表
        JSONArray strokesArr = obj.getJSONArray("strokes");
        if (strokesArr != null) {
            vo.setStrokes((List<String>) (List<?>) strokesArr.toJavaList(String.class));
        } else {
            vo.setStrokes(Collections.emptyList());
        }

        // medians: 坐标参考线（List<List<List<Integer>>>）
        JSONArray mediansArr = obj.getJSONArray("medians");
        if (mediansArr != null) {
            vo.setMedians((List<List<List<Integer>>>) (List<?>) mediansArr.toJavaList(List.class));
        } else {
            vo.setMedians(Collections.emptyList());
        }

        // radStrokes: 部首笔画索引（可选字段，可能不存在）
        JSONArray radArr = obj.getJSONArray("radStrokes");
        if (radArr != null) {
            vo.setRadStrokes((List<Integer>) (List<?>) radArr.toJavaList(Integer.class));
        }

        return vo;
    }
}
```

---

### Task 6: 创建 CharStrokeService

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/character/CharStrokeService.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharStrokeServiceImpl.java`

- [ ] **Step 1: 编写 Service 接口**

```java
package com.naon.grid.backend.service.character;

public interface CharStrokeService {

    /**
     * 根据汉字查询笔顺JSON
     *
     * @param character 汉字
     * @return 笔顺JSON字符串（hanzi-writer格式），不存在返回null
     */
    String findByCharacter(String character);
}
```

- [ ] **Step 2: 编写 Service 实现**

```java
package com.naon.grid.backend.service.character.impl;

import com.naon.grid.backend.domain.character.CharStroke;
import com.naon.grid.backend.repo.character.CharStrokeRepository;
import com.naon.grid.backend.service.character.CharStrokeService;
import com.naon.grid.enums.StatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CharStrokeServiceImpl implements CharStrokeService {

    private final CharStrokeRepository charStrokeRepository;

    @Override
    public String findByCharacter(String character) {
        return charStrokeRepository
                .findByCharacterAndStatus(character, StatusEnum.ENABLED.getCode())
                .map(CharStroke::getStroke)
                .orElse(null);
    }
}
```

---

### Task 7: Controller 新增笔顺查询接口

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java`

- [ ] **Step 1: 注入 CharStrokeService 并新增接口**

在 `CharCharacterController` 中新增字段和接口：

1. 在 `private final CharCharacterService charCharacterService;` 后面增加：
```java
    private final CharStrokeService charStrokeService;
```

2. 在文件末尾（delete 方法之后）新增查询接口：

```java
    @Log("查询汉字笔顺")
    @ApiOperation("根据汉字查询笔顺数据（SVG路径、坐标参考线）")
    @AnonymousGetMapping("/stroke/{character}")
    public ResponseEntity<CharStrokeVO> findStrokeByCharacter(@PathVariable String character) {
        String strokeJson = charStrokeService.findByCharacter(character);
        CharStrokeVO vo = CharStrokeWrapper.toStrokeVO(character, strokeJson);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }
```

3. 新增 import：
```java
import com.naon.grid.backend.rest.vo.CharStrokeVO;
import com.naon.grid.backend.rest.wrapper.CharStrokeWrapper;
import com.naon.grid.backend.service.character.CharStrokeService;
```

---

### Task 8: 从 Entity 和 DTO 中移除 stroke 字段

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharCharacter.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDraftDto.java`

- [ ] **Step 1: CharCharacter.java — 删除 stroke 字段及相关注解**

删除以下行（约第 59-60 行）：
```java
    @Column(name = "stroke", length = 4096)
    private String stroke;
```

- [ ] **Step 2: CharCharacterDto.java — 删除 stroke 字段**

删除以下行（约第 43-44 行）：
```java
    @ApiModelProperty(value = "笔画")
    private String stroke;
```

- [ ] **Step 3: CharCharacterDraftDto.java — 删除 stroke 字段**

删除以下行（约第 42-43 行）：
```java
    @ApiModelProperty(value = "笔画")
    private String stroke;
```

---

### Task 9: 从 Request 和 VO 中移除 stroke 字段

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java`

- [ ] **Step 1: CharCharacterCreateRequest.java — 删除 stroke 字段**

删除以下行（约第 47-48 行）：
```java
    @ApiModelProperty(value = "笔画")
    private String stroke;
```

- [ ] **Step 2: CharCharacterBaseVO.java — 删除 stroke 字段**

删除以下行（约第 43-44 行）：
```java
    @ApiModelProperty(value = "笔顺")
    private String stroke;
```

- [ ] **Step 3: CharCharacterVO.java — 删除 stroke 字段**

删除以下行（约第 49-50 行）：
```java
    @ApiModelProperty(value = "笔画")
    private String stroke;
```

---

### Task 10: 从 Wrapper 和 ServiceImpl 中移除 stroke 引用

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapper.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: CharCharacterWrapper.java — 删除 stroke 映射**

在 `toDto()` 方法中删除：
```java
        dto.setStroke(request.getStroke());
```

在 `toBaseVO()` 方法中删除：
```java
        vo.setStroke(dto.getStroke());
```

在 `toVO()` 方法中删除：
```java
        vo.setStroke(dto.getStroke());
```

- [ ] **Step 2: CharCharacterServiceImpl.java — 删除 stroke 引用**

在 `applyDraftOverlay()` 方法中删除：
```java
        if (draft.getStroke() != null)                dto.setStroke(draft.getStroke());
```

在 `publishDraft()` 方法中删除：
```java
        charCharacter.setStroke(draftDto.getStroke());
```

---

### Task 11: 更新测试

**Files:**
- Modify: `grid-system/src/test/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapperTest.java`
- Modify: `grid-system/src/test/java/com/naon/grid/backend/domain/character/CharacterEntityMappingTest.java`

- [ ] **Step 1: CharCharacterWrapperTest.java — 移除 stroke 断言**

在 `toDtoMapsNewCharacterFieldsAndSingleWordSentence()` 方法中：
1. 删除 `request.setStroke("stroke-json");` 行
2. 删除 `assertEquals("stroke-json", dto.getStroke());` 行

- [ ] **Step 2: CharacterEntityMappingTest.java — 移除 stroke 断言 + 新增 CharStroke 断言**

1. 在 `charCharacterUsesSqlColumnNamesAndLengths()` 中删除：
```java
        assertColumn(CharCharacter.class, "stroke", "stroke", 4096, null);
```

2. 新增 `charStrokeUsesSqlColumnNamesAndLengths()` 测试方法：

```java
    @Test
    void charStrokeUsesSqlColumnNamesAndLengths() throws Exception {
        assertColumn(CharStroke.class, "character", "`character`", 32, null);
        assertColumn(CharStroke.class, "stroke", "stroke", 255, "text");
        assertColumn(CharStroke.class, "status", "status", 255, null);
    }
```

3. 新增 import:
```java
import com.naon.grid.backend.domain.character.CharStroke;
```

- [ ] **Step 3: 运行测试确认通过**

```bash
cd "C:/Users/nano/Desktop/nano-gemini"
mvn test -pl grid-system -Dtest="CharCharacterWrapperTest,CharacterEntityMappingTest" -DskipTests=false
```

预期：所有测试通过（绿色 PASS）。

---

### Task 12: 提交代码

- [ ] **Step 1: Git 提交**

```bash
cd "C:/Users/nano/Desktop/nano-gemini"
git add -A
git commit -m "feat: add char_stroke module with independent stroke query

- add char_stroke table DDL to biz_character.sql
- add Python export script for hanzi-writer data
- add CharStroke entity, repository, service, VO, wrapper
- add GET /api/character/stroke/{character} endpoint
- remove stroke field from char_character and all related Java classes
- update tests

Co-Authored-By: Claude <noreply@anthropic.com>"
```
