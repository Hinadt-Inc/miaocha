-- 机器元信息表
CREATE TABLE `machine`
(
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    name varchar(50) NOT NULL COMMENT '机器名称',
    ip varchar(50) NOT NULL COMMENT '机器IP',
    port int NOT NULL COMMENT '机器端口',
    username varchar(50) NOT NULL COMMENT '机器用户名',
    password varchar(50) NULL COMMENT '机器密码',
    ssh_key text NULL COMMENT '机器SSH密钥',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '机器元信息表';

-- Logstash进程任务信息表
CREATE TABLE `logstash_process`
(
    id            bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name          varchar(50) NOT NULL COMMENT '进程名称',
    machine_name  bigint      NOT NULL COMMENT '机器名称',
    module        varchar(50) NOT NULL COMMENT '模块名称',
    config_json   text        NOT NULL COMMENT 'logstash配置文件JSON',
    doris_sql     text        NULL COMMENT '与logstash配置文件JSON对应的doris日志表 sql',
    datasource_id bigint      NOT NULL COMMENT '关联的数据源ID',
    `state`       varchar(50) NOT NULL COMMENT '状态，枚举有：未启动，正在启动，运行中，失败，四种状态枚举',
    create_time   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY `uk_name` (`name`),
    CONSTRAINT fk_logstash_datasource FOREIGN KEY (datasource_id) REFERENCES datasource (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = 'logstash进程任务信息表';

-- Logstash部署在机器上的关联表
CREATE TABLE `logstash_machine`
(
    id                  bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    logstash_process_id bigint NOT NULL COMMENT 'logstash_process 的 id',
    machine_id          bigint NOT NULL COMMENT 'machine 的 id',
    create_time         datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY `uk_logstash_machine` (`logstash_process_id`, `machine_id`),
    CONSTRAINT fk_lm_logstash FOREIGN KEY (logstash_process_id) REFERENCES logstash_process (id) ON DELETE CASCADE,
    CONSTRAINT fk_lm_machine FOREIGN KEY (machine_id) REFERENCES machine (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = 'logstash部署在机器上的关联表'; 