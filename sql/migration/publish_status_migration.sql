-- 为 vocab_word 新增字段
ALTER TABLE vocab_word
ADD COLUMN publish_status VARCHAR(20) DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
ADD COLUMN edit_status VARCHAR(20) DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
ADD COLUMN draft_content JSON COMMENT '草稿内容JSON',
ADD COLUMN create_by VARCHAR(255) DEFAULT NULL COMMENT '创建人',
ADD COLUMN update_by VARCHAR(255) DEFAULT NULL COMMENT '更新人';

-- 为 char_character 新增字段
ALTER TABLE char_character
ADD COLUMN publish_status VARCHAR(20) DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
ADD COLUMN edit_status VARCHAR(20) DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
ADD COLUMN draft_content JSON COMMENT '草稿内容JSON',
ADD COLUMN create_by VARCHAR(255) DEFAULT NULL COMMENT '创建人',
ADD COLUMN update_by VARCHAR(255) DEFAULT NULL COMMENT '更新人';

-- 初始化已有数据为已发布状态
UPDATE vocab_word SET publish_status = 'published' WHERE status = 1;
UPDATE char_character SET publish_status = 'published' WHERE status = 1;

-- 创建索引
CREATE INDEX idx_vocab_publish_status ON vocab_word(publish_status);
CREATE INDEX idx_vocab_edit_status ON vocab_word(edit_status);
CREATE INDEX idx_char_publish_status ON char_character(publish_status);
CREATE INDEX idx_char_edit_status ON char_character(edit_status);
