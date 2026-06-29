-- 例句文案表
-- 业务表通过 FK 列或 sentence_ids JSON 引用本表
CREATE TABLE `example_sentence`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '例句ID',
  `sentence` varchar(1024) NOT NULL COMMENT '例句中文文案',
  `pinyin` varchar(2048) NULL DEFAULT NULL COMMENT '例句拼音',
  `audio_id` bigint NULL DEFAULT NULL COMMENT '例句音频资源ID',
  `translations` text NULL COMMENT '例句外文翻译列表',
  `image_id` bigint NULL DEFAULT NULL COMMENT '例句图片(ID)',
  `order` int NOT NULL DEFAULT 0 COMMENT '例句排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='例句文案表';


-- 练习题目表
-- 注：
-- 1. 一个大题（如：用“A比B+Adj……”改写句子）是一条记录（parent_id = 0）
-- 2. 每个小题是一条记录（parent_id 指向大题）
-- 3. 题型不同时，options / answer 结构不同，统一使用 JSON存储
CREATE TABLE `exercise_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '题目ID',

  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父题ID（大题的父题ID=0）',
  `question_type` varchar(32) NOT NULL COMMENT '题目类型, 参考枚举：QuestionTypeEnum',
  `stem` varchar(512) DEFAULT NULL COMMENT '题干',
  `content` varchar(4096) DEFAULT NULL COMMENT '题目内容材料(题目内容信息, 可能是文本也可能是图片链接), JSON参考：QuestionContent',
  `options` varchar(2048) DEFAULT NULL COMMENT '选项列表, JSON列表, List<QuestionOption>',
  `answer` varchar(512) DEFAULT NULL COMMENT '答案列表, JSON列表, List<String>',
  `explanation` varchar(1024) DEFAULT NULL COMMENT '解析',
  `audio_id` bigint DEFAULT NULL COMMENT '听力音频ID',
  `sort` int DEFAULT '0' COMMENT '排序号（值越大越靠前）',

  `draft_content` text COMMENT '草稿内容（JSON结构）',
    `edit_status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '编辑状态：draft-草稿 reviewing-审核中',
    `publish_status` VARCHAR(20) NOT NULL DEFAULT 'unpublished' COMMENT '发布状态：unpublished-未发布 published-已发布',
    `create_by` VARCHAR(255) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(255) DEFAULT NULL COMMENT '更新人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效，0-无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_question_type` (`question_type`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '练习题目表';

