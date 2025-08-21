package com.hinadt.miaocha.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/** 数据源类型枚举 */
@Getter
@Schema(description = "数据源类型枚举")
public enum DatasourceType {
    @Schema(description = "MySQL数据库")
    MYSQL("mysql", "jdbc:mysql://%s:%d/%s", "com.mysql.cj.jdbc.Driver"),

    @Schema(description = "Apache Doris数据库")
    DORIS("doris", "jdbc:mysql://%s:%d/%s", "com.mysql.cj.jdbc.Driver");

    private final String type;
    private final String urlTemplate;
    private final String driverClassName;

    DatasourceType(String type, String urlTemplate, String driverClassName) {
        this.type = type;
        this.urlTemplate = urlTemplate;
        this.driverClassName = driverClassName;
    }

    /** 根据类型获取枚举 */
    public static DatasourceType fromType(String type) {
        if (type == null) {
            return null;
        }

        for (DatasourceType datasourceType : DatasourceType.values()) {
            if (datasourceType.getType().equalsIgnoreCase(type)) {
                return datasourceType;
            }
        }
        return null;
    }

    /** 构建基础JDBC URL（不包含额外参数） */
    private String buildBaseJdbcUrl(String ip, int port, String database) {
        return String.format(urlTemplate, ip, port, database);
    }

    /**
     * 构建完整JDBC URL（包含额外参数）
     *
     * @param ip 数据库IP
     * @param port 数据库端口
     * @param database 数据库名称
     * @param jdbcParams 额外的JDBC参数
     * @return 完整的JDBC URL
     */
    public String buildJdbcUrl(String ip, int port, String database, String jdbcParams) {
        String baseUrl = buildBaseJdbcUrl(ip, port, database);

        if (jdbcParams == null || jdbcParams.isEmpty()) {
            return baseUrl;
        }

        // 添加JDBC参数
        if (baseUrl.contains("?")) {
            return baseUrl + "&" + jdbcParams;
        } else {
            return baseUrl + "?" + jdbcParams;
        }
    }

    /** 构建JDBC URL（向后兼容的方法） */
    public String buildJdbcUrl(String ip, int port, String database) {
        return buildJdbcUrl(ip, port, database, null);
    }

    /**
     * 从 JDBC URL 中解析数据库名称
     *
     * @param jdbcUrl JDBC连接URL
     * @return 数据库名称，如果解析失败则返回空字符串
     */
    public static String extractDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            return "";
        }

        try {
            // 处理 MySQL/Doris 格式: jdbc:mysql://host:port/database?params
            if (jdbcUrl.startsWith("jdbc:mysql://")) {
                String afterProtocol = jdbcUrl.substring(jdbcUrl.indexOf("://") + 3);
                int slashIndex = afterProtocol.indexOf('/');
                if (slashIndex > 0 && slashIndex < afterProtocol.length() - 1) {
                    String afterSlash = afterProtocol.substring(slashIndex + 1);
                    int questionIndex = afterSlash.indexOf('?');
                    return questionIndex > 0 ? afterSlash.substring(0, questionIndex) : afterSlash;
                }
            }
        } catch (Exception e) {
            // 如果解析失败，返回空字符串
        }

        return "";
    }
}
