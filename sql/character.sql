################################################################## 汉字相关 #####################################################################

-- 汉字表
-- 注：一个汉字对应一条数据。
CREATE TABLE `char_character` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '汉字唯一ID',
  `sequence_no` int(11) DEFAULT NULL COMMENT 'Excel中的序号',
  `character` varchar(10) NOT NULL COMMENT '汉字',
  `level` varchar(20) DEFAULT NULL COMMENT '等级（如：3）',
  `pinyin` varchar(100) DEFAULT NULL COMMENT '拼音',
  `audio_id` bigint DEFAULT NULL COMMENT '音频资源ID',
  `traditional` varchar(10) DEFAULT NULL COMMENT '繁体字',
  `radical` varchar(10) DEFAULT NULL COMMENT '部首',
  `stroke` varchar(4096) DEFAULT NULL COMMENT '笔顺',
  `char_desc` varchar(1024) DEFAULT NULL COMMENT '部件组合中文说明',
  `desc_translations` text DEFAULT NULL COMMENT '汉字说明的多语种翻译,JSON列表格式(List<TextTranslation>)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` TINYINT(4) NOT null DEFAULT '1' COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`),
  KEY `idx_character` (`character`),
  KEY `idx_pinyin` (`pinyin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='汉字主表';


-- 汉字辨析表
-- 注：一个汉字可能有多个辨析字。
CREATE TABLE `char_discrimination` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '辨析记录ID',
  `char_id` int(11) NOT NULL COMMENT '所属汉字ID',
  `discrim_char` varchar(10) NOT NULL COMMENT '辨析汉字',
  `discrim_pinyin` varchar(100) DEFAULT NULL COMMENT '辨析字拼音',
  `discrim_char_translations` text DEFAULT NULL COMMENT '辨析字说明的多语种翻译,JSON列表格式(List<TextTranslation>)',
  `comparison_translations` text DEFAULT NULL COMMENT '对比辨析说明的多语种翻译,JSON列表格式(List<TextTranslation>)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` TINYINT(4) NOT null DEFAULT '1' COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`),
  KEY `idx_char_id` (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='汉字辨析表';


-- 汉字组词表
-- 注：一个汉字可能有多个组词。
CREATE TABLE `char_word` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '组词记录ID',
  `char_id` int(11) NOT NULL COMMENT '所属汉字ID',
  `word_item` varchar(50) NOT NULL COMMENT '组词',
  `level` varchar(20) DEFAULT NULL COMMENT '组词HSK等级',
  `pinyin` varchar(100) DEFAULT NULL COMMENT '组词拼音',
  `part_of_speech` varchar(50) DEFAULT NULL COMMENT '词性',
  `word_item_translations` text DEFAULT NULL COMMENT '组词说明的多语种翻译,JSON列表格式(List<TextTranslation>)',
  `example_sentence` text DEFAULT NULL COMMENT '中文例句',
  `example_pinyin` varchar(500) DEFAULT NULL COMMENT '例句拼音',
  `example_translations` text DEFAULT NULL COMMENT '例句的多语种翻译,JSON列表格式(List<TextTranslation>)',
  `example_image` varchar(255) DEFAULT NULL COMMENT '例句图片路径',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` TINYINT(4) NOT null DEFAULT '1' COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`),
  KEY `idx_char_id` (`char_id`),
  KEY `idx_word_item` (`word_item`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='汉字组词表';

