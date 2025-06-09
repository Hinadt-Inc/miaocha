-- =============================================
-- V20: 对齐生产环境schema
-- 重命名表名、调整字段长度、删除外键约束、修改唯一键名称
-- =============================================

-- 1. 重命名表名并调整datasource表字段长度
RENAME TABLE `datasource` TO `datasource_info`;
ALTER TABLE `datasource_info` 
    MODIFY COLUMN `name` varchar(128) NOT NULL COMMENT '数据源名称',
    MODIFY COLUMN `type` varchar(64) NOT NULL COMMENT '数据源类型',
    MODIFY COLUMN `ip` varchar(128) NOT NULL COMMENT '数据源IP',
    MODIFY COLUMN `username` varchar(128) NOT NULL COMMENT '数据源用户名',
    MODIFY COLUMN `password` varchar(128) NOT NULL COMMENT '数据源密码',
    MODIFY COLUMN `database` varchar(128) NOT NULL COMMENT '数据源数据库';

-- 2. 调整user表字段长度
ALTER TABLE `user`
    MODIFY COLUMN `nickname` varchar(128) NOT NULL COMMENT '用户昵称',
    MODIFY COLUMN `email` varchar(128) NOT NULL COMMENT '用户邮箱',
    MODIFY COLUMN `uid` varchar(128) NOT NULL COMMENT '用户ID',
    MODIFY COLUMN `password` varchar(128) DEFAULT NULL COMMENT '用户密码',
    MODIFY COLUMN `role` varchar(32) NOT NULL DEFAULT 'USER' COMMENT '用户角色: SUPER_ADMIN, ADMIN, USER';

-- 3. 调整sql_query_history表并删除外键约束
-- 创建临时存储过程来安全删除外键
DELIMITER $$
CREATE PROCEDURE DropForeignKeyIfExists(
    IN table_name VARCHAR(64),
    IN constraint_name VARCHAR(64)
)
BEGIN
    DECLARE constraint_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO constraint_exists
    FROM information_schema.REFERENTIAL_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = DATABASE() 
      AND TABLE_NAME = table_name 
      AND CONSTRAINT_NAME = constraint_name;
    
    IF constraint_exists > 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', table_name, '` DROP FOREIGN KEY `', constraint_name, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 删除外键约束
CALL DropForeignKeyIfExists('sql_query_history', 'fk_history_user');
CALL DropForeignKeyIfExists('sql_query_history', 'fk_history_datasource');

-- 删除临时存储过程
DROP PROCEDURE DropForeignKeyIfExists;

-- 调整字段长度
ALTER TABLE `sql_query_history`
    MODIFY COLUMN `table_name` varchar(128) NOT NULL COMMENT '表名',
    MODIFY COLUMN `result_file_path` varchar(256) DEFAULT NULL COMMENT '查询结果文件路径';

-- 4. 重命名表名并调整machine表字段长度
RENAME TABLE `machine` TO `machine_info`;
ALTER TABLE `machine_info`
    MODIFY COLUMN `name` varchar(64) NOT NULL COMMENT '机器名称',
    MODIFY COLUMN `ip` varchar(64) NOT NULL COMMENT '机器IP',
    MODIFY COLUMN `username` varchar(64) NOT NULL COMMENT '机器用户名',
    MODIFY COLUMN `password` varchar(64) NULL COMMENT '机器密码';

-- 5. 调整logstash_process表并删除外键约束
-- 重新创建临时存储过程
DELIMITER $$
CREATE PROCEDURE DropForeignKeyIfExists(
    IN table_name VARCHAR(64),
    IN constraint_name VARCHAR(64)
)
BEGIN
    DECLARE constraint_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO constraint_exists
    FROM information_schema.REFERENTIAL_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = DATABASE() 
      AND TABLE_NAME = table_name 
      AND CONSTRAINT_NAME = constraint_name;
    
    IF constraint_exists > 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', table_name, '` DROP FOREIGN KEY `', constraint_name, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 删除外键约束
CALL DropForeignKeyIfExists('logstash_process', 'fk_logstash_datasource');
CALL DropForeignKeyIfExists('logstash_machine', 'fk_lm_logstash');
CALL DropForeignKeyIfExists('logstash_machine', 'fk_lm_machine');

-- 删除临时存储过程
DROP PROCEDURE DropForeignKeyIfExists;

-- 调整logstash_process字段长度
ALTER TABLE `logstash_process`
    MODIFY COLUMN `name` varchar(64) NOT NULL COMMENT '进程名称',
    MODIFY COLUMN `module` varchar(64) NOT NULL COMMENT '模块名称',
    MODIFY COLUMN `table_name` varchar(128) DEFAULT NULL COMMENT 'Doris表名';

-- 6. 调整logstash_machine表字段长度和注释
ALTER TABLE `logstash_machine`
    MODIFY COLUMN `process_pid` varchar(64) NULL COMMENT '目标机器上的进程ID',
    MODIFY COLUMN `state` varchar(64) NULL COMMENT '进程在机器上的状态，枚举有：INITIALIZING(初始化中), NOT_STARTED(未启动), STARTING(正在启动), RUNNING(运行中), STOPPING(正在停止), START_FAILED(启动失败), STOP_FAILED(停止失败), INITIALIZE_FAILED(初始化失败)';

-- 7. 调整logstash_task表字段长度
ALTER TABLE `logstash_task`
    MODIFY COLUMN `id` varchar(64) NOT NULL COMMENT '任务ID (UUID)',
    MODIFY COLUMN `name` varchar(128) NOT NULL COMMENT '任务名称',
    MODIFY COLUMN `description` varchar(248) NULL COMMENT '任务描述',
    MODIFY COLUMN `status` varchar(32) NOT NULL COMMENT '任务状态 (PENDING, RUNNING, COMPLETED, FAILED, CANCELLED)',
    MODIFY COLUMN `operation_type` varchar(32) NOT NULL COMMENT '操作类型 (INITIALIZE, START, STOP, RESTART, UPDATE_CONFIG, REFRESH_CONFIG)';

-- 8. 调整logstash_task_machine_step表字段长度
ALTER TABLE `logstash_task_machine_step`
    MODIFY COLUMN `task_id` varchar(64) NOT NULL COMMENT '任务ID',
    MODIFY COLUMN `step_id` varchar(64) NOT NULL COMMENT '步骤ID',
    MODIFY COLUMN `step_name` varchar(128) NOT NULL COMMENT '步骤名称',
    MODIFY COLUMN `status` varchar(32) NOT NULL COMMENT '步骤状态 (PENDING, RUNNING, COMPLETED, FAILED, SKIPPED)';

-- 9. 调整user_module_permission表
-- 先删除原有唯一键 (使用类似的存储过程方法)
DELIMITER $$
CREATE PROCEDURE DropIndexIfExists(
    IN table_name VARCHAR(64),
    IN index_name VARCHAR(64)
)
BEGIN
    DECLARE index_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO index_exists
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = table_name 
      AND INDEX_NAME = index_name;
    
    IF index_exists > 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', table_name, '` DROP INDEX `', index_name, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 删除原有唯一键
CALL DropIndexIfExists('user_module_permission', 'uk_user_datasource_module');

-- 删除临时存储过程
DROP PROCEDURE DropIndexIfExists;

-- 调整字段长度
ALTER TABLE `user_module_permission`
    MODIFY COLUMN `module` varchar(128) NOT NULL COMMENT '模块名称';

-- 添加新的唯一键
ALTER TABLE `user_module_permission`
    ADD CONSTRAINT `uniq_udmodule` UNIQUE (`user_id`, `datasource_id`, `module`);
