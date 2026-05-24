# 汉字管理后台设计

## 背景

项目已经有词汇教学后台功能，汉字教学需要新增一套管理后台。汉字数据后续由用户自行通过 SQL 导入，本次只实现后台 CRUD，不实现 Excel 解析或导入。

## 数据模型

### `char_character`

汉字主表，一条记录对应一个汉字教学条目。主要字段包括序号、汉字、等级、拼音、音频 ID、繁体、部首、笔顺、部件说明、多语种说明、创建时间和更新时间。

调整后的约束和类型：

- `audio_id` 使用 `bigint`，Java 中使用 `Long`。
- `char_desc` 使用 `varchar(1024)`。
- `desc_translations` 使用 `text`，由调用方自行存储 JSON 字符串。
- `stroke` 保持 `varchar(4096)`。
- `character` 只建普通索引，不建唯一索引。

### `char_discrimination`

汉字辨析表，一个汉字可以有多个辨析字。通过 `char_id` 关联主表，不加数据库外键，只建立 `idx_char_id` 普通索引。

多语种翻译字段使用 `text`，由调用方自行存储 JSON 字符串。

### `char_word`

汉字组词表，一个汉字可以有多个组词。通过 `char_id` 关联主表，不加数据库外键，只建立 `idx_char_id` 普通索引，同时保留 `idx_word_item` 方便按组词检索。

多语种翻译字段使用 `text`，由调用方自行存储 JSON 字符串。

## SQL

```sql
CREATE TABLE `char_character` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '汉字唯一ID',
  `sequence_no` int(11) DEFAULT NULL COMMENT 'Excel中的序号',
  `character` varchar(10) NOT NULL COMMENT '汉字',
  `level` varchar(20) DEFAULT NULL COMMENT '等级（如：3）',
  `pinyin` varchar(100) NOT NULL COMMENT '拼音',
  `audio_id` bigint DEFAULT NULL COMMENT '音频资源ID',
  `traditional` varchar(10) DEFAULT NULL COMMENT '繁体字',
  `radical` varchar(10) DEFAULT NULL COMMENT '部首',
  `stroke` varchar(4096) DEFAULT NULL COMMENT '笔顺',
  `char_desc` varchar(1024) DEFAULT NULL COMMENT '部件组合中文说明',
  `desc_translations` text DEFAULT NULL COMMENT '汉字说明的多语种翻译',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_character` (`character`),
  KEY `idx_pinyin` (`pinyin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='汉字主表';

CREATE TABLE `char_discrimination` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '辨析记录ID',
  `char_id` int(11) NOT NULL COMMENT '所属汉字ID',
  `discrim_char` varchar(10) NOT NULL COMMENT '辨析汉字',
  `discrim_pinyin` varchar(100) DEFAULT NULL COMMENT '辨析字拼音',
  `discrim_char_translations` text DEFAULT NULL COMMENT '辨析字说明的多语种翻译',
  `comparison_translations` text DEFAULT NULL COMMENT '对比辨析说明的多语种翻译',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_char_id` (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='汉字辨析表';

CREATE TABLE `char_word` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '组词记录ID',
  `char_id` int(11) NOT NULL COMMENT '所属汉字ID',
  `word_item` varchar(50) NOT NULL COMMENT '组词',
  `level` varchar(20) DEFAULT NULL COMMENT '组词HSK等级',
  `pinyin` varchar(100) DEFAULT NULL COMMENT '组词拼音',
  `part_of_speech` varchar(50) DEFAULT NULL COMMENT '词性',
  `word_item_translations` text DEFAULT NULL COMMENT '组词说明的多语种翻译',
  `example_sentence` text DEFAULT NULL COMMENT '中文例句',
  `example_pinyin` varchar(500) DEFAULT NULL COMMENT '例句拼音',
  `example_translations` text DEFAULT NULL COMMENT '例句的多语种翻译',
  `example_image` varchar(255) DEFAULT NULL COMMENT '例句图片路径',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_char_id` (`char_id`),
  KEY `idx_word_item` (`word_item`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='汉字组词表';
```

## 后端组件

新增代码参考词汇模块风格，放在 `character` 相关包下。

- `domain.character`
  - `CharCharacter`
  - `CharDiscrimination`
  - `CharWord`
- `repo.character`
  - `CharCharacterRepository`
  - `CharDiscriminationRepository`
  - `CharWordRepository`
- `service.character`
  - `CharCharacterService`
- `service.character.impl`
  - `CharCharacterServiceImpl`
- `service.character.dto`
  - `CharCharacterDto`
  - `CharDiscriminationDto`
  - `CharWordDto`
  - `CharCharacterQueryCriteria`
- `service.character.mapstruct`
  - `CharCharacterMapper`
- `rest.request`
  - `CharCharacterQueryRequest`
  - `CharCharacterCreateRequest`
- `rest.vo`
  - `CharCharacterBaseVO`
  - `CharCharacterVO`
  - `CharCharacterCreateVO`
- `rest.controller`
  - `CharCharacterController`

## 接口设计

控制器路径为 `/api/character`，Swagger tag 为 `汉字：汉字管理`。接口使用匿名 mapping，开发阶段不需要鉴权。

- `GET /api/character/{id}`：根据 ID 查询汉字详情。
- `GET /api/character`：分页查询汉字列表。
- `POST /api/character`：新增汉字。
- `PUT /api/character/{id}`：更新汉字。
- `DELETE /api/character/{id}`：删除汉字。

列表接口只返回主表基础字段，不返回辨析和组词，避免列表响应过重。详情接口返回主表字段、`discriminations` 和 `words`。

查询参数先提供 `blurry`，匹配 `character` 和 `pinyin`。

## 数据流

- `GET /api/character`：请求参数转换为 Criteria，Repository 分页查询主表，Mapper 转 DTO，再转换为 BaseVO 列表。
- `GET /api/character/{id}`：查询主表，主表不存在时抛 `EntityNotFoundException`；存在时查询辨析和组词子表并组装详情。
- `POST /api/character`：请求转换为 DTO，先保存主表，再使用新主表 ID 保存辨析和组词。
- `PUT /api/character/{id}`：校验主表存在，更新主表字段，删除旧子表，再保存请求中的辨析和组词。
- `DELETE /api/character/{id}`：校验主表存在，先删除子表，再删除主表。

## 错误处理和校验

沿用词汇模块风格，不增加复杂业务校验。

- 主表必填：`character`、`pinyin`。
- 辨析表必填：`charId`、`discrimChar`。
- 组词表必填：`charId`、`wordItem`。
- text 类型的多语种翻译字段不校验 JSON 合法性。
- 查询、更新、删除不存在的 ID 时抛 `EntityNotFoundException`。

## 验证方式

完成实现后运行：

```bash
mvn -pl grid-system -am compile
```

接口层按新增、列表、详情、更新、删除的顺序验证 CRUD 主流程。
