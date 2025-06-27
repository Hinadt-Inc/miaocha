package com.hinadt.miaocha.application.service.sql.builder;

import com.hinadt.miaocha.application.service.impl.logsearch.validator.QueryConfigValidationService;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 日志SQL构建器门面
 *
 * <p>统一的SQL构建入口，委托给专门的构建器执行具体任务
 */
@Component
public class LogSqlBuilder {

    private final DistributionSqlBuilder distributionSqlBuilder;
    private final DetailSqlBuilder detailSqlBuilder;
    private final FieldDistributionSqlBuilder fieldDistributionSqlBuilder;
    private final KeywordConditionBuilder keywordConditionBuilder;
    private final QueryConfigValidationService queryConfigValidationService;

    public LogSqlBuilder(
            DistributionSqlBuilder distributionSqlBuilder,
            DetailSqlBuilder detailSqlBuilder,
            FieldDistributionSqlBuilder fieldDistributionSqlBuilder,
            KeywordConditionBuilder keywordConditionBuilder,
            QueryConfigValidationService queryConfigValidationService) {
        this.distributionSqlBuilder = distributionSqlBuilder;
        this.detailSqlBuilder = detailSqlBuilder;
        this.fieldDistributionSqlBuilder = fieldDistributionSqlBuilder;
        this.keywordConditionBuilder = keywordConditionBuilder;
        this.queryConfigValidationService = queryConfigValidationService;
    }

    /** 构建支持自定义间隔的日志分布统计SQL */
    public String buildDistributionSqlWithInterval(
            LogSearchDTO dto, String tableName, String timeUnit, int intervalValue) {
        String timeField = getTimeField(dto.getModule());
        return distributionSqlBuilder.buildCustomIntervalDistribution(
                dto, tableName, timeField, timeUnit, intervalValue);
    }

    /** 构建详细日志查询SQL（带时间字段参数） */
    public String buildDetailQuery(LogSearchDTO dto, String tableName, String timeField) {
        return detailSqlBuilder.buildDetailQuery(dto, tableName, timeField);
    }

    /** 构建总数查询SQL */
    public String buildCountQuery(LogSearchDTO dto, String tableName, String timeField) {
        return detailSqlBuilder.buildCountQuery(dto, tableName, timeField);
    }

    /** 构建字段分布TOP N查询SQL */
    public String buildFieldDistributionSql(
            LogSearchDTO dto,
            String tableName,
            List<String> fields,
            List<String> originalFields,
            int topN) {
        String timeField = getTimeField(dto.getModule());
        return fieldDistributionSqlBuilder.buildFieldDistribution(
                dto, tableName, timeField, fields, originalFields, topN);
    }

    /** 从配置中获取时间字段，如果未配置则使用默认值 */
    private String getTimeField(String module) {
        try {
            return queryConfigValidationService.getTimeField(module);
        } catch (Exception e) {
            // 如果配置未找到，返回默认的log_time字段以保持兼容性
            return "log_time";
        }
    }
}
