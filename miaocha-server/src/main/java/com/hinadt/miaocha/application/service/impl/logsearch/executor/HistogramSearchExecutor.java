package com.hinadt.miaocha.application.service.impl.logsearch.executor;

import com.hinadt.miaocha.application.service.impl.logsearch.template.LogSearchTemplate.SearchExecutor;
import com.hinadt.miaocha.application.service.impl.logsearch.template.SearchContext;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.application.service.sql.builder.LogSqlBuilder;
import com.hinadt.miaocha.application.service.sql.processor.QueryResult;
import com.hinadt.miaocha.application.service.sql.processor.ResultProcessor;
import com.hinadt.miaocha.application.service.sql.processor.TimeGranularityCalculator;
import com.hinadt.miaocha.application.service.sql.processor.TimeRangeProcessor;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.exception.LogQueryException;
import com.hinadt.miaocha.domain.dto.LogHistogramResultDTO;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import java.sql.Connection;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 直方图搜索执行器
 *
 * <p>专门处理时间分布查询逻辑，支持并行查询优化
 */
@Component
@Slf4j
public class HistogramSearchExecutor extends BaseSearchExecutor
        implements SearchExecutor<LogHistogramResultDTO> {

    private final LogSqlBuilder logSqlBuilder;
    private final ResultProcessor resultProcessor;
    private final TimeRangeProcessor timeRangeProcessor;

    public HistogramSearchExecutor(
            JdbcQueryExecutor jdbcQueryExecutor,
            LogSqlBuilder logSqlBuilder,
            ResultProcessor resultProcessor,
            TimeRangeProcessor timeRangeProcessor,
            @Qualifier("logQueryExecutor") Executor logQueryExecutor) {
        super(jdbcQueryExecutor, logQueryExecutor);
        this.logSqlBuilder = logSqlBuilder;
        this.resultProcessor = resultProcessor;
        this.timeRangeProcessor = timeRangeProcessor;
    }

    @Override
    public LogHistogramResultDTO execute(SearchContext context) throws LogQueryException {

        LogSearchDTO dto = context.getDto();
        String tableName = context.getTableName();
        Connection conn = context.getConnection();

        LogHistogramResultDTO result = new LogHistogramResultDTO();

        // 1. 计算最优时间颗粒度
        TimeGranularityCalculator.TimeGranularityResult granularityResult =
                timeRangeProcessor.calculateOptimalTimeGranularity(dto, dto.getTargetBuckets());

        // 2. 构建分布统计查询SQL
        String distributionSql =
                logSqlBuilder.buildDistributionSqlWithInterval(
                        dto,
                        tableName,
                        granularityResult.getTimeUnit(),
                        granularityResult.getInterval());

        log.debug(
                "分布统计SQL: {}, 颗粒度详情: {}",
                distributionSql,
                granularityResult.getDetailedDescription());

        // 3. 异步执行查询
        CompletableFuture<QueryResult> distributionFuture =
                executeQueryAsync(
                        conn,
                        distributionSql,
                        ErrorCode.LOG_HISTOGRAM_QUERY_FAILED,
                        "HistogramQuery");

        try {
            // 等待查询完成
            QueryResult distributionQueryResult = distributionFuture.get();
            resultProcessor.processDistributionResult(distributionQueryResult, result);

            // 4. 设置时间颗粒度信息
            setGranularityInfo(result, granularityResult);

            return result;
        } catch (ExecutionException | InterruptedException e) {
            // 其他异常（超时、中断等）让外层处理，转为BusinessException
            throw new RuntimeException(e);
        }
    }

    /** 设置时间颗粒度相关信息 */
    private void setGranularityInfo(
            LogHistogramResultDTO result,
            TimeGranularityCalculator.TimeGranularityResult granularityResult) {
        result.setTimeUnit(granularityResult.getTimeUnit());
        result.setTimeInterval(granularityResult.getInterval());
        result.setEstimatedBuckets(granularityResult.getEstimatedBuckets());
        result.setCalculationMethod(granularityResult.getCalculationMethod());

        // 计算实际桶数量
        if (result.getDistributionData() != null) {
            result.setActualBuckets(result.getDistributionData().size());
        }
    }
}
