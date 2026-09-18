-- 为 search_history 表添加 file_types 列，用于存储文件类型的JSON数组
ALTER TABLE search_history ADD COLUMN IF NOT EXISTS file_types VARCHAR(500);
