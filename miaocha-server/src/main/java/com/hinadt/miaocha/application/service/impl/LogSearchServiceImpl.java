package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.LogSearchService;
import com.hinadt.miaocha.application.service.ModuleInfoService;
import com.hinadt.miaocha.application.service.SystemCacheService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataServiceFactory;
import com.hinadt.miaocha.application.service.impl.logsearch.executor.DetailSearchExecutor;
import com.hinadt.miaocha.application.service.impl.logsearch.executor.FieldDistributionSearchExecutor;
import com.hinadt.miaocha.application.service.impl.logsearch.executor.HistogramSearchExecutor;
import com.hinadt.miaocha.application.service.impl.logsearch.template.LogSearchTemplate;
import com.hinadt.miaocha.application.service.impl.logsearch.validator.LogSearchValidator;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.util.CacheKeyUtils;
import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.dto.cache.BatchDeleteCacheDTO;
import com.hinadt.miaocha.domain.dto.cache.SystemCacheDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogDetailResultDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogHistogramResultDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchCacheDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.enums.CacheGroup;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 日志检索服务实现类
 *
 * <p>重构后的简洁版本，职责清晰，代码优雅
 */
@Service
public class LogSearchServiceImpl implements LogSearchService {

    private final LogSearchValidator validator;
    private final LogSearchTemplate searchTemplate;
    private final DetailSearchExecutor detailExecutor;
    private final HistogramSearchExecutor histogramExecutor;
    private final FieldDistributionSearchExecutor fieldDistributionExecutor;
    private final DatabaseMetadataServiceFactory metadataServiceFactory;
    private final ModuleInfoService moduleInfoService;
    private final JdbcQueryExecutor jdbcQueryExecutor;
    private final SystemCacheService systemCacheService;

    public LogSearchServiceImpl(
            LogSearchValidator validator,
            LogSearchTemplate searchTemplate,
            DetailSearchExecutor detailExecutor,
            HistogramSearchExecutor histogramExecutor,
            FieldDistributionSearchExecutor fieldDistributionExecutor,
            DatabaseMetadataServiceFactory metadataServiceFactory,
            ModuleInfoService moduleInfoService,
            JdbcQueryExecutor jdbcQueryExecutor,
            SystemCacheService systemCacheService) {
        this.validator = validator;
        this.searchTemplate = searchTemplate;
        this.detailExecutor = detailExecutor;
        this.histogramExecutor = histogramExecutor;
        this.fieldDistributionExecutor = fieldDistributionExecutor;
        this.metadataServiceFactory = metadataServiceFactory;
        this.moduleInfoService = moduleInfoService;
        this.jdbcQueryExecutor = jdbcQueryExecutor;
        this.systemCacheService = systemCacheService;
    }

    /** 执行日志明细查询 */
    @Override
    @Transactional
    public LogDetailResultDTO searchDetails(LogSearchDTO dto) {
        // 验证参数
        validator.validatePaginationParams(dto);
        validator.validateSortFields(dto);
        DatasourceInfo datasourceInfo = validator.validateAndGetDatasource(dto.getModule());

        // 执行搜索
        return searchTemplate.execute(datasourceInfo, dto, detailExecutor);
    }

    /** 执行日志时间分布查询（柱状图数据） */
    @Override
    @Transactional
    public LogHistogramResultDTO searchHistogram(LogSearchDTO dto) {
        // 验证参数
        DatasourceInfo datasourceInfo = validator.validateAndGetDatasource(dto.getModule());

        // 执行搜索
        return searchTemplate.execute(datasourceInfo, dto, histogramExecutor);
    }

    /** 执行字段TOP5分布查询 */
    @Override
    @Transactional
    public com.hinadt.miaocha.domain.dto.logsearch.LogFieldDistributionResultDTO
            searchFieldDistributions(LogSearchDTO dto) {
        // 验证参数
        validator.validateFields(dto);
        DatasourceInfo datasourceInfo = validator.validateAndGetDatasource(dto.getModule());

        // 执行搜索
        return searchTemplate.execute(datasourceInfo, dto, fieldDistributionExecutor);
    }

    /** 获取表字段信息 */
    @Override
    public List<SchemaInfoDTO.ColumnInfoDTO> getTableColumns(String module) {
        // 验证并获取数据源信息
        DatasourceInfo datasourceInfo = validator.validateAndGetDatasource(module);

        // 获取表名
        String tableName = moduleInfoService.getTableNameByModule(module);

        try (Connection conn = jdbcQueryExecutor.getConnection(datasourceInfo)) {
            // 获取数据库元数据服务并查询字段信息
            DatabaseMetadataService metadataService =
                    metadataServiceFactory.getService(datasourceInfo.getType());
            List<SchemaInfoDTO.ColumnInfoDTO> columns =
                    metadataService.getColumnInfo(conn, tableName);

            // 获取查询配置并排除配置的字段
            return filterExcludeFields(module, columns);
        } catch (SQLException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "获取表字段信息失败: " + e.getMessage());
        }
    }

    /**
     * 根据模块查询配置过滤排除字段
     *
     * @param module 模块名称
     * @param columns 原始字段列表
     * @return 过滤后的字段列表
     */
    private List<SchemaInfoDTO.ColumnInfoDTO> filterExcludeFields(
            String module, List<SchemaInfoDTO.ColumnInfoDTO> columns) {
        // 获取模块查询配置
        var queryConfig = moduleInfoService.getQueryConfigByModule(module);

        // 如果查询配置存在且排除字段不为空，则过滤字段
        if (queryConfig != null
                && queryConfig.getExcludeFields() != null
                && !queryConfig.getExcludeFields().isEmpty()) {

            return columns.stream()
                    .filter(
                            column ->
                                    !queryConfig
                                            .getExcludeFields()
                                            .contains(column.getColumnName()))
                    .toList();
        }

        // 否则返回原始字段列表
        return columns;
    }

    /** 保存用户个性化的日志搜索条件 */
    @Override
    public String saveSearchCondition(LogSearchCacheDTO searchCondition) {
        // 生成缓存键
        String cacheKey = CacheKeyUtils.generateSearchConditionKey();

        // 保存搜索条件到缓存
        systemCacheService.saveCache(CacheGroup.LOG_SEARCH_CONDITION, cacheKey, searchCondition);

        return cacheKey;
    }

    /** 获取用户个性化的日志搜索条件数据 */
    @Override
    public List<SystemCacheDTO<LogSearchCacheDTO>> getUserSearchConditions() {
        return systemCacheService.getUserCacheData(CacheGroup.LOG_SEARCH_CONDITION);
    }

    /** 批量删除用户个性化的日志搜索条件 */
    @Override
    @Transactional
    public void batchDeleteSearchConditions(BatchDeleteCacheDTO deleteCacheDTO) {
        systemCacheService.batchDeleteCache(
                CacheGroup.LOG_SEARCH_CONDITION, deleteCacheDTO.getCacheKeys());
    }
}
