# 用户端词汇书接口设计

## 概述

为词汇书（`vocab_book`）提供用户侧 API，包含书籍列表查询和书籍下词汇列表查询两个接口。实现模式完全参照已有的汉字书（`AppCharBookController`）实现。

## 表结构

```sql
CREATE TABLE `vocab_book`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '词汇书ID',
  `type` varchar(32) NOT NULL COMMENT '词汇书类型, 参考枚举：VocabBookTypeEnum',
  `name` varchar(32) NOT NULL COMMENT '词汇书名称',
  `sub_name` varchar(32) NOT NULL COMMENT '词汇书子名称',
  `cover_image` varchar(512) NOT NULL COMMENT '词汇书封面图',
  `desc` varchar(1024) DEFAULT NULL COMMENT '词汇书描述',
  `hsk_level` varchar(32) DEFAULT NULL COMMENT 'HSK等级(如果有等级则按照等级去检索词汇)',
  `word_ids` text DEFAULT NULL COMMENT '词汇ID列表(可能为空，比如通过HSK等级去检索词汇的情况)',
  `order` int NULL DEFAULT 0 COMMENT '排序(值大的排前面)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '词汇书表';
```

词汇来源：
1. **HSK 等级模式**：`hsk_level` 不为空时，去 `vocab_word` 表查询该等级下所有已发布的词汇。
2. **word_ids 模式**：`hsk_level` 为空时，使用 `word_ids` 中的词汇 ID 列表按序查询。

## 文件清单

### grid-system（DO / Repo / Service）

| 文件 | 包路径 |
|---|---|
| `VocabBook.java` | `com.naon.grid.backend.domain.vocabulary` |
| `VocabBookRepository.java` | `com.naon.grid.backend.repo.vocabulary` |
| `VocabBookService.java` | `com.naon.grid.backend.service.vocabulary` |
| `VocabBookServiceImpl.java` | `com.naon.grid.backend.service.vocabulary.impl` |

### grid-app（Controller / VO）

| 文件 | 包路径 |
|---|---|
| `AppVocabBookController.java` | `com.naon.grid.modules.app.rest` |
| `AppVocabBookListVO.java` | `com.naon.grid.modules.app.rest.vo` |
| `AppVocabBookWordVO.java` | `com.naon.grid.modules.app.rest.vo` |

### 修改文件

| 文件 | 改动 |
|---|---|
| `VocabWordRepository.java` | 新增 `findByHskLevelAndStatusAndPublishStatus` 和 `findByIdIn` 两个查询方法 |

## DO：VocabBook

与 `CharBook` 结构一致，映射 `vocab_book` 表。

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键，自增 |
| type | String | 词汇书类型，对应 `VocabBookTypeEnum` |
| name | String | 词汇书名称 |
| subName | String | 词汇书子名称 |
| coverImage | String | 封面图 URL |
| desc | String | 描述 |
| hskLevel | String | HSK 等级 |
| wordIds | String | 词汇 ID 列表（JSON 数组字符串） |
| order | Integer | 排序权重 |
| createTime | Timestamp | 创建时间 |
| updateTime | Timestamp | 更新时间 |
| status | Integer | 有效状态（1=有效，0=无效） |

使用 `@Entity`、`@Table(name = "vocab_book")`，不使用 `BaseEntity` 继承（与 `CharBook` 一致，手动声明时间戳字段）。

## Repository：VocabBookRepository

继承 `JpaRepository<VocabBook, Long>`，提供：

```java
List<VocabBook> findByStatusOrderByOrderDesc(Integer status);
```

## Service：VocabBookService

接口定义三个方法：

```java
List<VocabBook> findAvailableBooks();           // 查询所有有效书籍
VocabBook findAvailableById(Long id);           // 按 ID 查询单本书
List<VocabWord> findWordsByBook(VocabBook book); // 查询书籍词汇
```

### findWordsByBook 实现逻辑

```
if hskLevel 不为空 → VocabWordRepository.findByHskLevelAndStatusAndPublishStatus(level, 1, "published")
else if wordIds 不为空 → 解析 word_ids JSON → findByIdIn → 按 word_ids 顺序排序 → 过滤有效已发布
else → 返回空列表
```

排序逻辑（word_ids 模式）：
1. 从数据库查询出所有匹配的词汇
2. 过滤 `status=1` 且 `publishStatus="published"`
3. 按 `word_ids` 的原始顺序重排

## VocabWordRepository 新增方法

```java
List<VocabWord> findByHskLevelAndStatusAndPublishStatus(String hskLevel, Integer status, String publishStatus);
List<VocabWord> findByIdIn(List<Integer> ids);
```

## VO 设计

### AppVocabBookListVO

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 词汇书 ID |
| type | String | 词汇书类型 |
| name | String | 词汇书名称 |
| subName | String | 词汇书子名称 |
| coverImage | String | 封面图 |
| desc | String | 描述 |

### AppVocabBookWordVO

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Integer | 词汇 ID（对应 VocabWord.id） |
| word | String | 词汇 |
| pinyin | String | 拼音 |
| hskLevel | String | HSK 等级 |

## API 接口

### GET /api/app/vocab-book

查询词汇书列表。

- **注解**：`@AnonymousGetMapping`
- **响应**：`List<AppVocabBookListVO>`

### GET /api/app/vocab-book/{id}/words

查询词汇书下的词汇列表。

- **注解**：`@AnonymousGetMapping`
- **入参**：路径参数 `id`（书籍 ID）
- **响应**：`List<AppVocabBookWordVO>`

## 错误处理

- 书籍不存在或已下架：抛出 `EntityNotFoundException`，返回 404
- `word_ids` JSON 解析失败：抛出 `BadRequestException`，返回 400

## 测试要点

1. 书籍列表正常返回已发布、按 order 排序的书籍
2. HSK 等级模式：正确查询对应等级的词汇
3. word_ids 模式：正确按原始顺序返回词汇
4. 书籍不存在时返回 404
5. 已下架（status=0）的书籍不显示
6. 词汇需要同时满足 `status=1` 和 `publish_status="published"`
