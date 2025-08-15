-- UNDO V28 (SAFE): remove alert_recipients column from logstash_process
-- Note: this will drop configured alert recipients data.

ALTER TABLE logstash_process
    DROP COLUMN alert_recipients;
