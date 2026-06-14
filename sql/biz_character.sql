################################################################## 汉字相关 #####################################################################

-- 汉字表
-- 注：一个汉字对应一条数据。
CREATE TABLE `char_character`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '汉字ID',
  `character` varchar(16) NOT NULL COMMENT '汉字',
  `hsk_level` varchar(20) NULL DEFAULT NULL COMMENT 'HSK等级',
  `pinyin` varchar(32) NULL DEFAULT NULL COMMENT '拼音',
  `traditional` varchar(16) NULL DEFAULT NULL COMMENT '繁体字',
  `audio_id` bigint NULL DEFAULT NULL COMMENT '音频资源ID',
  `radical` varchar(16) NULL DEFAULT NULL COMMENT '部首',
  `radical_id` bigint NULL DEFAULT NULL COMMENT '部首ID',
  `component_combination` varchar(64) NULL DEFAULT NULL COMMENT '部件组合',
  `char_desc` varchar(1024) NULL DEFAULT NULL COMMENT '汉字中文说明',
  `char_desc_translations` text NULL COMMENT '汉字说明外文翻译',

  `draft_content` text NULL COMMENT '草稿内容JSON',
  `create_by` varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(255) NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `edit_status` varchar(20) NULL DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
  `publish_status` varchar(20) NULL DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_character`(`character`),
  INDEX `idx_pinyin`(`pinyin`),
  INDEX `idx_char_publish_status`(`publish_status`),
  INDEX `idx_char_edit_status`(`edit_status`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '汉字主表';

-- 汉字辨析表
-- 注：一个汉字可能有多个辨析字。
CREATE TABLE `char_comparison`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '辨析记录ID',
  `char_id` bigint NOT NULL COMMENT '所属汉字ID',
  `comparison_char` varchar(10) NOT NULL COMMENT '辨析汉字',
  `comparison_pinyin` varchar(100) NULL DEFAULT NULL COMMENT '辨析字拼音',
  `comparison_char_translations` text NULL COMMENT '辨析字外文翻译',
  `comparison_desc_translations` text NULL COMMENT '对比辨析说明外文翻译',
  `order` int NULL DEFAULT 0 COMMENT '辨析排序(值大的排前面)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_char_id`(`char_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '汉字辨析表';

-- 汉字组词表
-- 注：一个汉字可能有多个组词。组成的例句存放在例句表。
CREATE TABLE `char_word`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '组词记录ID',
  `char_id` bigint NOT NULL COMMENT '所属汉字ID',
  `word_item` varchar(50) NOT NULL COMMENT '组词',
  `hsk_level` varchar(20) NULL DEFAULT NULL COMMENT 'HSK等级',
  `pinyin` varchar(100) NULL DEFAULT NULL COMMENT '组词拼音',
  `part_of_speech` varchar(50) NULL DEFAULT NULL COMMENT '组词词性',
  `word_item_translations` text NULL COMMENT '组词的外文翻译',
  `sentence_id` bigint NULL DEFAULT NULL COMMENT '组词例句ID（对应 example_sentence.id）',
  `order` int NULL DEFAULT 0 COMMENT '组词排序(值大的排前面)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_char_id`(`char_id` ASC) USING BTREE,
  INDEX `idx_word_item`(`word_item` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '汉字组词表';

-- 汉字部首表
CREATE TABLE `char_radical` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '部首ID',
    `radical` VARCHAR(10) NOT NULL COMMENT '部首名称',
    `stroke_num` int(11) NOT NULL COMMENT '笔画数',
    `evolution_desc` VARCHAR(2048) DEFAULT NULL COMMENT '演化解说',
    `evolution_desc_translations` TEXT DEFAULT NULL COMMENT '演化解说外文翻译（JSON多语言）',
    `evolution_image_id` VARCHAR(255) DEFAULT NULL COMMENT '演化解说图片（路径或资源ID）',

    `draft_content` text NULL COMMENT '草稿内容JSON',
    `create_by` varchar(255) NULL DEFAULT NULL COMMENT '创建人',
    `update_by` varchar(255) NULL DEFAULT NULL COMMENT '更新人',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `edit_status` varchar(20) NULL DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
    `publish_status` varchar(20) NULL DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='汉字部首表';

-- 汉字笔顺表
CREATE TABLE `char_stroke` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '汉字笔顺ID',
    `character` VARCHAR(32) NOT NULL COMMENT '汉字',
    `stroke` TEXT DEFAULT NULL COMMENT '汉字笔顺JSON（hanzi-writer格式，含strokes/medians/radStrokes）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效，0-无效',
    PRIMARY KEY (`id`),
    INDEX `idx_character` (`character`),
    UNIQUE KEY `uk_character` (`character`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='汉字笔顺表';

-- 汉字书表
-- 注：一本书对应一条数据，汉字来源通过 hsk_level 或 word_ids 两种方式。
CREATE TABLE `char_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '汉字书ID',
  `type` varchar(32) NOT NULL COMMENT '汉字书类型, 参考枚举：CharBookTypeEnum',
  `name` varchar(32) NOT NULL COMMENT '汉字书名称',
  `sub_name` varchar(32) NOT NULL COMMENT '汉字书子名称',
  `cover_image` varchar(512) NOT NULL COMMENT '汉字书封面图',
  `desc` varchar(1024) DEFAULT NULL COMMENT '汉字书描述',
  `hsk_level` varchar(32) DEFAULT NULL COMMENT 'HSK等级(如果有等级则按照等级去检索汉字)',
  `word_ids` text DEFAULT NULL COMMENT '汉字ID列表(如果hsk_level为空则使用此字段)',
  `order` int DEFAULT 0 COMMENT '排序(值大的排前面)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '汉字书表';

-- 汉字部首表
CREATE TABLE `char_radical` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '部首ID',
    `radical` VARCHAR(10) NOT NULL COMMENT '部首名称',
    `stroke_num` int(11) DEFAULT NULL COMMENT '笔画数',
    `relation_id` bigint(20) DEFAULT NULL COMMENT '关联部首ID',
    `evolution_desc` VARCHAR(2048) DEFAULT NULL COMMENT '演化解说',
    `evolution_desc_translations` TEXT DEFAULT NULL COMMENT '演化解说外文翻译（JSON多语言）',
    `evolution_image_id` VARCHAR(255) DEFAULT NULL COMMENT '演化解说图片（路径或资源ID）',
    `draft_content` text NULL COMMENT '草稿内容JSON',
    `create_by` varchar(255) NULL DEFAULT NULL COMMENT '创建人',
    `update_by` varchar(255) NULL DEFAULT NULL COMMENT '更新人',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `edit_status` varchar(20) NULL DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
    `publish_status` varchar(20) NULL DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='汉字部首表';
