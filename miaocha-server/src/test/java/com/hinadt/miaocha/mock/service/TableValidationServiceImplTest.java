package com.hinadt.miaocha.mock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.database.DatabaseMetadataService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataServiceFactory;
import com.hinadt.miaocha.application.service.impl.TableValidationServiceImpl;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.exception.QueryFieldNotExistsException;
import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.infrastructure.mapper.DatasourceMapper;
import com.hinadt.miaocha.infrastructure.mapper.ModuleInfoMapper;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** TableValidationServiceImpl 全面单元测试 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TableValidationServiceImpl 全面单元测试")
class TableValidationServiceImplTest {

    @Mock private ModuleInfoMapper moduleInfoMapper;
    @Mock private DatasourceMapper datasourceMapper;
    @Mock private JdbcQueryExecutor jdbcQueryExecutor;
    @Mock private DatabaseMetadataServiceFactory metadataServiceFactory;
    @Mock private DatabaseMetadataService databaseMetadataService;
    @Mock private Connection connection;

    @InjectMocks private TableValidationServiceImpl tableValidationService;

    private ModuleInfo testModuleInfo;
    private DatasourceInfo testDatasourceInfo;

    @BeforeEach
    void setUp() throws Exception {
        // 创建测试模块信息
        testModuleInfo = new ModuleInfo();
        testModuleInfo.setId(1L);
        testModuleInfo.setName("测试模块");
        testModuleInfo.setTableName("test_table");
        testModuleInfo.setDatasourceId(1L);

        // 创建测试数据源信息
        testDatasourceInfo = new DatasourceInfo();
        testDatasourceInfo.setId(1L);
        testDatasourceInfo.setName("测试数据源");
        testDatasourceInfo.setType("DORIS");
        testDatasourceInfo.setJdbcUrl("jdbc:mysql://localhost:9030/test_db");

        // 设置默认mock行为（lenient模式避免不必要的stubbing警告）
        lenient().when(datasourceMapper.selectById(1L)).thenReturn(testDatasourceInfo);
        lenient().when(jdbcQueryExecutor.getConnection(testDatasourceInfo)).thenReturn(connection);
        lenient()
                .when(metadataServiceFactory.getService("DORIS"))
                .thenReturn(databaseMetadataService);
    }

    @Nested
    @DisplayName("validateDorisSql 方法测试 - 表名提取场景")
    class ValidateDorisSqlTableNameTest {

        @ParameterizedTest
        @CsvSource({
            "CREATE TABLE simple_table (id INT), simple_table",
            "CREATE TABLE `backtick_table` (id INT), backtick_table",
            "CREATE TABLE database.simple_table (id INT), simple_table",
            "CREATE TABLE `database`.simple_table (id INT), simple_table",
            "CREATE TABLE database.`backtick_table` (id INT), backtick_table",
            "CREATE TABLE `database`.`backtick_table` (id INT), backtick_table",
            "CREATE TABLE IF NOT EXISTS test_table (id INT), test_table",
            "CREATE TABLE IF NOT EXISTS `test_table` (id INT), test_table",
            "CREATE TABLE IF NOT EXISTS db.`test_table` (id INT), test_table",
            "CREATE TABLE IF NOT EXISTS `db`.`test_table` (id INT), test_table"
        })
        @DisplayName("各种表名格式的CREATE TABLE语句验证通过")
        void testValidateDorisSql_VariousTableNameFormats(String sql, String expectedTableName) {
            testModuleInfo.setTableName(expectedTableName);

            assertDoesNotThrow(() -> tableValidationService.validateDorisSql(testModuleInfo, sql));
        }

        @Test
        @DisplayName("复杂数据库前缀表名验证")
        void testValidateDorisSql_ComplexDatabasePrefix() {
            testModuleInfo.setTableName("my_log_table");

            String sql =
                    "CREATE TABLE `analytics_db`.`my_log_table` ("
                            + "`id` BIGINT NOT NULL, "
                            + "`message` TEXT "
                            + ") ENGINE=OLAP";

            assertDoesNotThrow(() -> tableValidationService.validateDorisSql(testModuleInfo, sql));
        }

        @Test
        @DisplayName("带特殊字符的表名验证")
        void testValidateDorisSql_SpecialCharacterTableName() {
            testModuleInfo.setTableName("log_table_2024_01");

            String sql =
                    "CREATE TABLE `log_table_2024_01` ("
                            + "`timestamp` DATETIME, "
                            + "`log_level` VARCHAR(10) "
                            + ")";

            assertDoesNotThrow(() -> tableValidationService.validateDorisSql(testModuleInfo, sql));
        }

        @ParameterizedTest
        @CsvSource({
            "CREATE TABLE wrong_table (id INT), test_table",
            "CREATE TABLE `wrong_table` (id INT), test_table",
            "CREATE TABLE db.wrong_table (id INT), test_table",
            "CREATE TABLE `db`.`wrong_table` (id INT), test_table"
        })
        @DisplayName("表名不匹配时抛出异常")
        void testValidateDorisSql_TableNameMismatch(String sql, String moduleTableName) {
            testModuleInfo.setTableName(moduleTableName);

            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> tableValidationService.validateDorisSql(testModuleInfo, sql));

            assertEquals(ErrorCode.SQL_TABLE_NAME_MISMATCH, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("parseFieldNamesFromCreateTableSql 方法测试 - 复杂SQL场景")
    class ParseFieldNamesComplexSqlTest {

        @Test
        @DisplayName("标准MySQL建表语句解析")
        void testParseFieldNames_StandardMySQL() {
            String sql =
                    """
                CREATE TABLE user_logs (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    username VARCHAR(50) NOT NULL,
                    email VARCHAR(100) UNIQUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    status ENUM('active', 'inactive') DEFAULT 'active',
                    INDEX idx_user_id (user_id),
                    INDEX idx_email (email),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """;

            List<String> result = tableValidationService.parseFieldNamesFromCreateTableSql(sql);

            assertEquals(7, result.size());
            assertEquals(
                    Arrays.asList(
                            "id",
                            "user_id",
                            "username",
                            "email",
                            "created_at",
                            "updated_at",
                            "status"),
                    result);
        }

        @Test
        @DisplayName("带反引号字段名的建表语句解析")
        void testParseFieldNames_BacktickFields() {
            String sql =
                    """
                CREATE TABLE `order_logs` (
                    `order_id` BIGINT NOT NULL,
                    `user_name` VARCHAR(100),
                    `order_time` DATETIME,
                    `total_amount` DECIMAL(10,2),
                    `status` VARCHAR(20),
                    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

            List<String> result = tableValidationService.parseFieldNamesFromCreateTableSql(sql);

            assertEquals(6, result.size());
            assertEquals(
                    Arrays.asList(
                            "order_id",
                            "user_name",
                            "order_time",
                            "total_amount",
                            "status",
                            "create_time"),
                    result);
        }

        @Test
        @DisplayName("混合反引号字段名的建表语句解析")
        void testParseFieldNames_MixedBacktickFields() {
            String sql =
                    """
                CREATE TABLE mixed_table (
                    id BIGINT NOT NULL,
                    `user_id` BIGINT,
                    normal_field VARCHAR(50),
                    `special_field` TEXT,
                    another_field INT,
                    `final_field` DATETIME
                )
                """;

            List<String> result = tableValidationService.parseFieldNamesFromCreateTableSql(sql);

            assertEquals(6, result.size());
            assertEquals(
                    Arrays.asList(
                            "id",
                            "user_id",
                            "normal_field",
                            "special_field",
                            "another_field",
                            "final_field"),
                    result);
        }

        @Test
        @DisplayName("真实Doris复杂建表语句解析")
        void testParseFieldNames_ComplexDorisCreateTable() {
            String sql =
                    """
                CREATE TABLE `log_table_hina_cloud_variant` (
                  `log_time` datetime(3) NOT NULL COMMENT '日志时间',
                  `host` text NULL COMMENT "hostname or ip",
                  `source` text NULL COMMENT "log path",
                  `log_offset` text NULL COMMENT "日志所在kafka主题偏移量",
                  `logId` text NULL COMMENT "日志ID",
                  `time` text NULL COMMENT "日志打印时间",
                  `message` variant NULL COMMENT "消息内容",
                  `level` varchar(10) NULL COMMENT "日志级别",
                  `thread` varchar(50) NULL COMMENT "线程名",
                  `logger` text NULL COMMENT "日志器名称",
                  INDEX idx_message (`message`) USING INVERTED PROPERTIES("support_phrase" = "true", "parser" = "unicode", "lower_case" = "true"),
                  INDEX idx_level (`level`) USING INVERTED,
                  INDEX idx_time_range (`log_time`) USING INVERTED
                ) ENGINE=OLAP
                DUPLICATE KEY(`log_time`)
                AUTO PARTITION BY RANGE (date_trunc(`log_time`, 'hour'))
                ()
                DISTRIBUTED BY RANDOM BUCKETS 6
                PROPERTIES (
                "file_cache_ttl_seconds" = "0",
                "dynamic_partition.enable" = "true",
                "storage_format" = "V2"
                );
                """;

            List<String> result = tableValidationService.parseFieldNamesFromCreateTableSql(sql);

            assertEquals(10, result.size());
            assertEquals(
                    Arrays.asList(
                            "log_time",
                            "host",
                            "source",
                            "log_offset",
                            "logId",
                            "time",
                            "message",
                            "level",
                            "thread",
                            "logger"),
                    result);

            // 确保索引定义不被解析为字段
            assertFalse(result.contains("idx_message"));
            assertFalse(result.contains("idx_level"));
            assertFalse(result.contains("idx_time_range"));
        }

        @Test
        @DisplayName("带COMMENT和约束的复杂建表语句解析")
        void testParseFieldNames_WithCommentsAndConstraints() {
            String sql =
                    """
                CREATE TABLE complex_table (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    tenant_id BIGINT NOT NULL COMMENT '租户ID',
                    user_name VARCHAR(100) NOT NULL COMMENT '用户名',
                    email VARCHAR(255) UNIQUE COMMENT '电子邮件',
                    phone VARCHAR(20) NULL COMMENT '手机号码',
                    birth_date DATE COMMENT '出生日期',
                    salary DECIMAL(10,2) DEFAULT 0.00 COMMENT '薪资',
                    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
                    metadata JSON COMMENT '元数据',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_email (email),
                    KEY idx_tenant_user (tenant_id, user_name),
                    KEY idx_phone (phone),
                    CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';
                """;

            List<String> result = tableValidationService.parseFieldNamesFromCreateTableSql(sql);

            assertEquals(11, result.size());
            assertEquals(
                    Arrays.asList(
                            "id",
                            "tenant_id",
                            "user_name",
                            "email",
                            "phone",
                            "birth_date",
                            "salary",
                            "is_active",
                            "metadata",
                            "created_at",
                            "updated_at"),
                    result);

            // 确保约束定义不被解析为字段
            assertFalse(result.contains("PRIMARY"));
            assertFalse(result.contains("UNIQUE"));
            assertFalse(result.contains("CONSTRAINT"));
            assertFalse(result.contains("uk_email"));
            assertFalse(result.contains("idx_tenant_user"));
            assertFalse(result.contains("fk_tenant"));
        }

        @Test
        @DisplayName("ClickHouse建表语句解析")
        void testParseFieldNames_ClickHouseCreateTable() {
            String sql =
                    """
                CREATE TABLE events (
                    event_id UInt64,
                    event_time DateTime,
                    user_id UInt32,
                    event_type LowCardinality(String),
                    properties Map(String, String),
                    count UInt32 DEFAULT 1,
                    created_at DateTime DEFAULT now()
                ) ENGINE = MergeTree()
                PARTITION BY toYYYYMM(event_time)
                ORDER BY (user_id, event_time)
                SETTINGS index_granularity = 8192;
                """;

            List<String> result = tableValidationService.parseFieldNamesFromCreateTableSql(sql);

            assertEquals(7, result.size());
            assertEquals(
                    Arrays.asList(
                            "event_id",
                            "event_time",
                            "user_id",
                            "event_type",
                            "properties",
                            "count",
                            "created_at"),
                    result);
        }

        @Test
        @DisplayName("PostgreSQL建表语句解析")
        void testParseFieldNames_PostgreSQLCreateTable() {
            String sql =
                    """
                CREATE TABLE user_activity (
                    id SERIAL PRIMARY KEY,
                    user_id INTEGER NOT NULL,
                    activity_type VARCHAR(50) NOT NULL,
                    activity_data JSONB,
                    ip_address INET,
                    user_agent TEXT,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id),
                    CONSTRAINT chk_activity_type CHECK (activity_type IN ('login', 'logout', 'view', 'action'))
                );
                """;

            List<String> result = tableValidationService.parseFieldNamesFromCreateTableSql(sql);

            assertEquals(7, result.size());
            assertEquals(
                    Arrays.asList(
                            "id",
                            "user_id",
                            "activity_type",
                            "activity_data",
                            "ip_address",
                            "user_agent",
                            "created_at"),
                    result);
        }

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "CREATE TABLE test (  id  BIGINT  ,  name  VARCHAR(255)  )", // 额外空格
                    "CREATE\nTABLE\ntest\n(\nid\nBIGINT,\nname\nVARCHAR(255)\n)", // 换行
                    "CREATE TABLE test(id BIGINT,name VARCHAR(255))", // 无空格
                    "CREATE TABLE test ( id BIGINT , name VARCHAR(255) )", // 标准格式
                })
        @DisplayName("不同格式的SQL语句都能正确解析")
        void testParseFieldNames_DifferentFormats(String sql) {
            List<String> result = tableValidationService.parseFieldNamesFromCreateTableSql(sql);

            assertEquals(2, result.size());
            assertEquals(Arrays.asList("id", "name"), result);
        }

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "SELECT * FROM table", // 非CREATE TABLE语句
                    "CREATE VIEW test AS SELECT * FROM table", // CREATE VIEW
                    "CREATE INDEX idx ON table(col)", // CREATE INDEX
                    "CREATE TABLE", // 不完整语句
                    "CREATE TABLE test_table", // 缺少字段定义
                    "CREATE TABLE test_table ()", // 空字段定义
                    "invalid sql statement", // 无效SQL
                    "", // 空字符串
                    "   ", // 空白字符
                })
        @DisplayName("无效或不支持的SQL语句返回空列表")
        void testParseFieldNames_InvalidSQL(String sql) {
            List<String> result = tableValidationService.parseFieldNamesFromCreateTableSql(sql);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("字段名包含SQL关键字的建表语句解析")
        void testParseFieldNames_FieldNamesWithKeywords() {
            String sql =
                    """
                CREATE TABLE keyword_table (
                    `order` BIGINT,
                    `select` VARCHAR(50),
                    `from` VARCHAR(100),
                    `where` TEXT,
                    `group` VARCHAR(20),
                    `table` VARCHAR(30)
                )
                """;

            List<String> result = tableValidationService.parseFieldNamesFromCreateTableSql(sql);

            assertEquals(6, result.size());
            assertEquals(
                    Arrays.asList("order", "select", "from", "where", "group", "table"), result);
        }

        @Test
        @DisplayName("字段名包含数字和下划线的建表语句解析")
        void testParseFieldNames_FieldNamesWithNumbersAndUnderscores() {
            String sql =
                    """
                CREATE TABLE number_underscore_table (
                    field_1 BIGINT,
                    field2 VARCHAR(50),
                    _field3 TEXT,
                    field_4_name VARCHAR(100),
                    field5_2024 DATETIME,
                    `2024_field` VARCHAR(20)
                )
                """;

            List<String> result = tableValidationService.parseFieldNamesFromCreateTableSql(sql);

            assertEquals(6, result.size());
            assertEquals(
                    Arrays.asList(
                            "field_1",
                            "field2",
                            "_field3",
                            "field_4_name",
                            "field5_2024",
                            "2024_field"),
                    result);
        }
    }

    @Nested
    @DisplayName("isTableExists 方法测试")
    class IsTableExistsTest {

        @Test
        @DisplayName("模块信息为null时返回false")
        void testIsTableExists_NullModuleInfo() {
            boolean result = tableValidationService.isTableExists(null);
            assertFalse(result);
        }

        @Test
        @DisplayName("未配置表名时返回false")
        void testIsTableExists_NoTableName() {
            testModuleInfo.setTableName(null);

            boolean result = tableValidationService.isTableExists(testModuleInfo);

            assertFalse(result);
        }

        @Test
        @DisplayName("表在数据库中存在时返回true")
        void testIsTableExists_TableExistsInDatabase() throws Exception {
            List<String> allTables = Arrays.asList("test_table", "other_table");
            when(databaseMetadataService.getAllTables(connection)).thenReturn(allTables);

            boolean result = tableValidationService.isTableExists(testModuleInfo);

            assertTrue(result);
        }

        @Test
        @DisplayName("表在数据库中不存在时返回false")
        void testIsTableExists_TableNotExistsInDatabase() throws Exception {
            List<String> allTables = Arrays.asList("other_table1", "other_table2");
            when(databaseMetadataService.getAllTables(connection)).thenReturn(allTables);

            boolean result = tableValidationService.isTableExists(testModuleInfo);

            assertFalse(result);
        }

        @Test
        @DisplayName("数据库连接异常时返回false")
        void testIsTableExists_DatabaseException() throws Exception {
            when(jdbcQueryExecutor.getConnection(testDatasourceInfo))
                    .thenThrow(new RuntimeException("连接失败"));

            boolean result = tableValidationService.isTableExists(testModuleInfo);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("getTableFieldNames 方法测试")
    class GetTableFieldNamesTest {

        @Test
        @DisplayName("优先从建表SQL解析字段名")
        void testGetTableFieldNames_FromSQL() {
            testModuleInfo.setDorisSql("CREATE TABLE test_table (id BIGINT, name VARCHAR(255))");
            when(moduleInfoMapper.selectById(1L)).thenReturn(testModuleInfo);

            List<String> result = tableValidationService.getTableFieldNames(1L);

            assertEquals(2, result.size());
            assertEquals(Arrays.asList("id", "name"), result);
            // 不应该调用数据库元数据相关方法
            verify(datasourceMapper, never()).selectById(any());
        }

        @Test
        @DisplayName("SQL解析失败时从数据库元数据获取")
        void testGetTableFieldNames_FromDatabaseMetadata() throws Exception {
            testModuleInfo.setDorisSql(null); // 没有SQL
            when(moduleInfoMapper.selectById(1L)).thenReturn(testModuleInfo);

            // 模拟数据库元数据返回的列信息
            SchemaInfoDTO.ColumnInfoDTO column1 = new SchemaInfoDTO.ColumnInfoDTO();
            column1.setColumnName("id");
            SchemaInfoDTO.ColumnInfoDTO column2 = new SchemaInfoDTO.ColumnInfoDTO();
            column2.setColumnName("name");
            List<SchemaInfoDTO.ColumnInfoDTO> columns = Arrays.asList(column1, column2);

            when(databaseMetadataService.getColumnInfo(connection, "test_table"))
                    .thenReturn(columns);

            List<String> result = tableValidationService.getTableFieldNames(1L);

            assertEquals(2, result.size());
            assertEquals(Arrays.asList("id", "name"), result);
            verify(databaseMetadataService).getColumnInfo(connection, "test_table");
        }
    }

    @Nested
    @DisplayName("validateQueryConfigFields 方法测试")
    class ValidateQueryConfigFieldsTest {

        @Test
        @DisplayName("所有字段都存在时验证通过")
        void testValidateQueryConfigFields_AllFieldsExist() {
            testModuleInfo.setDorisSql(
                    "CREATE TABLE test_table (id BIGINT, name VARCHAR(255), age INT)");
            when(moduleInfoMapper.selectById(1L)).thenReturn(testModuleInfo);

            assertDoesNotThrow(
                    () ->
                            tableValidationService.validateQueryConfigFields(
                                    testModuleInfo, Arrays.asList("id", "name", "age")));
        }

        @Test
        @DisplayName("部分字段不存在时抛出QueryFieldNotExistsException")
        void testValidateQueryConfigFields_SomeFieldsNotExist() {
            testModuleInfo.setDorisSql("CREATE TABLE test_table (id BIGINT, name VARCHAR(255))");
            when(moduleInfoMapper.selectById(1L)).thenReturn(testModuleInfo);

            QueryFieldNotExistsException exception =
                    assertThrows(
                            QueryFieldNotExistsException.class,
                            () ->
                                    tableValidationService.validateQueryConfigFields(
                                            testModuleInfo,
                                            Arrays.asList(
                                                    "id",
                                                    "name",
                                                    "non_existent_field",
                                                    "another_missing")));

            assertEquals("测试模块", exception.getModuleName());
            assertEquals("test_table", exception.getTableName());
            assertEquals(
                    Arrays.asList("non_existent_field", "another_missing"),
                    exception.getNonExistentFields());
        }

        @Test
        @DisplayName("没有建表SQL时跳过验证")
        void testValidateQueryConfigFields_NoDorisSql() {
            testModuleInfo.setDorisSql(null);

            assertDoesNotThrow(
                    () ->
                            tableValidationService.validateQueryConfigFields(
                                    testModuleInfo, Arrays.asList("field1", "field2")));
        }

        @Test
        @DisplayName("配置字段为null或空时不抛出异常")
        void testValidateQueryConfigFields_NullOrEmptyFields() {
            assertDoesNotThrow(
                    () -> tableValidationService.validateQueryConfigFields(testModuleInfo, null));

            assertDoesNotThrow(
                    () ->
                            tableValidationService.validateQueryConfigFields(
                                    testModuleInfo, Collections.emptyList()));
        }
    }

    // ==================== 新增的 SQL 处理功能测试 ====================

    @Nested
    @DisplayName("SQL语句处理功能测试")
    class SqlProcessingTest {

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "SELECT * FROM users",
                    "select id, name from users where id > 100",
                    "  SELECT COUNT(*) FROM logs  ",
                    "SELECT u.name, l.message FROM users u JOIN logs l ON u.id = l.user_id"
                })
        @DisplayName("SELECT语句检测")
        void testIsSelectStatement_SelectQueries(String sql) {
            assertTrue(tableValidationService.isSelectStatement(sql), "应该正确识别SELECT语句: " + sql);
        }

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "INSERT INTO users (name) VALUES ('test')",
                    "UPDATE users SET name = 'updated' WHERE id = 1",
                    "DELETE FROM users WHERE id = 1",
                    "CREATE TABLE test_table (id INT, name VARCHAR(50))",
                    "DROP TABLE test_table",
                    "ALTER TABLE users ADD COLUMN email VARCHAR(100)",
                    "",
                    "   ",
                    "invalid sql"
                })
        @DisplayName("非SELECT语句检测")
        void testIsSelectStatement_NonSelectQueries(String sql) {
            assertFalse(tableValidationService.isSelectStatement(sql), "应该正确识别非SELECT语句: " + sql);
        }

        @Test
        @DisplayName("LIMIT处理 - SELECT语句添加默认LIMIT")
        void testProcessSqlWithLimit_SelectStatements() {
            String selectSql = "SELECT * FROM users";
            String result = tableValidationService.processSqlWithLimit(selectSql);

            assertTrue(result.contains("LIMIT 1000"), "SELECT语句应该添加默认LIMIT: " + result);
        }

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "INSERT INTO users (name) VALUES ('test')",
                    "UPDATE users SET name = 'updated' WHERE id = 1",
                    "DELETE FROM users WHERE id = 1",
                    "CREATE TABLE test_table (id INT, name VARCHAR(50))"
                })
        @DisplayName("LIMIT处理 - 非SELECT语句保持不变")
        void testProcessSqlWithLimit_NonSelectStatements(String sql) {
            String result = tableValidationService.processSqlWithLimit(sql);

            assertEquals(sql, result, "非SELECT语句不应该被修改: " + result);
        }

        @Test
        @DisplayName("LIMIT处理 - 已有合法LIMIT保持不变")
        void testProcessSqlWithLimit_ExistingValidLimit() {
            String sqlWithLimit = "SELECT * FROM users LIMIT 500";
            String result = tableValidationService.processSqlWithLimit(sqlWithLimit);

            assertEquals(sqlWithLimit, result, "已有合法LIMIT应该保持不变: " + result);
        }

        @Test
        @DisplayName("LIMIT处理 - 以分号结尾的LIMIT保持不变")
        void testProcessSqlWithLimit_ExistingValidLimitWithSemicolon() {
            String sqlWithLimit = "SELECT *, EXPREC(host) FROM log_table_xunxin_variant LIMIT 10 ;";
            String result = tableValidationService.processSqlWithLimit(sqlWithLimit);

            assertEquals(sqlWithLimit, result, "以分号结尾的合法LIMIT应该保持不变: " + result);
        }

        @Test
        @DisplayName("LIMIT处理 - 以分号结尾的LIMIT OFFSET格式保持不变")
        void testProcessSqlWithLimit_ExistingValidLimitOffsetWithSemicolon() {
            String sqlWithLimit = "SELECT * FROM users LIMIT 10, 20;";
            String result = tableValidationService.processSqlWithLimit(sqlWithLimit);

            assertEquals(sqlWithLimit, result, "以分号结尾的合法LIMIT OFFSET应该保持不变: " + result);
        }

        @Test
        @DisplayName("LIMIT处理 - 超过最大LIMIT抛出异常")
        void testProcessSqlWithLimit_ExceedsMaxLimit() {
            String sqlWithLargeLimit = "SELECT * FROM users LIMIT 20000";

            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> {
                                tableValidationService.processSqlWithLimit(sqlWithLargeLimit);
                            });

            assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("查询结果数量限制不能超过"));
        }

        @Test
        @DisplayName("表名提取 - FROM子句")
        void testExtractTableNames_FromClause() {
            String sql = "SELECT * FROM users WHERE id = 1";
            java.util.Set<String> tableNames = tableValidationService.extractTableNames(sql);

            assertEquals(1, tableNames.size());
            assertTrue(tableNames.contains("users"));
        }

        @Test
        @DisplayName("表名提取 - 数据库前缀")
        void testExtractTableNames_WithDatabasePrefix() {
            String sql = "SELECT * FROM mydb.users WHERE id = 1";
            java.util.Set<String> tableNames = tableValidationService.extractTableNames(sql);

            assertEquals(1, tableNames.size());
            assertTrue(tableNames.contains("users"), "应该提取表名部分，忽略数据库前缀");
        }

        @Test
        @DisplayName("表名提取 - 空SQL返回空集合")
        void testExtractTableNames_EmptySQL() {
            java.util.Set<String> tableNames = tableValidationService.extractTableNames("");
            assertTrue(tableNames.isEmpty());

            tableNames = tableValidationService.extractTableNames(null);
            assertTrue(tableNames.isEmpty());
        }

        @Test
        @DisplayName("带注释和空行的复杂SELECT语句检测")
        void testIsSelectStatement_WithCommentsAndBlanks() {
            String sql =
                    """
                -- 用于模板中携带最后一条message和查询时间范围

                SELECT
                    message[\"msg\"] AS \"raw_message\",
                    CONCAT(
                        DATE_FORMAT(NOW() - INTERVAL 10 minute, '%Y-%m-%d %H:%i:%S'),
                        ' 至 ',
                        DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%S')
                    ) AS raw_time_range,
                    1 as \"value\"
                FROM log_db.log_xunxin_pro
                WHERE
                    log_time >= NOW() - INTERVAL 10 MINUTE
                    and message[\"msg\"] LIKE '%集群,批次记录新增失败%'
                ORDER BY log_time DESC
                LIMIT 1
                """;
            assertTrue(tableValidationService.isSelectStatement(sql), "带注释和空行的复杂SELECT语句应该被识别");
        }

        @Nested
        @DisplayName("复杂 SQL 语句类型检测测试")
        class ComplexSqlStatementDetectionTest {

            @ParameterizedTest
            @ValueSource(
                    strings = {
                        // CTE (公用表表达式) 查询
                        "WITH user_stats AS (SELECT user_id, COUNT(*) as cnt FROM logs GROUP BY"
                                + " user_id) SELECT * FROM user_stats WHERE cnt > 10",

                        // 多层嵌套 CTE
                        "WITH RECURSIVE t(n) AS (VALUES (1) UNION ALL SELECT n+1 FROM t WHERE n <"
                                + " 100) SELECT sum(n) FROM t",

                        // 括号包围的 SELECT
                        "(SELECT id, name FROM users WHERE active = 1)",

                        // 嵌套子查询
                        "SELECT u.name, (SELECT COUNT(*) FROM orders o WHERE o.user_id = u.id) as"
                                + " order_count FROM users u",

                        // 括号包围的 CTE
                        "(WITH top_users AS (SELECT * FROM users ORDER BY score DESC LIMIT 10)"
                                + " SELECT * FROM top_users)",

                        // 复杂的 UNION 查询
                        "SELECT 'active' as status, COUNT(*) FROM users WHERE active = 1 UNION ALL"
                                + " SELECT 'inactive', COUNT(*) FROM users WHERE active = 0"
                    })
            @DisplayName("CTE 和复杂嵌套查询检测")
            void testIsSelectStatement_ComplexQueries(String sql) {
                assertTrue(tableValidationService.isSelectStatement(sql), "应该正确识别复杂查询类型: " + sql);
            }

            @ParameterizedTest
            @ValueSource(
                    strings = {
                        // 带有复杂注释的 SELECT
                        """
                /* 多行注释
                   查询用户统计信息
                   创建日期: 2025-01-23 */
                SELECT user_id, COUNT(*) as total
                FROM user_actions -- 行尾注释
                WHERE action_date >= '2025-01-01'
                GROUP BY user_id
                """,

                        // 字符中包含注释符号
                        "SELECT '-- 这不是注释' as comment_text, '/* 这也不是注释 */' as block_comment FROM"
                                + " dual",

                        // 混合注释类型
                        """
                -- 单行注释
                /* 块注释 */ SELECT
                    field1, -- 另一个单行注释
                    field2 /* 行内块注释 */
                FROM table1
                /*
                   多行块注释
                   包含多行内容
                */
                WHERE condition = 'value -- 字符串中的注释符号'
                """,

                        // 注释中包含 SQL 关键字
                        """
                -- INSERT INTO fake_table VALUES (1)
                /* DELETE FROM another_table WHERE id = 1 */
                SELECT real_field FROM real_table
                """,

                        // 嵌套注释（如果数据库支持）
                        """
                /* 外层注释
                   /* 内层注释 */
                   外层注释继续
                */
                SELECT nested_comment_test FROM test_table
                """
                    })
            @DisplayName("带有各种复杂注释的 SELECT 语句检测")
            void testIsSelectStatement_ComplexComments(String sql) {
                assertTrue(
                        tableValidationService.isSelectStatement(sql),
                        "应该正确处理复杂注释并识别 SELECT 语句: " + sql);
            }

            @ParameterizedTest
            @ValueSource(
                    strings = {
                        // 带转义字符的查询
                        "SELECT 'It\\'s a test' as escaped_quote, \"He said \\\"Hello\\\"\" as"
                                + " escaped_double FROM test",

                        // 反引号标识符
                        "SELECT `user`.`name`, `user`.`email` FROM `user_table` `user`",

                        // 复杂的字符串处理
                        "SELECT CONCAT('SELECT * FROM users; -- ', 'This is not a real query') as"
                                + " fake_query FROM dual",

                        // 包含各种引号的复杂查询
                        """
                SELECT
                    field1,
                    'Single quote string with -- comment symbols',
                    "Double quote string with /* comment */ symbols",
                    `backtick_field`,
                    CONCAT('String with \\' escaped quote')
                FROM complex_table
                WHERE description LIKE '%-- not a comment%'
                """
                    })
            @DisplayName("字符串转义和引号处理测试")
            void testIsSelectStatement_StringEscaping(String sql) {
                assertTrue(tableValidationService.isSelectStatement(sql), "应该正确处理字符串转义和引号: " + sql);
            }

            @ParameterizedTest
            @ValueSource(
                    strings = {
                        // 前面有多个空行和注释
                        """


                -- 注释1

                /* 注释2 */


                SELECT * FROM table1
                """,

                        // 复杂的空白字符
                        "\t\n\r  \t  SELECT   field1  \t\n  FROM  \r\n  table2  \t",

                        // 混合空白和注释
                        """
                \t-- 制表符开头的注释
                \r\n/* 换行符和回车符 */\r
                \n\t  SELECT * FROM mixed_whitespace  \t\r\n
                """
                    })
            @DisplayName("复杂空白字符和格式处理")
            void testIsSelectStatement_ComplexWhitespace(String sql) {
                assertTrue(
                        tableValidationService.isSelectStatement(sql), "应该正确处理复杂的空白字符格式: " + sql);
            }

            @ParameterizedTest
            @ValueSource(
                    strings = {
                        // 伪装成 SELECT 的非查询语句
                        "UPDATE users SET description = 'SELECT * FROM fake' WHERE id = 1",

                        // 注释中的 SELECT
                        "-- SELECT * FROM commented_table\nINSERT INTO real_table VALUES (1)",

                        // 字符串中的 SELECT
                        "INSERT INTO logs (message) VALUES ('User executed: SELECT * FROM users')",

                        // 复杂的非 SELECT 语句
                        """
                /* 这个注释包含 SELECT 关键字
                   SELECT * FROM fake_table
                */
                CREATE TABLE test_table (
                    id INT,
                    query_text VARCHAR(255) DEFAULT 'SELECT * FROM default_table'
                )
                """,

                        // EXPLAIN 等查询分析语句
                        "EXPLAIN SELECT * FROM users WHERE id = 1",
                        "DESCRIBE users",
                        "SHOW TABLES LIKE 'user_%'"
                    })
            @DisplayName("易混淆的非 SELECT 语句检测")
            void testIsSelectStatement_FalsePositives(String sql) {
                assertFalse(
                        tableValidationService.isSelectStatement(sql),
                        "应该正确识别非 SELECT 语句，避免误判: " + sql);
            }

            @Test
            @DisplayName("边界条件测试")
            void testIsSelectStatement_EdgeCases() {
                // null 和空字符串
                assertFalse(tableValidationService.isSelectStatement(null), "null 输入应该返回 false");
                assertFalse(tableValidationService.isSelectStatement(""), "空字符串应该返回 false");
                assertFalse(tableValidationService.isSelectStatement("   "), "纯空白字符应该返回 false");

                // 只有注释
                assertFalse(
                        tableValidationService.isSelectStatement("-- 只有注释"), "只有注释的输入应该返回 false");
                assertFalse(
                        tableValidationService.isSelectStatement("/* 只有块注释 */"),
                        "只有块注释的输入应该返回 false");

                // 不完整的 SELECT
                assertFalse(
                        tableValidationService.isSelectStatement("SELECT"),
                        "不完整的 SELECT 语句应该返回 false");
                assertFalse(
                        tableValidationService.isSelectStatement("SELEC * FROM table"),
                        "拼写错误的 SELECT 应该返回 false");
            }
        }
    }
}
