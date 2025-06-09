-- 为logstash_process表的module字段添加唯一约束
ALTER TABLE `logstash_process`
ADD CONSTRAINT `uk_logstash_process_module` UNIQUE (`module`);
