-- 将 logstash_process 表中的 config_json 列重命名为 config_content
ALTER TABLE `logstash_process` CHANGE COLUMN `config_json` `config_content` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Logstash配置内容';
