-- ============================================
-- 迁移脚本: 拆分故障描述为 symptom + solution
-- 执行日期: 2026-06-12
-- ============================================

-- 1. 新增 symptom（故障描述）列
ALTER TABLE repair_knowledge ADD COLUMN IF NOT EXISTS symptom TEXT;

COMMENT ON COLUMN repair_knowledge.symptom IS '故障描述';

-- 2. 新增 solution（解决方案）列
ALTER TABLE repair_knowledge ADD COLUMN IF NOT EXISTS solution TEXT;

COMMENT ON COLUMN repair_knowledge.solution IS '解决方案';

-- 3. 将现有 content 数据迁移到 symptom 列（默认全部视为故障描述）
--    后续可手动拆分已有数据
UPDATE repair_knowledge SET symptom = content WHERE symptom IS NULL;

-- 4. content 列保留作为兼容字段（自动由 symptom + solution 拼接）
