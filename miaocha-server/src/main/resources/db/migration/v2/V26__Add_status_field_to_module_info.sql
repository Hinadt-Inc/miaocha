-- =============================================
-- V26: 为module_info表添加状态字段
-- 支持模块的启用/禁用状态管理
-- =============================================

-- 1. 为module_info表添加状态字段
ALTER TABLE module_info
ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '模块状态：1-启用，0-禁用' AFTER query_config;


-- 2. 为现有数据设置默认状态为启用（1）
UPDATE module_info
SET status = 1
WHERE status IS NULL;
