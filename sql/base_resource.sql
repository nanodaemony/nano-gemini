
################################################################## 基础资源表 #####################################################################


-- ----------------------------
-- OSS资源元数据表
-- ----------------------------
DROP TABLE IF EXISTS `oss_resource_meta`;
CREATE TABLE `oss_resource_meta` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `file_name` varchar(255) NOT NULL COMMENT '文件名称',
  `file_real_name` varchar(255) NOT NULL COMMENT '真实存储的名称',
  `file_size` varchar(100) NOT NULL COMMENT '文件大小',
  `file_mime_type` varchar(50) NOT NULL COMMENT '文件MIME 类型',
  `file_type` varchar(50) NOT NULL COMMENT '文件类型',
  `file_url` tinytext NOT NULL COMMENT '文件访问地址',
  `bucket_name` varchar(255) NOT NULL COMMENT 'Bucket 名称',
  `business_type` varchar(100) DEFAULT NULL COMMENT '业务类型',
  `custom_path` varchar(255) DEFAULT NULL COMMENT '自定义路径',
  `create_by` varchar(255) NOT NULL COMMENT '创建者',
  `update_by` varchar(255) NOT NULL COMMENT '更新者',
  `create_time` datetime NOT NULL COMMENT '创建日期',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='OSS资源元数据';

-- ----------------------------
-- TTS语音合成记录表
-- ----------------------------
DROP TABLE IF EXISTS `tts_record`;
CREATE TABLE `tts_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `voice` varchar(255) NOT NULL COMMENT '音色',
    `text` text NOT NULL COMMENT '合成文本',
    `instructions` text DEFAULT NULL COMMENT '指令',
    `model` varchar(255) NOT NULL COMMENT '模型',
    `vendor` varchar(50) DEFAULT 'aliyun' COMMENT '厂商: aliyun/volcengine',
    `language_type` varchar(255) DEFAULT NULL COMMENT '语言类型',
    `final_audio_url` varchar(255) NOT NULL COMMENT '最终音频地址',
    `request_id` varchar(255) NOT NULL COMMENT '请求ID',
    `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
    `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
    `create_time` datetime DEFAULT NULL COMMENT '创建日期',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='TTS语音合成记录';

-- ----------------------------
-- 文生图生成记录表（千问 Qwen-Image）
-- ----------------------------
DROP TABLE IF EXISTS `image_record`;
CREATE TABLE `image_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `prompt` text NOT NULL COMMENT '正向提示词',
    `negative_prompt` text DEFAULT NULL COMMENT '反向提示词',
    `model` varchar(255) NOT NULL COMMENT '模型名称',
    `size` varchar(50) DEFAULT NULL COMMENT '分辨率',
    `image_count` int(11) DEFAULT 1 COMMENT '生成图像数量',
    `prompt_extend` bit(1) DEFAULT b'1' COMMENT '是否开启 Prompt 智能改写',
    `watermark` bit(1) DEFAULT b'0' COMMENT '是否添加水印',
    `seed` bigint(20) DEFAULT NULL COMMENT '随机数种子',
    `final_image_url` varchar(500) NOT NULL COMMENT '最终图片地址',
    `request_id` varchar(255) NOT NULL COMMENT '请求ID',
    `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
    `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
    `create_time` datetime DEFAULT NULL COMMENT '创建日期',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文生图生成记录';

-- ----------------------------
-- 系统日志表
-- ----------------------------
CREATE TABLE `sys_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `log_type` varchar(10) NOT NULL COMMENT '日志类型：INFI/ERROR',
  `method` varchar(255) DEFAULT NULL COMMENT '方法名',
  `params` text DEFAULT NULL COMMENT '参数',
  `request_ip` varchar(255) DEFAULT NULL COMMENT '请求IP',
  `time` bigint(20) DEFAULT NULL COMMENT '执行时间',
  `username` varchar(255) DEFAULT NULL COMMENT '用户名',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `browser` varchar(255) DEFAULT NULL COMMENT '浏览器',
  `exception_detail` text DEFAULT NULL COMMENT '异常',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`log_id`) USING BTREE,
  KEY `idx_create_time_index` (`create_time`),
  KEY `idx_log_type` (`log_type`)
) ENGINE=InnoDB AUTO_INCREMENT=3636 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='系统日志';


-- ----------------------------
-- 音频资源表
-- 存储音频相关的资源。
-- ----------------------------
DROP TABLE IF EXISTS `audio_resource`;
CREATE TABLE `audio_resource` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `text_content` varchar(4096) NOT NULL COMMENT '音频对应的文字内容',
    `source_type` varchar(50) NOT NULL COMMENT '来源类型: tts/upload',
    `file_url` varchar(500) NOT NULL COMMENT '音频文件地址',
    `file_format` varchar(20) DEFAULT 'mp3' COMMENT '文件格式: mp3/wav/m4a',
    `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小(字节)',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT(4) NOT NULL DEFAULT '1' COMMENT '有效状态, 1:有效 0:无效',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='音频资源表';


-- 翻译记录表
CREATE TABLE IF NOT EXISTS `translate_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `source_text` text COMMENT '源文本（中文）',
  `target_text` text COMMENT '目标文本（译文）',
  `source_language` VARCHAR(50) DEFAULT NULL COMMENT '源语言代码'
  `target_language` varchar(50) DEFAULT NULL COMMENT '目标语言代码',
  `model` varchar(100) DEFAULT NULL COMMENT '使用的模型',
  `request_id` varchar(100) DEFAULT NULL COMMENT '阿里云请求 ID',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='翻译记录表';

