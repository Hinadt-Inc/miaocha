package com.hina.log.service.impl;

import com.hina.log.dto.LogSearchDTO;
import com.hina.log.dto.LogSearchResultDTO;
import com.hina.log.dto.SchemaInfoDTO;
import com.hina.log.entity.Datasource;
import com.hina.log.entity.User;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.exception.KeywordSyntaxException;
import com.hina.log.mapper.DatasourceMapper;
import com.hina.log.mapper.UserMapper;
import com.hina.log.service.LogSearchService;
import com.hina.log.service.ModuleTableMappingService;
import com.hina.log.service.database.DatabaseMetadataService;
import com.hina.log.service.database.DatabaseMetadataServiceFactory;
import com.hina.log.service.sql.JdbcQueryExecutor;
import com.hina.log.service.sql.builder.LogSqlBuilder;
import com.hina.log.service.sql.processor.FieldDistributionProcessor;
import com.hina.log.service.sql.processor.ResultProcessor;
import com.hina.log.service.sql.processor.TimeRangeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    private TimeRangeProcessor timeRangeProcessor;

    @Autowired
    private LogSqlBuilder logSqlBuilder;

    @Autowired
    private ResultProcessor resultProcessor;

    @Autowired
    private FieldDistributionProcessor fieldDistributionProcessor;

    @Autowired
    private DatabaseMetadataServiceFactory metadataServiceFactory;

    @Autowired
    private ModuleTableMappingService moduleTableMappingService;


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

        // 处理时间范围
        try {
            timeRangeProcessor.processTimeRange(dto);

            // 确定时间分组单位
            String timeUnit = timeRangeProcessor.determineTimeUnit(dto);

            // 构建SQL并执行查询
            return executeSearch(datasource, dto, timeUnit);
        } catch (KeywordSyntaxException e) {
            // 捕获关键字语法异常，构造友好的错误结果
            logger.warn("关键字表达式语法错误: {}, 表达式: {}", e.getMessage(), e.getExpression());

            LogSearchResultDTO errorResult = new LogSearchResultDTO();
            errorResult.setSuccess(false);
            errorResult.setErrorMessage("关键字表达式语法错误: " + e.getMessage());
            errorResult.setRows(Collections.emptyList());
            errorResult.setTotalCount(0);
            errorResult.setDistributionData(Collections.emptyList());
            return errorResult;
        } catch (Exception e) {
            // 其他异常处理
            logger.error("日志检索异常", e);

            LogSearchResultDTO errorResult = new LogSearchResultDTO();
            errorResult.setSuccess(false);
            errorResult.setErrorMessage("日志检索失败: " + e.getMessage());
            errorResult.setRows(Collections.emptyList());
            errorResult.setTotalCount(0);
            errorResult.setDistributionData(Collections.emptyList());
            return errorResult;
        }
    }

    private LogSearchResultDTO executeSearch(Datasource datasource, LogSearchDTO dto, String timeUnit) {
        long startTime = System.currentTimeMillis();
        LogSearchResultDTO result = new LogSearchResultDTO();
        result.setSuccess(true);

        // 获取模块对应的表名
        String tableName = moduleTableMappingService.getTableNameByModule(dto.getModule());

        try (Connection conn = jdbcQueryExecutor.getConnection(datasource)) {
            // 1. 执行分布统计查询
            String distributionSql = logSqlBuilder.buildDistributionSql(dto, tableName, timeUnit);
            logger.debug("分布统计SQL: {}", distributionSql);

            // 执行分布查询并获取原始结果
            Map<String, Object> distributionQueryResult = jdbcQueryExecutor.executeRawQuery(conn, distributionSql);

            // 处理分布查询结果
            resultProcessor.processDistributionResult(distributionQueryResult, result);

            // 2. 执行详细日志查询
            String detailSql = logSqlBuilder.buildDetailSql(dto, tableName);
            logger.debug("详细日志SQL: {}", detailSql);

            // 执行详细查询并获取原始结果
            Map<String, Object> detailQueryResult = jdbcQueryExecutor.executeRawQuery(conn, detailSql);

            // 处理详细查询结果
            resultProcessor.processDetailResult(detailQueryResult, result);

            // 3. 执行总数查询
            StringBuilder countSql = new StringBuilder();
            countSql.append("SELECT COUNT(1) as total FROM ").append(tableName)
                    .append(" WHERE log_time >= '").append(dto.getStartTime()).append("'")
                    .append(" AND log_time <= '").append(dto.getEndTime()).append("'");

            // 在where条件部分添加搜索条件
            String searchConditions = logSqlBuilder.buildSearchConditionsOnly(dto);
            if (!searchConditions.isEmpty()) {
                countSql.append(" AND ").append(searchConditions);
            }

            logger.debug("总数SQL: {}", countSql);

            // 执行总数查询并获取原始结果
            Map<String, Object> countQueryResult = jdbcQueryExecutor.executeRawQuery(conn, countSql.toString());

            // 处理总数查询结果
            int totalCount = resultProcessor.processTotalCountResult(countQueryResult);
            result.setTotalCount(totalCount);

        } catch (SQLException e) {
            logger.error("执行SQL查询失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "执行SQL查询失败: " + e.getMessage());
        }

        // 在SQL查询完成后，异步计算字段分布统计信息
        fieldDistributionProcessor.processFieldDistributionsAsync(result)
                .exceptionally(ex -> {
                    logger.error("计算字段分布统计信息时发生异常", ex);
                    return null;
                });

        long endTime = System.currentTimeMillis();
        result.setExecutionTimeMs(endTime - startTime);

        return result;
    }

    @Override
    public List<SchemaInfoDTO.ColumnInfoDTO> getTableColumns(Long userId, Long datasourceId, String module) {
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

        // 获取模块对应的表名
        String tableName = moduleTableMappingService.getTableNameByModule(module);

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