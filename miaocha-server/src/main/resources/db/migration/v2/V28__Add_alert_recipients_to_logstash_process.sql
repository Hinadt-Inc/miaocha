-- Add alert recipients JSON column to logstash_process
ALTER TABLE logstash_process
    ADD COLUMN alert_recipients JSON NULL COMMENT 'Alert recipients List<String> emails' AFTER logstash_yml;

