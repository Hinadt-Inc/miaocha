-- 添加进程ID字段到logstash_machine表
ALTER TABLE logstash_machine
ADD COLUMN process_pid varchar(50) NULL COMMENT '目标机器上的进程ID'; 