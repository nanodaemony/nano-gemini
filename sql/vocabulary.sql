################################################################## 词汇相关 #####################################################################

-- 词汇表
-- 注：一个词汇对应一条数据，注意对于 “啊” 这种词可能有多个读音，每个读音都是一个词汇，一个词汇对应一条数据。

CREATE TABLE `vocab_word` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '词汇唯一ID',
  `word` varchar(50) NOT NULL COMMENT '词汇（如：啊）',
  `word_traditional` varchar(50) DEFAULT NULL COMMENT '繁体词汇',
  `pinyin` varchar(100) DEFAULT NULL COMMENT '标准拼音（含声调）',
  `audio_id` int(11) DEFAULT NULL COMMENT '词汇读音音频资源ID',
  `hsk_level` varchar(20) DEFAULT NULL COMMENT 'HSK等级(1-9)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` TINYINT(4) NOT null DEFAULT '1' COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`),
  KEY `idx_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='词汇表';


-- 词汇义项表
-- 注：一个词汇可能有多个词汇义项，一个词义项对应一条数据。
CREATE TABLE `vocab_sense` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增ID, 义项ID',
  `word_id` int(11) NOT NULL COMMENT '所属词汇ID',
  `part_of_speech` varchar(50) DEFAULT NULL COMMENT '词性（名词、动词、形容词等）',
  `chinese_def` text DEFAULT NULL COMMENT '中文释义',
  `def_audio_id` int(11) DEFAULT NULL COMMENT '中文释义音频资源ID',
  `def_image` int(11) DEFAULT NULL COMMENT '中文释义图片(ID)',
  `translations` json DEFAULT NULL COMMENT '词汇的多语种翻译,JSON列表格式(List<TextTranslation>)',
  `synonyms` text DEFAULT NULL COMMENT '近义词列表（展示用）, JSON列表格式(List<String>)',
  `antonyms` text DEFAULT NULL COMMENT '反义词列表（展示用）, JSON列表格式List<String>',
  `related_forward` text DEFAULT NULL COMMENT '正序关联词汇, JSON列表格式List<String>',
  `related_backward` text DEFAULT NULL COMMENT '逆序关联词汇, JSON列表格式List<String>',
  `related_other` text DEFAULT NULL COMMENT '其他关联词汇, JSON列表格式List<String>',
  `sense_order` int(11) NOT NULL DEFAULT '0' COMMENT '义项排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` TINYINT(4) NOT null DEFAULT '1' COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`),
  KEY `idx_word_id` (`word_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='词汇义项表';

-- 词汇结构搭配表
-- 注：一个词汇义项可能有多个结构搭配，一个结构搭配对应一条数据。
CREATE TABLE `vocab_structure` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增ID, 结构搭配ID',
  `word_id` int(11) NOT NULL COMMENT '所属词汇ID',
  `sense_id` int(11) NOT NULL COMMENT '所属义项ID',
  `pattern` varchar(255) NOT NULL COMMENT '结构搭配文案',
  `pattern_def` varchar(512) DEFAULT NULL COMMENT '结构搭配释义(可空)',
  `pattern_def_translations` varchar(1024) DEFAULT NULL COMMENT '结构搭配释义外文翻译, JSON列表格式(List<TextTranslation>)',
  `structure_order` int(11) NOT NULL DEFAULT '0' COMMENT '搭配排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` TINYINT(4) NOT null DEFAULT '1' COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`),
  KEY `idx_sense_id` (`sense_id`),
  KEY `idx_word_id` (`word_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='词汇结构搭配表';


-- 词汇搭配例句表
-- 注：一个词汇结构搭配可能有多个例句，一个例句对应一条数据。注意例句里面可能有图片，这块可以存成富文本的字符串。
CREATE TABLE `vocab_example` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '例句唯一ID',
  `word_id` int(11) NOT NULL COMMENT '所属词汇ID',
  `sense_id` int(11) NOT NULL COMMENT '所属义项ID',
  `structure_id` int(11) NOT NULL COMMENT '所属结构搭配ID',
  `sentence` text NOT NULL COMMENT '例句中文文案',
  `pinyin` varchar(500) DEFAULT NULL COMMENT '例句拼音',
  `audio_id` int(11) DEFAULT NULL COMMENT '例句音频资源ID',
  `translations` json DEFAULT NULL COMMENT '例句的多语种翻译,JSON列表格式(List<TextTranslation>)',
  `image` int(11) DEFAULT NULL COMMENT '例句图片(ID)',
  `example_order` int(11) NOT NULL DEFAULT '0' COMMENT '例句排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` TINYINT(4) NOT null DEFAULT '1' COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`),
  KEY `idx_structure_id` (`structure_id`),
  KEY `idx_sense_id` (`sense_id`),
  KEY `idx_word_id` (`word_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='词汇搭配例句表';


-- 词汇练习表
-- 注：一个词汇可能有多个练习题目，一个练习题目对应一条数据。
CREATE TABLE `vocab_exercise` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '练习题目唯一ID',
  `word_id` int(11) NOT NULL COMMENT '所属词汇ID',
  `question_type` varchar(20) NOT NULL COMMENT '题目类型（选择/填空等）,参考枚举：QuestionTypeEnum',
  `question_text` text NOT NULL COMMENT '练习题干描述',
  `options` text DEFAULT NULL COMMENT '选项列表（JSON），结构：[{option:A/B/C/D, text:选项文案}]',
  `answers` text DEFAULT NULL COMMENT '答案列表（可多选），结构: ["A", "B"]',
  `exercise_order` int(11) NOT NULL DEFAULT '0' COMMENT '练习题目排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` TINYINT(4) NOT null DEFAULT '1' COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`),
  KEY `idx_word_id` (`word_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='词汇练习表';


