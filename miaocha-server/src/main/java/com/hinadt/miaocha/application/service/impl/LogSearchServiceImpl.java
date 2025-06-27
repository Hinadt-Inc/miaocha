package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.LogSearchService;
import com.hinadt.miaocha.application.service.ModuleInfoService;
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
import com.hinadt.miaocha.domain.dto.LogDetailResultDTO;
import com.hinadt.miaocha.domain.dto.LogFieldDistributionResultDTO;
import com.hinadt.miaocha.domain.dto.LogHistogramResultDTO;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
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

    public LogSearchServiceImpl(
            LogSearchValidator validator,
            LogSearchTemplate searchTemplate,
            DetailSearchExecutor detailExecutor,
            HistogramSearchExecutor histogramExecutor,
            FieldDistributionSearchExecutor fieldDistributionExecutor,
            DatabaseMetadataServiceFactory metadataServiceFactory,
            ModuleInfoService moduleInfoService,
            JdbcQueryExecutor jdbcQueryExecutor) {
        this.validator = validator;
        this.searchTemplate = searchTemplate;
        this.detailExecutor = detailExecutor;
        this.histogramExecutor = histogramExecutor;
        this.fieldDistributionExecutor = fieldDistributionExecutor;
        this.metadataServiceFactory = metadataServiceFactory;
        this.moduleInfoService = moduleInfoService;
        this.jdbcQueryExecutor = jdbcQueryExecutor;
    }

    /** 执行日志明细查询 */
    @Override
    @Transactional
    public LogDetailResultDTO searchDetails(LogSearchDTO dto) {
        // 验证参数
        validator.validatePaginationParams(dto);
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
    public LogFieldDistributionResultDTO searchFieldDistributions(LogSearchDTO dto) {
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
            return metadataService.getColumnInfo(conn, tableName);
        } catch (SQLException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "获取表字段信息失败: " + e.getMessage());
        }
    }
}
