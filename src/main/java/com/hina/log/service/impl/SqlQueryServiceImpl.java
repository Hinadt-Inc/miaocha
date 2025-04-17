package com.hina.log.service.impl;

import com.hina.log.dto.SchemaInfoDTO;
import com.hina.log.dto.SqlQueryDTO;
import com.hina.log.dto.SqlQueryResultDTO;
import com.hina.log.entity.Datasource;
import com.hina.log.entity.SqlQueryHistory;
import com.hina.log.entity.User;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.DatasourceMapper;
import com.hina.log.mapper.SqlQueryHistoryMapper;
import com.hina.log.mapper.UserMapper;
import com.hina.log.service.SqlQueryService;
import com.hina.log.service.database.DatabaseMetadataService;
import com.hina.log.service.database.DatabaseMetadataServiceFactory;
import com.hina.log.service.export.FileExporter;
import com.hina.log.service.export.FileExporterFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL查询服务实现类
 */
@Service
public class SqlQueryServiceImpl implements SqlQueryService {

    @Autowired
    private DatasourceMapper datasourceMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SqlQueryHistoryMapper sqlQueryHistoryMapper;

    @Autowired
    private JdbcQueryExecutor jdbcQueryExecutor;

    @Autowired
    private QueryPermissionChecker permissionChecker;

    @Autowired
    private FileExporterFactory exporterFactory;

    @Autowired
    private DatabaseMetadataServiceFactory metadataServiceFactory;

    @Value("${sql.query.export.dir:/tmp/sql-exports}")
    private String exportDir;

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("\\bFROM\\s+[\"'`]?([\\w\\d_\\.]+)[\"'`]?",
            Pattern.CASE_INSENSITIVE);

    @Override
    @Transactional
    public SqlQueryResultDTO executeQuery(Long userId, SqlQueryDTO dto) {
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

        // 使用权限检查器验证用户权限
        permissionChecker.checkQueryPermission(user, dto.getDatasourceId(), dto.getSql());

        // 记录SQL历史
        SqlQueryHistory history = recordSqlHistory(userId, dto);

        // 使用JDBC执行器执行查询
        long startTime = System.currentTimeMillis();
        SqlQueryResultDTO result = jdbcQueryExecutor.executeQuery(datasource, dto.getSql());
        long endTime = System.currentTimeMillis();
        result.setExecutionTimeMs(endTime - startTime);

        // 如果需要导出结果
        if (dto.getExportResult() && result.getRows() != null && !result.getRows().isEmpty()) {
            String exportFormat = dto.getExportFormat();
            if (StringUtils.isBlank(exportFormat)) {
                exportFormat = "xlsx"; // 默认为Excel格式
            }

            String fileName = history.getId() + "." + exportFormat;
            String filePath = buildExportFilePath(fileName);

            try {
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
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.EXPORT_FAILED, "导出失败: " + e.getMessage());
            }
        }

        return result;
    }

    @Override
    public SchemaInfoDTO getSchemaInfo(Long userId, Long datasourceId) {
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

        SchemaInfoDTO schemaInfo = new SchemaInfoDTO();
        schemaInfo.setDatabaseName(datasource.getDatabase());

        try {
            // 获取JDBC连接
            Connection conn = jdbcQueryExecutor.getConnection(datasource);

            // 获取对应数据库类型的元数据服务
            DatabaseMetadataService metadataService = metadataServiceFactory.getService(datasource.getType());

            List<SchemaInfoDTO.TableInfoDTO> tables = new ArrayList<>();
            List<String> permittedTables;

            // 如果是管理员，则可以查看所有表
            if (user.getIsAdmin()) {
                permittedTables = metadataService.getAllTables(conn);
            } else {
                // 获取用户有权限的表
                permittedTables = permissionChecker.getPermittedTables(userId, datasourceId, conn);
            }

            // 获取表信息
            for (String tableName : permittedTables) {
                SchemaInfoDTO.TableInfoDTO tableInfo = new SchemaInfoDTO.TableInfoDTO();
                tableInfo.setTableName(tableName);
                tableInfo.setTableComment(metadataService.getTableComment(conn, tableName));
                tableInfo.setColumns(metadataService.getColumnInfo(conn, tableName));
                tables.add(tableInfo);
            }

            schemaInfo.setTables(tables);
            conn.close();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "获取数据库结构失败: " + e.getMessage());
        }

        return schemaInfo;
    }

    @Override
    public Resource getQueryResult(Long queryId) {
        SqlQueryHistory history = sqlQueryHistoryMapper.selectById(queryId);
        if (history == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "查询记录不存在");
        }

        if (StringUtils.isBlank(history.getResultFilePath())) {
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "查询结果文件不存在");
        }

        File resultFile = new File(history.getResultFilePath());
        if (!resultFile.exists()) {
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "查询结果文件不存在");
        }

        return new FileSystemResource(resultFile);
    }

    /**
     * 记录SQL历史
     */
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

    /**
     * 从SQL中提取表名
     */
    private String extractTableName(String sql) {
        Matcher matcher = TABLE_NAME_PATTERN.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 构建导出文件路径
     */
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
}