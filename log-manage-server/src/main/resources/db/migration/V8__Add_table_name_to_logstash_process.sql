-- 添加表名字段到logstash_process表
ALTER TABLE `logstash_process`
ADD COLUMN `table_name` varchar(100) DEFAULT NULL COMMENT 'Doris表名';
