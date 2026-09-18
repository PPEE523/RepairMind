-- ============================================
-- 作业执行记录表
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
COMMENT ON COLUMN work_execution.id IS '记录ID';
COMMENT ON COLUMN work_execution.workflow_id IS '作业流程ID';
COMMENT ON COLUMN work_execution.user_id IS '执行用户ID';
COMMENT ON COLUMN work_execution.step_index IS '步骤索引';
COMMENT ON COLUMN work_execution.step_status IS '步骤状态: pending/in_progress/completed/skipped/exception';
COMMENT ON COLUMN work_execution.completed_time IS '完成时间';
COMMENT ON COLUMN work_execution.remark IS '备注';
COMMENT ON COLUMN work_execution.image_urls IS '异常图片URL，多个用逗号分隔';
COMMENT ON COLUMN work_execution.create_time IS '创建时间';
COMMENT ON COLUMN work_execution.update_time IS '更新时间';

CREATE INDEX idx_work_execution_workflow ON work_execution(workflow_id);
CREATE INDEX idx_work_execution_user ON work_execution(user_id);

-- ============================================
-- 作业执行主表
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
COMMENT ON COLUMN work_execution_main.id IS '执行ID';
COMMENT ON COLUMN work_execution_main.workflow_id IS '作业流程ID';
COMMENT ON COLUMN work_execution_main.user_id IS '执行用户ID';
COMMENT ON COLUMN work_execution_main.status IS '状态: in_progress/completed/exception';
COMMENT ON COLUMN work_execution_main.start_time IS '开始时间';
COMMENT ON COLUMN work_execution_main.end_time IS '结束时间';
COMMENT ON COLUMN work_execution_main.total_steps IS '总步骤数';
COMMENT ON COLUMN work_execution_main.completed_steps IS '已完成步骤数';

CREATE INDEX idx_execution_main_workflow ON work_execution_main(workflow_id);
CREATE INDEX idx_execution_main_user ON work_execution_main(user_id);
