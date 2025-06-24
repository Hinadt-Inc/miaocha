-- =============================================
-- UNDO V29: 删除module_info表的查询配置字段
-- 撤销为module_info表添加的查询配置功能
-- =============================================

-- 1. 删除module_info表的query_config字段
ALTER TABLE module_info 
DROP COLUMN query_config; 