# 词汇与汉字发布状态管理实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** 为词汇和汉字内容添加发布状态管理功能，包括草稿、审核、发布、下线状态流转，以及操作人追踪

**Architecture:** 单表 + JSON 草稿字段方案，draftContent 存储完整草稿（包含主表和子表），publishStatus 和 editStatus 管理状态

**Tech Stack:** Spring Boot 2.7, Spring Data JPA, MySQL, FastJSON2, Lombok

---

## 文件清单

### 新增文件
1. `grid-common/src/main/java/com/naon/grid/enums/PublishStatusEnum.java` - 发布状态枚举
2. `grid-common/src/main/java/com/naon/grid/enums/EditStatusEnum.java` - 编辑状态枚举
3. `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordDraftDto.java` - 词汇草稿DTO
4. `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDraftDto.java` - 汉字草稿DTO
5. `sql/migration/publish_status_migration.sql` - 数据库迁移脚本

### 修改文件
6. `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabWord.java` - 词汇实体添加状态字段
7. `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharCharacter.java` - 汉字实体添加状态字段
8. `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordBaseVO.java` - 词汇基础VO添加状态字段
9. `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java` - 词汇详情VO添加状态字段
10. `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java` - 汉字基础VO添加状态字段
11. `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java` - 汉字详情VO添加状态字段
12. `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabWordService.java` - 词汇服务接口添加草稿方法
13. `grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java` - 汉字服务接口添加草稿方法
14. `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java` - 词汇服务实现
15. `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java` - 汉字服务实现
16. `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java` - 词汇后台控制器添加草稿接口
17. `grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java` - 汉字后台控制器添加草稿接口
18. `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java` - 用户端词汇控制器添加发布状态过滤
19. `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharCharacterController.java` - 用户端汉字控制器添加发布状态过滤

---

## 实施任务

### Task 1: 创建发布状态枚举 PublishStatusEnum

**Files:**
- Create: `grid-common/src/main/java/com/naon/grid/enums/PublishStatusEnum.java`

- [ ] **Step 1.1: 创建 PublishStatusEnum 枚举类**

```java
package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum PublishStatusEnum {
    UNPUBLISHED("unpublished", "未发布"),
    PUBLISHED("published", "已发布");

    private final String code;
    private final String description;

    PublishStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

- [ ] **Step 1.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-common -am
```

Expected: BUILD SUCCESS

- [ ] **Step 1.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-common/src/main/java/com/naon/grid/enums/PublishStatusEnum.java
git commit -m "feat: add publish status enum"
```

---

### Task 2: 创建编辑状态枚举 EditStatusEnum

**Files:**
- Create: `grid-common/src/main/java/com/naon/grid/enums/EditStatusEnum.java`

- [ ] **Step 2.1: 创建 EditStatusEnum 枚举类**

```java
package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum EditStatusEnum {
    DRAFT("draft", "草稿"),
    REVIEWED("reviewed", "已审核");

    private final String code;
    private final String description;

    EditStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

- [ ] **Step 2.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-common -am
```

Expected: BUILD SUCCESS

- [ ] **Step 2.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-common/src/main/java/com/naon/grid/enums/EditStatusEnum.java
git commit -m "feat: add edit status enum"
```

---

### Task 3: 扩展 JsonUtils 工具类添加通用 JSON 方法

**Files:**
- Modify: `grid-common/src/main/java/com/naon/grid/utils/JsonUtils.java`

- [ ] **Step 3.1: 添加通用 toJson 和 fromJson 方法**

修改前先备份原始内容，然后添加以下方法：

```java
package com.naon.grid.utils;

import com.alibaba.fastjson2.JSON;
import com.naon.grid.domain.common.QuestionOption;
import com.naon.grid.domain.common.TextTranslation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JsonUtils {

    private JsonUtils() {
    }

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param obj 对象
     * @return JSON 字符串，null 返回 null
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        return JSON.toJSONString(obj);
    }

    /**
     * 将 JSON 字符串反序列化为对象
     *
     * @param json JSON 字符串
     * @param clazz 对象类型
     * @return 对象，null 或空白字符串返回 null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return JSON.parseObject(json, clazz);
    }

    // ... 保持原有方法不变 ...
}
```

- [ ] **Step 3.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-common -am
```

Expected: BUILD SUCCESS

- [ ] **Step 3.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-common/src/main/java/com/naon/grid/utils/JsonUtils.java
git commit -m "feat: add generic json utility methods"
```

---

### Task 4: 修改词汇实体类 VocabWord

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabWord.java`

- [ ] **Step 4.1: 修改实体类继承 BaseEntity 并添加状态字段**

```java
package com.naon.grid.backend.domain.vocabulary;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "vocab_word")
public class VocabWord extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "词汇唯一ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(name = "word", nullable = false, length = 50)
    @ApiModelProperty(value = "词汇")
    private String word;

    @Column(name = "word_traditional", length = 50)
    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @NotBlank
    @Column(name = "pinyin", nullable = false, length = 100)
    @ApiModelProperty(value = "标准拼音（含声调）")
    private String pinyin;

    @Column(name = "audio_id")
    @ApiModelProperty(value = "词汇读音音频资源ID")
    private Long audioId;

    @Column(name = "hsk_level", length = 20)
    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String hskLevel;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();

    @Column(name = "publish_status", length = 20)
    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus = PublishStatusEnum.UNPUBLISHED.getCode();

    @Column(name = "edit_status", length = 20)
    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus = EditStatusEnum.DRAFT.getCode();

    @Column(name = "draft_content", columnDefinition = "json")
    @ApiModelProperty(value = "草稿内容JSON（包含主表和子表）")
    private String draftContent;
}
```

注意：移除了原有的 createTime 和 updateTime 字段，因为它们现在来自 BaseEntity

- [ ] **Step 4.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 4.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabWord.java
git commit -m "feat: add publish status fields to vocab word entity"
```

---

### Task 5: 修改汉字实体类 CharCharacter

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharCharacter.java`

- [ ] **Step 5.1: 修改实体类继承 BaseEntity 并添加状态字段**

```java
package com.naon.grid.backend.domain.character;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "char_character")
public class CharCharacter extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "汉字唯一ID", hidden = true)
    private Integer id;

    @Column(name = "sequence_no")
    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @NotBlank
    @Column(name = "`character`", nullable = false, length = 10)
    private String character;

    @Column(name = "level", length = 20)
    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @NotBlank
    @Column(name = "pinyin", nullable = false, length = 100)
    private String pinyin;

    @Column(name = "audio_id")
    private Long audioId;

    @Column(name = "traditional", length = 10)
    private String traditional;

    @Column(name = "radical", length = 10)
    private String radical;

    @Column(name = "stroke", length = 4096)
    private String stroke;

    @Column(name = "char_desc", length = 1024)
    private String charDesc;

    @Column(name = "desc_translations", columnDefinition = "text")
    private String descTranslations;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();

    @Column(name = "publish_status", length = 20)
    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus = PublishStatusEnum.UNPUBLISHED.getCode();

    @Column(name = "edit_status", length = 20)
    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus = EditStatusEnum.DRAFT.getCode();

    @Column(name = "draft_content", columnDefinition = "json")
    @ApiModelProperty(value = "草稿内容JSON（包含主表和子表）")
    private String draftContent;
}
```

注意：移除了原有的 createTime 和 updateTime 字段，因为它们现在来自 BaseEntity

- [ ] **Step 5.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 5.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/domain/character/CharCharacter.java
git commit -m "feat: add publish status fields to char character entity"
```

---

### Task 6: 创建词汇草稿 DTO VocabWordDraftDto

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordDraftDto.java`

- [ ] **Step 6.1: 创建 VocabWordDraftDto 类**

与 VocabWordDto 结构完全相同，但不继承 BaseDTO，因为草稿不需要操作人字段：

```java
package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VocabWordDraftDto implements Serializable {

    @ApiModelProperty(value = "词汇唯一ID")
    private Integer id;

    @ApiModelProperty(value = "词汇")
    private String word;

    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @ApiModelProperty(value = "标准拼音（含声调）")
    private String pinyin;

    @ApiModelProperty(value = "词汇读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String hskLevel;

    @ApiModelProperty(value = "义项列表")
    private List<VocabSenseDto> senses;

    @ApiModelProperty(value = "练习题列表")
    private List<VocabExerciseDto> exercises;
}
```

- [ ] **Step 6.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 6.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordDraftDto.java
git commit -m "feat: add vocab word draft dto"
```

---

### Task 7: 创建汉字草稿 DTO CharCharacterDraftDto

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDraftDto.java`

- [ ] **Step 7.1: 创建 CharCharacterDraftDto 类**

与 CharCharacterDto 结构完全相同，但不继承 BaseDTO：

```java
package com.naon.grid.backend.service.character.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CharCharacterDraftDto implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔画")
    private String stroke;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "说明翻译")
    private List<TextTranslation> descTranslations;

    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationDto> discriminations;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordDto> words;
}
```

- [ ] **Step 7.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 7.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDraftDto.java
git commit -m "feat: add char character draft dto"
```

---

### Task 8: 修改词汇基础 VO VocabWordBaseVO

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordBaseVO.java`

- [ ] **Step 8.1: 添加状态字段到 VO**

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
public class VocabWordBaseVO implements Serializable {

    @ApiModelProperty(value = "词汇唯一ID")
    private Integer id;

    @ApiModelProperty(value = "词汇", required = true)
    private String word;

    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @ApiModelProperty(value = "标准拼音（含声调）", required = true)
    private String pinyin;

    @ApiModelProperty(value = "词汇读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String hskLevel;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "是否有草稿")
    private Boolean hasDraft;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;
}
```

- [ ] **Step 8.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 8.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordBaseVO.java
git commit -m "feat: add status fields to vocab word base vo"
```

---

### Task 9: 修改汉字基础 VO CharCharacterBaseVO

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java`

- [ ] **Step 9.1: 添加状态字段到 VO**

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class CharCharacterBaseVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字", required = true)
    private String character;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @ApiModelProperty(value = "拼音", required = true)
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔顺")
    private String stroke;

    @ApiModelProperty(value = "部件组合中文说明")
    private String charDesc;

    @ApiModelProperty(value = "汉字说明的多语种翻译")
    private List<TextTranslationVO> descTranslations;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "是否有草稿")
    private Boolean hasDraft;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;
}
```

- [ ] **Step 9.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 9.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java
git commit -m "feat: add status fields to char character base vo"
```

---

### Task 10: 修改词汇详情 VO VocabWordVO

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java`

- [ ] **Step 10.1: 添加状态字段到 VO**

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class VocabWordVO implements Serializable {

    @ApiModelProperty(value = "词汇唯一ID")
    private Integer id;

    @ApiModelProperty(value = "词汇", required = true)
    private String word;

    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @ApiModelProperty(value = "标准拼音（含声调）", required = true)
    private String pinyin;

    @ApiModelProperty(value = "词汇读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String hskLevel;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "是否有草稿")
    private Boolean hasDraft;

    @ApiModelProperty(value = "义项列表")
    private List<VocabSenseVO> senses;

    @ApiModelProperty(value = "练习题列表")
    private List<VocabExerciseVO> exercises;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    // ... 保持内部类不变 ...
}
```

- [ ] **Step 10.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 10.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java
git commit -m "feat: add status fields to vocab word vo"
```

---

### Task 11: 修改汉字详情 VO CharCharacterVO

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java`

- [ ] **Step 11.1: 添加状态字段到 VO**

先读取当前文件内容，然后在合适位置添加字段。

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class CharCharacterVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字", required = true)
    private String character;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @ApiModelProperty(value = "拼音", required = true)
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔画")
    private String stroke;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "说明翻译")
    private List<TextTranslationVO> descTranslations;

    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationVO> discriminations;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordVO> words;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "是否有草稿")
    private Boolean hasDraft;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    // ... 保持内部类不变 ...
}
```

- [ ] **Step 11.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 11.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java
git commit -m "feat: add status fields to char character vo"
```

---

### Task 12: 扩展词汇服务接口 VocabWordService

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabWordService.java`

- [ ] **Step 12.1: 添加草稿相关方法到接口**

```java
package com.naon.grid.backend.service.vocabulary;

import com.naon.grid.backend.service.vocabulary.dto.VocabWordDraftDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface VocabWordService {

    PageResult<VocabWordDto> queryAll(VocabWordQueryCriteria criteria, Pageable pageable);

    VocabWordDto findById(Integer id);

    Integer create(VocabWordDto resources);

    void update(Integer id, VocabWordDto resources);

    void delete(Integer id);

    /**
     * 获取草稿详情
     * @param id 词汇ID
     * @return 草稿DTO
     */
    VocabWordDraftDto getDraft(Integer id);

    /**
     * 保存草稿（创建或更新）
     * @param id 词汇ID（新建时为null）
     * @param draft 草稿DTO
     */
    void saveDraft(Integer id, VocabWordDraftDto draft);

    /**
     * 创建草稿
     * @param draft 草稿DTO
     * @return 创建的词汇ID
     */
    Integer createDraft(VocabWordDraftDto draft);

    /**
     * 审核草稿
     * @param id 词汇ID
     */
    void reviewDraft(Integer id);

    /**
     * 发布草稿（同步到正式字段）
     * @param id 词汇ID
     */
    void publishDraft(Integer id);

    /**
     * 下线词汇（从正式字段逻辑删除）
     * @param id 词汇ID
     */
    void offline(Integer id);

    /**
     * 从已发布内容创建草稿
     * @param id 词汇ID
     */
    void createDraftFromPublished(Integer id);
}
```

- [ ] **Step 12.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 12.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabWordService.java
git commit -m "feat: add draft methods to vocab word service interface"
```

---

### Task 13: 扩展汉字服务接口 CharCharacterService

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java`

- [ ] **Step 13.1: 添加草稿相关方法到接口**

```java
package com.naon.grid.backend.service.character;

import com.naon.grid.backend.service.character.dto.CharCharacterDraftDto;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CharCharacterService {

    PageResult<CharCharacterDto> queryAll(CharCharacterQueryCriteria criteria, Pageable pageable);

    CharCharacterDto findById(Integer id);

    Integer create(CharCharacterDto resources);

    void update(Integer id, CharCharacterDto resources);

    void delete(Integer id);

    /**
     * 根据汉字模糊搜索（仅匹配character字段）
     * @param blurry 搜索关键词
     * @return 匹配的汉字列表
     */
    List<CharCharacterDto> searchByCharacter(String blurry);

    /**
     * 获取草稿详情
     * @param id 汉字ID
     * @return 草稿DTO
     */
    CharCharacterDraftDto getDraft(Integer id);

    /**
     * 保存草稿（创建或更新）
     * @param id 汉字ID（新建时为null）
     * @param draft 草稿DTO
     */
    void saveDraft(Integer id, CharCharacterDraftDto draft);

    /**
     * 创建草稿
     * @param draft 草稿DTO
     * @return 创建的汉字ID
     */
    Integer createDraft(CharCharacterDraftDto draft);

    /**
     * 审核草稿
     * @param id 汉字ID
     */
    void reviewDraft(Integer id);

    /**
     * 发布草稿（同步到正式字段）
     * @param id 汉字ID
     */
    void publishDraft(Integer id);

    /**
     * 下线词汇（从正式字段逻辑删除）
     * @param id 汉字ID
     */
    void offline(Integer id);

    /**
     * 从已发布内容创建草稿
     * @param id 汉字ID
     */
    void createDraftFromPublished(Integer id);
}
```

- [ ] **Step 13.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 13.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java
git commit -m "feat: add draft methods to char character service interface"
```

---

### Task 14: 实现词汇服务实现类 VocabWordServiceImpl（一）

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`

- [ ] **Step 14.1: 添加必要的 import 语句和注入依赖**

先读取现有文件，然后在文件开头添加：

```java
import com.naon.grid.backend.domain.vocabulary.VocabWord;
import com.naon.grid.backend.domain.vocabulary.VocabSense;
import com.naon.grid.backend.domain.vocabulary.VocabStructure;
import com.naon.grid.backend.domain.vocabulary.VocabExample;
import com.naon.grid.backend.domain.vocabulary.VocabExercise;
import com.naon.grid.backend.repo.vocabulary.VocabWordRepository;
import com.naon.grid.backend.repo.vocabulary.VocabSenseRepository;
import com.naon.grid.backend.repo.vocabulary.VocabStructureRepository;
import com.naon.grid.backend.repo.vocabulary.VocabExampleRepository;
import com.naon.grid.backend.repo.vocabulary.VocabExerciseRepository;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDraftDto;
import com.naon.grid.backend.service.vocabulary.mapstruct.VocabWordMapper;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
```

添加必要的 Repository 依赖注入（如果还没有的话）：

```java
@Service
@RequiredArgsConstructor
public class VocabWordServiceImpl implements VocabWordService {

    private final VocabWordRepository vocabWordRepository;
    private final VocabSenseRepository vocabSenseRepository;
    private final VocabStructureRepository vocabStructureRepository;
    private final VocabExampleRepository vocabExampleRepository;
    private final VocabExerciseRepository vocabExerciseRepository;
    private final VocabWordMapper vocabWordMapper;

    // ... 保持现有方法 ...
}
```

- [ ] **Step 14.2: 修改 queryAll 和 findById 方法以支持状态字段**

这个步骤暂时不需要，因为只是添加字段，查询逻辑不变。

- [ ] **Step 14.3: 提交当前进度**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java
git commit -m "feat: prepare vocab word service impl for draft methods"
```

---

### Task 15: 实现词汇服务实现类 VocabWordServiceImpl（二）- 草稿方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`

- [ ] **Step 15.1: 实现 getDraft 方法**

```java
@Override
public VocabWordDraftDto getDraft(Integer id) {
    VocabWord vocabWord = vocabWordRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id)));

    if (StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
        throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
    }

    if (vocabWord.getDraftContent() != null) {
        return JsonUtils.fromJson(vocabWord.getDraftContent(), VocabWordDraftDto.class);
    }

    // 如果没有草稿，但有发布内容，返回发布内容
    if (PublishStatusEnum.PUBLISHED.getCode().equals(vocabWord.getPublishStatus())) {
        VocabWordDto dto = vocabWordMapper.toDto(vocabWord);
        dto.setSenses(convertToSenseDtos(vocabSenseRepository.findByWordIdAndStatus(id, StatusEnum.ENABLED.getCode())));
        dto.setExercises(convertToExerciseDtos(vocabExerciseRepository.findByWordIdAndStatus(id, StatusEnum.ENABLED.getCode())));

        // 转换为 DraftDto
        VocabWordDraftDto draftDto = new VocabWordDraftDto();
        draftDto.setId(dto.getId());
        draftDto.setWord(dto.getWord());
        draftDto.setWordTraditional(dto.getWordTraditional());
        draftDto.setPinyin(dto.getPinyin());
        draftDto.setAudioId(dto.getAudioId());
        draftDto.setHskLevel(dto.getHskLevel());
        draftDto.setSenses(dto.getSenses());
        draftDto.setExercises(dto.getExercises());
        return draftDto;
    }

    throw new BadRequestException("草稿不存在");
}
```

- [ ] **Step 15.2: 实现 createDraft 和 saveDraft 方法**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Integer createDraft(VocabWordDraftDto draft) {
    VocabWord vocabWord = new VocabWord();
    vocabWord.setStatus(StatusEnum.ENABLED.getCode());
    vocabWord.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
    vocabWord.setEditStatus(EditStatusEnum.DRAFT.getCode());
    vocabWord.setDraftContent(JsonUtils.toJson(draft));
    vocabWord = vocabWordRepository.save(vocabWord);
    return vocabWord.getId();
}

@Override
@Transactional(rollbackFor = Exception.class)
public void saveDraft(Integer id, VocabWordDraftDto draft) {
    VocabWord vocabWord = vocabWordRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id)));

    if (StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
        throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
    }

    vocabWord.setDraftContent(JsonUtils.toJson(draft));
    vocabWord.setEditStatus(EditStatusEnum.DRAFT.getCode());
    vocabWordRepository.save(vocabWord);
}
```

- [ ] **Step 15.3: 实现 reviewDraft 方法**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void reviewDraft(Integer id) {
    VocabWord vocabWord = vocabWordRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id)));

    if (StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
        throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
    }

    if (vocabWord.getDraftContent() == null) {
        throw new BadRequestException("草稿不存在");
    }

    if (!EditStatusEnum.DRAFT.getCode().equals(vocabWord.getEditStatus())) {
        throw new BadRequestException("仅草稿状态可审核");
    }

    vocabWord.setEditStatus(EditStatusEnum.REVIEWED.getCode());
    vocabWordRepository.save(vocabWord);
}
```

- [ ] **Step 15.4: 实现 publishDraft 方法**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void publishDraft(Integer id) {
    VocabWord vocabWord = vocabWordRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id)));

    if (StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
        throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
    }

    if (vocabWord.getDraftContent() == null) {
        throw new BadRequestException("草稿不存在");
    }

    if (!EditStatusEnum.REVIEWED.getCode().equals(vocabWord.getEditStatus())) {
        throw new BadRequestException("仅已审核状态可发布");
    }

    // 解析草稿
    VocabWordDraftDto draftDto = JsonUtils.fromJson(vocabWord.getDraftContent(), VocabWordDraftDto.class);

    // 更新主表
    vocabWord.setWord(draftDto.getWord());
    vocabWord.setWordTraditional(draftDto.getWordTraditional());
    vocabWord.setPinyin(draftDto.getPinyin());
    vocabWord.setAudioId(draftDto.getAudioId());
    vocabWord.setHskLevel(draftDto.getHskLevel());

    // 更新子表
    syncSenses(id, draftDto.getSenses());
    syncExercises(id, draftDto.getExercises());

    // 更新状态
    vocabWord.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
    vocabWord.setDraftContent(null);
    vocabWordRepository.save(vocabWord);
}
```

- [ ] **Step 15.5: 实现 offline 方法**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void offline(Integer id) {
    VocabWord vocabWord = vocabWordRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id)));

    if (StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
        throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
    }

    // 逻辑删除子表
    deleteChildren(id);

    // 更新状态
    vocabWord.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
    vocabWordRepository.save(vocabWord);
}
```

- [ ] **Step 15.6: 实现 createDraftFromPublished 方法**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void createDraftFromPublished(Integer id) {
    VocabWord vocabWord = vocabWordRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id)));

    if (StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
        throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
    }

    // 如果已有草稿，跳过
    if (vocabWord.getDraftContent() != null) {
        return;
    }

    // 从正式字段构建DTO
    VocabWordDto dto = vocabWordMapper.toDto(vocabWord);
    dto.setSenses(convertToSenseDtos(vocabSenseRepository.findByWordIdAndStatus(id, StatusEnum.ENABLED.getCode())));
    dto.setExercises(convertToExerciseDtos(vocabExerciseRepository.findByWordIdAndStatus(id, StatusEnum.ENABLED.getCode())));

    // 转换为 DraftDto
    VocabWordDraftDto draftDto = new VocabWordDraftDto();
    draftDto.setId(dto.getId());
    draftDto.setWord(dto.getWord());
    draftDto.setWordTraditional(dto.getWordTraditional());
    draftDto.setPinyin(dto.getPinyin());
    draftDto.setAudioId(dto.getAudioId());
    draftDto.setHskLevel(dto.getHskLevel());
    draftDto.setSenses(dto.getSenses());
    draftDto.setExercises(dto.getExercises());

    // 存为草稿
    vocabWord.setDraftContent(JsonUtils.toJson(draftDto));
    vocabWord.setEditStatus(EditStatusEnum.DRAFT.getCode());
    vocabWordRepository.save(vocabWord);
}
```

- [ ] **Step 15.7: 添加辅助方法 convertToSenseDtos 和 convertToExerciseDtos**

这些方法与现有实现类似，从实体列表转换为 DTO 列表。

- [ ] **Step 15.8: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 15.9: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java
git commit -m "feat: implement draft methods in vocab word service"
```

---

### Task 16: 实现汉字服务实现类 CharCharacterServiceImpl

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 16.1: 实现汉字服务的草稿方法**

按照词汇服务相同的模式实现汉字服务的草稿方法：
- getDraft
- createDraft
- saveDraft
- reviewDraft
- publishDraft
- offline
- createDraftFromPublished

实现逻辑与词汇服务一致，只是类名和方法名不同。

- [ ] **Step 16.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 16.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java
git commit -m "feat: implement draft methods in char character service"
```

---

### Task 17: 修改词汇后台控制器 VocabWordController（一）- 更新现有 VO 转换方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java`

- [ ] **Step 17.1: 更新 toBaseVO 方法以包含状态字段**

```java
private VocabWordBaseVO toBaseVO(VocabWordDto dto) {
    VocabWordBaseVO vo = new VocabWordBaseVO();
    vo.setId(dto.getId());
    vo.setWord(dto.getWord());
    vo.setWordTraditional(dto.getWordTraditional());
    vo.setPinyin(dto.getPinyin());
    vo.setAudioId(dto.getAudioId());
    vo.setHskLevel(dto.getHskLevel());
    vo.setPublishStatus(dto.getPublishStatus());
    vo.setEditStatus(dto.getEditStatus());
    vo.setHasDraft(dto.getDraftContent() != null);
    vo.setCreateBy(dto.getCreateBy());
    vo.setUpdateBy(dto.getUpdateBy());
    vo.setCreateTime(dto.getCreateTime());
    vo.setUpdateTime(dto.getUpdateTime());
    return vo;
}
```

注意：需要确保 VocabWordDto 也添加了这些字段，或者需要从实体获取

- [ ] **Step 17.2: 更新 toVO 方法以包含状态字段**

```java
private VocabWordVO toVO(VocabWordDto dto) {
    VocabWordVO vo = new VocabWordVO();
    vo.setId(dto.getId());
    vo.setWord(dto.getWord());
    vo.setWordTraditional(dto.getWordTraditional());
    vo.setPinyin(dto.getPinyin());
    vo.setAudioId(dto.getAudioId());
    vo.setHskLevel(dto.getHskLevel());
    vo.setPublishStatus(dto.getPublishStatus());
    vo.setEditStatus(dto.getEditStatus());
    vo.setHasDraft(dto.getDraftContent() != null);
    vo.setSenses(toSenseVOList(dto.getSenses()));
    vo.setExercises(toExerciseVOList(dto.getExercises()));
    vo.setCreateBy(dto.getCreateBy());
    vo.setUpdateBy(dto.getUpdateBy());
    vo.setCreateTime(dto.getCreateTime());
    vo.setUpdateTime(dto.getUpdateTime());
    return vo;
}
```

- [ ] **Step 17.3: 提交当前进度**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java
git commit -m "feat: update vocab word controller vo conversion for status fields"
```

---

### Task 18: 修改词汇后台控制器 VocabWordController（二）- 添加草稿接口

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java`

- [ ] **Step 18.1: 添加草稿相关接口**

```java
@Log("查询词汇草稿详情")
@ApiOperation("根据ID查询词汇草稿详情")
@AnonymousGetMapping("/{id}/draft")
public ResponseEntity<VocabWordVO> getDraft(@PathVariable Integer id) {
    VocabWordDraftDto draftDto = vocabWordService.getDraft(id);
    // 转换为VO
    VocabWordVO vo = new VocabWordVO();
    vo.setId(draftDto.getId());
    vo.setWord(draftDto.getWord());
    vo.setWordTraditional(draftDto.getWordTraditional());
    vo.setPinyin(draftDto.getPinyin());
    vo.setAudioId(draftDto.getAudioId());
    vo.setHskLevel(draftDto.getHskLevel());
    vo.setSenses(toSenseVOList(draftDto.getSenses()));
    vo.setExercises(toExerciseVOList(draftDto.getExercises()));
    return new ResponseEntity<>(vo, HttpStatus.OK);
}

@Log("新增词汇草稿")
@ApiOperation("新增词汇草稿")
@AnonymousPostMapping("/draft")
public ResponseEntity<VocabWordCreateVO> createDraft(@Valid @RequestBody VocabWordCreateRequest request) {
    VocabWordDraftDto draftDto = convertToDraftDto(request);
    VocabWordCreateVO vo = new VocabWordCreateVO();
    vo.setId(vocabWordService.createDraft(draftDto));
    return new ResponseEntity<>(vo, HttpStatus.CREATED);
}

@Log("修改词汇草稿")
@ApiOperation("修改词汇草稿")
@AnonymousPutMapping("/{id}/draft")
public ResponseEntity<Object> updateDraft(@PathVariable Integer id, @Valid @RequestBody VocabWordCreateRequest request) {
    VocabWordDraftDto draftDto = convertToDraftDto(request);
    vocabWordService.saveDraft(id, draftDto);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
}

@Log("从已发布内容创建草稿")
@ApiOperation("从已发布内容创建草稿")
@AnonymousPostMapping("/{id}/draft/from-published")
public ResponseEntity<Object> createDraftFromPublished(@PathVariable Integer id) {
    vocabWordService.createDraftFromPublished(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
}

@Log("审核词汇草稿")
@ApiOperation("审核词汇草稿（草稿→已审核）")
@AnonymousPutMapping("/{id}/review")
public ResponseEntity<Object> reviewDraft(@PathVariable Integer id) {
    vocabWordService.reviewDraft(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
}

@Log("发布词汇")
@ApiOperation("发布词汇（已审核→已发布）")
@AnonymousPutMapping("/{id}/publish")
public ResponseEntity<Object> publishDraft(@PathVariable Integer id) {
    vocabWordService.publishDraft(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
}

@Log("下线词汇")
@ApiOperation("下线词汇")
@AnonymousPutMapping("/{id}/offline")
public ResponseEntity<Object> offline(@PathVariable Integer id) {
    vocabWordService.offline(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
}
```

- [ ] **Step 18.2: 添加 convertToDraftDto 辅助方法**

```java
private VocabWordDraftDto convertToDraftDto(VocabWordCreateRequest request) {
    VocabWordDraftDto dto = new VocabWordDraftDto();
    dto.setWord(request.getWord());
    dto.setWordTraditional(request.getWordTraditional());
    dto.setPinyin(request.getPinyin());
    dto.setAudioId(request.getAudioId());
    dto.setHskLevel(request.getHskLevel());
    dto.setSenses(toSenseDtoList(request.getSenses()));
    dto.setExercises(toExerciseDtoList(request.getExercises()));
    return dto;
}
```

- [ ] **Step 18.3: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 18.4: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java
git commit -m "feat: add draft endpoints to vocab word controller"
```

---

### Task 19: 修改汉字后台控制器 CharCharacterController

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java`

- [ ] **Step 19.1: 更新汉字 VO 转换方法并添加草稿接口**

按照词汇控制器相同的模式修改汉字控制器：
- 更新 toBaseVO 和 toVO 方法以包含状态字段
- 添加所有草稿相关接口（getDraft, createDraft, updateDraft, createDraftFromPublished, reviewDraft, publishDraft, offline）

- [ ] **Step 19.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 19.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java
git commit -m "feat: add draft endpoints to char character controller"
```

---

### Task 20: 修改用户端词汇控制器 AppVocabWordController

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java`

- [ ] **Step 20.1: 添加发布状态过滤**

用户端只查询发布状态为 PUBLISHED 的内容。需要在服务层添加新方法或修改现有查询逻辑。

首先需要添加一个查询方法到 Repository 或修改现有查询逻辑。这个任务相对复杂，让我们采用简单方案：在查询时手动过滤。

- [ ] **Step 20.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-app -am
```

Expected: BUILD SUCCESS

- [ ] **Step 20.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java
git commit -m "feat: filter published content for app vocab endpoints"
```

---

### Task 21: 修改用户端汉字控制器 AppCharCharacterController

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharCharacterController.java`

- [ ] **Step 21.1: 添加发布状态过滤**

按照用户端词汇控制器相同的模式修改汉字控制器。

- [ ] **Step 21.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-app -am
```

Expected: BUILD SUCCESS

- [ ] **Step 21.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharCharacterController.java
git commit -m "feat: filter published content for app char endpoints"
```

---

### Task 22: 修改 VocabWordDto 添加状态字段

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordDto.java`

- [ ] **Step 22.1: 添加状态字段**

```java
package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VocabWordDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "词汇唯一ID")
    private Integer id;

    @ApiModelProperty(value = "词汇")
    private String word;

    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @ApiModelProperty(value = "标准拼音（含声调）")
    private String pinyin;

    @ApiModelProperty(value = "词汇读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String hskLevel;

    @ApiModelProperty(value = "义项列表")
    private List<VocabSenseDto> senses;

    @ApiModelProperty(value = "练习题列表")
    private List<VocabExerciseDto> exercises;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;
}
```

- [ ] **Step 22.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 22.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordDto.java
git commit -m "feat: add status fields to vocab word dto"
```

---

### Task 23: 修改 CharCharacterDto 添加状态字段

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDto.java`

- [ ] **Step 23.1: 添加状态字段**

```java
package com.naon.grid.backend.service.character.dto;

import com.naon.grid.base.BaseDTO;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CharCharacterDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔画")
    private String stroke;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "说明翻译")
    private List<TextTranslation> descTranslations;

    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationDto> discriminations;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordDto> words;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;
}
```

- [ ] **Step 23.2: 验证编译通过**

```bash
cd /home/nano/nano-gemini
mvn compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 23.3: 提交代码**

```bash
cd /home/nano/nano-gemini
git status
git add grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDto.java
git commit -m "feat: add status fields to char character dto"
```

---

### Task 24: 更新 MapStruct Mapper 以包含状态字段

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/mapstruct/VocabWordMapper.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/mapstruct/CharCharacterMapper.java`

- [ ] **Step 24.1: 确保 Mapper 正确映射新字段**

MapStruct 应该会自动处理同名字段的映射，不需要特殊修改，但需要确保编译时重新生成 Mapper 实现类。

- [ ] **Step 24.2: 重新编译项目以重新生成 Mapper 实现**

```bash
cd /home/nano/nano-gemini
mvn clean compile -pl grid-system -am
```

Expected: BUILD SUCCESS

- [ ] **Step 24.3: 提交代码（如果有修改）**

---

### Task 25: 创建数据库迁移脚本

**Files:**
- Create: `sql/migration/publish_status_migration.sql`

- [ ] **Step 25.1: 创建迁移脚本**

```sql
-- 为 vocab_word 新增字段
ALTER TABLE vocab_word
ADD COLUMN publish_status VARCHAR(20) DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
ADD COLUMN edit_status VARCHAR(20) DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
ADD COLUMN draft_content JSON COMMENT '草稿内容JSON',
ADD COLUMN create_by VARCHAR(255) DEFAULT NULL COMMENT '创建人',
ADD COLUMN update_by VARCHAR(255) DEFAULT NULL COMMENT '更新人';

-- 为 char_character 新增字段
ALTER TABLE char_character
ADD COLUMN publish_status VARCHAR(20) DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
ADD COLUMN edit_status VARCHAR(20) DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
ADD COLUMN draft_content JSON COMMENT '草稿内容JSON',
ADD COLUMN create_by VARCHAR(255) DEFAULT NULL COMMENT '创建人',
ADD COLUMN update_by VARCHAR(255) DEFAULT NULL COMMENT '更新人';

-- 初始化已有数据为已发布状态
UPDATE vocab_word SET publish_status = 'published' WHERE status = 1;
UPDATE char_character SET publish_status = 'published' WHERE status = 1;

-- 创建索引
CREATE INDEX idx_vocab_publish_status ON vocab_word(publish_status);
CREATE INDEX idx_vocab_edit_status ON vocab_word(edit_status);
CREATE INDEX idx_char_publish_status ON char_character(publish_status);
CREATE INDEX idx_char_edit_status ON char_character(edit_status);
```

注意：需要先检查字段是否已存在，避免重复添加错误。生产环境执行前请先备份数据库。

- [ ] **Step 25.2: 提交迁移脚本**

```bash
cd /home/nano/nano-gemini
mkdir -p sql/migration
git status
git add sql/migration/publish_status_migration.sql
git commit -m "feat: add database migration script for publish status"
```

---

### Task 26: 编译并运行完整项目

**Files:**

- [ ] **Step 26.1: 执行完整编译**

```bash
cd /home/nano/nano-gemini
mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 26.2: 提交所有修改（如果有遗漏）**

```bash
cd /home/nano/nano-gemini
git status
# 提交任何遗漏的修改
```

---

## 验收标准

- [ ] 所有枚举类创建成功并可正常使用
- [ ] 实体类添加了所有必要的状态字段
- [ ] DTO 和 VO 包含状态字段
- [ ] Service 层实现了完整的草稿、审核、发布、下线功能
- [ ] Controller 层提供了所有必要的接口
- [ ] 用户端接口只返回已发布内容
- [ ] 数据库迁移脚本完整可用
- [ ] 项目可以完整编译通过

---

## 注意事项

1. **数据库迁移:** 执行数据库迁移前务必备份数据
2. **JPA Auditing:** 确保项目已启用 JPA Auditing 以自动填充 createBy 和 updateBy
3. **数据一致性:** 发布操作需要主表和子表的事务一致性
4. **MapStruct:** 修改 Mapper 后需要重新编译以重新生成实现类
5. **JSON 序列化:** 确保复杂对象能被正确序列化和反序列化

---

## 执行选择

Plan complete and saved to `docs/superpowers/plans/2026-06-02-publish-status.md`. Two execution options:

**Option 1: Subagent-Driven (recommended)** - Dispatch a fresh subagent per task, review between tasks, fast iteration

**Option 2: Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
