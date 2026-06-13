# 汉字书（Char Book）功能设计

## 概述

为汉语教学系统新增"汉字书"功能。用户可浏览汉字书列表，点击书籍查看该书下的汉字列表，再点击汉字跳转到汉字详情页。

## 数据表设计

### char_book（汉字书表）

```sql
CREATE TABLE `char_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '汉字书ID',
  `type` varchar(32) NOT NULL COMMENT '汉字书类型, 参考枚举：CharBookTypeEnum',
  `name` varchar(32) NOT NULL COMMENT '汉字书名称',
  `sub_name` varchar(32) NOT NULL COMMENT '汉字书子名称',
  `cover_image` varchar(512) NOT NULL COMMENT '汉字书封面图',
  `desc` varchar(1024) DEFAULT NULL COMMENT '汉字书描述',
  `hsk_level` varchar(32) DEFAULT NULL COMMENT 'HSK等级(如果有等级则按照等级去检索汉字)',
  `word_ids` text DEFAULT NULL COMMENT '汉字ID列表(可能为空，此时使用hsk_level查询)',
  `order` int DEFAULT 0 COMMENT '排序(值大的排前面)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '汉字书表';
```

**汉字来源策略**：
- 如果 `hsk_level` 不为空，则查询汉字表中所有该 HSK 等级的已发布汉字
- 如果 `hsk_level` 为空，则根据 `word_ids` 中的汉字 ID 列表查询，并保持 `word_ids` 中的顺序

### CharBookTypeEnum

定义在 `grid-common/src/main/java/com/naon/grid/enums/CharBookTypeEnum.java`：

| 编码 | 说明 |
|------|------|
| `HANDWRITING` | 手写汉字书 |

## 模块结构

### grid-system（DO / Repository / Service）

所有文件在 `com.naon.grid.backend` 包下：

#### Entity — `domain/character/CharBook`

- 不继承 `BaseEntity`（表不含 `create_by`/`update_by`）
- 与 `CharStroke` 模式一致：自身带 `createTime`、`updateTime`、`status`
- `id` 使用 `Long` 类型（匹配 SQL `bigint`）
- `wordIds` 字段映射 `word_ids` 列，存储 JSON 数组字符串
- `desc` 映射 `` `desc` `` 列
- `order` 映射 `` `order` `` 列

#### Repository — `repo/character/CharBookRepository`

```java
@Repository
public interface CharBookRepository extends JpaRepository<CharBook, Long> {
    List<CharBook> findByStatusOrderByOrderDesc(Integer status);
}
```

#### Service — `service/character/CharBookService`

接口：
```java
public interface CharBookService {
    List<CharBook> findAvailableBooks();
    CharBook findAvailableById(Long id);
    List<CharCharacter> findCharactersByBook(CharBook book);
}
```

实现要点：

- **`findAvailableBooks()`**：调用 `charBookRepository.findByStatusOrderByOrderDesc(StatusEnum.ENABLED.getCode())`
- **`findAvailableById(Long id)`**：查询 ID + 校验 `status=1`，不存在抛 `EntityNotFoundException`
- **`findCharactersByBook(CharBook book)`**：
  - 若 `book.getHskLevel() != null`：调用 `charCharacterRepository.findByHskLevelAndStatusAndPublishStatus(hskLevel, StatusEnum.ENABLED.getCode(), PublishStatusEnum.PUBLISHED.getCode())`
  - 若 `book.getWordIds() != null`：解析 JSON 为 `List<Integer>` → 调用 `charCharacterRepository.findByIdIn(ids)` → 按 `word_ids` 原始顺序排序结果 → 过滤仅保留已发布的有效汉字
  - 若两者均为空：返回空列表

> 需要给 `CharCharacterRepository` 新增两个查询方法：
> - `findByHskLevelAndStatusAndPublishStatus(String hskLevel, Integer status, String publishStatus)`
> - `findByIdIn(List<Integer> ids)`

### grid-app（Controller / VO）

#### VO — `modules/app/rest/vo/AppCharBookListVO`

```java
@Getter
@Setter
public class AppCharBookListVO implements Serializable {
    private Long id;
    private String type;
    private String name;
    private String subName;
    private String coverImage;
    private String desc;
}
```

#### VO — `modules/app/rest/vo/AppCharBookCharVO`

```java
@Getter
@Setter
public class AppCharBookCharVO implements Serializable {
    private Integer id;
    private String character;
    private String pinyin;
    private String hskLevel;
}
```

#### Controller — `modules/app/rest/AppCharBookController`

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/char-book")
@Api(tags = "用户：汉字书接口")
```

**接口列表**：

| 方法 | 路径 | 说明 | 出参 |
|------|------|------|------|
| `GET` | `/api/app/char-book` | 查询汉字书列表 | `List<AppCharBookListVO>` |
| `GET` | `/api/app/char-book/{id}/characters` | 查询书籍下汉字列表 | `List<AppCharBookCharVO>` |

两个接口均为 `@AnonymousGetMapping`（无需登录认证）。

## 数据流

### 书籍列表

```
AppCharBookController.listBooks()
  → charBookService.findAvailableBooks()
    → charBookRepository.findByStatusOrderByOrderDesc(1)
  → entity → AppCharBookListVO 转换
```

### 书籍汉字

```
AppCharBookController.listCharacters(id)
  → charBookService.findAvailableById(id)
    → charBookRepository.findById(id) + status=1 校验
  → charBookService.findCharactersByBook(book)
    ├─ hskLevel != null: charCharacterRepository.findByHskLevelAndPublishStatus(...)
    └─ wordIds != null: charCharacterRepository.findByIdIn(ids) + 按 word_ids 序重排
  → entity → AppCharBookCharVO 转换
```

## 错误处理

- 书籍不存在或已下架（status=0）：返回 HTTP 404 (`EntityNotFoundException`)
- `word_ids` JSON 解析失败：返回 HTTP 400 (`BadRequestException`)

## 注意事项

- 只返回 `publishStatus = published` 且 `status = 1` 的汉字
- 不涉及草稿/发布工作流，书籍数据通过 SQL 直接插入
- 不新增后台管理接口
