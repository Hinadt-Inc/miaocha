package com.hina.log.domain.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 数据源类型枚举
 */
@Getter
@Schema(description = "数据源类型枚举")
public enum DatasourceType {
    @Schema(description = "MySQL数据库")
    MYSQL("mysql", "jdbc:mysql://%s:%d/%s"),

    @Schema(description = "PostgreSQL数据库")
    POSTGRESQL("postgresql", "jdbc:postgresql://%s:%d/%s"),

    @Schema(description = "Oracle数据库")
    ORACLE("oracle", "jdbc:oracle:thin:@%s:%d:%s"),

    @Schema(description = "Apache Doris数据库")
    DORIS("doris", "jdbc:mysql://%s:%d/%s");

    private final String type;
    private final String urlTemplate;

    DatasourceType(String type, String urlTemplate) {
        this.type = type;
        this.urlTemplate = urlTemplate;
    }

    /**
     * 根据类型获取枚举
     */
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

    /**
     * 构建基础JDBC URL（不包含额外参数）
     */
    private String buildBaseJdbcUrl(String ip, int port, String database) {
        return String.format(urlTemplate, ip, port, database);
    }

    /**
     * 构建完整JDBC URL（包含额外参数）
     * 
     * @param ip         数据库IP
     * @param port       数据库端口
     * @param database   数据库名称
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

    /**
     * 构建JDBC URL（向后兼容的方法）
     */
    public String buildJdbcUrl(String ip, int port, String database) {
        return buildJdbcUrl(ip, port, database, null);
    }
}