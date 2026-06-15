-- 语法点主表
-- 一行数据对应一个语法点
CREATE TABLE `grammar_point` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '语法点ID',
    `name` VARCHAR(50) NOT NULL COMMENT '语法点名称',
    `hsk_level` VARCHAR(20) DEFAULT NULL COMMENT 'HSK等级',
    `project` VARCHAR(20) DEFAULT NULL COMMENT '项目',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '类别',
    `sub_category` VARCHAR(50) DEFAULT NULL COMMENT '细目',

    `draft_content` text COMMENT '草稿内容（JSON结构）',
    `edit_status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '编辑状态：draft-草稿 reviewing-审核中',
    `publish_status` VARCHAR(20) NOT NULL DEFAULT 'unpublished' COMMENT '发布状态：unpublished-未发布 published-已发布',
    `create_by` VARCHAR(255) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(255) DEFAULT NULL COMMENT '更新人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效，0-无效',
    PRIMARY KEY (`id`),
    INDEX `idx_name` (`name`),
    INDEX `idx_category` (`category`),
    INDEX `idx_publish_status` (`publish_status`),
    INDEX `idx_edit_status` (`edit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='语法点主表';

-- 语法意义表
-- 一条数据对应一个语法意义，一个语法点可能有多个语法意义
-- 语法意义的例句存在例句表
CREATE TABLE `grammar_meaning` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '语法意义ID',
    `grammar_id` BIGINT NOT NULL COMMENT '语法点ID',
    `meaning_content` VARCHAR(2048) NOT NULL COMMENT '语法意义内容（如：表示在上面）',
    `meaning_content_translations` text COMMENT '语法意义外文翻译',
    `image_id` BIGINT DEFAULT NULL COMMENT '语法意义图片ID',
    `meaning_sentence_ids` VARCHAR(128) DEFAULT NULL COMMENT '语法意义例句ID列表, JSON格式',
    `order` INT NOT NULL DEFAULT 0 COMMENT '排序权重(值大的排前面)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效，0-无效',
    PRIMARY KEY (`id`),
    INDEX `idx_grammar_id` (`grammar_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='语法意义表';

-- 语法结构表
-- 一条数据对应一个语法结构 一个语法点可能有多个语法结构
-- 语法结构的例句存在例句表
CREATE TABLE `grammar_structure` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '语法结构ID',
    `grammar_id` BIGINT NOT NULL COMMENT '语法点ID',
    `structure_content` VARCHAR(1024) NOT NULL COMMENT '结构文本（如：S + 把 + O + V + 结果）',
    `structure_sentence_ids` VARCHAR(128) DEFAULT NULL COMMENT '语法结构例句ID列表, JSON格式',
    `order` INT NOT NULL DEFAULT 0 COMMENT '排序权重(值大的排前面)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效，0-无效',
    PRIMARY KEY (`id`),
    INDEX `idx_grammar_id` (`grammar_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='语法结构表';

-- 语法点注意表
-- 一条数据对应一个语法注意 一个语法点可能有多个语法注意
-- 语法注意的例句存在例句表
CREATE TABLE `grammar_notice` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '语法注意ID',
    `grammar_id` BIGINT NOT NULL COMMENT '语法点ID',
    `notice_content` VARCHAR(1024) NOT NULL COMMENT '注意内容',
    `notice_content_translations` TEXT DEFAULT NULL COMMENT '注意内容的外文翻译',
    `notice_sentence_ids` VARCHAR(128) DEFAULT NULL COMMENT '语法注意例句ID列表, JSON格式',
    `order` INT NOT NULL DEFAULT 0 COMMENT '排序权重(值大的排前面)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效，0-无效',
    PRIMARY KEY (`id`),
    INDEX `idx_grammar_id` (`grammar_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='语法点注意表';

-- 语法点偏误表
CREATE TABLE `grammar_error` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '语法偏误ID',
    `grammar_id` BIGINT NOT NULL COMMENT '语法点ID',
    `error_content` varchar(1024) NOT NULL COMMENT '偏误描述',
    `error_analysis` varchar(1024) DEFAULT NULL COMMENT '偏误分析',
    `error_analysis_translations` TEXT DEFAULT NULL COMMENT '偏误分析外文翻译',
    `order` INT NOT NULL DEFAULT 0 COMMENT '排序权重(值大的排前面)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效，0-无效',
    PRIMARY KEY (`id`),
    INDEX `idx_grammar_id` (`grammar_id`),
    INDEX `idx_status_order` (`status`, `order`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='语法偏误表';


