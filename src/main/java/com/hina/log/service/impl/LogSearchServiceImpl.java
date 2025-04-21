package com.hina.log.service.impl;

import com.hina.log.dto.LogSearchDTO;
import com.hina.log.dto.LogSearchResultDTO;
import com.hina.log.dto.SchemaInfoDTO;
import com.hina.log.entity.Datasource;
import com.hina.log.entity.User;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.DatasourceMapper;
import com.hina.log.mapper.UserMapper;
import com.hina.log.service.LogSearchService;
import com.hina.log.service.sql.JdbcQueryExecutor;
import com.hina.log.service.sql.builder.LogSqlBuilder;
import com.hina.log.service.database.DatabaseMetadataService;
import com.hina.log.service.database.DatabaseMetadataServiceFactory;
import com.hina.log.service.sql.processor.ResultProcessor;
import com.hina.log.service.sql.processor.TimeRangeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * 日志检索服务实现类
 */
@Service
public class LogSearchServiceImpl implements LogSearchService {
    private static final Logger logger = LoggerFactory.getLogger(LogSearchServiceImpl.class);

    @Autowired
    private DatasourceMapper datasourceMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcQueryExecutor jdbcQueryExecutor;

    @Autowired
    private QueryPermissionChecker permissionChecker;

    @Autowired
    private TimeRangeProcessor timeRangeProcessor;

    @Autowired
    private LogSqlBuilder logSqlBuilder;

    @Autowired
    private ResultProcessor resultProcessor;

    @Autowired
    private DatabaseMetadataServiceFactory metadataServiceFactory;

    @Autowired(required = false)
    @Qualifier("logQueryExecutor")
    private Executor logQueryExecutor;

    /**
     * 获取线程执行器，如果注入的执行器为空（如在测试环境中），则使用公共线程池
     */
    private Executor getExecutor() {
        return logQueryExecutor != null ? logQueryExecutor : ForkJoinPool.commonPool();
    }

    @Override
    @Transactional
    public LogSearchResultDTO search(Long userId, LogSearchDTO dto) {
        // 获取数据源
        Datasource datasource = datasourceMapper.selectById(dto.getDatasourceId());
        if (datasource == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 使用权限检查器验证用户权限 (实际逻辑可以先fake)
        // TODO: 实现实际的权限检查
        // permissionChecker.checkQueryPermission(user, dto.getDatasourceId(), null);

        // 处理时间范围
        timeRangeProcessor.processTimeRange(dto);

        // 构建查询SQL
        String timeUnit = timeRangeProcessor.determineTimeUnit(dto);
        String distributionSql = logSqlBuilder.buildDistributionSql(dto, timeUnit);
        String detailSql = logSqlBuilder.buildDetailSql(dto);

        LogSearchResultDTO result = new LogSearchResultDTO();
        long startTime = System.currentTimeMillis();

        try {
            // 为了单元测试的兼容性，检查是否在测试环境中
            if (logQueryExecutor == null) {
                // 测试环境，同步执行
                executeLogSearchSync(datasource, distributionSql, detailSql, result);
            } else {
                // 生产环境，异步执行
                executeLogSearchAsync(datasource, distributionSql, detailSql, result);
            }
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException) {
                throw (BusinessException) cause;
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "执行日志查询失败: " + (cause != null ? cause.getMessage() : e.getMessage()));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "执行日志查询失败: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        result.setExecutionTimeMs(endTime - startTime);
        logger.info("日志查询完成，耗时: {}ms", result.getExecutionTimeMs());

        return result;
    }

    /**
     * 同步执行日志搜索查询 (用于测试)
     */
    private void executeLogSearchSync(Datasource datasource, String distributionSql, String detailSql,
                                      LogSearchResultDTO result) throws SQLException {
        try (Connection conn = jdbcQueryExecutor.getConnection(datasource)) {
            // 执行日志分布查询
            logger.debug("执行日志分布查询SQL: {}", distributionSql);
            resultProcessor.processDistributionResult(conn, distributionSql, result);

            // 执行日志详情查询
            logger.debug("执行日志详情查询SQL: {}", detailSql);
            resultProcessor.processDetailResult(conn, detailSql, result);
        }
    }

    /**
     * 异步执行日志搜索查询 (用于生产)
     */
    private void executeLogSearchAsync(Datasource datasource, String distributionSql, String detailSql,
                                       LogSearchResultDTO result) {
        // 创建两个并行任务
        CompletableFuture<Void> distributionFuture = createQueryTask(
                () -> executeDistributionQuery(datasource, distributionSql, result),
                "日志分布查询");

        CompletableFuture<Void> detailFuture = createQueryTask(
                () -> executeDetailQuery(datasource, detailSql, result),
                "日志详情查询");

        // 等待所有任务完成
        CompletableFuture.allOf(distributionFuture, detailFuture)
                .exceptionally(throwable -> {
                    logger.error("日志查询过程中发生异常", throwable);
                    throw new CompletionException(throwable);
                })
                .join();
    }

    /**
     * 创建异步查询任务
     *
     * @param supplier 查询任务
     * @param taskName 任务名称（用于日志）
     * @return CompletableFuture
     */
    private CompletableFuture<Void> createQueryTask(Supplier<Void> supplier, String taskName) {
        return CompletableFuture.supplyAsync(supplier, getExecutor())
                .exceptionally(throwable -> {
                    logger.error("{}任务执行失败", taskName, throwable);
                    if (throwable instanceof CompletionException && throwable.getCause() != null) {
                        throwable = throwable.getCause();
                    }
                    if (throwable instanceof BusinessException) {
                        throw (BusinessException) throwable;
                    }
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, taskName + "失败: " + throwable.getMessage());
                });
    }

    /**
     * 执行日志分布查询
     */
    private Void executeDistributionQuery(Datasource datasource, String sql, LogSearchResultDTO result) {
        try (Connection conn = jdbcQueryExecutor.getConnection(datasource)) {
            logger.debug("执行日志分布查询SQL: {}", sql);
            resultProcessor.processDistributionResult(conn, sql, result);
            return null;
        } catch (SQLException e) {
            logger.error("执行日志分布查询失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "执行日志分布查询失败: " + e.getMessage());
        }
    }

    /**
     * 执行日志详情查询
     */
    private Void executeDetailQuery(Datasource datasource, String sql, LogSearchResultDTO result) {
        try (Connection conn = jdbcQueryExecutor.getConnection(datasource)) {
            logger.debug("执行日志详情查询SQL: {}", sql);
            resultProcessor.processDetailResult(conn, sql, result);
            return null;
        } catch (SQLException e) {
            logger.error("执行日志详情查询失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "执行日志详情查询失败: " + e.getMessage());
        }
    }

    @Override
    public List<SchemaInfoDTO.ColumnInfoDTO> getTableColumns(Long userId, Long datasourceId, String tableName) {
        // 获取数据源
        Datasource datasource = datasourceMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        try (Connection conn = jdbcQueryExecutor.getConnection(datasource)) {
            // 获取对应数据库类型的元数据服务
            DatabaseMetadataService metadataService = metadataServiceFactory.getService(datasource.getType());

            // 获取表的完整字段信息
            return metadataService.getColumnInfo(conn, tableName);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "获取表结构失败: " + e.getMessage());
        }
    }
}