-- 语法辨析组表
-- 一行对应一个对比组，如"会 vs 能"或"把 vs 被 vs 让"
CREATE TABLE `grammar_comparison_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '语法辨析组ID',
    `group_key` VARCHAR(64) NOT NULL COMMENT '辨析组标识（如"会vs能"），查询时直接返回此字段作为对比头',
    `exercise_question_ids` VARCHAR(256) DEFAULT NULL COMMENT '练习题ID列表JSON',
    `order` INT NOT NULL DEFAULT 0 COMMENT '组排序权重（大的在前）',

    `draft_content` TEXT NULL COMMENT '草稿内容JSON',
    `edit_status` VARCHAR(20) NULL DEFAULT 'draft' COMMENT '编辑状态：draft-草稿 reviewing-审核中',
    `publish_status` VARCHAR(20) NULL DEFAULT 'unpublished' COMMENT '发布状态：unpublished-未发布 published-已发布',
    `create_by` VARCHAR(255) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(255) DEFAULT NULL COMMENT '更新人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效，0-无效',
    PRIMARY KEY (`id`),
    INDEX `idx_publish_status` (`publish_status`),
    INDEX `idx_edit_status` (`edit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='语法辨析组表';

-- 语法辨析条目表
-- 一条数据对应一个语法点在某个辨析组中的对比内容
-- 例如"会vs能"组中，会有一条"会"的条目和一条"能"的条目
CREATE TABLE `grammar_comparison_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '语法辨析条目ID',
    `group_id` BIGINT NOT NULL COMMENT '所属辨析组ID',
    `grammar_id` BIGINT NOT NULL COMMENT '语法点ID',
    `grammar_name` VARCHAR(50) NOT NULL COMMENT '语法点名称（冗余，方便查询和显示）',
    `usage_comparison` VARCHAR(2048) DEFAULT NULL COMMENT '用法对比：该语法点与其他语法点的差异说明',
    `usage_comparison_translations` TEXT DEFAULT NULL COMMENT '用法对比外文翻译（JSON数组）',
    `example_sentences` TEXT DEFAULT NULL COMMENT '例句（每行一条，含正误标记如✓✗）',
    `usage_sentence_id` bigint NULL DEFAULT NULL COMMENT '用法例句ID',
    `order` INT NOT NULL DEFAULT 0 COMMENT '组内排序权重（大的在前）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效，0-无效',
    PRIMARY KEY (`id`),
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_grammar_id` (`grammar_id`),
    INDEX `idx_grammar_name` (`grammar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='语法辨析条目表';


-- 语法辨析情景对话表
-- 例句存储在通用表example_sentence中
CREATE TABLE `grammar_comparison_chat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '对话ID',
  `group_id` bigint NOT NULL COMMENT '所属语法辨析组ID',
  `role` varchar(20) NOT NULL COMMENT '角色: teacher=老师, student=学生',
  `content` varchar(1024) NOT NULL COMMENT '中文对话内容',
  `example_sentence_id` bigint(20) DEFAULT NULL COMMENT '对话例句内容(文案、翻译、音频等，对应表example_sentence，由发布流程回填)',
  `order` int NOT NULL DEFAULT '0' COMMENT '排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '有效状态: 1=有效 0=无效',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_group_id` (`group_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='语法辨析情景对话表';

