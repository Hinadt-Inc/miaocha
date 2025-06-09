-- Add state and configuration fields to logstash_machine table
ALTER TABLE `logstash_machine`
ADD COLUMN `state` varchar(50) NULL COMMENT '进程在机器上的状态，枚举有：未启动，正在启动，运行中，失败等状态',
ADD COLUMN `config_content` text NULL COMMENT '机器特定的logstash配置文件内容',
ADD COLUMN `jvm_options` text NULL COMMENT 'JVM配置选项',
ADD COLUMN `logstash_yml` text NULL COMMENT 'Logstash系统配置';

-- Add configuration template fields to logstash_process table
ALTER TABLE `logstash_process`
ADD COLUMN `jvm_options` text NULL COMMENT 'JVM配置选项模板',
ADD COLUMN `logstash_yml` text NULL COMMENT 'Logstash系统配置模板';

-- Update existing logstash_machine records to have the same state as their parent process
UPDATE `logstash_machine` lm
JOIN `logstash_process` lp ON lm.logstash_process_id = lp.id
SET lm.state = lp.state;
