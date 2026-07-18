-- ============================================================
-- 文化点相关表
-- 设计说明：
--   1. 文化点主表 culture — 存储文化点的核心信息、介绍文本
--   2. 文化关键词表 culture_keyword — 存储文化点的关键词（一个文化点可多个关键词）
--   3. 学一学的具体例句数据存入 example_sentence 表，culture.sentence_ids 只存 JSON ID 列表
--   4. 练一练的具体题目数据存入 exercise_question 表，culture.question_ids 只存 JSON ID 列表
--   5. 草稿/审核/发布工作流沿用现有三状态模型（edit_status + publish_status + status）
--   6. 音频/图片均通过 audio_resource / oss_resource_meta 资源表的 ID 引用
-- ============================================================

-- 文化点主表
-- 一行数据对应一个文化点
CREATE TABLE `culture` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文化点ID',
    `name` VARCHAR(128) NOT NULL COMMENT '文化点名称（如：饺子、北京的四合院、春节）',
    `pinyin` VARCHAR(256) DEFAULT NULL COMMENT '文化点拼音',
    `audio_id` BIGINT DEFAULT NULL COMMENT '文化点名称音频资源ID（关联 audio_resource.id）',
    `translations` TEXT COMMENT '文化点名称多语言翻译（JSON，List<TextTranslation>）',

    `cover_image_id` BIGINT DEFAULT NULL COMMENT '封面图片资源ID（关联 oss_resource_meta.id）',

    `level` VARCHAR(20) DEFAULT NULL COMMENT '等级：初等/中等/高等',
    `project` VARCHAR(50) DEFAULT NULL COMMENT '一级项目（如：社会交往）',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '二级项目（如：饮食、居住、节日）',

    `one_sentence_intro` VARCHAR(1024) DEFAULT NULL COMMENT '一句话介绍',
    `one_sentence_intro_translations` TEXT COMMENT '一句话介绍多语言翻译（JSON，List<TextTranslation>）',
    `one_sentence_intro_audio_id` BIGINT DEFAULT NULL COMMENT '一句话介绍音频资源ID（关联 audio_resource.id）',
    `one_sentence_intro_image_id` BIGINT DEFAULT NULL COMMENT '一句话介绍图片资源ID（关联 oss_resource_meta.id）',

    `detailed_intro` TEXT COMMENT '详细介绍',
    `detailed_intro_translations` TEXT COMMENT '详细介绍多语言翻译（JSON，List<TextTranslation>）',
    `detailed_intro_audio_id` BIGINT DEFAULT NULL COMMENT '详细介绍音频资源ID（关联 audio_resource.id）',
    `detailed_intro_image_id` BIGINT DEFAULT NULL COMMENT '详细介绍图片资源ID（关联 oss_resource_meta.id）',

    `sentence_ids` TEXT COMMENT '学一学例句ID列表（JSON数组，如 [1,2,3]，对应 example_sentence.id）',
    `question_ids` TEXT COMMENT '练一练习题ID列表（JSON数组，如 [1,2,3]，对应 exercise_question.id）',

    `draft_content` TEXT COMMENT '草稿内容（JSON结构，发布时回写主表和子表字段）',
    `edit_status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '编辑状态：draft-草稿 reviewed-已审核',
    `publish_status` VARCHAR(20) NOT NULL DEFAULT 'unpublished' COMMENT '发布状态：unpublished-未发布 published-已发布',
    `create_by` VARCHAR(255) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(255) DEFAULT NULL COMMENT '更新人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效，0-无效',
    PRIMARY KEY (`id`),
    INDEX `idx_culture_name` (`name`),
    INDEX `idx_culture_level` (`level`),
    INDEX `idx_culture_project` (`project`),
    INDEX `idx_culture_category` (`category`),
    INDEX `idx_culture_publish_status` (`publish_status`),
    INDEX `idx_culture_edit_status` (`edit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文化点主表';


-- 文化关键词表
-- 一个文化点可以有多个关键词
-- 关键词按 order 字段排序展示
CREATE TABLE `culture_keyword` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关键词ID',
    `culture_id` BIGINT NOT NULL COMMENT '所属文化点ID（关联 culture.id）',
    `keyword` VARCHAR(128) NOT NULL COMMENT '关键词名称（如：团圆、辞旧迎新、好运和财富）',
    `keyword_description` TEXT COMMENT '关键词详细说明',
    `keyword_translations` TEXT COMMENT '关键词名称多语言翻译（JSON，List<TextTranslation>）',
    `keyword_description_translations` TEXT COMMENT '关键词说明多语言翻译（JSON，List<TextTranslation>）',
    `audio_id` BIGINT DEFAULT NULL COMMENT '关键词音频资源ID（关联 audio_resource.id）',
    `image_id` BIGINT DEFAULT NULL COMMENT '关键词图片资源ID（关联 oss_resource_meta.id）',
    `order` INT NOT NULL DEFAULT 0 COMMENT '排序权重（值大的排前面）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效，0-无效',
    PRIMARY KEY (`id`),
    INDEX `idx_keyword_culture_id` (`culture_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文化关键词表';
