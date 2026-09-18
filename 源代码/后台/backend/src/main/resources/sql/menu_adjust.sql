-- ============================================
-- 菜单结构调整SQL
-- 执行时间: 2026-06-09
-- 说明: 
--   1. 将"案例审核"从"作业管理"移到"知识库"菜单下
--   2. 删除已废弃的"设备管理"菜单
--   3. 更新菜单排序
--   4. 清理角色菜单关联
-- ============================================

-- 1. 将案例审核(ID=11)移到知识库(parent_id=4)下
UPDATE sys_menu 
SET parent_id = 4, sort_order = 2 
WHERE id = 11;

-- 2. 删除设备管理菜单(ID=21)
DELETE FROM sys_menu WHERE id = 21;

-- 3. 删除角色菜单关联中的设备管理菜单
DELETE FROM sys_role_menu WHERE menu_id = 21;

-- 4. 更新知识库下的菜单排序
-- 知识库管理(ID=20) 排序1
-- 案例审核(ID=11) 排序2
UPDATE sys_menu SET sort_order = 1 WHERE id = 20;
UPDATE sys_menu SET sort_order = 2 WHERE id = 11;

-- 5. 更新作业管理下的菜单排序（移除案例审核后）
-- 检修作业指引(ID=10) 排序1
-- AI修正统计(ID=12) 排序2
UPDATE sys_menu SET sort_order = 1 WHERE id = 10;
UPDATE sys_menu SET sort_order = 2 WHERE id = 12;

-- ============================================
-- 验证结果SQL（可选执行）
-- ============================================

-- 查看菜单结构
SELECT m.id, m.parent_id, m.name, m.path, m.sort_order,
       p.name as parent_name
FROM sys_menu m
LEFT JOIN sys_menu p ON m.parent_id = p.id
ORDER BY m.parent_id, m.sort_order;

-- 查看知识库菜单下的子菜单
SELECT id, name, path, sort_order 
FROM sys_menu 
WHERE parent_id = 4 
ORDER BY sort_order;

-- 查看作业管理菜单下的子菜单
SELECT id, name, path, sort_order 
FROM sys_menu 
WHERE parent_id = 3 
ORDER BY sort_order;