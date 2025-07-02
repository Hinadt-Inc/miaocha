package com.hinadt.miaocha.application.service.impl.logsearch.executor;

import com.hinadt.miaocha.application.service.impl.logsearch.template.LogSearchTemplate.SearchExecutor;
import com.hinadt.miaocha.application.service.impl.logsearch.template.SearchContext;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.application.service.sql.builder.LogSqlBuilder;
import com.hinadt.miaocha.application.service.sql.processor.QueryResult;
import com.hinadt.miaocha.application.service.sql.processor.ResultProcessor;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.exception.LogQueryException;
import com.hinadt.miaocha.domain.dto.logsearch.LogDetailResultDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import java.sql.Connection;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 详情搜索执行器
 *
 * <p>专门处理日志详情查询逻辑，支持并行查询优化
 */
@Component
@Slf4j
public class DetailSearchExecutor extends BaseSearchExecutor
        implements SearchExecutor<LogDetailResultDTO> {

    private final LogSqlBuilder logSqlBuilder;
    private final ResultProcessor resultProcessor;

    public DetailSearchExecutor(
            JdbcQueryExecutor jdbcQueryExecutor,
            LogSqlBuilder logSqlBuilder,
            ResultProcessor resultProcessor,
            @Qualifier("logQueryExecutor") Executor logQueryExecutor) {
        super(jdbcQueryExecutor, logQueryExecutor);
        this.logSqlBuilder = logSqlBuilder;
        this.resultProcessor = resultProcessor;
    }

    @Override
    public LogDetailResultDTO execute(SearchContext context) throws LogQueryException {

        LogSearchDTO dto = context.getDto();
        String tableName = context.getTableName();
        Connection conn = context.getConnection();
        String timeField = context.getTimeField();

        LogDetailResultDTO result = new LogDetailResultDTO();

        // 构建SQL
        String detailSql = logSqlBuilder.buildDetailQuery(dto, tableName, timeField);
        String countSql = logSqlBuilder.buildCountQuery(dto, tableName, timeField);

        log.debug("详细日志SQL: {}", detailSql);
        log.debug("总数SQL: {}", countSql);

        // 并行执行两个查询
        CompletableFuture<QueryResult> detailFuture =
                executeQueryAsync(
                        conn, detailSql, ErrorCode.LOG_DETAIL_QUERY_FAILED, "DetailQuery");
        CompletableFuture<QueryResult> countFuture =
                executeQueryAsync(conn, countSql, ErrorCode.LOG_COUNT_QUERY_FAILED, "CountQuery");

        try {
            // 等待两个查询完成，设置超时时间
            QueryResult detailQueryResult = detailFuture.get();
            QueryResult countQueryResult = countFuture.get();

            resultProcessor.processDetailResult(detailQueryResult, result);
            long totalCount = resultProcessor.processTotalCountResult(countQueryResult);
            result.setTotalCount(totalCount);

            return result;
        } catch (ExecutionException | InterruptedException e) {
            // 其他异常（超时、中断等）让外层处理，转为BusinessException
            throw new RuntimeException(e);
        }
    }
}
