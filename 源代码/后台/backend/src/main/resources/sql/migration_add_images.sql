-- 迁移脚本：为 repair_knowledge 表添加 images 字段（存储多张图片URL的JSON数组）
ALTER TABLE repair_knowledge ADD COLUMN IF NOT EXISTS images TEXT;

COMMENT ON COLUMN repair_knowledge.images IS '案例图片URL列表（JSON数组格式，最多5张）';
