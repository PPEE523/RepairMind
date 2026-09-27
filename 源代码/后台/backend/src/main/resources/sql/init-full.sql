-- ============================================
-- 设备检修系统 - 完整数据库初始化脚本
-- 数据库: PostgreSQL 14+
-- 执行命令: psql -h 192.168.110.107 -U postgres -d repair_db -f init.sql
-- ============================================

-- ============================================
-- 1. 角色表
-- ============================================
DROP TABLE IF EXISTS sys_role CASCADE;
CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_role IS '角色表';

-- ============================================
-- 2. 用户表
-- ============================================
DROP TABLE IF EXISTS sys_user CASCADE;
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    role_id BIGINT,
    dept_id BIGINT,
    status SMALLINT DEFAULT 1,
    password_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_user IS '用户表';

-- ============================================
-- 3. 设备档案表
-- ============================================
DROP TABLE IF EXISTS equipment_info CASCADE;
CREATE TABLE equipment_info (
    id BIGSERIAL PRIMARY KEY,
    equip_type VARCHAR(50) NOT NULL,
    equip_model VARCHAR(100) NOT NULL,
    dept_id BIGINT,
    manual_path VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE equipment_info IS '设备档案表';

-- ============================================
-- 4. 检修案例表
-- ============================================
DROP TABLE IF EXISTS repair_knowledge CASCADE;
CREATE TABLE repair_knowledge (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    symptom TEXT,
    solution TEXT,
    img_url VARCHAR(500),
    images TEXT,
    equip_model VARCHAR(100),
    audit_status SMALLINT DEFAULT 0,
    upload_user_id BIGINT,
    audit_user_id BIGINT,
    audit_remark VARCHAR(500),
    document_id VARCHAR(500),
    dataset_id VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE repair_knowledge IS '检修案例表';

-- ============================================
-- 5. 标准化作业表
-- ============================================
DROP TABLE IF EXISTS work_flow CASCADE;
CREATE TABLE work_flow (
    id BIGSERIAL PRIMARY KEY,
    equip_model VARCHAR(100) NOT NULL,
    repair_level VARCHAR(50) NOT NULL,
    step_json TEXT NOT NULL,
    version INTEGER DEFAULT 1,
    create_user BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI 生成的 SOP 流程；原 init-full.sql 遗漏此表。
CREATE TABLE work_sop_flow (
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
CREATE INDEX idx_sop_flow_status ON work_sop_flow(status);
CREATE INDEX idx_sop_flow_create_user ON work_sop_flow(create_user);

COMMENT ON TABLE work_flow IS '标准化作业表';

-- ============================================
-- 6. 操作日志表
-- ============================================
DROP TABLE IF EXISTS sys_oper_log CASCADE;
CREATE TABLE sys_oper_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    oper_type VARCHAR(50),
    content VARCHAR(500),
    oper_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50)
);

COMMENT ON TABLE sys_oper_log IS '操作日志表';

-- ============================================
-- 7. Dify配置表
-- ============================================
DROP TABLE IF EXISTS sys_model_config CASCADE;
CREATE TABLE sys_model_config (
    id BIGSERIAL PRIMARY KEY,
    dify_base_url VARCHAR(200),
    dify_app_api_key VARCHAR(200),
    dify_dataset_api_key VARCHAR(200),
    dify_dataset_id VARCHAR(100),
    enable SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_model_config IS 'Dify配置表';

-- ============================================
-- 8. 系统参数表
-- ============================================
DROP TABLE IF EXISTS sys_config CASCADE;
CREATE TABLE sys_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    remark VARCHAR(200),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_config IS '系统参数表';

-- ============================================
-- 9. 检索历史表
-- ============================================
DROP TABLE IF EXISTS search_history CASCADE;
CREATE TABLE search_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    query_text VARCHAR(500),
    equip_model VARCHAR(100),
    image_url VARCHAR(500),
    image_urls TEXT,
    file_types VARCHAR(500),
    file_names VARCHAR(1000),
    result_answer TEXT,
    search_success BOOLEAN DEFAULT TRUE,
    error_message VARCHAR(500),
    conversation_id VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE search_history IS '检索历史表';
COMMENT ON COLUMN search_history.search_success IS '是否检索成功';
COMMENT ON COLUMN search_history.image_urls IS '故障图片URL列表，JSON数组格式';
COMMENT ON COLUMN search_history.error_message IS '检索失败时的错误信息';

-- ============================================
-- 10. AI反馈表
-- ============================================
DROP TABLE IF EXISTS ai_feedback CASCADE;
CREATE TABLE ai_feedback (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    search_history_id BIGINT,
    original_answer TEXT,
    rating SMALLINT,
    corrected_answer TEXT,
    feedback_type VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_feedback IS 'AI反馈表';

-- ============================================
-- 11. 菜单表
-- ============================================
DROP TABLE IF EXISTS sys_menu CASCADE;
CREATE TABLE sys_menu (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(50) NOT NULL,
    path VARCHAR(100),
    component VARCHAR(100),
    icon VARCHAR(50),
    sort_order INTEGER DEFAULT 0,
    visible SMALLINT DEFAULT 1,
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_menu IS '菜单表';

-- ============================================
-- 12. 角色菜单关联表
-- ============================================
DROP TABLE IF EXISTS sys_role_menu CASCADE;
CREATE TABLE sys_role_menu (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, menu_id)
);

COMMENT ON TABLE sys_role_menu IS '角色菜单关联表';

-- ============================================
-- 13. 作业执行主表
-- ============================================
DROP TABLE IF EXISTS work_execution_main CASCADE;
CREATE TABLE work_execution_main (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'in_progress',
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    total_steps INTEGER,
    completed_steps INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE work_execution_main IS '作业执行主表';

-- ============================================
-- 14. 作业执行记录表
-- ============================================
DROP TABLE IF EXISTS work_execution CASCADE;
CREATE TABLE work_execution (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    step_index INTEGER NOT NULL,
    step_status VARCHAR(20) DEFAULT 'pending',
    completed_time TIMESTAMP,
    remark TEXT,
    image_urls TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE work_execution IS '作业执行记录表';

-- 对话和检索数据：看板登录后会立即查询 chat_history。
CREATE TABLE chat_history (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(64),
    user_question TEXT NOT NULL,
    device_model VARCHAR(128),
    answer_content TEXT,
    message_id VARCHAR(64),
    status VARCHAR(20) DEFAULT 'success',
    round_num INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT
);
CREATE INDEX idx_chat_history_conversation_id ON chat_history(conversation_id);
CREATE INDEX idx_chat_history_user_id ON chat_history(user_id);
CREATE INDEX idx_chat_history_create_time ON chat_history(create_time);

CREATE TABLE chat_feedback (
    id BIGSERIAL PRIMARY KEY,
    chat_history_id BIGINT NOT NULL,
    feedback_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    feedback_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT
);
CREATE INDEX idx_chat_feedback_history_id ON chat_feedback(chat_history_id);

CREATE TABLE chat_document_resource (
    id BIGSERIAL PRIMARY KEY,
    chat_history_id BIGINT NOT NULL,
    dataset_id VARCHAR(64),
    dataset_name VARCHAR(256),
    document_id VARCHAR(64),
    document_name VARCHAR(256),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_chat_doc_resource_history_id ON chat_document_resource(chat_history_id);
CREATE INDEX idx_chat_doc_resource_document_id ON chat_document_resource(document_id);

CREATE TABLE chat_segment (
    id BIGSERIAL PRIMARY KEY,
    document_resource_id BIGINT NOT NULL,
    segment_id VARCHAR(64),
    content TEXT,
    score DOUBLE PRECISION DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_chat_segment_doc_resource_id ON chat_segment(document_resource_id);
CREATE INDEX idx_chat_segment_segment_id ON chat_segment(segment_id);

CREATE TABLE chat_file (
    id BIGSERIAL PRIMARY KEY,
    chat_history_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(20) NOT NULL,
    dify_file_id VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_chat_file_history ON chat_file(chat_history_id);

-- ============================================
-- 创建索引
-- ============================================
CREATE INDEX idx_user_username ON sys_user(username);
CREATE INDEX idx_user_role_id ON sys_user(role_id);
CREATE INDEX idx_knowledge_audit_status ON repair_knowledge(audit_status);
CREATE INDEX idx_knowledge_equip_model ON repair_knowledge(equip_model);
CREATE INDEX idx_knowledge_upload_user ON repair_knowledge(upload_user_id);
CREATE INDEX idx_workflow_equip_model ON work_flow(equip_model);
CREATE INDEX idx_operlog_user_id ON sys_oper_log(user_id);
CREATE INDEX idx_operlog_oper_time ON sys_oper_log(oper_time);
CREATE INDEX idx_search_history_user ON search_history(user_id);
CREATE INDEX idx_ai_feedback_user ON ai_feedback(user_id);
CREATE INDEX idx_role_menu_role ON sys_role_menu(role_id);
CREATE INDEX idx_role_menu_menu ON sys_role_menu(menu_id);
CREATE INDEX idx_execution_main_workflow ON work_execution_main(workflow_id);
CREATE INDEX idx_execution_main_user ON work_execution_main(user_id);
CREATE INDEX idx_work_execution_workflow ON work_execution(workflow_id);
CREATE INDEX idx_work_execution_user ON work_execution(user_id);

-- ============================================
-- 初始化数据
-- ============================================

-- 初始化角色
INSERT INTO sys_role (role_name, role_code, description) VALUES
('系统管理员', 'ADMIN', '全平台最高权限'),
('资深检修工程师', 'ENGINEER', '检修案例上传、多模态故障检索'),
('实习检修人员', 'INTERN', '仅知识库检索、查看作业指引');

-- 初始化管理员用户 (密码: Admin@123456 BCrypt加密)
INSERT INTO sys_user (username, password, nickname, role_id, dept_id, status, password_update_time) VALUES
('admin', '$2a$10$B4L.eM..tUNJzIXnUQeH..WL7GDDvDPv755hd89bH0h68eebPSmYS', '系统管理员', 1, 1, 1, NOW());

-- 初始化设备数据
INSERT INTO equipment_info (equip_type, equip_model, dept_id) VALUES
('发动机', 'ENGINE-V8-2024', 1),
('变速箱', 'TRANS-6AT-2024', 1),
('液压系统', 'HYD-SYS-2024', 2),
('电气系统', 'ELEC-SYS-2024', 2);

-- 初始化Dify配置
INSERT INTO sys_model_config (dify_base_url, dify_app_api_key, dify_dataset_api_key, dify_dataset_id, enable) VALUES
('http://192.168.110.107', 'app-Nt08PvVSlwfR4KaS2SVW6pHG', 'dataset-fzQHmDfXpDukf9HRk9uvg3Mk', 'b4ae60fa-d748-4259-b569-0366b31aff20', 1);

-- 初始化系统参数
INSERT INTO sys_config (config_key, config_value, remark) VALUES
('search.topK', '5', '检索返回结果数量'),
('search.threshold', '0.7', '相似度阈值'),
('search.temperature', '0.7', '大模型Temperature'),
('password.expireDays', '90', '密码有效期(天)'),
('login.maxRetry', '5', '最大登录失败次数'),
('login.lockMinutes', '15', '锁定时间(分钟)');

-- 初始化菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order) VALUES
(1, 0, '数据看板', '/dashboard', 'Dashboard', 'DataAnalysis', 1),
(2, 0, '多模态检索', '/search', 'Search', 'Search', 2),
(3, 0, '作业管理', NULL, NULL, 'Document', 3),
(4, 0, '知识库', NULL, NULL, 'Collection', 4),
(5, 0, '系统管理', NULL, NULL, 'Setting', 5),
(6, 0, '个人中心', '/profile', 'Profile', 'UserFilled', 6),
(10, 3, '检修作业指引', '/workflow', 'WorkFlow', NULL, 1),
(12, 3, 'AI修正统计', '/ai-feedback', 'AiFeedbackDashboard', NULL, 2),
(20, 4, '知识库管理', '/knowledge', 'Knowledge', NULL, 1),
(11, 4, '案例审核', '/audit', 'Audit', NULL, 2),
(30, 5, '用户管理', '/user-management', 'UserManagement', NULL, 1),
(31, 5, '角色管理', '/role', 'RoleManagement', NULL, 2),
(32, 5, '系统配置', '/system', 'System', NULL, 3);

-- 初始化角色菜单关联
-- 系统管理员: 所有菜单
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6),
(1, 10), (1, 11), (1, 12), (1, 20), (1, 30), (1, 31), (1, 32);

-- 资深检修工程师
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 6),
(2, 10), (2, 11), (2, 12), (2, 20);

-- 实习检修人员
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 1), (3, 2), (3, 4), (3, 6),
(3, 20);

-- 初始化作业流程示例
INSERT INTO work_flow (equip_model, repair_level, step_json, version, create_user) VALUES
('ENGINE-V8-2024', '日常保养', '[
    {"title": "安全检查", "description": "确认设备已停机，切断电源，挂警示牌"},
    {"title": "外观检查", "description": "检查发动机外观是否有漏油、漏水现象"},
    {"title": "机油检查", "description": "检查机油液位和油质，必要时更换"},
    {"title": "滤清器检查", "description": "检查空气滤清器、机油滤清器是否需要更换"},
    {"title": "皮带检查", "description": "检查各传动皮带张紧度和磨损情况"},
    {"title": "冷却液检查", "description": "检查冷却液液位和浓度"},
    {"title": "启动测试", "description": "恢复供电，启动设备运行测试"},
    {"title": "记录归档", "description": "填写保养记录，归档保存"}
]', 1, 1);
