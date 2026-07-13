-- 话题主表
CREATE TABLE `topic` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '话题ID',
  `name` varchar(128) NOT NULL COMMENT '话题名称（如"希望"）',
  `pinyin` varchar(256) DEFAULT NULL COMMENT '拼音',
  `audio_id` bigint DEFAULT NULL COMMENT '音频资源ID',
  `cover_image_id` bigint DEFAULT NULL COMMENT '封面图片资源ID',
  `translations` text COMMENT '话题多语言翻译（JSON，List<TextTranslation>）',

  `draft_content` text COMMENT '草稿内容JSON',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `edit_status` varchar(20) DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
  `publish_status` varchar(20) DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_topic_name`(`name`),
  INDEX `idx_topic_publish_status`(`publish_status`),
  INDEX `idx_topic_edit_status`(`edit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='话题主表';

-- 话题句式表
CREATE TABLE `topic_pattern` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '句式ID',
  `topic_id` bigint NOT NULL COMMENT '所属话题ID',
  `pattern` varchar(512) NOT NULL COMMENT '句式文本（如"（某人）+希望……"）',
  `image_id` bigint DEFAULT NULL COMMENT '句式示意图资源ID',
  `order` int NOT NULL DEFAULT 0 COMMENT '组内排序权重（大的在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pattern_topic_id`(`topic_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='话题句式表';

-- 话题情景对话表
CREATE TABLE `topic_chat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '对话ID',
  `topic_id` bigint NOT NULL COMMENT '所属话题ID（冗余，方便查询）',
  `pattern_id` bigint NOT NULL COMMENT '所属句式ID',
  `role` varchar(20) NOT NULL COMMENT '角色: teacher=老师, student=学生',
  `content` varchar(1024) NOT NULL COMMENT '中文对话内容',
  `example_sentence_id` bigint DEFAULT NULL COMMENT '对话例句ID（对应example_sentence表，发布时回填）',
  `order` int NOT NULL DEFAULT 0 COMMENT '组内排序权重（大的在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_chat_topic_id`(`topic_id`) USING BTREE,
  INDEX `idx_chat_pattern_id`(`pattern_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='话题情景对话表';
