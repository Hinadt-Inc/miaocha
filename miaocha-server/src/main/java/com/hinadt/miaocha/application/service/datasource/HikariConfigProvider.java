package com.hinadt.miaocha.application.service.datasource;

import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.enums.DatasourceType;
import com.zaxxer.hikari.HikariConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * HikariCP 连接池配置提供者
 *
 * <p>为所有数据源提供统一的 HikariCP 配置。
 *
 * @author miaocha
 */
@Setter
@Getter
@Slf4j
@Component
@ConfigurationProperties(prefix = "miaocha.datasource.hikari")
public class HikariConfigProvider {

    /** 最大连接池大小 */
    private int maximumPoolSize = 100;

    /** 最小空闲连接数 */
    private int minimumIdle = 10;

    /** 获取连接超时时间（毫秒） */
    private long connectionTimeout = 30000;

    /** 空闲连接超时时间（毫秒） */
    private long idleTimeout = 600000;

    /** 连接最大存活时间（毫秒） */
    private long maxLifetime = 1800000;

    /** 连接验证查询 */
    private String connectionTestQuery = "SELECT 1";

    /** 验证超时时间（毫秒） */
    private long validationTimeout = 5000;

    /** 连接泄露检测阈值（毫秒），0 表示禁用 */
    private long leakDetectionThreshold = 60000;

    /** 是否自动提交 */
    private boolean autoCommit = true;

    /** 是否只读 */
    private boolean readOnly = false;

    /** 是否隔离内部查询 */
    private boolean isolateInternalQueries = false;

    /** 是否启用JMX监控，默认关闭 */
    private boolean enableJmxMonitoring = false;

    /**
     * 为指定数据源创建 HikariConfig
     *
     * @param datasourceInfo 数据源信息
     * @return HikariConfig 实例
     */
    public HikariConfig createConfig(DatasourceInfo datasourceInfo) {
        HikariConfig config = new HikariConfig();

        // 设置基础连接参数
        config.setJdbcUrl(datasourceInfo.getJdbcUrl());
        config.setUsername(datasourceInfo.getUsername());
        config.setPassword(datasourceInfo.getPassword());

        // 应用连接池配置
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTestQuery(connectionTestQuery);
        config.setValidationTimeout(validationTimeout);

        if (leakDetectionThreshold > 0) {
            config.setLeakDetectionThreshold(leakDetectionThreshold);
        }

        config.setAutoCommit(autoCommit);
        config.setReadOnly(readOnly);
        config.setIsolateInternalQueries(isolateInternalQueries);

        // 配置JMX监控
        if (enableJmxMonitoring) {
            config.setRegisterMbeans(true);
            log.debug("为数据源 {} 启用JMX监控", datasourceInfo.getName());
        } else {
            config.setRegisterMbeans(false);
        }

        // 根据数据库类型设置驱动类名
        setDriverClassName(config, datasourceInfo.getType());

        // 设置连接池名称，便于监控
        config.setPoolName(
                String.format(
                        "HikariPool-%s-%s", datasourceInfo.getName(), datasourceInfo.getId()));

        log.debug(
                "为数据源 {} 创建 HikariCP 配置: maxPoolSize={}, minIdle={}, connectionTimeout={}ms",
                datasourceInfo.getName(),
                maximumPoolSize,
                minimumIdle,
                connectionTimeout);

        return config;
    }

    /**
     * 根据数据库类型设置驱动类名
     *
     * @param config HikariConfig 实例
     * @param dbType 数据库类型字符串
     */
    private void setDriverClassName(HikariConfig config, String dbType) {
        if (dbType == null) {
            return;
        }

        DatasourceType datasourceType = DatasourceType.fromType(dbType);
        if (datasourceType != null) {
            config.setDriverClassName(datasourceType.getDriverClassName());
            log.debug("为数据库类型 {} 设置驱动类: {}", dbType, datasourceType.getDriverClassName());
        } else {
            log.warn("不支持的数据库类型: {}，仅支持 MySQL 和 Doris", dbType);
            // 对于不支持的类型，让 HikariCP 自动检测驱动
        }
    }

    // Getters and Setters for configuration properties

}
