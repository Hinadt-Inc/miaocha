-- 从logstash_process表中删除machine_name字段
ALTER TABLE `logstash_process` DROP COLUMN `machine_name`;