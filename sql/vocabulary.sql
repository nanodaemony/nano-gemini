################################################################## 词汇相关 #####################################################################

-- 词汇表
-- 注：一个词汇对应一条数据，注意对于 “啊” 这种词可能有多个读音，每个读音都是一个词汇，一个词汇对应一条数据。
CREATE TABLE `vocab_word`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '词汇唯一ID',
  `word` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '词汇（如：啊）',
  `word_traditional` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '繁体词汇',
  `pinyin` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标准拼音（含声调）',
  `audio_id` int NULL DEFAULT NULL COMMENT '词汇读音音频资源ID',
  `hsk_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'HSK等级',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `publish_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
  `edit_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
  `draft_content` json NULL COMMENT '草稿内容JSON',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_word`(`word` ASC) USING BTREE,
  INDEX `idx_vocab_publish_status`(`publish_status` ASC) USING BTREE,
  INDEX `idx_vocab_edit_status`(`edit_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '词汇表' ROW_FORMAT = COMPACT;


-- 词汇义项表
-- 注：一个词汇可能有多个词汇义项，一个词义项对应一条数据。
CREATE TABLE `vocab_sense`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增ID, 义项ID',
  `word_id` int NOT NULL COMMENT '所属词汇ID',
  `part_of_speech` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '词性（名词、动词、形容词等）',
  `chinese_def` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '中文释义',
  `def_audio_id` int NULL DEFAULT NULL COMMENT '中文释义音频资源ID',
  `def_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '中文释义图片(ID)',
  `translations` json NULL COMMENT '外文翻译列表（语种+翻译）',
  `synonyms` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '近义词列表（展示用）, JSON列表格式',
  `antonyms` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '反义词列表（展示用）, JSON列表格式',
  `related_forward` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '正序关联词汇, JSON列表格式',
  `related_backward` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '逆序关联词汇, JSON列表格式',
  `related_other` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '其他关联词汇, JSON列表格式List<String>',
  `sense_order` int NOT NULL DEFAULT 0 COMMENT '义项排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_word_id`(`word_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '词汇义项表' ROW_FORMAT = COMPACT;

-- 词汇结构搭配表
-- 注：一个词汇义项可能有多个结构搭配，一个结构搭配对应一条数据。
CREATE TABLE `vocab_structure`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增ID, 结构搭配ID',
  `word_id` int NOT NULL COMMENT '所属词汇ID',
  `sense_id` int NOT NULL COMMENT '所属义项ID',
  `pattern` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '结构搭配文案',
  `pattern_def` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '结构搭配释义(可空)',
  `pattern_def_translations` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '结构搭配释义外文翻译,\r\n  JSON列表格式(List<TextTranslation>)',
  `structure_order` int NOT NULL DEFAULT 0 COMMENT '搭配排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sense_id`(`sense_id` ASC) USING BTREE,
  INDEX `idx_word_id`(`word_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '词汇结构搭配表' ROW_FORMAT = COMPACT;

-- 词汇搭配例句表
-- 注：一个词汇结构搭配可能有多个例句，一个例句对应一条数据。注意例句里面可能有图片，这块可以存成富文本的字符串。
CREATE TABLE `vocab_example`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '例句唯一ID',
  `word_id` int NOT NULL COMMENT '所属词汇ID',
  `sense_id` int NOT NULL COMMENT '所属义项ID',
  `structure_id` int NOT NULL COMMENT '所属结构搭配ID',
  `sentence` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '例句中文文案',
  `audio_id` int NULL DEFAULT NULL COMMENT '例句音频资源ID',
  `pinyin` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '例句拼音',
  `translations` json NULL COMMENT '例句外文翻译列表',
  `image` int NULL DEFAULT NULL COMMENT '例句图片(ID)',
  `example_order` int NOT NULL DEFAULT 0 COMMENT '例句排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_structure_id`(`structure_id` ASC) USING BTREE,
  INDEX `idx_sense_id`(`sense_id` ASC) USING BTREE,
  INDEX `idx_word_id`(`word_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '词汇搭配例句表' ROW_FORMAT = COMPACT;

-- 词汇练习表
-- 注：一个词汇可能有多个练习题目，一个练习题目对应一条数据。
CREATE TABLE `vocab_exercise`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '练习题目唯一ID',
  `word_id` int NOT NULL COMMENT '所属词汇ID',
  `question_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '题目类型（选择/填空等）',
  `question_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '练习题干描述',
  `options` json NULL COMMENT '选项列表（JSON），结构：[{option:A/B/C/D, text:选项文案}]',
  `answers` json NULL COMMENT '答案列表（可多选）',
  `exercise_order` int NOT NULL DEFAULT 0 COMMENT '练习题目排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_word_id`(`word_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '词汇练习表' ROW_FORMAT = COMPACT;

-- 纲外词记录表
CREATE TABLE `vocab_outline_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
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

