package com.hinadt.miaocha.application.service.datasource;

import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/**
 * HikariCP 数据源管理器
 *
 * <p>负责管理多个数据源的连接池，提供连接获取、缓存管理、资源释放等功能。
 *
 * <p>主要特性：
 *
 * <ul>
 *   <li>线程安全的连接池缓存管理
 *   <li>数据源配置变更时自动失效对应连接池
 *   <li>优雅的资源释放和连接池关闭
 *   <li>连接池健康状态监控
 * </ul>
 *
 * @author miaocha
 */
@Slf4j
@Component
public class HikariDatasourceManager implements DisposableBean {

    /** 数据源连接池缓存，key 为数据源配置的唯一标识 */
    private final ConcurrentHashMap<String, HikariDataSource> datasourceCache =
            new ConcurrentHashMap<>();

    /** 读写锁，保证缓存操作的线程安全 */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    /** 连接池配置提供者 */
    private final HikariConfigProvider configProvider;

    public HikariDatasourceManager(HikariConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    /**
     * 获取数据库连接
     *
     * @param datasourceInfo 数据源信息
     * @return 数据库连接
     * @throws SQLException 如果获取连接失败
     */
    public Connection getConnection(DatasourceInfo datasourceInfo) throws SQLException {
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "数据源信息不能为空");
        }

        String cacheKey = buildCacheKey(datasourceInfo);
        HikariDataSource dataSource = getOrCreateDataSource(cacheKey, datasourceInfo);

        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            log.error("获取数据源连接失败，数据源: {}, 错误: {}", datasourceInfo.getName(), e.getMessage());
            // 如果连接失败，可能是连接池有问题，移除缓存的数据源
            invalidateDataSource(cacheKey);
            throw new BusinessException(
                    ErrorCode.DATASOURCE_CONNECTION_FAILED, "获取数据库连接失败: " + e.getMessage());
        }
    }

    /**
     * 使数据源连接池失效
     *
     * @param datasourceInfo 数据源信息
     */
    public void invalidateDataSource(DatasourceInfo datasourceInfo) {
        if (datasourceInfo == null) {
            return;
        }

        String cacheKey = buildCacheKey(datasourceInfo);
        invalidateDataSource(cacheKey);
    }

    /**
     * 根据数据源ID使连接池失效
     *
     * @param datasourceId 数据源ID
     */
    public void invalidateDataSourceById(Long datasourceId) {
        if (datasourceId == null) {
            return;
        }

        String cacheKey = String.valueOf(datasourceId);
        invalidateDataSource(cacheKey);
    }

    /**
     * 使指定缓存键的数据源连接池失效
     *
     * @param cacheKey 缓存键
     */
    private void invalidateDataSource(String cacheKey) {
        writeLock.lock();
        try {
            HikariDataSource dataSource = datasourceCache.remove(cacheKey);
            if (dataSource != null) {
                log.info("正在关闭数据源连接池: {}", cacheKey);
                closeDataSourceSafely(dataSource);
                log.info("数据源连接池已关闭: {}", cacheKey);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /** 清除所有缓存的数据源连接池 */
    public void clearAllDataSources() {
        writeLock.lock();
        try {
            log.info("正在清除所有数据源连接池，当前缓存数量: {}", datasourceCache.size());
            datasourceCache.forEach(
                    (key, dataSource) -> {
                        log.info("正在关闭数据源连接池: {}", key);
                        closeDataSourceSafely(dataSource);
                    });
            datasourceCache.clear();
            log.info("所有数据源连接池已清除");
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 获取或创建数据源
     *
     * @param cacheKey 缓存键
     * @param datasourceInfo 数据源信息
     * @return HikariDataSource 实例
     */
    private HikariDataSource getOrCreateDataSource(String cacheKey, DatasourceInfo datasourceInfo) {
        // 首先尝试从缓存获取
        readLock.lock();
        try {
            HikariDataSource cached = datasourceCache.get(cacheKey);
            if (cached != null && !cached.isClosed()) {
                return cached;
            }
        } finally {
            readLock.unlock();
        }

        // 使用写锁创建新的数据源
        writeLock.lock();
        try {
            // 双重检查锁定模式
            HikariDataSource cached = datasourceCache.get(cacheKey);
            if (cached != null && !cached.isClosed()) {
                return cached;
            }

            // 如果缓存中的数据源已关闭，先移除
            if (cached != null && cached.isClosed()) {
                datasourceCache.remove(cacheKey);
            }

            // 创建新的数据源
            log.info("正在创建新的数据源连接池: {}", cacheKey);
            HikariDataSource newDataSource = createDataSource(datasourceInfo);
            datasourceCache.put(cacheKey, newDataSource);
            log.info("数据源连接池创建成功: {}", cacheKey);

            return newDataSource;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 创建 HikariDataSource 实例
     *
     * @param datasourceInfo 数据源信息
     * @return HikariDataSource 实例
     */
    private HikariDataSource createDataSource(DatasourceInfo datasourceInfo) {
        try {
            HikariConfig config = configProvider.createConfig(datasourceInfo);
            return new HikariDataSource(config);
        } catch (Exception e) {
            log.error("创建数据源连接池失败，数据源: {}, 错误: {}", datasourceInfo.getName(), e.getMessage(), e);
            throw new BusinessException(
                    ErrorCode.DATASOURCE_CONNECTION_FAILED, "创建数据源连接池失败: " + e.getMessage());
        }
    }

    /**
     * 构建缓存键
     *
     * @param datasourceInfo 数据源信息
     * @return 缓存键
     */
    private String buildCacheKey(DatasourceInfo datasourceInfo) {
        // 直接使用数据源ID作为缓存键，确保数据源信息变更时能正确失效对应连接池
        return String.valueOf(datasourceInfo.getId());
    }

    /**
     * 安全地关闭数据源
     *
     * @param dataSource 要关闭的数据源
     */
    private void closeDataSourceSafely(HikariDataSource dataSource) {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
        } catch (Exception e) {
            log.warn("关闭数据源时发生异常: {}", e.getMessage(), e);
        }
    }

    /** Spring 容器销毁时调用，清理所有资源 */
    @Override
    public void destroy() {
        log.info("HikariDatasourceManager 正在销毁，清理所有 Doris 数据源连接池");
        clearAllDataSources();
    }
}
