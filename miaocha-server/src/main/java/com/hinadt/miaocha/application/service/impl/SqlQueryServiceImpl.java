package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.SqlQueryService;
import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataServiceFactory;
import com.hinadt.miaocha.application.service.export.FileExporter;
import com.hinadt.miaocha.application.service.export.FileExporterFactory;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.converter.SchemaConverter;
import com.hinadt.miaocha.domain.converter.SqlQueryHistoryConverter;
import com.hinadt.miaocha.domain.dto.DatabaseTableListDTO;
import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.dto.SqlHistoryQueryDTO;
import com.hinadt.miaocha.domain.dto.SqlHistoryResponseDTO;
import com.hinadt.miaocha.domain.dto.SqlQueryDTO;
import com.hinadt.miaocha.domain.dto.SqlQueryResultDTO;
import com.hinadt.miaocha.domain.dto.TableSchemaDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.SqlQueryHistory;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.enums.DatasourceType;
import com.hinadt.miaocha.domain.enums.UserRole;
import com.hinadt.miaocha.infrastructure.mapper.DatasourceMapper;
import com.hinadt.miaocha.infrastructure.mapper.SqlQueryHistoryMapper;
import com.hinadt.miaocha.infrastructure.mapper.UserMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SQL查询服务实现类 */
@Service
public class SqlQueryServiceImpl implements SqlQueryService {
    private static final Logger logger = LoggerFactory.getLogger(SqlQueryServiceImpl.class);

    @Autowired private DatasourceMapper datasourceMapper;

    @Autowired private UserMapper userMapper;

    @Autowired private SqlQueryHistoryMapper sqlQueryHistoryMapper;

    @Autowired private JdbcQueryExecutor jdbcQueryExecutor;

    @Autowired private QueryPermissionChecker permissionChecker;

    @Autowired private FileExporterFactory exporterFactory;

    @Autowired private DatabaseMetadataServiceFactory metadataServiceFactory;

    @Autowired private TableValidationService tableValidationService;

    @Autowired(required = false)
    @Qualifier("sqlQueryExecutor") private Executor sqlQueryExecutor;

    @Value("${sql.query.export.dir:/tmp/sql-exports}")
    private String exportDir;

    private static final Pattern TABLE_NAME_PATTERN =
            Pattern.compile("\\bFROM\\s+[\"'`]?([\\w\\d_\\.]+)[\"'`]?", Pattern.CASE_INSENSITIVE);

    @Autowired private SqlQueryHistoryConverter sqlQueryHistoryConverter;

    @Autowired private SchemaConverter schemaConverter;

    /** 获取线程执行器，如果注入的执行器为空（如在测试环境中），则使用公共线程池 */
    private Executor getExecutor() {
        return sqlQueryExecutor != null ? sqlQueryExecutor : ForkJoinPool.commonPool();
    }

    @Override
    @Transactional
    public SqlQueryResultDTO executeQuery(Long userId, SqlQueryDTO dto) {
        // 获取数据源
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(dto.getDatasourceId());
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 使用权限检查器验证用户权限
        permissionChecker.checkQueryPermission(user, dto.getSql(), dto.getDatasourceId());

        // 检查并添加查询限制（委托给TableValidationService）
        String processedSql = tableValidationService.processSqlWithLimit(dto.getSql());
        dto.setSql(processedSql);

        // 记录SQL历史
        SqlQueryHistory history = recordSqlHistory(userId, dto);

        // 使用线程池异步执行SQL查询
        long startTime = System.currentTimeMillis();
        SqlQueryResultDTO result;

        try {
            // 使用CompletableFuture异步执行SQL查询
            // 在测试环境中，可能不会注入sqlQueryExecutor，所以使用getExecutor获取执行器
            result =
                    CompletableFuture.supplyAsync(
                                    () -> {
                                        logger.debug("开始执行SQL查询: {}", dto.getSql());
                                        return jdbcQueryExecutor.executeQuery(
                                                datasourceInfo, dto.getSql());
                                    },
                                    getExecutor())
                            .exceptionally(
                                    throwable -> {
                                        logger.error("SQL查询执行失败", throwable);
                                        if (throwable instanceof CompletionException
                                                && throwable.getCause() != null) {
                                            throwable = throwable.getCause();
                                        }
                                        if (throwable instanceof BusinessException) {
                                            throw (BusinessException) throwable;
                                        }
                                        throw new BusinessException(
                                                ErrorCode.INTERNAL_ERROR,
                                                "SQL执行失败: " + throwable.getMessage());
                                    })
                            .join();

            long endTime = System.currentTimeMillis();
            result.setExecutionTimeMs(endTime - startTime);
            logger.info("SQL查询执行完成，耗时: {}ms", result.getExecutionTimeMs());

        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException) {
                throw (BusinessException) cause;
            }
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "SQL执行失败: " + (cause != null ? cause.getMessage() : e.getMessage()));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "SQL执行失败: " + e.getMessage());
        }

        // 如果需要导出结果
        if (dto.getExportResult() && result.getRows() != null && !result.getRows().isEmpty()) {
            try {
                // 统一同步执行导出
                doExportResult(result, dto, history);
            } catch (Exception e) {
                if (e instanceof BusinessException) {
                    throw (BusinessException) e;
                }
                throw new BusinessException(ErrorCode.EXPORT_FAILED, "导出失败: " + e.getMessage());
            }
        }

        return result;
    }

    /** 执行导出结果的具体逻辑 */
    private void doExportResult(
            SqlQueryResultDTO result, SqlQueryDTO dto, SqlQueryHistory history) {
        String exportFormat = dto.getExportFormat();
        if (StringUtils.isBlank(exportFormat)) {
            exportFormat = "xlsx"; // 默认为Excel格式
        }

        String fileName = history.getId() + "." + exportFormat;
        String filePath = buildExportFilePath(fileName);

        try {
            logger.debug("开始导出SQL查询结果: {}", filePath);
            // 获取对应格式的导出器
            FileExporter exporter = exporterFactory.getExporter(exportFormat);
            if (exporter == null) {
                throw new BusinessException(ErrorCode.EXPORT_FAILED, "不支持的导出格式: " + exportFormat);
            }

            // 导出到文件
            exporter.exportToFile(result, filePath);

            // 更新SQL查询历史中的结果文件路径
            history.setResultFilePath(filePath);
            sqlQueryHistoryMapper.update(history);

            // 设置下载链接
            result.setDownloadUrl("/api/sql/result/" + history.getId());
            logger.info("SQL查询结果导出完成: {}", filePath);
        } catch (Exception e) {
            logger.error("SQL查询结果导出失败", e);
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "导出失败: " + e.getMessage());
        }
    }

    @Override
    public Resource getQueryResult(Long queryId) {
        // 查询历史记录
        SqlQueryHistory history = sqlQueryHistoryMapper.selectById(queryId);
        if (history == null) {
            logger.warn("查询记录不存在, ID: {}", queryId);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "查询记录不存在");
        }

        // 检查结果文件路径
        if (StringUtils.isBlank(history.getResultFilePath())) {
            logger.warn("查询结果文件路径为空, ID: {}", queryId);
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "查询结果文件不存在");
        }

        // 检查文件是否存在
        File resultFile = new File(history.getResultFilePath());
        if (!resultFile.exists()) {
            logger.warn("查询结果文件不存在: {}", history.getResultFilePath());
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "查询结果文件不存在");
        }

        logger.debug("获取查询结果文件: {}", history.getResultFilePath());
        return new FileSystemResource(resultFile);
    }

    /** 记录SQL历史 */
    private SqlQueryHistory recordSqlHistory(Long userId, SqlQueryDTO dto) {
        SqlQueryHistory history = new SqlQueryHistory();
        history.setUserId(userId);
        history.setDatasourceId(dto.getDatasourceId());
        history.setSqlQuery(dto.getSql());

        // 提取表名
        String tableName = extractTableName(dto.getSql());
        history.setTableName(tableName != null ? tableName : "unknown");

        sqlQueryHistoryMapper.insert(history);
        return history;
    }

    /** 从SQL中提取表名 */
    private String extractTableName(String sql) {
        Matcher matcher = TABLE_NAME_PATTERN.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /** 构建导出文件路径 */
    private String buildExportFilePath(String fileName) {
        try {
            // 确保目录存在
            Path dirPath = Paths.get(exportDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            return exportDir + File.separator + fileName;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "创建导出目录失败: " + e.getMessage());
        }
    }

    @Override
    public SqlHistoryResponseDTO getQueryHistory(Long userId, SqlHistoryQueryDTO dto) {
        // 计算分页参数
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        int limit = dto.getPageSize();

        // 查询总记录数
        Long total =
                sqlQueryHistoryMapper.countTotal(
                        userId, dto.getDatasourceId(), dto.getTableName(), dto.getQueryKeyword());

        // 查询记录列表
        List<SqlQueryHistory> historyList =
                sqlQueryHistoryMapper.selectByPage(
                        userId,
                        dto.getDatasourceId(),
                        dto.getTableName(),
                        dto.getQueryKeyword(),
                        offset,
                        limit);

        // 构建响应对象
        SqlHistoryResponseDTO response = new SqlHistoryResponseDTO();
        response.setPageNum(dto.getPageNum());
        response.setPageSize(dto.getPageSize());
        response.setTotal(total);
        response.setPages((int) Math.ceil((double) total / dto.getPageSize()));

        // 如果没有记录，直接返回空列表
        if (historyList == null || historyList.isEmpty()) {
            response.setRecords(new ArrayList<>());
            return response;
        }

        // 收集所有用户ID
        Set<Long> userIds =
                historyList.stream().map(SqlQueryHistory::getUserId).collect(Collectors.toSet());

        // 批量查询用户信息
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectByIds(new ArrayList<>(userIds));
            if (users != null) {
                userMap = users.stream().collect(Collectors.toMap(User::getId, user -> user));
            }
        }

        // 使用转换器批量转换记录
        List<SqlHistoryResponseDTO.SqlHistoryItemDTO> records =
                sqlQueryHistoryConverter.convertToDtoList(historyList, userMap);

        response.setRecords(records);
        return response;
    }

    @Override
    public DatabaseTableListDTO getDatabaseTableList(Long userId, Long datasourceId) {
        // 获取数据源
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(datasourceId);
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        DatabaseTableListDTO tableListDTO = new DatabaseTableListDTO();
        tableListDTO.setDatabaseName(
                DatasourceType.extractDatabaseName(datasourceInfo.getJdbcUrl()));

        try (Connection conn = jdbcQueryExecutor.getConnection(datasourceInfo)) {
            // 获取对应数据库类型的元数据服务
            DatabaseMetadataService metadataService =
                    metadataServiceFactory.getService(datasourceInfo.getType());

            List<String> permittedTables;

            // 如果是管理员，则可以查看所有表
            if (user.getRole().equals(UserRole.ADMIN.name())
                    || user.getRole().equals(UserRole.SUPER_ADMIN.name())) {
                permittedTables = metadataService.getAllTables(conn);
            } else {
                // 获取用户有权限的表
                permittedTables =
                        permissionChecker.getPermittedTables(userId, datasourceInfo.getId(), conn);
            }

            // 使用 converter 创建表基本信息列表
            List<DatabaseTableListDTO.TableBasicInfoDTO> tables =
                    schemaConverter.createTableBasicInfoList(permittedTables);

            tableListDTO.setTables(tables);
            return tableListDTO;
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "获取数据库表列表失败: " + e.getMessage());
        }
    }

    @Override
    public TableSchemaDTO getTableSchema(Long userId, Long datasourceId, String tableName) {
        // 获取数据源
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(datasourceId);
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        try (Connection conn = jdbcQueryExecutor.getConnection(datasourceInfo)) {
            // 获取对应数据库类型的元数据服务
            DatabaseMetadataService metadataService =
                    metadataServiceFactory.getService(datasourceInfo.getType());

            // 验证用户是否有权限访问该表
            List<String> permittedTables;
            if (user.getRole().equals(UserRole.ADMIN.name())
                    || user.getRole().equals(UserRole.SUPER_ADMIN.name())) {
                permittedTables = metadataService.getAllTables(conn);
            } else {
                permittedTables =
                        permissionChecker.getPermittedTables(userId, datasourceInfo.getId(), conn);
            }

            if (!permittedTables.contains(tableName)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权限访问表: " + tableName);
            }

            // 获取表的元数据信息
            String databaseName = DatasourceType.extractDatabaseName(datasourceInfo.getJdbcUrl());
            String tableComment = metadataService.getTableComment(conn, tableName);
            List<SchemaInfoDTO.ColumnInfoDTO> columns =
                    metadataService.getColumnInfo(conn, tableName);

            // 使用 converter 创建表schema信息
            return schemaConverter.createTableSchema(
                    databaseName, tableName, tableComment, columns);
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "获取表字段信息失败: " + e.getMessage());
        }
    }
}
