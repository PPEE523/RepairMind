-- ============================================
-- 迁移脚本：用户管理菜单路径修正
-- 将 /user 改为 /user-management，避免与用户端 /user 入口冲突
-- 执行方式：psql -U your_user -d your_db -f fix_menu.sql
-- ============================================
UPDATE sys_menu SET path = '/user-management' WHERE id = 30 AND path = '/user';
