-- Add foreign key constraint to machine table
ALTER TABLE logstash_task ADD CONSTRAINT fk_logstash_task_machine_id 
    FOREIGN KEY (machine_id) REFERENCES machine(id) ON DELETE CASCADE;

COMMENT ON COLUMN logstash_task.machine_id IS 'Machine ID for machine-specific tasks, NULL for global tasks'; 