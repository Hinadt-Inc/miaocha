-- =============================================
-- V29: 为module_info表添加查询配置字段
-- 支持配置分析时间字段和关键词检索字段
-- =============================================

-- 1. 为module_info表添加查询配置JSON字段
ALTER TABLE module_info 
ADD COLUMN query_config JSON COMMENT '查询配置JSON，包含时间字段和关键词检索字段配置' AFTER doris_sql;

-- 2. 为query_config字段添加JSON格式校验
-- JSON结构说明：
-- {
--   "timeField": "log_time",                    // 分析时间字段名
--   "keywordFields": [                          // 关键词检索字段配置数组
--     {
--       "fieldName": "message",                 // 字段名
--       "searchMethod": "LIKE"                  // 检索方法：LIKE、MATCH_ALL、MATCH_ANY、MATCH_PHRASE
--     },
--     {
--       "fieldName": "level",
--       "searchMethod": "MATCH_ALL"
--     }
--   ]
-- }

-- 3. 设置默认值为空JSON对象（对于现有数据）
UPDATE module_info 
SET query_config = JSON_OBJECT() 
WHERE query_config IS NULL; 