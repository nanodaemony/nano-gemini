################################################################## 词汇相关 #####################################################################

-- 词汇表
-- 注：一个词汇对应一条数据，注意对于 “啊” 这种词可能有多个读音，每个读音都是一个词汇，一个词汇对应一条数据。
CREATE TABLE `vocab_word`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '词汇ID',
  `word` varchar(50)  NOT NULL COMMENT '词汇词头（如：啊）',
  `word_traditional` varchar(50) NULL DEFAULT NULL COMMENT '词汇繁体',
  `pinyin` varchar(100) NULL DEFAULT NULL COMMENT '词汇拼音',
  `audio_id` bigint NULL DEFAULT NULL COMMENT '词汇音频资源ID',
  `hsk_level` varchar(20) NULL DEFAULT NULL COMMENT 'HSK等级',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(255) NULL DEFAULT NULL COMMENT '更新人',
  `draft_content` text NULL COMMENT '草稿内容JSON',
  `edit_status` varchar(20) NULL DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
  `publish_status` varchar(20) NULL DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_word`(`word` ASC) USING BTREE,
  INDEX `idx_publish_status`(`publish_status` ASC) USING BTREE,
  INDEX `idx_edit_status`(`edit_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '词汇表';


-- 词汇义项表
-- 注：一个词汇可能有多个词汇义项，一个词义项对应一条数据。
-- 义项的近义词、反义词、正序词、逆序词、乱序词等都存储在关联词汇表中。
CREATE TABLE `vocab_sense`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '义项ID',
  `word_id` bigint NOT NULL COMMENT '所属词汇ID',
  `part_of_speech` varchar(50) DEFAULT NULL COMMENT '词性（名词、动词、形容词等）, 参考枚举：PartOfSpeechEnum',
  `chinese_def` varchar(512) DEFAULT NULL COMMENT '中文释义',
  `def_translations` text NULL COMMENT '释义翻译列表',
  `def_audio_id` bigint NULL DEFAULT NULL COMMENT '中文释义音频资源ID',
  `def_image_id` bigint NULL DEFAULT NULL COMMENT '中文释义图片(ID)',
  `def_image_sentence_id` bigint NULL DEFAULT NULL COMMENT '释义图片例句ID（对应 example_sentence.id）',

  `order` int NOT NULL DEFAULT 0 COMMENT '义项排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_word_id`(`word_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '词汇义项表';

-- 词汇结构表
-- 注：一个词汇义项可能有多个结构，一个结构对应一条数据。
CREATE TABLE `vocab_structure`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '结构ID',
  `word_id` bigint NOT NULL COMMENT '所属词汇ID',
  `sense_id` bigint NOT NULL COMMENT '所属义项ID',
  `pattern` varchar(255) NOT NULL COMMENT '结构文案',
  `pattern_def` varchar(512) NULL DEFAULT NULL COMMENT '结构释义(可空)',
  `pattern_def_translations` text NULL DEFAULT NULL COMMENT '结构释义外文翻译,JSON列表格式(List<TextTranslation>)',
  `order` int NOT NULL DEFAULT 0 COMMENT '搭配排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sense_id`(`sense_id` ASC) USING BTREE,
  INDEX `idx_word_id`(`word_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '词汇结构表';


-- 词汇练习表
-- 注：一个词汇可能有多个练习题目，一个练习题目对应一条数据。
CREATE TABLE `vocab_exercise`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '练习题目ID',
  `word_id` bigint NOT NULL COMMENT '所属词汇ID',
  `question_type` varchar(20) NOT NULL COMMENT '题目类型（选择/填空等）',
  `question_text` varchar(512) DEFAULT NULL COMMENT '练习题干描述',
  `options` varchar(1024) DEFAULT NULL COMMENT '选项列表（JSON），结构：[{option:A/B/C/D, text:选项文案}]',
  `answers` varchar(512) DEFAULT NULL COMMENT '答案列表（可多选）',
  `order` int NOT NULL DEFAULT 0 COMMENT '练习题目排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_word_id`(`word_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '词汇练习表';


-- 关联词汇表
-- 记录词汇与词汇之间的关联关系
-- 关联义项ID暂时不需要
CREATE TABLE `vocab_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联词汇ID',
  `word_id` bigint NOT NULL COMMENT '词汇ID',
  `sense_id` bigint NOT NULL COMMENT '义项ID',
  `word` varchar(32) NOT NULL COMMENT '当前词汇',
  `relation_type` varchar(32) NULL DEFAULT NULL COMMENT '关联类型，参考枚举：VocabRelationTypeEnum（近义词、反义词、正序词、逆序词、乱序词等）',
  `relation_word_id` bigint NOT NULL COMMENT '关联词汇ID',
  `relation_sense_id` bigint NOT NULL COMMENT '关联义项ID',
  `relation_word` varchar(32) NOT NULL COMMENT '关联词汇',
  `order` int NOT NULL DEFAULT 0 COMMENT '关联词汇权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sense_id`(`sense_id` ASC) USING BTREE,
  INDEX `idx_word_id`(`word_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '关联词汇表';


-- 纲外词记录表
CREATE TABLE `vocab_outline_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `word` varchar(50) NOT NULL COMMENT '词汇文本（去空格后）',
  `search_count` int(11) NOT NULL DEFAULT '1' COMMENT '未搜到次数',
  `status` TINYINT(4) NOT NULL DEFAULT '0' COMMENT '处理状态, 0:未处理 1:已处理',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`),
  KEY `idx_status` (`status`),
  KEY `idx_search_count` (`search_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='纲外词记录表';

