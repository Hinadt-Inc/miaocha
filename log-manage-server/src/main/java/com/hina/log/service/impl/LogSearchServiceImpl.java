package com.hina.log.service.impl;

import com.hina.log.dto.*;
import com.hina.log.dto.LogDetailResultDTO;
import com.hina.log.dto.LogFieldDistributionResultDTO;
import com.hina.log.dto.LogHistogramResultDTO;
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

import com.hina.log.service.sql.processor.ResultProcessor;
import com.hina.log.service.sql.processor.TimeRangeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    private DatabaseMetadataServiceFactory metadataServiceFactory;

    @Autowired
    private ModuleTableMappingService moduleTableMappingService;




    /**
     * 仅执行日志明细查询
     */
    @Override
    @Transactional
    public LogDetailResultDTO searchDetails(Long userId, LogSearchDTO dto) {
        // 获取数据源和用户信息
        Datasource datasource = validateAndGetDatasource(dto.getDatasourceId());
        validateUser(userId);

        // 处理时间范围
        try {
            timeRangeProcessor.processTimeRange(dto);

            // 执行明细查询
            return executeDetailSearch(datasource, dto);
        } catch (KeywordSyntaxException e) {
            return createDetailErrorResult("关键字表达式语法错误: " + e.getMessage());
        } catch (Exception e) {
            logger.error("日志明细查询异常", e);
            return createDetailErrorResult("日志明细查询失败: " + e.getMessage());
        }
    }

    /**
     * 仅执行日志时间分布查询（柱状图数据）
     */
    @Override
    @Transactional
    public LogHistogramResultDTO searchHistogram(Long userId, LogSearchDTO dto) {
        // 获取数据源和用户信息
        Datasource datasource = validateAndGetDatasource(dto.getDatasourceId());
        validateUser(userId);

        // 处理时间范围
        try {
            timeRangeProcessor.processTimeRange(dto);

            // 确定时间分组单位
            String timeUnit = timeRangeProcessor.determineTimeUnit(dto);

            // 执行时间分布查询
            return executeHistogramSearch(datasource, dto, timeUnit);
        } catch (KeywordSyntaxException e) {
            return createHistogramErrorResult("关键字表达式语法错误: " + e.getMessage());
        } catch (Exception e) {
            logger.error("日志时间分布查询异常", e);
            return createHistogramErrorResult("日志时间分布查询失败: " + e.getMessage());
        }
    }

    /**
     * 执行字段TOP5分布查询，使用Doris TOPN函数
     * 使用LogSearchDTO中的fields字段指定需要查询分布的字段列表
     */
    @Override
    @Transactional
    public LogFieldDistributionResultDTO searchFieldDistributions(Long userId, LogSearchDTO dto) {
        // 获取数据源和用户信息
        Datasource datasource = validateAndGetDatasource(dto.getDatasourceId());
        validateUser(userId);

        // 处理时间范围
        try {
            timeRangeProcessor.processTimeRange(dto);

            // 检查fields字段是否存在
            if (dto.getFields() == null || dto.getFields().isEmpty()) {
                return createFieldDistributionErrorResult("字段列表不能为空");
            }

            // 执行字段分布查询
            return executeFieldDistributionSearch(datasource, dto);
        } catch (KeywordSyntaxException e) {
            return createFieldDistributionErrorResult("关键字表达式语法错误: " + e.getMessage());
        } catch (Exception e) {
            logger.error("字段分布查询异常", e);
            return createFieldDistributionErrorResult("字段分布查询失败: " + e.getMessage());
        }
    }

    /**
     * 执行明细查询
     */
    private LogDetailResultDTO executeDetailSearch(Datasource datasource, LogSearchDTO dto) {
        long startTime = System.currentTimeMillis();
        LogDetailResultDTO result = new LogDetailResultDTO();
        result.setSuccess(true);

        // 获取模块对应的表名
        String tableName = moduleTableMappingService.getTableNameByModule(dto.getModule());

        try (Connection conn = jdbcQueryExecutor.getConnection(datasource)) {
            // 执行详细日志查询
            String detailSql = logSqlBuilder.buildDetailSql(dto, tableName);
            logger.debug("详细日志SQL: {}", detailSql);

            // 执行详细查询并获取原始结果
            Map<String, Object> detailQueryResult = jdbcQueryExecutor.executeRawQuery(conn, detailSql);

            // 处理详细查询结果
            resultProcessor.processDetailResult(detailQueryResult, result);

            // 执行总数查询
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

        long endTime = System.currentTimeMillis();
        result.setExecutionTimeMs(endTime - startTime);

        return result;
    }

    /**
     * 执行时间分布查询
     */
    private LogHistogramResultDTO executeHistogramSearch(Datasource datasource, LogSearchDTO dto, String timeUnit) {
        long startTime = System.currentTimeMillis();
        LogHistogramResultDTO result = new LogHistogramResultDTO();
        result.setSuccess(true);

        // 获取模块对应的表名
        String tableName = moduleTableMappingService.getTableNameByModule(dto.getModule());

        try (Connection conn = jdbcQueryExecutor.getConnection(datasource)) {
            // 执行分布统计查询
            String distributionSql = logSqlBuilder.buildDistributionSql(dto, tableName, timeUnit);
            logger.debug("分布统计SQL: {}", distributionSql);

            // 执行分布查询并获取原始结果
            Map<String, Object> distributionQueryResult = jdbcQueryExecutor.executeRawQuery(conn, distributionSql);

            // 处理分布查询结果
            resultProcessor.processDistributionResult(distributionQueryResult, result);

        } catch (SQLException e) {
            logger.error("执行SQL查询失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "执行SQL查询失败: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        result.setExecutionTimeMs(endTime - startTime);

        return result;
    }

    /**
     * 执行字段分布查询，使用Doris TOPN函数
     */
    private LogFieldDistributionResultDTO executeFieldDistributionSearch(Datasource datasource, LogSearchDTO dto) {
        long startTime = System.currentTimeMillis();
        LogFieldDistributionResultDTO result = new LogFieldDistributionResultDTO();
        result.setSuccess(true);

        // 获取模块对应的表名
        String tableName = moduleTableMappingService.getTableNameByModule(dto.getModule());

        try (Connection conn = jdbcQueryExecutor.getConnection(datasource)) {
            // 执行字段分布查询
            String fieldDistributionSql = logSqlBuilder.buildFieldDistributionSql(dto, tableName, dto.getFields(), 5); // 默认TOP 5
            logger.debug("字段分布SQL: {}", fieldDistributionSql);

            // 执行字段分布查询并获取原始结果
            Map<String, Object> fieldDistributionResult = jdbcQueryExecutor.executeRawQuery(conn, fieldDistributionSql);

            // 处理字段分布查询结果
            List<FieldDistributionDTO> fieldDistributions = processTopnResult(fieldDistributionResult, dto.getFields());
            result.setFieldDistributions(fieldDistributions);

        } catch (SQLException e) {
            logger.error("执行SQL查询失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "执行SQL查询失败: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        result.setExecutionTimeMs(endTime - startTime);

        return result;
    }

    /**
     * 处理TOPN查询结果，转换为FieldDistributionDTO列表
     */
    private List<FieldDistributionDTO> processTopnResult(Map<String, Object> queryResult, List<String> fields) {
        List<FieldDistributionDTO> result = new ArrayList<>();
        List<Map<String, Object>> rows = (List<Map<String, Object>>) queryResult.get("rows");

        if (rows != null && !rows.isEmpty()) {
            Map<String, Object> row = rows.get(0); // TOPN查询只返回一行数据

            for (String field : fields) {
                String topnColumnName = "topn(" + field + ", 5)"; // 列名格式：topn(field, 5)
                if (row.containsKey(topnColumnName)) {
                    String jsonValue = (String) row.get(topnColumnName);
                    FieldDistributionDTO dto = new FieldDistributionDTO();
                    dto.setFieldName(field);

                    // 解析JSON格式的TOPN结果，格式如：{"value1":count1,"value2":count2,...}
                    // 这里简化处理，实际应该使用JSON解析库
                    List<FieldDistributionDTO.ValueDistribution> valueDistributions = parseTopnJson(jsonValue);
                    dto.setValueDistributions(valueDistributions);

                    // 计算总数（所有值的计数之和）
                    int totalCount = valueDistributions.stream()
                            .mapToInt(FieldDistributionDTO.ValueDistribution::getCount)
                            .sum();

                    dto.setTotalCount(totalCount);
                    dto.setNonNullCount(totalCount); // 简化处理，实际应该计算非空值数量
                    dto.setNullCount(0); // 简化处理，实际应该计算空值数量
                    dto.setUniqueValueCount(valueDistributions.size());

                    result.add(dto);
                }
            }
        }

        return result;
    }

    /**
     * 解析TOPN函数返回的JSON字符串
     * 格式如：{"value1":count1,"value2":count2,...}
     */
    private List<FieldDistributionDTO.ValueDistribution> parseTopnJson(String jsonValue) {
        List<FieldDistributionDTO.ValueDistribution> result = new ArrayList<>();

        // 移除首尾的花括号
        String content = jsonValue.trim();
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1);
        }

        // 分割键值对
        String[] pairs = content.split(",");
        int totalCount = 0;

        // 先计算总数
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                int count = Integer.parseInt(keyValue[1].trim());
                totalCount += count;
            }
        }

        // 解析每个键值对
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                // 移除键的引号
                if (key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1);
                }

                int count = Integer.parseInt(keyValue[1].trim());

                FieldDistributionDTO.ValueDistribution vd = new FieldDistributionDTO.ValueDistribution();
                vd.setValue(key);
                vd.setCount(count);

                // 计算百分比，保留2位小数
                double percentage = (double) count / totalCount * 100;
                vd.setPercentage(Math.round(percentage * 100) / 100.0);

                result.add(vd);
            }
        }

        return result;
    }

    /**
     * 创建日志明细查询错误结果
     */
    private LogDetailResultDTO createDetailErrorResult(String errorMessage) {
        LogDetailResultDTO errorResult = new LogDetailResultDTO();
        errorResult.setSuccess(false);
        errorResult.setErrorMessage(errorMessage);
        errorResult.setRows(Collections.emptyList());
        errorResult.setTotalCount(0);
        return errorResult;
    }

    /**
     * 创建时间分布查询错误结果
     */
    private LogHistogramResultDTO createHistogramErrorResult(String errorMessage) {
        LogHistogramResultDTO errorResult = new LogHistogramResultDTO();
        errorResult.setSuccess(false);
        errorResult.setErrorMessage(errorMessage);
        errorResult.setDistributionData(Collections.emptyList());
        return errorResult;
    }

    /**
     * 创建字段分布查询错误结果
     */
    private LogFieldDistributionResultDTO createFieldDistributionErrorResult(String errorMessage) {
        LogFieldDistributionResultDTO errorResult = new LogFieldDistributionResultDTO();
        errorResult.setSuccess(false);
        errorResult.setErrorMessage(errorMessage);
        errorResult.setFieldDistributions(Collections.emptyList());
        return errorResult;
    }

    /**
     * 验证并获取数据源
     */
    private Datasource validateAndGetDatasource(Long datasourceId) {
        Datasource datasource = datasourceMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }
        return datasource;
    }

    /**
     * 验证用户
     */
    private void validateUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Override
    public List<SchemaInfoDTO.ColumnInfoDTO> getTableColumns(Long userId, Long datasourceId, String module) {
        // 获取数据源
        Datasource datasource = validateAndGetDatasource(datasourceId);

        // 验证用户
        validateUser(userId);

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