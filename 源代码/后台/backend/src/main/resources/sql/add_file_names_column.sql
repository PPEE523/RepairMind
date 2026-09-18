-- 添加原始文件名字段，用于在多模态检索历史中显示用户上传时的原始文件名
ALTER TABLE search_history ADD COLUMN IF NOT EXISTS file_names VARCHAR(1000);