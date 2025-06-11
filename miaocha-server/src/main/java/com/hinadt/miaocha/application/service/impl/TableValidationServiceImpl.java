package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataServiceFactory;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.common.constants.FieldConstants;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.enums.DatasourceType;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 表验证服务实现类 用于验证数据源中表是否存在 */
@Service
public class TableValidationServiceImpl implements TableValidationService {
    private static final Logger logger = LoggerFactory.getLogger(TableValidationServiceImpl.class);

    /** 数据库类型到JDBC驱动类的映射 */
    private static final Map<DatasourceType, String> DRIVER_CLASS_MAP = new HashMap<>();

    static {
        DRIVER_CLASS_MAP.put(DatasourceType.MYSQL, "com.mysql.cj.jdbc.Driver");
        DRIVER_CLASS_MAP.put(DatasourceType.DORIS, "com.mysql.cj.jdbc.Driver");
        DRIVER_CLASS_MAP.put(DatasourceType.POSTGRESQL, "org.postgresql.Driver");
        DRIVER_CLASS_MAP.put(DatasourceType.ORACLE, "oracle.jdbc.OracleDriver");
    }

    private final DatasourceMapper datasourceMapper;
    private final JdbcQueryExecutor jdbcQueryExecutor;
    private final DatabaseMetadataServiceFactory metadataServiceFactory;

    public TableValidationServiceImpl(
            DatasourceMapper datasourceMapper,
            JdbcQueryExecutor jdbcQueryExecutor,
            DatabaseMetadataServiceFactory metadataServiceFactory) {
        this.datasourceMapper = datasourceMapper;
        this.jdbcQueryExecutor = jdbcQueryExecutor;
        this.metadataServiceFactory = metadataServiceFactory;
    }

    /**
     * 检查指定数据源中是否存在指定的表
     *
     * @param datasourceId 数据源ID
     * @param tableName 表名
     * @return 表是否存在
     */
    @Override
    public boolean isTableExists(Long datasourceId, String tableName) {
        // 参数验证
        validateParameters(datasourceId, tableName);

        // 获取数据源信息
        DatasourceInfo datasourceInfo = getDatasource(datasourceId);

        try {
            // 获取数据源类型
            DatasourceType datasourceType = getDatasourceType(datasourceInfo);

            // 方法1: 使用JdbcTemplate查询
            return checkTableExistsWithJdbcTemplate(datasourceInfo, datasourceType, tableName);

            // 方法2: 使用DatabaseMetadataService (备用方案)
            // return checkTableExistsWithMetadataService(datasource, tableName);
        } catch (Exception e) {
            logger.error("检查表 {} 是否存在时发生错误: {}", tableName, e.getMessage(), e);
            return false;
        }
    }

    /** 验证参数 */
    private void validateParameters(Long datasourceId, String tableName) {
        if (datasourceId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "数据源ID不能为空");
        }

        if (!StringUtils.hasText(tableName)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "表名不能为空");
        }
    }

    /** 获取数据源 */
    private DatasourceInfo getDatasource(Long datasourceId) {
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(datasourceId);
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND, "指定的数据源不存在");
        }
        return datasourceInfo;
    }

    /** 获取数据源类型 */
    private DatasourceType getDatasourceType(DatasourceInfo datasourceInfo) {
        DatasourceType datasourceType = DatasourceType.fromType(datasourceInfo.getType());
        if (datasourceType == null) {
            throw new BusinessException(
                    ErrorCode.DATASOURCE_TYPE_NOT_SUPPORTED,
                    "不支持的数据源类型: " + datasourceInfo.getType());
        }
        return datasourceType;
    }

    /** 使用JdbcTemplate检查表是否存在 */
    private boolean checkTableExistsWithJdbcTemplate(
            DatasourceInfo datasourceInfo, DatasourceType datasourceType, String tableName) {
        // 创建临时数据源连接
        DriverManagerDataSource dataSource = createDataSource(datasourceInfo, datasourceType);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // 根据数据库类型选择不同的查询语句
        String dbType = datasourceInfo.getType().toLowerCase();

        // MySQL和Doris使用相同的查询
        if (dbType.contains("mysql") || dbType.contains("doris")) {
            String query =
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND"
                            + " table_name = ?";
            String dbName = datasourceInfo.getDatabase();
            return jdbcTemplate.queryForObject(query, Integer.class, dbName, tableName) > 0;
        } else {
            // 通用查询，尝试直接查询表
            try {
                jdbcTemplate.queryForList("SELECT 1 FROM " + tableName + " LIMIT 1");
                return true;
            } catch (Exception e) {
                logger.debug("尝试查询表 {} 失败，可能表不存在: {}", tableName, e.getMessage());
                return false;
            }
        }
    }

    @Override
    public void validateTableStructure(Long datasourceId, String tableName) {
        logger.debug("开始验证表结构: 数据源ID={}, 表名={}", datasourceId, tableName);

        // 首先检查表是否存在
        if (!isTableExists(datasourceId, tableName)) {
            throw new BusinessException(
                    ErrorCode.LOGSTASH_TARGET_TABLE_NOT_FOUND,
                    String.format("数据源(ID: %d)中不存在表 '%s'", datasourceId, tableName));
        }

        // 获取数据源信息
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(datasourceId);
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        try (Connection conn = jdbcQueryExecutor.getConnection(datasourceInfo)) {
            // 获取数据库元数据服务
            DatabaseMetadataService metadataService =
                    metadataServiceFactory.getService(datasourceInfo.getType());

            // 获取表的列信息
            List<SchemaInfoDTO.ColumnInfoDTO> columns =
                    metadataService.getColumnInfo(conn, tableName);

            // 检查是否包含message字段
            boolean hasMessageField =
                    columns.stream()
                            .anyMatch(
                                    column ->
                                            FieldConstants.MESSAGE_FIELD.equalsIgnoreCase(
                                                    column.getColumnName()));

            if (!hasMessageField) {
                logger.warn("表 '{}' 缺少必需的message字段，无法进行关键字搜索", tableName);
                throw new BusinessException(
                        ErrorCode.TABLE_MESSAGE_FIELD_MISSING,
                        String.format(
                                "表 '%s' 缺少必需的 '%s' 字段，无法进行关键字搜索",
                                tableName, FieldConstants.MESSAGE_FIELD));
            }

            logger.info("表结构验证成功: 数据源ID={}, 表名={}, 包含message字段", datasourceId, tableName);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("验证表结构时发生异常: 数据源ID={}, 表名={}", datasourceId, tableName, e);
            throw new BusinessException(
                    ErrorCode.TABLE_FIELD_VALIDATION_FAILED,
                    String.format("验证表 '%s' 结构时发生异常: %s", tableName, e.getMessage()));
        }
    }

    /** 使用DatabaseMetadataService检查表是否存在 备用方法，当前未使用 */
    private boolean checkTableExistsWithMetadataService(
            DatasourceInfo datasourceInfo, String tableName) {
        try (Connection connection = jdbcQueryExecutor.getConnection(datasourceInfo)) {
            // 获取对应的元数据服务
            var metadataService = metadataServiceFactory.getService(datasourceInfo.getType());
            // 获取所有表并检查是否包含目标表
            List<String> tables = metadataService.getAllTables(connection);
            return tables.contains(tableName);
        } catch (SQLException e) {
            logger.error("使用元数据服务检查表 {} 是否存在时发生错误: {}", tableName, e.getMessage(), e);
            return false;
        }
    }

    /** 创建数据源 */
    private DriverManagerDataSource createDataSource(
            DatasourceInfo datasourceInfo, DatasourceType datasourceType) {
        // 构建 JDBC URL
        String url =
                datasourceType.buildJdbcUrl(
                        datasourceInfo.getIp(),
                        datasourceInfo.getPort(),
                        datasourceInfo.getDatabase(),
                        datasourceInfo.getJdbcParams());

        // 创建临时数据源连接
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        // 设置驱动类
        String driverClassName = DRIVER_CLASS_MAP.get(datasourceType);
        if (driverClassName != null) {
            dataSource.setDriverClassName(driverClassName);
        }

        dataSource.setUrl(url);
        dataSource.setUsername(datasourceInfo.getUsername());
        dataSource.setPassword(datasourceInfo.getPassword());

        return dataSource;
    }
}
