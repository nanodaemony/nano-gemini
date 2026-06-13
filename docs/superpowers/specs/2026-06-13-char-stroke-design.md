# 汉字笔顺（Char Stroke）模块设计

## 概述

为汉字管理系统增加笔顺查询功能。将 hanzi-writer-data 中的笔顺动画数据从 JSON 文件导入数据库，提供独立的笔顺查询接口，同时从原有的 `char_character` 表中移除冗余的 `stroke` 字段。

## 目标

1. 新增 `char_stroke` 表，存放汉字笔顺数据（JSON 格式）
2. 编写 Python 脚本将 9574 个 JSON 文件导出为 SQL INSERT 语句
3. 新增 `CharStroke` Entity、Repository、Service 层
4. 在 `CharCharacterController` 新增笔顺查询接口
5. 从 `char_character` 表及相关 Java 类中移除 `stroke` 字段

## 数据库设计

### 新增表 `char_stroke`

```sql
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

### 修改 `char_character` 表

删除 `stroke` 列：

```sql
ALTER TABLE `char_character` DROP COLUMN `stroke`;
```

注意：`biz_character.sql` 中的 `CREATE TABLE` 也要同步移除该列。

## 数据导出脚本

### 技术选型

Python 3 脚本，使用内置库（`json`、`os`、`glob`），无外部依赖。

### 脚本行为

1. 扫描 `hanzi-writer-data/data/` 下所有 `.json` 文件
2. 文件名去掉 `.json` 作为 `character` 字段值
3. JSON 文件内容原样作为 `stroke` 字段值
4. 生成一条 INSERT 语句包含全部数据
5. 输出到 `sql/char_stroke_data.sql`

### 脚本输出格式

```sql
INSERT INTO `char_stroke` (`character`, `stroke`, `status`) VALUES
('一', '{"strokes":[...],"medians":[...]}', 1),
('丁', '{"strokes":[...],"medians":[...],"radStrokes":[1]}', 1),
...;
```

## 后端代码结构

### 包路径

```
grid-system/src/main/java/com/naon/grid/backend/
├── domain/character/
│   ├── CharCharacter.java              # 移除 stroke 字段
│   └── CharStroke.java                 # 【新建】
├── repo/character/
│   ├── CharCharacterRepository.java
│   └── CharStrokeRepository.java       # 【新建】
├── rest/
│   ├── controller/
│   │   └── CharCharacterController.java # 新增笔顺查询接口
│   ├── request/
│   │   └── CharCharacterCreateRequest.java  # 移除 stroke 字段
│   └── vo/
│       ├── CharCharacterBaseVO.java    # 移除 stroke 字段
│       ├── CharCharacterVO.java        # 移除 stroke 字段
│       ├── CharCharacterCreateVO.java  # 不变
│       └── CharStrokeVO.java           # 【新建】
├── service/character/
│   ├── CharCharacterService.java       # 不变
│   ├── CharStrokeService.java          # 【新建】接口
│   ├── dto/
│   │   ├── CharCharacterDto.java       # 移除 stroke 字段
│   │   ├── CharCharacterDraftDto.java  # 移除 stroke 字段
│   │   └── ...
│   └── impl/
│       ├── CharCharacterServiceImpl.java  # 移除 stroke 相关处理
│       └── CharStrokeServiceImpl.java     # 【新建】
└── wrapper/
    └── CharCharacterWrapper.java       # 移除 stroke 映射
```

### Entity: `CharStroke.java`

```java
@Entity
@Getter
@Setter
@Table(name = "char_stroke")
public class CharStroke implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "`character`", nullable = false, length = 32)
    private String character;

    @Column(name = "stroke", columnDefinition = "text")
    private String stroke;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @Column(name = "status")
    private Integer status = StatusEnum.ENABLED.getCode();
}
```

### Repository: `CharStrokeRepository.java`

```java
public interface CharStrokeRepository extends JpaRepository<CharStroke, Long> {
    Optional<CharStroke> findByCharacterAndStatus(String character, Integer status);
}
```

### Service 接口 + 实现

接口：`CharStrokeService.java`

```java
public interface CharStrokeService {
    /**
     * 根据汉字查询笔顺JSON
     * @param character 汉字
     * @return 笔顺JSON字符串，不存在返回null
     */
    String findByCharacter(String character);
}
```

实现：`CharStrokeServiceImpl.java`

```java
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

### VO: `CharStrokeVO.java`

```java
@Getter
@Setter
public class CharStrokeVO implements Serializable {

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "笔顺SVG路径数据")
    private List<String> strokes;

    @ApiModelProperty(value = "笔顺坐标参考线数据")
    private List<List<List<Integer>>> medians;

    @ApiModelProperty(value = "部首笔画索引")
    private List<Integer> radStrokes;

    // 也可将 stroke JSON 解析后填充到上述字段
}
```

### Controller 新增接口

```java
@Log("查询汉字笔顺")
@ApiOperation("根据汉字查询笔顺")
@AnonymousGetMapping("/stroke/{character}")
public ResponseEntity<CharStrokeVO> findStrokeByCharacter(@PathVariable String character) {
    String strokeJson = charStrokeService.findByCharacter(character);
    CharStrokeVO vo = CharStrokeWrapper.toStrokeVO(character, strokeJson);
    return new ResponseEntity<>(vo, HttpStatus.OK);
}
```

### Wrapper 工具

新建 `CharStrokeWrapper.java`，使用项目已有的 Fastjson2 进行 JSON 解析：

```java
public class CharStrokeWrapper {

    public static CharStrokeVO toStrokeVO(String character, String strokeJson) {
        CharStrokeVO vo = new CharStrokeVO();
        vo.setCharacter(character);
        if (strokeJson == null) {
            return vo;
        }
        JSONObject obj = JSON.parseObject(strokeJson);
        vo.setStrokes(obj.getJSONArray("strokes").toJavaList(String.class));
        vo.setMedians(obj.getJSONArray("medians").toJavaList(List.class));
        // radStrokes 可能为 null（部分汉字无此字段）
        JSONArray radArr = obj.getJSONArray("radStrokes");
        if (radArr != null) {
            vo.setRadStrokes(radArr.toJavaList(Integer.class));
        }
        return vo;
    }
}
```

## 接口定义

### 查询笔顺

```
GET /api/character/stroke/{character}
```

不需要认证（`@AnonymousGetMapping`）。

**响应 200：**

```json
{
  "character": "一",
  "strokes": ["M 518 382 Q 572 385 623 389 ..."],
  "medians": [[[121, 393], [193, 372], ...]],
  "radStrokes": [1]
}
```

**不存在时：** 返回 `200` + `strokes`/`medians`/`radStrokes` 均为 null 或空列表。

## 涉及的文件修改清单

### 删除的字段（8 个文件 + 1 个 SQL）

| 文件 | 改动 |
|------|------|
| `CharCharacter.java` | 删除 `stroke` 字段 |
| `CharCharacterCreateRequest.java` | 删除 `stroke` 字段 |
| `CharCharacterBaseVO.java` | 删除 `stroke` 字段 |
| `CharCharacterVO.java` | 删除 `stroke` 字段 |
| `CharCharacterDto.java` | 删除 `stroke` 字段 |
| `CharCharacterDraftDto.java` | 删除 `stroke` 字段 |
| `CharCharacterWrapper.java` | 删除 3 处 `stroke` 映射 |
| `CharCharacterServiceImpl.java` | 删除 `applyDraftOverlay` 和 `publishDraft` 中的 `stroke` |
| `biz_character.sql` | 从 `CREATE TABLE` 中删除 `stroke` 列 |

### 新增的文件（5 个）

| 文件 | 说明 |
|------|------|
| `CharStroke.java` | Entity |
| `CharStrokeRepository.java` | Repository |
| `CharStrokeService.java` | Service 接口 |
| `CharStrokeServiceImpl.java` | Service 实现 |
| `CharStrokeVO.java` | VO |

### 修改的文件（1 个）

| 文件 | 说明 |
|------|------|
| `CharCharacterController.java` | 新增笔顺查询接口 |

## SQL 脚本

最终 DDL 追加写入 `sql/biz_character.sql` 末尾。

数据导入脚本输出到 `sql/char_stroke_data.sql`，执行方式：

```sql
SOURCE /path/to/char_stroke_data.sql;
```

## 非功能性设计

- 笔顺数据为静态只读，不需要缓存（单表查询，MySQL 索引直接命中）
- 不存在时返回空值，不抛出异常
- 数据全量由脚本导入，不提供管理端增删改接口
