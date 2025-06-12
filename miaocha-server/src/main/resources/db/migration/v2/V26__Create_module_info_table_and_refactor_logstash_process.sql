-- =============================================
-- V26: 创建module_info表并重构logstash_process表
-- 将模块管理从Logstash管理中拆分出来
-- =============================================

-- 1. 创建模块信息表
CREATE TABLE module_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模块ID',
    name VARCHAR(100) NOT NULL COMMENT '模块名称',
    datasource_id BIGINT NOT NULL COMMENT '数据源ID',
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    doris_sql TEXT COMMENT 'Doris SQL语句',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_user VARCHAR(50) COMMENT '创建人',
    update_user VARCHAR(50) COMMENT '修改人',
    UNIQUE KEY uk_name (name) COMMENT '模块名称唯一索引',
    KEY idx_datasource_id (datasource_id) COMMENT '数据源ID索引'
) COMMENT='模块信息表';

-- 2. 将logstash_process表中的模块数据迁移到module_info表
INSERT INTO module_info (name, datasource_id, table_name, doris_sql, create_user, update_user)
SELECT DISTINCT 
    module as name,
    datasource_id,
    table_name,
    doris_sql,
    'admin@hinadt.com' as create_user,
    'admin@hinadt.com' as update_user
FROM logstash_process 
WHERE module IS NOT NULL AND module != '';

-- 3. 为logstash_process表添加module_id字段
ALTER TABLE logstash_process ADD COLUMN module_id BIGINT COMMENT '关联的模块ID';

-- 4. 更新logstash_process表的module_id字段
UPDATE logstash_process lp 
SET module_id = (
    SELECT mi.id 
    FROM module_info mi 
    WHERE mi.name = lp.module 
    AND mi.datasource_id = lp.datasource_id 
    AND mi.table_name = lp.table_name
    LIMIT 1
) 
WHERE lp.module IS NOT NULL AND lp.module != '';

-- 5. 为logstash_process表的创建人和修改人字段
ALTER TABLE logstash_process ADD COLUMN create_user VARCHAR(50) COMMENT '创建人';
ALTER TABLE logstash_process ADD COLUMN update_user VARCHAR(50) COMMENT '修改人';

-- 6. 初始化用户信息
UPDATE logstash_process SET create_user = 'admin@hinadt.com', update_user = 'admin@hinadt.com' WHERE create_user IS NULL;

-- 7. 删除logstash_process表中的冗余字段
ALTER TABLE logstash_process DROP COLUMN module;
ALTER TABLE logstash_process DROP COLUMN datasource_id;
ALTER TABLE logstash_process DROP COLUMN table_name;
ALTER TABLE logstash_process DROP COLUMN doris_sql;

-- 8. 为其他相关表添加创建人和修改人字段
ALTER TABLE datasource_info
ADD COLUMN create_user VARCHAR(50) NULL COMMENT '创建人' AFTER update_time,
ADD COLUMN update_user VARCHAR(50) NULL COMMENT '修改人' AFTER create_user;

ALTER TABLE machine_info
ADD COLUMN create_user VARCHAR(50) NULL COMMENT '创建人' AFTER update_time,
ADD COLUMN update_user VARCHAR(50) NULL COMMENT '修改人' AFTER create_user;

ALTER TABLE logstash_machine
ADD COLUMN create_user VARCHAR(50) NULL COMMENT '创建人' AFTER update_time,
ADD COLUMN update_user VARCHAR(50) NULL COMMENT '修改人' AFTER create_user;

ALTER TABLE user_module_permission
ADD COLUMN create_user VARCHAR(50) NULL COMMENT '创建人' AFTER update_time,
ADD COLUMN update_user VARCHAR(50) NULL COMMENT '修改人' AFTER create_user;

-- 9. 初始化现有数据的审计字段
UPDATE datasource_info SET create_user = 'admin@hinadt.com', update_user = 'admin@hinadt.com' WHERE create_user IS NULL;
UPDATE machine_info SET create_user = 'admin@hinadt.com', update_user = 'admin@hinadt.com' WHERE create_user IS NULL;
UPDATE logstash_machine SET create_user = 'admin@hinadt.com', update_user = 'admin@hinadt.com' WHERE create_user IS NULL;
UPDATE user_module_permission SET create_user = 'admin@hinadt.com', update_user = 'admin@hinadt.com' WHERE create_user IS NULL;

-- 10. 为logstash_process表的module_id字段添加索引
ALTER TABLE logstash_process ADD KEY idx_module_id (module_id) COMMENT '模块ID索引'; 