ALTER TABLE work_sop_flow
    ADD COLUMN IF NOT EXISTS category VARCHAR(50) DEFAULT '其他';

UPDATE work_sop_flow
SET category = '其他'
WHERE category IS NULL;

COMMENT ON COLUMN work_sop_flow.category IS '任务分类';
