-- Logstash部署任务表
CREATE TABLE `logstash_task` (
    id VARCHAR(36) NOT NULL COMMENT '任务ID (UUID)',
    process_id BIGINT NOT NULL COMMENT 'logstash进程数据库ID',
    name VARCHAR(100) NOT NULL COMMENT '任务名称',
    description VARCHAR(255) NULL COMMENT '任务描述',
    status VARCHAR(20) NOT NULL COMMENT '任务状态 (PENDING, RUNNING, COMPLETED, FAILED, CANCELLED)',
    operation_type VARCHAR(10) NOT NULL COMMENT '操作类型 (START, STOP)',
    start_time DATETIME NULL COMMENT '开始时间',
    end_time DATETIME NULL COMMENT '结束时间',
    error_message TEXT NULL COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_process_id (process_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Logstash部署任务表';

-- Logstash任务在机器上的执行步骤表
CREATE TABLE `logstash_task_machine_step` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id VARCHAR(36) NOT NULL COMMENT '任务ID',
    machine_id BIGINT NOT NULL COMMENT '机器ID',
    step_id VARCHAR(50) NOT NULL COMMENT '步骤ID',
    step_name VARCHAR(100) NOT NULL COMMENT '步骤名称',
    status VARCHAR(20) NOT NULL COMMENT '步骤状态 (PENDING, RUNNING, COMPLETED, FAILED, SKIPPED)',
    start_time DATETIME NULL COMMENT '开始时间',
    end_time DATETIME NULL COMMENT '结束时间',
    error_message TEXT NULL COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_machine_step (task_id, machine_id, step_id),
    KEY idx_task_id (task_id),
    KEY idx_machine_id (machine_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Logstash任务在机器上的执行步骤表'; 