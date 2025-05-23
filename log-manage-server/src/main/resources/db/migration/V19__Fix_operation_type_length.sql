-- 修复logstash_task表中operation_type字段长度不足的问题
-- 原字段定义为VARCHAR(10)，但枚举值UPDATE_CONFIG(13字符)和REFRESH_CONFIG(14字符)超出限制
-- 将字段长度扩展到20个字符以容纳所有可能的枚举值

ALTER TABLE logstash_task MODIFY COLUMN operation_type VARCHAR(20) NOT NULL COMMENT '操作类型 (INITIALIZE, START, STOP, RESTART, UPDATE_CONFIG, REFRESH_CONFIG)'; 