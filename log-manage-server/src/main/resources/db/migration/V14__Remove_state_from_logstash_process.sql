-- 从logstash_process表中删除state字段
ALTER TABLE `logstash_process` DROP COLUMN `state`;
