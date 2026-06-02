# 词汇与汉字发布状态管理设计文档

## 概述

为词汇和汉字内容添加发布状态管理功能，支持草稿、已审核、已发布状态流转，并添加操作人追踪。采用单表 + JSON 草稿方案，改动最小。

## 背景与目标

### 当前状态
- 词汇（VocabWord）和汉字（CharCharacter）仅有 `status` 字段，表示逻辑删除（0=已删除，1=可用）
- 实体类未继承 BaseEntity，缺少操作人追踪
- 后台和用户端接口查询逻辑相同，无发布状态过滤

### 优化目标
1. 新增 `draftContent` JSON 字段存储编辑中的草稿（包含子表）
2. 新增 `publishStatus` 字段：表示线上是否已发布
3. 新增 `editStatus` 字段：表示当前草稿的编辑状态
4. 后台接口编辑 draftContent，用户端只看正式字段
5. 发布时把 draftContent 同步到正式字段
6. 添加操作人追踪（createBy, updateBy）

## 核心设计

### 1. 状态枚举

#### 1.1 PublishStatusEnum（发布状态）

**文件**: `grid-common/src/main/java/com/naon/grid/enums/PublishStatusEnum.java`

```java
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

#### 1.2 EditStatusEnum（编辑状态）

**文件**: `grid-common/src/main/java/com/naon/grid/enums/EditStatusEnum.java`

```java
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

### 2. 实体类修改

#### 2.1 VocabWord 实体

**文件**: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabWord.java`

```java
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

    // ... 现有字段保持不变（这些是正式发布的内容）...

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

#### 2.2 CharCharacter 实体

**文件**: `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharCharacter.java`

```java
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

    // ... 现有字段保持不变（这些是正式发布的内容）...

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

### 3. DraftContent JSON 结构

#### 3.1 词汇草稿结构

```json
{
  "word": "词汇",
  "wordTraditional": "繁体",
  "pinyin": "拼音",
  "audioId": 1,
  "hskLevel": "1",
  "senses": [
    {
      "id": 1,
      "partOfSpeech": "n.",
      "chineseDef": "中文释义",
      "defAudioId": 2,
      "translations": [...],
      "synonyms": [...],
      "antonyms": [...],
      "relatedForward": [...],
      "relatedBackward": [...],
      "senseOrder": 1,
      "structures": [...]
    }
  ],
  "exercises": [...]
}
```

#### 3.2 汉字草稿结构

```json
{
  "sequenceNo": 1,
  "character": "汉字",
  "level": "1",
  "pinyin": "拼音",
  "audioId": 1,
  "traditional": "繁体",
  "radical": "部首",
  "stroke": "笔顺",
  "charDesc": "描述",
  "descTranslations": [...],
  "discriminations": [...],
  "words": [...]
}
```

### 4. DTO 新增

#### 4.1 VocabWordDraftDto

**文件**: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordDraftDto.java`

```java
@Getter
@Setter
public class VocabWordDraftDto implements Serializable {
    // 与 VocabWordDto 结构相同，用于接收草稿编辑
    private Integer id;
    private String word;
    private String wordTraditional;
    // ... 其他字段
    private List<VocabSenseDto> senses;
    private List<VocabExerciseDto> exercises;
}
```

#### 4.2 CharCharacterDraftDto

**文件**: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDraftDto.java`

```java
@Getter
@Setter
public class CharCharacterDraftDto implements Serializable {
    // 与 CharCharacterDto 结构相同，用于接收草稿编辑
    private Integer id;
    private Integer sequenceNo;
    private String character;
    // ... 其他字段
    private List<CharDiscriminationDto> discriminations;
    private List<CharWordDto> words;
}
```

### 5. Service 层设计

#### 5.1 后台查询流程

1. **查看草稿**：从 `draftContent` 解析并返回
2. **查看已发布内容**：从正式字段返回
3. **列表查询**：返回状态字段，前端决定显示什么

#### 5.2 后台编辑流程

1. **首次创建**：
   - 将内容写入 `draftContent`
   - `editStatus` = DRAFT
   - `publishStatus` = UNPUBLISHED

2. **编辑已有内容**：
   - 如果 `draftContent` 为空，先把正式字段复制到 `draftContent`
   - 更新 `draftContent`
   - `editStatus` 变为 DRAFT

3. **审核**：
   - `editStatus` = REVIEWED

4. **发布**：
   - 检查 `editStatus` = REVIEWED
   - 解析 `draftContent`，更新主表和子表的正式字段
   - `publishStatus` = PUBLISHED
   - 清空 `draftContent`（可选）

5. **下线**：
   - `publishStatus` = UNPUBLISHED
   - 逻辑删除主表和子表的正式字段

#### 5.3 VocabWordService 新增方法

**文件**: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabWordService.java`

```java
// 草稿相关
VocabWordDto getDraft(Integer id);
void saveDraft(Integer id, VocabWordDto draft);
void createDraft(VocabWordDto draft);

// 状态流转
void reviewDraft(Integer id);
void publishDraft(Integer id);
void offline(Integer id);

// 从已发布内容创建草稿
void createDraftFromPublished(Integer id);
```

#### 5.4 发布实现逻辑

```java
@Transactional(rollbackFor = Exception.class)
public void publishDraft(Integer id) {
    VocabWord vocabWord = vocabWordRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id)));

    // 状态检查
    if (!EditStatusEnum.REVIEWED.getCode().equals(vocabWord.getEditStatus())) {
        throw new BadRequestException("仅已审核状态可发布");
    }

    // 解析草稿
    VocabWordDraftDto draftDto = JsonUtils.fromJson(vocabWord.getDraftContent(), VocabWordDraftDto.class);

    // 更新主表
    vocabWord.setWord(draftDto.getWord());
    vocabWord.setWordTraditional(draftDto.getWordTraditional());
    vocabWord.setPinyin(draftDto.getPinyin());
    // ... 其他字段

    // 更新子表
    syncSenses(id, draftDto.getSenses());
    syncExercises(id, draftDto.getExercises());

    // 更新状态
    vocabWord.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
    vocabWord.setDraftContent(null); // 清空草稿

    vocabWordRepository.save(vocabWord);
}
```

#### 5.5 从已发布创建草稿

```java
@Transactional(rollbackFor = Exception.class)
public void createDraftFromPublished(Integer id) {
    VocabWord vocabWord = vocabWordRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id)));

    // 如果已有草稿，跳过
    if (vocabWord.getDraftContent() != null) {
        return;
    }

    // 从正式字段构建 DTO
    VocabWordDto dto = vocabWordMapper.toDto(vocabWord);
    dto.setSenses(convertToSenseDtos(vocabSenseRepository.findByWordIdAndStatus(id, StatusEnum.ENABLED.getCode())));
    dto.setExercises(convertToExerciseDtos(vocabExerciseRepository.findByWordIdAndStatus(id, StatusEnum.ENABLED.getCode())));

    // 存为草稿
    vocabWord.setDraftContent(JsonUtils.toJson(dto));
    vocabWord.setEditStatus(EditStatusEnum.DRAFT.getCode());
    vocabWordRepository.save(vocabWord);
}
```

### 6. Controller 层修改

#### 6.1 后台词汇接口

**文件**: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java`

```java
@Log("查询词汇草稿详情")
@ApiOperation("根据ID查询词汇草稿详情")
@AnonymousGetMapping("/{id}/draft")
public ResponseEntity<VocabWordVO> getDraft(@PathVariable Integer id) { ... }

@Log("新增词汇草稿")
@ApiOperation("新增词汇草稿")
@AnonymousPostMapping("/draft")
public ResponseEntity<VocabWordCreateVO> createDraft(@Valid @RequestBody VocabWordCreateRequest request) { ... }

@Log("修改词汇草稿")
@ApiOperation("修改词汇草稿")
@AnonymousPutMapping("/{id}/draft")
public ResponseEntity<Object> updateDraft(@PathVariable Integer id, @Valid @RequestBody VocabWordCreateRequest request) { ... }

@Log("从已发布内容创建草稿")
@ApiOperation("从已发布内容创建草稿")
@AnonymousPostMapping("/{id}/draft/from-published")
public ResponseEntity<Object> createDraftFromPublished(@PathVariable Integer id) { ... }

@Log("审核词汇草稿")
@ApiOperation("审核词汇草稿（草稿→已审核）")
@AnonymousPutMapping("/{id}/review")
public ResponseEntity<Object> reviewDraft(@PathVariable Integer id) { ... }

@Log("发布词汇")
@ApiOperation("发布词汇（已审核→已发布）")
@AnonymousPutMapping("/{id}/publish")
public ResponseEntity<Object> publishDraft(@PathVariable Integer id) { ... }

@Log("下线词汇")
@ApiOperation("下线词汇")
@AnonymousPutMapping("/{id}/offline")
public ResponseEntity<Object> offline(@PathVariable Integer id) { ... }

// 原有的查询接口保持不变，返回已发布内容
// 但需要在 VO 中增加状态字段
```

#### 6.2 后台汉字接口

**文件**: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java`

按同样模式修改。

#### 6.3 用户端接口（保持不变）

用户端继续查询正式字段，无需修改：
- `AppVocabWordController`
- `AppCharCharacterController`

但需要在查询条件中增加 `publishStatus = 'published'` 过滤。

### 7. VO 修改

#### 7.1 VocabWordBaseVO

**文件**: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordBaseVO.java`

```java
@Getter
@Setter
public class VocabWordBaseVO implements Serializable {
    // ... 现有字段 ...

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "是否有草稿")
    private Boolean hasDraft;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "修改人")
    private String updateBy;
}
```

#### 7.2 CharCharacterBaseVO

**文件**: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java`

同样增加状态字段。

### 8. Mapper 修改

不需要新增 Mapper，复用现有 Mapper，增加 JSON 转换工具类即可。

### 9. 状态流转图

```
[首次创建]
    ↓
draftContent 有内容
editStatus = DRAFT
publishStatus = UNPUBLISHED
    ↓ (审核)
editStatus = REVIEWED
    ↓ (发布)
publishStatus = PUBLISHED
draftContent 清空
正式字段更新
    ↓ (编辑)
从正式字段复制到 draftContent
editStatus = DRAFT
    ↓ (重新审核)
editStatus = REVIEWED
    ↓ (重新发布)
正式字段更新
draftContent 清空
    ↓ (下线)
publishStatus = UNPUBLISHED
正式字段逻辑删除
```

### 10. 数据库迁移

创建 SQL 脚本：`sql/migration/publish_status_migration.sql`

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

## 涉及文件清单

### 新增文件

#### 枚举类
1. `grid-common/src/main/java/com/naon/grid/enums/PublishStatusEnum.java`
2. `grid-common/src/main/java/com/naon/grid/enums/EditStatusEnum.java`

#### DTO
3. `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordDraftDto.java`
4. `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDraftDto.java`

#### 数据库脚本
5. `sql/migration/publish_status_migration.sql`

### 修改文件

#### 实体类
6. `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabWord.java`
7. `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharCharacter.java`

#### VO
8. `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordBaseVO.java`
9. `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java`
10. `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java`
11. `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java`

#### Service 接口
12. `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabWordService.java`
13. `grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java`

#### Service 实现
14. `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`
15. `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

#### 后台 Controller
16. `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java`
17. `grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java`

#### 用户端 Controller（增加 publishStatus 过滤）
18. `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java`
19. `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharCharacterController.java`

## 注意事项

1. **JSON 序列化**：确保 JSON 工具类能正确处理复杂嵌套结构
2. **JPA Auditing**：确保 `@EnableJpaAuditing` 已启用，以便 `createBy` 和 `updateBy` 自动填充
3. **数据库备份**：执行迁移脚本前请先备份数据库
4. **子表同步**：发布时要确保主表和所有子表都正确更新
5. **历史数据初始化**：迁移脚本中已把现有数据初始化为已发布状态
