-- =============================================
-- UNDO V28: 撤销JDBC URL字段合并
-- 将 jdbc_url 字段拆分回 ip, port, database, jdbc_params 字段
-- =============================================

-- 1. 添加回原来的字段
ALTER TABLE datasource_info 
ADD COLUMN ip varchar(128) NOT NULL DEFAULT '' COMMENT '数据源IP' AFTER description,
ADD COLUMN port int NOT NULL DEFAULT 0 COMMENT '数据源端口' AFTER ip,
ADD COLUMN `database` varchar(128) NOT NULL DEFAULT '' COMMENT '数据源数据库' AFTER port,
ADD COLUMN jdbc_params text DEFAULT NULL COMMENT '数据源JDBC参数,JSON格式' AFTER `database`;

-- 2. 数据迁移：从 jdbc_url 解析回原来的字段
-- 解析 MySQL/Doris 格式: jdbc:mysql://ip:port/database?params
UPDATE datasource_info 
SET 
    ip = CASE 
        WHEN jdbc_url REGEXP '^jdbc:[a-zA-Z]+://([^:]+):' THEN 
            SUBSTRING_INDEX(SUBSTRING_INDEX(SUBSTRING(jdbc_url FROM 'jdbc:[a-zA-Z]+://([^?]+)'), ':', 1), '//', -1)
        ELSE ''
    END,
    port = CASE 
        WHEN jdbc_url REGEXP '^jdbc:[a-zA-Z]+://[^:]+:([0-9]+)' THEN 
            CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(SUBSTRING(jdbc_url FROM '://[^:]+:([0-9]+)'), ':', -1), '/', 1) AS UNSIGNED)
        ELSE 0
    END,
    `database` = CASE 
        WHEN jdbc_url REGEXP '^jdbc:[a-zA-Z]+://[^/]+/([^?]+)' THEN 
            SUBSTRING_INDEX(SUBSTRING_INDEX(SUBSTRING(jdbc_url FROM '://[^/]+/([^?]*)'), '?', 1), '/', 1)
        WHEN jdbc_url REGEXP '^jdbc:oracle:thin:@[^:]+:[0-9]+:(.+)$' THEN 
            SUBSTRING(jdbc_url FROM '^jdbc:oracle:thin:@[^:]+:[0-9]+:(.+)$')
        ELSE ''
    END,
    jdbc_params = CASE 
        WHEN jdbc_url LIKE '%?%' THEN 
            CONCAT('{"', REPLACE(REPLACE(SUBSTRING_INDEX(jdbc_url, '?', -1), '&', '","'), '=', '":"'), '"}')
        ELSE NULL
    END
WHERE jdbc_url IS NOT NULL AND jdbc_url != '';

-- 3. 处理Oracle特殊格式: jdbc:oracle:thin:@ip:port:database
UPDATE datasource_info 
SET 
    ip = CASE 
        WHEN type = 'ORACLE' AND jdbc_url REGEXP '^jdbc:oracle:thin:@([^:]+):' THEN 
            SUBSTRING_INDEX(SUBSTRING_INDEX(SUBSTRING(jdbc_url FROM '@([^:]+):'), ':', 1), '@', -1)
        ELSE ip
    END,
    port = CASE 
        WHEN type = 'ORACLE' AND jdbc_url REGEXP '^jdbc:oracle:thin:@[^:]+:([0-9]+):' THEN 
            CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(SUBSTRING(jdbc_url FROM '@[^:]+:([0-9]+):'), ':', -1), ':', 1) AS UNSIGNED)
        ELSE port
    END
WHERE type = 'ORACLE' AND jdbc_url LIKE 'jdbc:oracle:thin:@%';

-- 4. 处理解析失败或格式不正确的数据
-- 对于无法解析的URL，设置默认值以避免NOT NULL约束错误
UPDATE datasource_info 
SET 
    ip = CASE WHEN ip = '' THEN 'localhost' ELSE ip END,
    port = CASE WHEN port = 0 THEN 3306 ELSE port END,
    `database` = CASE WHEN `database` = '' THEN 'default' ELSE `database` END
WHERE ip = '' OR port = 0 OR `database` = '';

-- 5. 修改字段为 NOT NULL（现在应该都有值了）
ALTER TABLE datasource_info 
MODIFY COLUMN ip varchar(128) NOT NULL COMMENT '数据源IP',
MODIFY COLUMN port int NOT NULL COMMENT '数据源端口',
MODIFY COLUMN `database` varchar(128) NOT NULL COMMENT '数据源数据库';

-- 6. 删除 jdbc_url 字段
ALTER TABLE datasource_info 
DROP COLUMN jdbc_url;

-- 注意：由于URL解析的复杂性，一些复杂的JDBC参数可能无法完全恢复
-- 建议在执行此撤销脚本前备份数据库
-- 执行后需要手动检查和修正解析后的数据 