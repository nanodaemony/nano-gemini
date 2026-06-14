-- 例句文案表
-- 1:1 场景（char_word、vocab_sense、vocab_comparison_chat）由业务表的 FK 列引用；
-- 1:N 场景（vocab_structure）由 structure_id 列指向父表。
CREATE TABLE `example_sentence`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '例句ID',
  `structure_id` bigint DEFAULT NULL COMMENT '所属词汇搭配结构ID（1:N 场景，对应 vocab_structure.id）',
  `sentence` varchar(1024) NOT NULL COMMENT '例句中文文案',
  `pinyin` varchar(2048) NULL DEFAULT NULL COMMENT '例句拼音',
  `audio_id` bigint NULL DEFAULT NULL COMMENT '例句音频资源ID',
  `translations` text NULL COMMENT '例句外文翻译列表',
  `image_id` bigint NULL DEFAULT NULL COMMENT '例句图片(ID)',
  `order` int NOT NULL DEFAULT 0 COMMENT '例句排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_structure_id`(`structure_id`)
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
  `stem` varchar(32) DEFAULT NULL COMMENT '题干',
  `material` varchar(32) DEFAULT NULL COMMENT '材料',
  `options` varchar(32) DEFAULT NULL COMMENT '选项列表',
  `answer` varchar(32) DEFAULT NULL COMMENT '答案列表',
  `explanation` varchar(32) DEFAULT NULL COMMENT '解析',
  `audio_id` bigint DEFAULT NULL COMMENT '听力音频ID',
  `sort` int DEFAULT '0' COMMENT '排序号（值越大越靠前）',

  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type` (`type`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '练习题目表';

