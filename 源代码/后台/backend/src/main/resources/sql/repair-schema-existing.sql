
-- 非破坏性补齐旧版初始化脚本遗漏的表和列；适用于已有 repair_db。
-- 此脚本可重复执行；不会重置用户或业务数据。
CREATE TABLE IF NOT EXISTS work_sop_flow (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(50) DEFAULT '其他',
    source_document_id VARCHAR(100),
    source_document_name VARCHAR(200),
    dataset_id VARCHAR(100),
    step_json TEXT NOT NULL,
    total_steps INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'active',
    create_user BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sop_flow_status ON work_sop_flow(status);
CREATE INDEX IF NOT EXISTS idx_sop_flow_create_user ON work_sop_flow(create_user);

CREATE TABLE IF NOT EXISTS chat_file (
    id BIGSERIAL PRIMARY KEY,
    chat_history_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(20) NOT NULL,
    dify_file_id VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chat_file_history ON chat_file(chat_history_id);

ALTER TABLE search_history ADD COLUMN IF NOT EXISTS file_types VARCHAR(500);
ALTER TABLE search_history ADD COLUMN IF NOT EXISTS file_names VARCHAR(1000);
ALTER TABLE search_history ADD COLUMN IF NOT EXISTS search_success BOOLEAN DEFAULT TRUE;
ALTER TABLE search_history ADD COLUMN IF NOT EXISTS error_message VARCHAR(500);
ALTER TABLE repair_knowledge ADD COLUMN IF NOT EXISTS images TEXT;

-- =============================================
-- AI智能问答功能建表脚本（PostgreSQL）
-- 对应需求：多模态检索功能重建
-- =============================================

-- 1. 对话历史表
CREATE TABLE IF NOT EXISTS chat_history (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(64),           -- Dify对话ID
    user_question TEXT NOT NULL,            -- 用户问题
    device_model VARCHAR(128),              -- 设备型号
    answer_content TEXT,                    -- AI回答内容
    message_id VARCHAR(64),                -- Dify消息ID（本轮）
    status VARCHAR(20) DEFAULT 'success',  -- 状态：success/fail
    round_num INTEGER DEFAULT 1,           -- 当前对话轮次
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 对话时间
    user_id BIGINT                         -- 用户ID（预留）
);

-- 对话历史表索引
CREATE INDEX IF NOT EXISTS idx_chat_history_conversation_id ON chat_history(conversation_id);
CREATE INDEX IF NOT EXISTS idx_chat_history_user_id ON chat_history(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_history_create_time ON chat_history(create_time);

-- 2. AI反馈表
CREATE TABLE IF NOT EXISTS chat_feedback (
    id BIGSERIAL PRIMARY KEY,
    chat_history_id BIGINT NOT NULL,        -- 关联chat_history.id
    feedback_status VARCHAR(20) NOT NULL DEFAULT 'pending', -- pending/correct/incorrect
    feedback_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 标注时间
    user_id BIGINT                          -- 用户ID（预留）
);

-- AI反馈表索引
CREATE INDEX IF NOT EXISTS idx_chat_feedback_history_id ON chat_feedback(chat_history_id);

-- 3. 文档资源表（存储对话命中的文档信息）
CREATE TABLE IF NOT EXISTS chat_document_resource (
    id BIGSERIAL PRIMARY KEY,
    chat_history_id BIGINT NOT NULL,        -- 关联chat_history.id
    dataset_id VARCHAR(64),                 -- 知识库ID
    dataset_name VARCHAR(256),              -- 知识库名称
    document_id VARCHAR(64),                -- 文档ID
    document_name VARCHAR(256),             -- 文档名称
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 文档资源表索引
CREATE INDEX IF NOT EXISTS idx_chat_doc_resource_history_id ON chat_document_resource(chat_history_id);
CREATE INDEX IF NOT EXISTS idx_chat_doc_resource_document_id ON chat_document_resource(document_id);

-- 4. 分段信息表（存储命中的分段ID，与文档资源一对多关联）
CREATE TABLE IF NOT EXISTS chat_segment (
    id BIGSERIAL PRIMARY KEY,
    document_resource_id BIGINT NOT NULL,   -- 关联chat_document_resource.id
    segment_id VARCHAR(64),                 -- 分段ID
    content TEXT,                           -- 分段内容
    score DOUBLE PRECISION DEFAULT 0,       -- 分段命中得分
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 分段信息表索引
CREATE INDEX IF NOT EXISTS idx_chat_segment_doc_resource_id ON chat_segment(document_resource_id);
CREATE INDEX IF NOT EXISTS idx_chat_segment_segment_id ON chat_segment(segment_id);
