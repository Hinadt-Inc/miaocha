package com.hinadt.miaocha.application.service.sql.builder;

import static com.hinadt.miaocha.application.service.sql.builder.SqlFragment.*;

import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import org.springframework.stereotype.Component;

/**
 * 时间分布SQL构建器
 *
 * <p>专门负责构建时间分布统计相关的SQL
 */
@Component
public class DistributionSqlBuilder {

    private final KeywordConditionBuilder keywordConditionBuilder;

    public DistributionSqlBuilder(KeywordConditionBuilder keywordConditionBuilder) {
        this.keywordConditionBuilder = keywordConditionBuilder;
    }

    /** 构建自定义间隔时间分布SQL */
    public String buildCustomIntervalDistribution(
            LogSearchDTO dto,
            String tableName,
            String timeField,
            String timeUnit,
            int intervalValue) {

        String customBucketExpr = customTimeBucket(timeField, timeUnit, intervalValue);
        String keywordConditions = keywordConditionBuilder.buildKeywordConditions(dto);

        return selectTimeDistribution(customBucketExpr)
                + from(tableName)
                + where(timeRange(timeField, dto))
                + and(keywordConditions)
                + groupBy(customBucketExpr)
                + orderBy(TIME_ALIAS, "ASC");
    }
}
