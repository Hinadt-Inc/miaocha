package com.hinadt.miaocha.application.service.impl.logsearch.executor;

import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.application.service.sql.processor.QueryResult;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.exception.LogQueryException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;

/**
 * 搜索执行器基类
 *
 * <p>提供通用的异步查询逻辑，避免代码重复
 */
@Slf4j
public abstract class BaseSearchExecutor {

    protected final JdbcQueryExecutor jdbcQueryExecutor;
    protected final Executor logQueryExecutor;

    protected BaseSearchExecutor(JdbcQueryExecutor jdbcQueryExecutor, Executor logQueryExecutor) {
        this.jdbcQueryExecutor = jdbcQueryExecutor;
        this.logQueryExecutor = logQueryExecutor;
    }

    /**
     * 异步执行查询 - 统一的异步查询方法
     *
     * @param conn 数据库连接
     * @param sql SQL语句
     * @param errorCode 错误码
     * @param queryType 查询类型（用于日志记录）
     * @return 异步查询结果
     */
    protected CompletableFuture<QueryResult> executeQueryAsync(
            Connection conn, String sql, ErrorCode errorCode, String queryType) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return jdbcQueryExecutor.executeStructuredQuery(conn, sql);
                    } catch (SQLException e) {
                        log.error("{} SQL执行失败: {}", queryType, e.getMessage(), e);
                        throw new LogQueryException(
                                errorCode,
                                queryType,
                                sql,
                                queryType + "执行失败: " + e.getMessage(),
                                e);
                    }
                    // 其他异常不在这里处理，让CompletableFuture传播出去
                },
                logQueryExecutor);
    }
}
