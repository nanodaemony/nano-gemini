-- 翻译记录表
CREATE TABLE IF NOT EXISTS `translate_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `source_text` text COMMENT '源文本（中文）',
  `target_text` text COMMENT '目标文本（译文）',
  `target_language` varchar(50) DEFAULT NULL COMMENT '目标语言代码',
  `model` varchar(100) DEFAULT NULL COMMENT '使用的模型',
  `request_id` varchar(100) DEFAULT NULL COMMENT '阿里云请求 ID',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='翻译记录表';
