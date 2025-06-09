-- 将logstash_machine表中的state字段初始化为NOT_STARTED（如果为空）
UPDATE `logstash_machine` SET `state` = 'NOT_STARTED' WHERE `state` IS NULL;
