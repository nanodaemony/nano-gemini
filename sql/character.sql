################################################################## 汉字相关 #####################################################################

-- 汉字表
-- 注：一个汉字对应一条数据。
CREATE TABLE `char_character`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '汉字唯一ID',
  `sequence_no` int NULL DEFAULT NULL COMMENT 'Excel中的序号',
  `character` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '汉字',
  `level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '等级（如：3）',
  `pinyin` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '拼音',
  `audio_id` bigint NULL DEFAULT NULL COMMENT '音频资源ID',
  `traditional` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '繁体字',
  `radical` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部首',
  `stroke` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '笔顺',
  `char_desc` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部件组合中文说明',
  `desc_translations` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '汉字说明的多语种翻译',
  `publish_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
  `edit_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
  `draft_content` json NULL COMMENT '草稿内容JSON',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_character`(`character` ASC) USING BTREE,
  INDEX `idx_pinyin`(`pinyin` ASC) USING BTREE,
  INDEX `idx_char_publish_status`(`publish_status` ASC) USING BTREE,
  INDEX `idx_char_edit_status`(`edit_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '汉字主表' ROW_FORMAT = COMPACT;


-- 汉字辨析表
-- 注：一个汉字可能有多个辨析字。
CREATE TABLE `char_discrimination`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '辨析记录ID',
  `char_id` int NOT NULL COMMENT '所属汉字ID',
  `discrim_char` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '辨析汉字',
  `discrim_pinyin` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '辨析字拼音',
  `discrim_char_translations` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '辨析字说明的多语种翻译',
  `comparison_translations` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '对比辨析说明的多语种翻译',
  `discrimination_order` int NULL DEFAULT 0 COMMENT '辨析排序(值大的排前面)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_char_id`(`char_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '汉字辨析表' ROW_FORMAT = COMPACT;

-- 汉字组词表
-- 注：一个汉字可能有多个组词。
CREATE TABLE `char_word`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '组词记录ID',
  `char_id` int NOT NULL COMMENT '所属汉字ID',
  `word_item` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组词',
  `level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组词HSK等级',
  `pinyin` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组词拼音',
  `part_of_speech` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '词性',
  `word_item_translations` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '组词说明的多语种翻译',
  `example_sentence` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '中文例句',
  `example_pinyin` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '例句拼音',
  `example_translations` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '例句的多语种翻译',
  `example_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '例句图片路径',
  `word_order` int NULL DEFAULT 0 COMMENT '组词排序(值大的排前面)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_char_id`(`char_id` ASC) USING BTREE,
  INDEX `idx_word_item`(`word_item` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '汉字组词表' ROW_FORMAT = COMPACT;

