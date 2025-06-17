-- =============================================
-- V28: 将数据源字段合并为JDBC URL
-- 将 ip, port, database, jdbc_params 字段合并为单个 jdbc_url 字段
-- =============================================

-- 1. 添加新的 jdbc_url 字段
ALTER TABLE datasource_info
ADD COLUMN jdbc_url VARCHAR(512) NOT NULL DEFAULT '' COMMENT 'JDBC连接URL' AFTER description;

-- 2. 数据迁移：根据现有字段构建 jdbc_url
-- 迁移 MySQL/Doris 类型的数据源
UPDATE datasource_info
SET jdbc_url = CASE
    WHEN type IN ('MYSQL', 'DORIS') THEN
        CONCAT('jdbc:mysql://', ip, ':', port, '/', `database`,
               CASE
                   WHEN jdbc_params IS NOT NULL AND jdbc_params != '' THEN
                       CONCAT('?', REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                           jdbc_params,
                           '{', ''),
                           '}', ''),
                           '"', ''),
                           ':', '='),
                           ',', '&'))
                   ELSE ''
               END)
    WHEN type = 'POSTGRESQL' THEN
        CONCAT('jdbc:postgresql://', ip, ':', port, '/', `database`,
               CASE
                   WHEN jdbc_params IS NOT NULL AND jdbc_params != '' THEN
                       CONCAT('?', REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                           jdbc_params,
                           '{', ''),
                           '}', ''),
                           '"', ''),
                           ':', '='),
                           ',', '&'))
                   ELSE ''
               END)
    WHEN type = 'ORACLE' THEN
        CONCAT('jdbc:oracle:thin:@', ip, ':', port, ':', `database`)
    ELSE
        CONCAT('jdbc:', LOWER(type), '://', ip, ':', port, '/', `database`)
END
WHERE ip IS NOT NULL AND port IS NOT NULL AND `database` IS NOT NULL;

-- 3. 对于有复杂 jdbc_params 的数据，进行手动清理（简化版本）
-- 这里主要处理常见的参数格式
UPDATE datasource_info
SET jdbc_url = CASE
    WHEN jdbc_url LIKE '%connectTimeout%' THEN
        REPLACE(jdbc_url, 'connectTimeout', 'connectTimeout')
    WHEN jdbc_url LIKE '%useSSL%' THEN
        REPLACE(jdbc_url, 'useSSL', 'useSSL')
    ELSE jdbc_url
END;

-- 4. 删除旧字段
ALTER TABLE datasource_info
DROP COLUMN ip,
DROP COLUMN port,
DROP COLUMN `database`,
DROP COLUMN jdbc_params;

-- 5. 修改 jdbc_url 字段为 NOT NULL（现在应该都有值了）
ALTER TABLE datasource_info
MODIFY COLUMN jdbc_url TEXT NOT NULL COMMENT 'JDBC连接URL';
