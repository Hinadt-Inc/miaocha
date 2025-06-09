-- Fix LogstashMachineMapper insert to include all needed columns
ALTER TABLE `logstash_machine` MODIFY COLUMN `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- Update the insert query to include all necessary fields
UPDATE `logstash_machine` 
SET 
  `config_content` = (SELECT `config_content` FROM `logstash_process` WHERE `id` = `logstash_machine`.`logstash_process_id`),
  `jvm_options` = (SELECT `jvm_options` FROM `logstash_process` WHERE `id` = `logstash_machine`.`logstash_process_id`),
  `logstash_yml` = (SELECT `logstash_yml` FROM `logstash_process` WHERE `id` = `logstash_machine`.`logstash_process_id`)
WHERE 
  `config_content` IS NULL OR `jvm_options` IS NULL OR `logstash_yml` IS NULL; 