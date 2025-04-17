-- 数据源表
CREATE TABLE datasource (
    id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    name varchar(100) NOT NULL COMMENT '数据源名称',
    type varchar(50) NOT NULL COMMENT '数据源类型',
    description text DEFAULT NULL COMMENT '数据源描述',
    ip varchar(100) NOT NULL COMMENT '数据源IP',
    port int(11) NOT NULL COMMENT '数据源端口',
    username varchar(100) NOT NULL COMMENT '数据源用户名',
    password varchar(100) NOT NULL COMMENT '数据源密码',
    database varchar(100) NOT NULL COMMENT '数据源数据库',
    jdbc_params text DEFAULT NULL COMMENT '数据源JDBC参数,JSON格式',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源表';

-- 用户表
CREATE TABLE user (
    id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    nickname varchar(100) NOT NULL COMMENT '用户昵称',
    uid varchar(100) NOT NULL COMMENT '用户ID',
    is_admin tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否管理员',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_uid (uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户数据源权限表
CREATE TABLE user_datasource_permission (
    id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id bigint(20) NOT NULL COMMENT '用户ID',
    datasource_id bigint(20) NOT NULL COMMENT '数据源ID',
    table_name varchar(100) NOT NULL COMMENT '表名, * 表示所有表',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_datasource_table (user_id, datasource_id, table_name),
    CONSTRAINT fk_permission_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    CONSTRAINT fk_permission_datasource FOREIGN KEY (datasource_id) REFERENCES datasource (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户数据源权限表';

-- SQL查询历史表
CREATE TABLE sql_query_history (
    id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id bigint(20) NOT NULL COMMENT '用户ID',
    datasource_id bigint(20) NOT NULL COMMENT '数据源ID',
    table_name varchar(100) NOT NULL COMMENT '表名',
    sql_query text NOT NULL COMMENT 'SQL查询语句',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    CONSTRAINT fk_history_datasource FOREIGN KEY (datasource_id) REFERENCES datasource (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL查询历史表';

-- 插入默认管理员用户
INSERT INTO user (nickname, uid, is_admin) VALUES ('系统管理员', 'admin', 1); 