-- 例句文案表
-- 注：可以保存多种不同业务的例句文案
CREATE TABLE `sentence_content`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '例句ID',
  `biz_type` varchar(64) NOT NULL COMMENT '例句业务类型, 参考枚举：SentenceBizTypeEnum',
  `biz_id` bigint(20) NOT NULL COMMENT '例句业务ID',
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
  INDEX `idx_bizType_bizId`(`biz_type`, `biz_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '例句文案表';