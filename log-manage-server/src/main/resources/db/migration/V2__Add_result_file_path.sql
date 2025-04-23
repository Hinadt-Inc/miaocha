-- 添加SQL查询历史表的结果文件路径字段
ALTER TABLE sql_query_history
ADD COLUMN result_file_path varchar(255) DEFAULT NULL COMMENT '查询结果文件路径'; 