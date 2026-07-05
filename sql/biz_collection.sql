-- ----------------------------
-- 用户收藏夹功能
-- ----------------------------
DROP TABLE IF EXISTS `biz_collection_item`;
DROP TABLE IF EXISTS `biz_collection_folder`;

CREATE TABLE `biz_collection_folder` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏夹ID',
    `user_id`         BIGINT NOT NULL COMMENT '用户ID',
    `name`            VARCHAR(32) NOT NULL COMMENT '收藏夹名称',
    `cover_image_id`  BIGINT DEFAULT NULL COMMENT '封面图资源ID',
    `is_default`      TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认收藏夹：0-否 1-是',
    `is_pinned`       TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否 1-是',
    `sort_order`      INT NOT NULL DEFAULT 0 COMMENT '排序权重（大在前）',
    `status`          TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效 0-已删除',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_default` (`user_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏夹表';

CREATE TABLE `biz_collection_item` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏记录ID',
    `folder_id`       BIGINT NOT NULL COMMENT '所属收藏夹ID',
    `user_id`         BIGINT NOT NULL COMMENT '用户ID（冗余，方便查询）',
    `biz_type`        VARCHAR(30) NOT NULL COMMENT '业务类型枚举：CHARACTER/VOCABULARY/RADICAL/GRAMMAR/GRAMMAR_COMPARISON/VOCAB_COMPARISON',
    `content_id`      BIGINT DEFAULT NULL COMMENT '收藏的内容ID（如词汇ID、汉字ID等）',
    `content_text`    VARCHAR(1024) DEFAULT NULL COMMENT '收藏内容文本（用于无结构化ID的内容，如好词好句）',
    `status`          TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效 0-已取消收藏',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_folder_id` (`folder_id`),
    KEY `idx_user_biz_content` (`user_id`, `biz_type`, `content_id`),
    KEY `idx_folder_status` (`folder_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏内容表';
