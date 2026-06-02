-- 大模型对话相关表

-- 预设提示词表
CREATE TABLE IF NOT EXISTS `chat_prompt` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(100) NOT NULL COMMENT '提示词名称',
  `description` varchar(500) DEFAULT NULL COMMENT '描述说明',
  `system_prompt` text NOT NULL COMMENT '系统提示词内容',
  `model` varchar(100) DEFAULT NULL COMMENT '推荐使用的模型',
  `temperature` decimal(3,2) DEFAULT NULL COMMENT '推荐温度参数',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1-有效，0-无效',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='预设提示词表';

-- 对话记录表
CREATE TABLE IF NOT EXISTS `chat_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `provider` varchar(50) NOT NULL COMMENT '厂商：ALIYUN/DEEPSEEK',
  `model` varchar(100) NOT NULL COMMENT '使用的模型',
  `prompt_name` varchar(100) DEFAULT NULL COMMENT '使用的预设提示词名称',
  `system_prompt` text DEFAULT NULL COMMENT '实际使用的系统提示词',
  `user_prompt` text NOT NULL COMMENT '用户输入提示词',
  `assistant_response` text NOT NULL COMMENT '模型原始响应',
  `temperature` decimal(3,2) DEFAULT NULL COMMENT '温度参数',
  `max_tokens` int(11) DEFAULT NULL COMMENT '最大 token 数',
  `top_p` decimal(3,2) DEFAULT NULL COMMENT 'top_p 参数',
  `request_id` varchar(100) DEFAULT NULL COMMENT '厂商请求 ID',
  `input_tokens` int(11) DEFAULT NULL COMMENT '输入 token 数',
  `output_tokens` int(11) DEFAULT NULL COMMENT '输出 token 数',
  `total_tokens` int(11) DEFAULT NULL COMMENT '总 token 数',
  `latency_ms` int(11) DEFAULT NULL COMMENT '请求耗时（毫秒）',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户 ID',
  `extra_params` json DEFAULT NULL COMMENT '其他额外参数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='对话记录表';

-- 插入一个示例预设提示词
INSERT INTO `chat_prompt` (`name`, `description`, `system_prompt`, `model`, `temperature`, `status`)
VALUES ('common_assistant', '通用助手', '你是一个乐于助人的AI助手，请用友好、专业的语气回答用户的问题。', 'qwen-plus', 0.7, 1);
