-- 汉字部首表
CREATE TABLE IF NOT EXISTS `char_radical` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '部首ID',
    `radical` VARCHAR(10) NOT NULL COMMENT '部首名称',
    `stroke_num` int(11) NOT NULL COMMENT '笔画数',
    `evolution_desc` VARCHAR(2048) DEFAULT NULL COMMENT '演化解说',
    `evolution_desc_translations` TEXT DEFAULT NULL COMMENT '演化解说外文翻译（JSON多语言）',
    `evolution_image_id` VARCHAR(255) DEFAULT NULL COMMENT '演化解说图片（路径或资源ID）',
    `draft_content` text NULL COMMENT '草稿内容JSON',
    `create_by` varchar(255) NULL DEFAULT NULL COMMENT '创建人',
    `update_by` varchar(255) NULL DEFAULT NULL COMMENT '更新人',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `edit_status` varchar(20) NULL DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
    `publish_status` varchar(20) NULL DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='汉字部首表';
