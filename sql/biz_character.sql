################################################################## 汉字相关 #####################################################################

-- 汉字表
-- 注：一个汉字对应一条数据。
CREATE TABLE `char_character`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '汉字ID',
  `character` varchar(16) NOT NULL COMMENT '汉字',
  `level` varchar(20) NULL DEFAULT NULL COMMENT 'HSK等级',
  `pinyin` varchar(32) NULL DEFAULT NULL COMMENT '拼音',
  `traditional` varchar(16) NULL DEFAULT NULL COMMENT '繁体字',
  `audio_id` bigint NULL DEFAULT NULL COMMENT '音频资源ID',
  `radical` varchar(16) NULL DEFAULT NULL COMMENT '部首',
  `radical_id` bigint NULL DEFAULT NULL COMMENT '部首ID',
  `component_combination` varchar(64) NULL DEFAULT NULL COMMENT '部件组合',
  `char_desc` varchar(1024) NULL DEFAULT NULL COMMENT '汉字中文说明',
  `char_desc_translations` text NULL COMMENT '汉字说明外文翻译',
  `stroke` varchar(4096) NULL DEFAULT NULL COMMENT '笔顺',

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
CREATE TABLE `char_discrimination`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '辨析记录ID',
  `char_id` bigint NOT NULL COMMENT '所属汉字ID',
  `discrim_char` varchar(10) NOT NULL COMMENT '辨析汉字',
  `discrim_pinyin` varchar(100) NULL DEFAULT NULL COMMENT '辨析字拼音',
  `discrim_char_translations` text NULL COMMENT '辨析字外文翻译',
  `comparison_translations` text NULL COMMENT '对比辨析外文翻译',
  `order` int NULL DEFAULT 0 COMMENT '辨析排序(值大的排前面)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_char_id`(`char_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '汉字辨析表';

-- 汉字组词表
-- 注：一个汉字可能有多个组词。组成的例句存放在例句表。
CREATE TABLE `char_word`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '组词记录ID',
  `char_id` bigint NOT NULL COMMENT '所属汉字ID',
  `word_item` varchar(50) NOT NULL COMMENT '组词',
  `level` varchar(20) NULL DEFAULT NULL COMMENT 'HSK等级',
  `pinyin` varchar(100) NULL DEFAULT NULL COMMENT '组词拼音',
  `part_of_speech` varchar(50) NULL DEFAULT NULL COMMENT '组词词性',
  `word_item_translations` text NULL COMMENT '组词的外文翻译',

  `order` int NULL DEFAULT 0 COMMENT '组词排序(值大的排前面)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_char_id`(`char_id` ASC) USING BTREE,
  INDEX `idx_word_item`(`word_item` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '汉字组词表';

-- 汉字部首表
CREATE TABLE `char_radical` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '部首ID',
    `radical` VARCHAR(10) NOT NULL COMMENT '部首名称',
    `evolution_desc` VARCHAR(2048) DEFAULT NULL COMMENT '演化解说',
    `evolution_desc_translations` TEXT DEFAULT NULL COMMENT '演化解说外文翻译（JSON多语言）',
    `evolution_image_id` VARCHAR(255) DEFAULT NULL COMMENT '演化解说图片（路径或资源ID）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效，0-无效',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='汉字部首表';
