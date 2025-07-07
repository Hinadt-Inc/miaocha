-- =============================================
-- UNDO V26: 回滚module_info表状态字段的添加
-- =============================================

-- 删除status字段
ALTER TABLE module_info
DROP COLUMN status;
