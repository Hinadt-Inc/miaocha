package com.hinadt.miaocha.application.service.impl.logsearch.executor;

import com.hinadt.miaocha.application.service.impl.logsearch.template.LogSearchTemplate.SearchExecutor;
import com.hinadt.miaocha.application.service.impl.logsearch.template.SearchContext;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.application.service.sql.builder.LogSqlBuilder;
import com.hinadt.miaocha.application.service.sql.processor.QueryResult;
import com.hinadt.miaocha.application.service.sql.processor.ResultProcessor;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.exception.LogQueryException;
import com.hinadt.miaocha.domain.dto.logsearch.LogFieldDistributionResultDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTODecorator;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 字段分布搜索执行器
 *
 * <p>专门处理字段分布查询逻辑，统一通过LogSqlBuilder构建SQL，支持并行查询优化
 */
@Component
@Slf4j
public class FieldDistributionSearchExecutor extends BaseSearchExecutor
        implements SearchExecutor<LogFieldDistributionResultDTO> {

    private final LogSqlBuilder logSqlBuilder;
    private final ResultProcessor resultProcessor;

    public FieldDistributionSearchExecutor(
            JdbcQueryExecutor jdbcQueryExecutor,
            LogSqlBuilder logSqlBuilder,
            ResultProcessor resultProcessor,
            @Qualifier("logQueryExecutor") Executor logQueryExecutor) {
        super(jdbcQueryExecutor, logQueryExecutor);
        this.logSqlBuilder = logSqlBuilder;
        this.resultProcessor = resultProcessor;
    }

    @Override
    public LogFieldDistributionResultDTO execute(SearchContext context) throws LogQueryException {

        LogSearchDTO dto = context.getDto();
        String tableName = context.getTableName();
        Connection conn = context.getConnection();

        LogFieldDistributionResultDTO result = new LogFieldDistributionResultDTO();

        // 1. 获取装饰器信息
        LogSearchDTODecorator decorator = (LogSearchDTODecorator) dto;
        List<String> originalFields = decorator.getOriginalFields(); // 用于结果处理

        // 2. 构建字段分布查询SQL - 让SQL Builder自己处理字段转换
        String fieldDistributionSql =
                logSqlBuilder.buildFieldDistributionSql(
                        dto, tableName, null, null, 5); // 传null，让Builder自己处理

        log.debug("字段分布SQL: {}", fieldDistributionSql);

        // 3. 异步执行查询
        CompletableFuture<QueryResult> fieldDistributionFuture =
                executeQueryAsync(
                        conn,
                        fieldDistributionSql,
                        ErrorCode.LOG_FIELD_DISTRIBUTION_QUERY_FAILED,
                        "FieldDistributionQuery");

        try {
            // 等待查询完成
            QueryResult fieldDistributionResult = fieldDistributionFuture.get();

            // 4. 处理字段分布结果
            resultProcessor.processFieldDistributionResult(
                    fieldDistributionResult, result, originalFields);

            return result;
        } catch (ExecutionException | InterruptedException e) {
            // 其他异常（超时、中断等）让外层处理，转为BusinessException
            throw new RuntimeException(e);
        }
    }
}
