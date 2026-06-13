-- 词汇辨析组表
CREATE TABLE `vocab_comparison_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '词汇辨析组ID',
  `group_key` varchar(64) NOT NULL COMMENT '辨析组标识（如"标准vs高度vs目标"），查询时直接返回此字段作为对比头',
  `exercise_question_ids` varchar(256) DEFAULT NULL COMMENT '练习题ID列表JSON',
  `group_order` int NOT NULL DEFAULT '0' COMMENT '组排序权重（大的在前）',

  `draft_content` text NULL COMMENT '草稿内容JSON',
  `create_by` varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(255) NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `edit_status` varchar(20) NULL DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
  `publish_status` varchar(20) NULL DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='词汇辨析组表';

-- 词汇辨析条目表
CREATE TABLE `vocab_comparison_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '词汇辨析条目ID',
  `group_id` bigint NOT NULL COMMENT '所属辨析组ID',
  `word_id` bigint NOT NULL COMMENT '词汇ID',
  `word` varchar(50) NOT NULL COMMENT '词汇词头（冗余字段，方便查询和显示）',
  `part_of_speech` varchar(50) DEFAULT NULL COMMENT '词性（如名词、动词、形容词等），同一词在同一组中可能有多个词性',
  `usage_comparison` varchar(512) DEFAULT NULL COMMENT '用法对比',
  `usage_comparison_translations` text COMMENT '用法对比外文翻译',
  `common_usage` varchar(512) DEFAULT NULL COMMENT '通用用法',
  `common_usage_translations` text COMMENT '通用用法外文翻译',

  `order` int NOT NULL DEFAULT '0' COMMENT '组内排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '有效状态: 1=有效 0=无效',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_group_id` (`group_id`) USING BTREE,
  KEY `idx_word_id` (`word_id`) USING BTREE,
  KEY `idx_word` (`word`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='词汇辨析条目表';

-- 词汇辨析情景对话表
-- 例句存储在通用表example_sentence中
CREATE TABLE `vocab_comparison_chat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '对话ID',
  `group_id` bigint NOT NULL COMMENT '所属词汇辨析组ID',
  `role` varchar(20) NOT NULL COMMENT '角色: teacher=老师, student=学生',
  `content` varchar(1024) NOT NULL COMMENT '中文对话内容',
  `example_sentence_id` bigint(20) DEFAULT NULL COMMENT '对话例句内容(文案、翻译、音频等，对应表example_sentence)',
  `order` int NOT NULL DEFAULT '0' COMMENT '组内排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '有效状态: 1=有效 0=无效',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_group_id` (`group_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='词汇辨析情景对话表';


