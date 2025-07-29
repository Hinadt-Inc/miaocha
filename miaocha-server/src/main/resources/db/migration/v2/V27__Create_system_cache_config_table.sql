-- =============================================
-- V27: 创建系统缓存配置表
-- 用于存储系统各种缓存配置信息,包括历史归档记录等信息
-- =============================================

-- 创建系统缓存表
CREATE TABLE system_cache_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    cache_group VARCHAR(128) NOT NULL COMMENT '缓存组信息/标签，如：log_search_group、user_config_group等',
    cache_key VARCHAR(256) NOT NULL COMMENT '缓存键，如：logstash_process_id、user_id等',
    content JSON NOT NULL COMMENT '缓存内容，JSON格式存储配置信息',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_user VARCHAR(128) NOT NULL COMMENT '创建人'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统缓存配置表';

-- 创建联合唯一索引，确保同一组内的同一key只有一条记录
CREATE UNIQUE INDEX uk_cache_group_key ON system_cache_config(cache_group, cache_key);

-- 创建缓存组索引，便于按组查询
CREATE INDEX idx_cache_group ON system_cache_config(cache_group,create_user);


-- 插入示例数据说明
-- INSERT INTO system_cache_config (cache_group, cache_key, content, create_user) VALUES
-- ('logstash_config', 'process_123', '{"config_path":"/opt/logstash/config","status":"running"}', 'system'),
-- ('user_search_config', 'user_456', '{"default_time_range":"1h","favorite_fields":["message","level"]}', 'admin');
