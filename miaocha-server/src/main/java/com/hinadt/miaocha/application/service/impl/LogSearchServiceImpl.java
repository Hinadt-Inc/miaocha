package com.hinadt.miaocha.application.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.application.service.LogSearchService;
import com.hinadt.miaocha.application.service.ModuleInfoService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataServiceFactory;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.application.service.sql.builder.LogSqlBuilder;
import com.hinadt.miaocha.application.service.sql.converter.LogSearchDTOConverter;
import com.hinadt.miaocha.application.service.sql.processor.ResultProcessor;
import com.hinadt.miaocha.application.service.sql.processor.TimeGranularityCalculator;
import com.hinadt.miaocha.application.service.sql.processor.TimeRangeProcessor;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.exception.KeywordSyntaxException;
import com.hinadt.miaocha.domain.dto.*;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 日志检索服务实现类 */
@Service
public class LogSearchServiceImpl implements LogSearchService {
    private static final Logger logger = LoggerFactory.getLogger(LogSearchServiceImpl.class);

    /** 分页查询的最大页面大小限制 */
    private static final int MAX_PAGE_SIZE = 5000;

    @Autowired private DatasourceMapper datasourceMapper;

    @Autowired private UserMapper userMapper;

    @Autowired private JdbcQueryExecutor jdbcQueryExecutor;

    @Autowired private TimeRangeProcessor timeRangeProcessor;

    @Autowired private LogSqlBuilder logSqlBuilder;

    @Autowired private ResultProcessor resultProcessor;

    @Autowired private DatabaseMetadataServiceFactory metadataServiceFactory;

    @Autowired private ModuleInfoService moduleInfoService;

    @Autowired private LogSearchDTOConverter dtoConverter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 仅执行日志明细查询 */
    @Override
    @Transactional
    public LogDetailResultDTO searchDetails(Long userId, LogSearchDTO dto) {
        // 验证分页参数
        validatePaginationParams(dto);

        // 获取数据源和用户信息
        DatasourceInfo datasourceInfo = validateAndGetDatasource(dto.getDatasourceId());
        validateUser(userId);

        // 处理时间范围
        try {
            timeRangeProcessor.processTimeRange(dto);

            // 执行明细查询
            return executeDetailSearch(datasourceInfo, dto);
        } catch (KeywordSyntaxException e) {
            return createDetailErrorResult("关键字表达式语法错误: " + e.getMessage());
        } catch (Exception e) {
            logger.error("日志明细查询异常", e);
            return createDetailErrorResult("日志明细查询失败: " + e.getMessage());
        }
    }

    /** 仅执行日志时间分布查询（柱状图数据） */
    @Override
    @Transactional
    public LogHistogramResultDTO searchHistogram(Long userId, LogSearchDTO dto) {
        // 获取数据源和用户信息
        DatasourceInfo datasourceInfo = validateAndGetDatasource(dto.getDatasourceId());
        validateUser(userId);

        // 处理时间范围
        try {
            timeRangeProcessor.processTimeRange(dto);

            // 使用新的智能时间颗粒度计算器，支持自定义目标桶数量
            TimeGranularityCalculator.TimeGranularityResult granularityResult =
                    timeRangeProcessor.calculateOptimalTimeGranularity(dto, dto.getTargetBuckets());

            // 执行时间分布查询
            return executeHistogramSearchWithGranularity(datasourceInfo, dto, granularityResult);
        } catch (KeywordSyntaxException e) {
            return createHistogramErrorResult("关键字表达式语法错误: " + e.getMessage());
        } catch (Exception e) {
            logger.error("日志时间分布查询异常", e);
            return createHistogramErrorResult("日志时间分布查询失败: " + e.getMessage());
        }
    }

    /** 执行字段TOP5分布查询，使用Doris TOPN函数 使用LogSearchDTO中的fields字段指定需要查询分布的字段列表 */
    @Override
    @Transactional
    public LogFieldDistributionResultDTO searchFieldDistributions(Long userId, LogSearchDTO dto) {
        // 获取数据源和用户信息
        DatasourceInfo datasourceInfo = validateAndGetDatasource(dto.getDatasourceId());
        validateUser(userId);

        // 处理时间范围
        try {
            timeRangeProcessor.processTimeRange(dto);

            // 检查fields字段是否存在
            if (dto.getFields() == null || dto.getFields().isEmpty()) {
                return createFieldDistributionErrorResult("字段列表不能为空");
            }

            // 执行字段分布查询
            return executeFieldDistributionSearch(datasourceInfo, dto);
        } catch (KeywordSyntaxException e) {
            return createFieldDistributionErrorResult("关键字表达式语法错误: " + e.getMessage());
        } catch (Exception e) {
            logger.error("字段分布查询异常", e);
            return createFieldDistributionErrorResult("字段分布查询失败: " + e.getMessage());
        }
    }

    /** 执行明细查询 */
    private LogDetailResultDTO executeDetailSearch(
            DatasourceInfo datasourceInfo, LogSearchDTO dto) {
        long startTime = System.currentTimeMillis();
        LogDetailResultDTO result = new LogDetailResultDTO();
        result.setSuccess(true);

        // 获取模块对应的表名
        String tableName = moduleInfoService.getTableNameByModule(dto.getModule());

        try (Connection conn = jdbcQueryExecutor.getConnection(datasourceInfo)) {
            // 转换DTO中的variant字段语法
            LogSearchDTO convertedDto = dtoConverter.convert(dto);

            // 执行详细日志查询
            String detailSql = logSqlBuilder.buildDetailSql(convertedDto, tableName);
            logger.debug("详细日志SQL: {}", detailSql);

            // 执行详细查询并获取原始结果
            Map<String, Object> detailQueryResult =
                    jdbcQueryExecutor.executeRawQuery(conn, detailSql);

            // 处理详细查询结果
            resultProcessor.processDetailResult(detailQueryResult, result);

            // 执行总数查询
            StringBuilder countSql = new StringBuilder();
            countSql.append("SELECT COUNT(1) as total FROM ")
                    .append(tableName)
                    .append(" WHERE log_time >= '")
                    .append(dto.getStartTime())
                    .append("'")
                    .append(" AND log_time <= '")
                    .append(dto.getEndTime())
                    .append("'");

            // 在where条件部分添加搜索条件
            String searchConditions = logSqlBuilder.buildSearchConditionsOnly(convertedDto);
            if (!searchConditions.isEmpty()) {
                countSql.append(" AND ").append(searchConditions);
            }

            logger.debug("总数SQL: {}", countSql);

            // 执行总数查询并获取原始结果
            Map<String, Object> countQueryResult =
                    jdbcQueryExecutor.executeRawQuery(conn, countSql.toString());

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

    /** 执行时间分布查询 - 基于颗粒度计算结果的新实现 */
    private LogHistogramResultDTO executeHistogramSearchWithGranularity(
            DatasourceInfo datasourceInfo,
            LogSearchDTO dto,
            TimeGranularityCalculator.TimeGranularityResult granularityResult) {
        long startTime = System.currentTimeMillis();
        LogHistogramResultDTO result = new LogHistogramResultDTO();
        result.setSuccess(true);

        // 获取模块对应的表名
        String tableName = moduleInfoService.getTableNameByModule(dto.getModule());

        try (Connection conn = jdbcQueryExecutor.getConnection(datasourceInfo)) {
            // 转换DTO中的variant字段语法
            LogSearchDTO convertedDto = dtoConverter.convert(dto);

            // 执行分布统计查询，使用计算出的时间颗粒度
            String distributionSql =
                    logSqlBuilder.buildDistributionSqlWithInterval(
                            convertedDto,
                            tableName,
                            granularityResult.getTimeUnit(),
                            granularityResult.getInterval());
            logger.debug(
                    "分布统计SQL: {}, 颗粒度详情: {}",
                    distributionSql,
                    granularityResult.getDetailedDescription());

            // 执行分布查询并获取原始结果
            Map<String, Object> distributionQueryResult =
                    jdbcQueryExecutor.executeRawQuery(conn, distributionSql);

            // 处理分布查询结果
            resultProcessor.processDistributionResult(distributionQueryResult, result);

        } catch (SQLException e) {
            logger.error("执行SQL查询失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "执行SQL查询失败: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        result.setExecutionTimeMs(endTime - startTime);

        // 设置更丰富的时间颗粒度信息
        result.setTimeUnit(granularityResult.getTimeUnit());
        result.setTimeInterval(granularityResult.getInterval());
        result.setEstimatedBuckets(granularityResult.getEstimatedBuckets());
        result.setCalculationMethod(granularityResult.getCalculationMethod());

        // 计算实际桶数量（基于返回的数据）
        if (result.getDistributionData() != null) {
            result.setActualBuckets(result.getDistributionData().size());
        }

        return result;
    }

    /** 执行字段分布查询，使用Doris TOPN函数 */
    private LogFieldDistributionResultDTO executeFieldDistributionSearch(
            DatasourceInfo datasourceInfo, LogSearchDTO dto) {
        long startTime = System.currentTimeMillis();
        LogFieldDistributionResultDTO result = new LogFieldDistributionResultDTO();
        result.setSuccess(true);

        // 获取模块对应的表名
        String tableName = moduleInfoService.getTableNameByModule(dto.getModule());

        try (Connection conn = jdbcQueryExecutor.getConnection(datasourceInfo)) {
            // 转换DTO中的variant字段语法
            LogSearchDTO convertedDto = dtoConverter.convert(dto);

            // 转换fields中的点语法为括号语法（用于TOPN函数）
            // 注意：这里要使用原始的dto.getFields()，而不是convertedDto.getFields()
            // 因为convertedDto.getFields()已经包含了AS语法，不适合用于TOPN函数
            List<String> convertedTopnFields =
                    dto.getFields().stream()
                            .map(dtoConverter::convertTopnField)
                            .collect(java.util.stream.Collectors.toList());

            // 执行字段分布查询
            String fieldDistributionSql =
                    logSqlBuilder.buildFieldDistributionSql(
                            convertedDto,
                            tableName,
                            convertedTopnFields,
                            dto.getFields(),
                            5); // 默认TOP 5
            logger.debug("字段分布SQL: {}", fieldDistributionSql);

            // 执行字段分布查询并获取原始结果
            Map<String, Object> fieldDistributionResult =
                    jdbcQueryExecutor.executeRawQuery(conn, fieldDistributionSql);

            // 使用采样大小常量进行百分比计算
            result.setActualSampleCount(LogSqlBuilder.FIELD_DISTRIBUTION_SAMPLE_SIZE);

            // 处理字段分布查询结果（使用原始字段名，保持用户友好的显示）
            List<FieldDistributionDTO> fieldDistributions =
                    processTopnResult(
                            fieldDistributionResult,
                            dto.getFields(),
                            LogSqlBuilder.FIELD_DISTRIBUTION_SAMPLE_SIZE);
            result.setFieldDistributions(fieldDistributions);

        } catch (SQLException e) {
            logger.error("执行SQL查询失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "执行SQL查询失败: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        result.setExecutionTimeMs(endTime - startTime);

        return result;
    }

    /** 处理TOPN查询结果，转换为FieldDistributionDTO列表 */
    private List<FieldDistributionDTO> processTopnResult(
            Map<String, Object> queryResult, List<String> fields, Integer sampleSize) {
        List<FieldDistributionDTO> result = new ArrayList<>();
        List<Map<String, Object>> rows = (List<Map<String, Object>>) queryResult.get("rows");

        if (rows != null && !rows.isEmpty()) {
            Map<String, Object> row = rows.get(0); // TOPN查询只返回一行数据

            for (String field : fields) {
                // 由于现在使用AS别名，直接使用字段名作为列名
                String jsonValue = null;

                // 尝试获取AS别名的列值（直接使用原字段名）
                if (row.containsKey(field)) {
                    jsonValue = (String) row.get(field);
                } else {
                    // 兼容旧格式：尝试TOPN函数格式的列名
                    String topnColumnNameLower = "topn(" + field + ", 5)"; // 小写列名格式：topn(field, 5)
                    String topnColumnNameUpper = "TOPN(" + field + ", 5)"; // 大写列名格式：TOPN(field, 5)

                    if (row.containsKey(topnColumnNameLower)) {
                        jsonValue = (String) row.get(topnColumnNameLower);
                    } else if (row.containsKey(topnColumnNameUpper)) {
                        jsonValue = (String) row.get(topnColumnNameUpper);
                    }
                }

                if (jsonValue != null || row.containsKey(field)) {
                    FieldDistributionDTO dto = new FieldDistributionDTO();
                    dto.setFieldName(field);

                    // 解析JSON格式的TOPN结果，格式如：{"value1":count1,"value2":count2,...}
                    List<FieldDistributionDTO.ValueDistribution> valueDistributions;
                    if (jsonValue != null) {
                        valueDistributions = parseTopnJson(jsonValue, sampleSize);
                    } else {
                        valueDistributions = new ArrayList<>();
                    }
                    dto.setValueDistributions(valueDistributions);

                    result.add(dto);
                }
            }
        }

        return result;
    }

    /** 解析TOPN函数返回的JSON字符串 格式如：{"value1":count1,"value2":count2,...} */
    private List<FieldDistributionDTO.ValueDistribution> parseTopnJson(
            String jsonValue, Integer sampleSize) {
        List<FieldDistributionDTO.ValueDistribution> result = new ArrayList<>();

        // 检查输入是否为null或空
        if (jsonValue == null || jsonValue.trim().isEmpty()) {
            return result;
        }

        try {
            // 使用Jackson解析JSON
            Map<String, Integer> jsonMap =
                    objectMapper.readValue(jsonValue, new TypeReference<Map<String, Integer>>() {});

            // 解析每个键值对
            for (Map.Entry<String, Integer> entry : jsonMap.entrySet()) {
                String key = entry.getKey();
                int count = entry.getValue();

                FieldDistributionDTO.ValueDistribution vd =
                        new FieldDistributionDTO.ValueDistribution();
                vd.setValue(key);
                vd.setCount(count);

                // 计算基于采样总数的百分比，保留2位小数
                double percentage = sampleSize > 0 ? (double) count / sampleSize * 100 : 0.0;
                vd.setPercentage(Math.round(percentage * 100) / 100.0);

                result.add(vd);
            }

        } catch (Exception e) {
            logger.error("解析TOPN JSON失败: {}", jsonValue, e);
            // 返回空结果而不是抛出异常，保证系统稳定性
        }

        return result;
    }

    /** 创建日志明细查询错误结果 */
    private LogDetailResultDTO createDetailErrorResult(String errorMessage) {
        LogDetailResultDTO errorResult = new LogDetailResultDTO();
        errorResult.setSuccess(false);
        errorResult.setErrorMessage(errorMessage);
        errorResult.setRows(Collections.emptyList());
        errorResult.setTotalCount(0);
        return errorResult;
    }

    /** 创建时间分布查询错误结果 */
    private LogHistogramResultDTO createHistogramErrorResult(String errorMessage) {
        LogHistogramResultDTO errorResult = new LogHistogramResultDTO();
        errorResult.setSuccess(false);
        errorResult.setErrorMessage(errorMessage);
        errorResult.setDistributionData(Collections.emptyList());
        return errorResult;
    }

    /** 创建字段分布查询错误结果 */
    private LogFieldDistributionResultDTO createFieldDistributionErrorResult(String errorMessage) {
        LogFieldDistributionResultDTO errorResult = new LogFieldDistributionResultDTO();
        errorResult.setSuccess(false);
        errorResult.setErrorMessage(errorMessage);
        errorResult.setFieldDistributions(Collections.emptyList());
        return errorResult;
    }

    /** 验证并获取数据源 */
    private DatasourceInfo validateAndGetDatasource(Long datasourceId) {
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(datasourceId);
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }
        return datasourceInfo;
    }

    /** 验证用户 */
    private void validateUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 验证分页参数，防止过大的分页查询导致系统性能问题
     *
     * @param dto 日志检索请求参数
     */
    private void validatePaginationParams(LogSearchDTO dto) {
        if (dto.getPageSize() != null && dto.getPageSize() > MAX_PAGE_SIZE) {
            logger.warn("分页大小超出限制，请求大小: {}, 最大限制: {}", dto.getPageSize(), MAX_PAGE_SIZE);
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    String.format("分页大小不能超过 %d 条，当前请求: %d 条", MAX_PAGE_SIZE, dto.getPageSize()));
        }

        if (dto.getOffset() != null && dto.getOffset() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "分页偏移量不能小于0");
        }
    }

    @Override
    public List<SchemaInfoDTO.ColumnInfoDTO> getTableColumns(
            Long userId, Long datasourceId, String module) {
        // 获取数据源
        DatasourceInfo datasourceInfo = validateAndGetDatasource(datasourceId);

        // 验证用户
        validateUser(userId);

        // 获取模块对应的表名
        String tableName = moduleInfoService.getTableNameByModule(module);

        try (Connection conn = jdbcQueryExecutor.getConnection(datasourceInfo)) {
            // 获取对应数据库类型的元数据服务
            DatabaseMetadataService metadataService =
                    metadataServiceFactory.getService(datasourceInfo.getType());

            // 获取表的完整字段信息
            return metadataService.getColumnInfo(conn, tableName);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "获取表结构失败: " + e.getMessage());
        }
    }
}
