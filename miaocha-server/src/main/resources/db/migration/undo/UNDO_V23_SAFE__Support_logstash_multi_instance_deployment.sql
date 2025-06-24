-- =============================================
-- UNDO V27 (SAFE): 安全回滚 Logstash 一机多实例支持迁移脚本
-- 撤销对同一台机器上部署多个LogstashProcess实例的支持
-- 此版本会检查数据冲突并提供处理建议
-- =============================================

-- 预检查：检查是否存在同一进程在同一机器上的多个实例
-- 如果存在，回滚可能会失败，需要先处理这些重复数据
SELECT 
    '检查结果：发现同一进程在同一机器上的多个实例' as warning_message,
    logstash_process_id,
    machine_id,
    COUNT(*) as instance_count,
    GROUP_CONCAT(id) as logstash_machine_ids,
    GROUP_CONCAT(deploy_path) as deploy_paths
FROM logstash_machine 
GROUP BY logstash_process_id, machine_id 
HAVING COUNT(*) > 1;

-- 如果上述查询返回结果，请在回滚前手动处理重复数据
-- 建议处理方式：
-- 1. 选择保留一个实例，删除其他实例
-- 2. 或者将其他实例迁移到不同的机器上
-- 示例删除重复实例的SQL（请根据实际情况修改）：
-- DELETE FROM logstash_machine WHERE id IN (需要删除的ID列表);

-- =============================================
-- 正式回滚步骤
-- =============================================

-- 1. 删除新的唯一约束 uk_task_machine_step_v2
ALTER TABLE `logstash_task_machine_step` DROP INDEX `uk_task_machine_step_v2`;

-- 2. 恢复原有的唯一约束 uk_task_machine_step
ALTER TABLE `logstash_task_machine_step` 
ADD CONSTRAINT `uk_task_machine_step` UNIQUE (`task_id`, `machine_id`, `step_id`);

-- 3. 清空logstash_task_machine_step表的logstash_machine_id字段数据
UPDATE `logstash_task_machine_step` SET `logstash_machine_id` = NULL;

-- 4. 删除logstash_task_machine_step表的logstash_machine_id字段索引
DROP INDEX `idx_task_step_logstash_machine_id` ON `logstash_task_machine_step`;

-- 5. 删除logstash_task_machine_step表的logstash_machine_id字段
ALTER TABLE `logstash_task_machine_step` DROP COLUMN `logstash_machine_id`;

-- 6. 清空logstash_task表的logstash_machine_id字段数据
UPDATE `logstash_task` SET `logstash_machine_id` = NULL;

-- 7. 删除logstash_task表的logstash_machine_id字段索引
DROP INDEX `idx_logstash_machine_id` ON `logstash_task`;

-- 8. 删除logstash_task表的logstash_machine_id字段
ALTER TABLE `logstash_task` DROP COLUMN `logstash_machine_id`;

-- 9. 删除新的唯一约束 uk_machine_deploy_path
ALTER TABLE `logstash_machine` DROP INDEX `uk_machine_deploy_path`;

-- 10. 尝试恢复原有的唯一约束 uk_logstash_machine
-- 如果失败，说明存在重复数据，需要手动处理
BEGIN;
    -- 检查是否会出现约束冲突
    SELECT 
        '准备添加唯一约束，检查是否存在冲突...' as step_message,
        logstash_process_id,
        machine_id,
        COUNT(*) as conflict_count
    FROM logstash_machine 
    GROUP BY logstash_process_id, machine_id 
    HAVING COUNT(*) > 1;
    
    -- 如果上述查询无结果，则可以安全添加约束
    ALTER TABLE `logstash_machine` 
    ADD CONSTRAINT `uk_logstash_machine` UNIQUE (`logstash_process_id`, `machine_id`);
    
    SELECT '唯一约束添加成功' as success_message;
COMMIT;

-- =============================================
-- 回滚完成后的验证查询
-- =============================================

-- 验证logstash_machine表结构
SHOW CREATE TABLE logstash_machine;

-- 验证logstash_task表结构
SHOW CREATE TABLE logstash_task;

-- 验证logstash_task_machine_step表结构
SHOW CREATE TABLE logstash_task_machine_step;

-- 验证数据一致性
SELECT 
    '回滚后数据验证' as verification_step,
    lm.id,
    lm.logstash_process_id,
    lm.machine_id,
    lm.deploy_path,
    mi.name as machine_name,
    lp.name as process_name
FROM logstash_machine lm
JOIN machine_info mi ON lm.machine_id = mi.id  
JOIN logstash_process lp ON lm.logstash_process_id = lp.id
ORDER BY lm.logstash_process_id, lm.machine_id;

-- =============================================
-- 重要提醒：
-- 1. 回滚完成后需要重启应用
-- 2. 需要将代码版本回退到V27之前的版本
-- 3. 建议在生产环境执行前先在测试环境验证
-- 4. 如果有自定义的deploy_path，回滚后这些配置将失效
-- ============================================= 