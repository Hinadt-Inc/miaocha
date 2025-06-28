package com.hinadt.miaocha.application.service.impl.logsearch.template;

import com.hinadt.miaocha.application.service.ModuleInfoService;
import com.hinadt.miaocha.application.service.impl.logsearch.validator.QueryConfigValidationService;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.application.service.sql.converter.LogSearchDTOConverter;
import com.hinadt.miaocha.application.service.sql.processor.TimeRangeProcessor;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.exception.LogQueryException;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchResultDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 日志搜索模板类
 *
 * <p>提取公共的执行逻辑，使用模板方法模式
 */
@Component
@Slf4j
public class LogSearchTemplate {

    private final JdbcQueryExecutor jdbcQueryExecutor;
    private final TimeRangeProcessor timeRangeProcessor;
    private final ModuleInfoService moduleInfoService;
    private final LogSearchDTOConverter dtoConverter;
    private final QueryConfigValidationService queryConfigValidationService;

    public LogSearchTemplate(
            JdbcQueryExecutor jdbcQueryExecutor,
            TimeRangeProcessor timeRangeProcessor,
            ModuleInfoService moduleInfoService,
            LogSearchDTOConverter dtoConverter,
            QueryConfigValidationService queryConfigValidationService) {
        this.jdbcQueryExecutor = jdbcQueryExecutor;
        this.timeRangeProcessor = timeRangeProcessor;
        this.moduleInfoService = moduleInfoService;
        this.dtoConverter = dtoConverter;
        this.queryConfigValidationService = queryConfigValidationService;
    }

    /** 执行搜索的模板方法 */
    public <T extends LogSearchResultDTO> T execute(
            DatasourceInfo datasourceInfo, LogSearchDTO dto, SearchExecutor<T> executor) {

        long startTime = System.currentTimeMillis();

        try {
            // 1. 处理时间范围
            timeRangeProcessor.processTimeRange(dto);

            // 2. 转换DTO
            LogSearchDTO convertedDto = dtoConverter.convert(dto);

            // 3. 获取表名和时间字段
            String tableName = moduleInfoService.getTableNameByModule(dto.getModule());
            String timeField = getTimeField(dto.getModule());

            // 4. 执行具体的搜索逻辑
            Connection conn = jdbcQueryExecutor.getConnection(datasourceInfo);
            SearchContext context = new SearchContext(conn, convertedDto, tableName, timeField);
            T result = executor.execute(context);

            // 5. 设置执行时间
            long endTime = System.currentTimeMillis();
            result.setExecutionTimeMs(endTime - startTime);

            // 注意：这里不关闭conn，让HikariCP管理连接生命周期
            return result;

        } catch (SQLException e) {
            log.error("数据库连接失败, {}", datasourceInfo, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "对应日志数据库连接异常: " + e.getMessage());
        }
    }

    /** 从配置中获取时间字段，如果未配置则使用默认值 */
    private String getTimeField(String module) {
        return queryConfigValidationService.getTimeField(module);
    }

    /** 搜索执行器接口 */
    public interface SearchExecutor<T extends LogSearchResultDTO> {

        /** 执行具体的搜索逻辑 */
        T execute(SearchContext context) throws LogQueryException;
    }
}
