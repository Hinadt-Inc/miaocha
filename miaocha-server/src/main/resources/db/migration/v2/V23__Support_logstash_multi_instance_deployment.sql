-- =============================================
-- V27: Logstash 一机多实例支持迁移脚本
-- 支持同一台机器上部署多个LogstashProcess实例
-- =============================================

-- 1. 删除原有的唯一约束
ALTER TABLE `logstash_machine` DROP INDEX `uk_logstash_machine`;

-- 2. 添加新的唯一约束，基于物理路径唯一性，确保同一台机器的同一路径只能有一个实例
ALTER TABLE `logstash_machine`
ADD CONSTRAINT `uk_machine_deploy_path` UNIQUE (`machine_id`, `deploy_path`);

-- 3. 为logstash_task表添加logstash_machine_id字段，支持任务与具体的LogstashMachine实例关联
ALTER TABLE `logstash_task`
ADD COLUMN `logstash_machine_id` BIGINT NULL COMMENT 'LogstashMachine实例ID，如果任务是针对特定LogstashMachine实例的';

-- 4. 为新字段添加索引以提高查询性能
ALTER TABLE `logstash_task` ADD INDEX `idx_logstash_machine_id` (`logstash_machine_id`);

-- 5. 为现有任务记录关联logstash_machine_id
-- 根据process_id和machine_id来匹配对应的logstash_machine记录
UPDATE `logstash_task` lt
JOIN `logstash_machine` lm ON lt.process_id = lm.logstash_process_id AND lt.machine_id = lm.machine_id
SET lt.logstash_machine_id = lm.id
WHERE lt.machine_id IS NOT NULL;

-- 6. 为logstash_task_machine_step表添加logstash_machine_id字段
ALTER TABLE `logstash_task_machine_step`
ADD COLUMN `logstash_machine_id` BIGINT NULL COMMENT 'LogstashMachine实例ID，用于精确定位实例';

-- 7. 为logstash_task_machine_step的新字段添加索引
ALTER TABLE `logstash_task_machine_step` ADD INDEX `idx_task_step_logstash_machine_id` (`logstash_machine_id`);

-- 8. 为现有步骤记录关联logstash_machine_id
-- 通过task_id找到对应的logstash_machine_id
UPDATE `logstash_task_machine_step` ltms
JOIN `logstash_task` lt ON ltms.task_id = lt.id
SET ltms.logstash_machine_id = lt.logstash_machine_id
WHERE lt.logstash_machine_id IS NOT NULL;

-- 9. 更新唯一约束，包含logstash_machine_id以支持精确匹配
-- 删除原有约束
ALTER TABLE `logstash_task_machine_step` DROP INDEX `uk_task_machine_step`;

-- 添加新的唯一约束
ALTER TABLE `logstash_task_machine_step`
ADD CONSTRAINT `uk_task_machine_step_v2` UNIQUE (`task_id`, `machine_id`, `step_id`, `logstash_machine_id`);

-- 10. 验证迁移结果的查询（可用于验证）
-- SELECT
--     lm.id,
--     lm.logstash_process_id,
--     lm.machine_id,
--     lm.deploy_path,
--     mi.name as machine_name,
--     lp.name as process_name
-- FROM logstash_machine lm
-- JOIN machine_info mi ON lm.machine_id = mi.id
-- JOIN logstash_process lp ON lm.logstash_process_id = lp.id
-- ORDER BY lm.logstash_process_id, lm.machine_id;
