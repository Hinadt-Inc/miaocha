-- Add update_time column to logstash_machine table
ALTER TABLE `logstash_machine`
ADD COLUMN `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'; 