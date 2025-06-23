package com.hinadt.miaocha.mock.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.hinadt.miaocha.application.service.impl.TableValidationServiceImpl;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TableValidationServiceImpl 单元测试类 重点测试SQL校验功能的健壮性和边界情况
 *
 * <p>测试范围： 1. validateDorisSql方法的各种场景 2. CREATE TABLE语句校验 3. 表名一致性校验 4. 异常处理和边界情况 5. 性能测试 6.
 * 正则表达式边界测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("表验证服务测试")
public class TableValidationServiceImplTest {

    private TableValidationServiceImpl tableValidationService;

    @BeforeEach
    public void setUp() {
        tableValidationService = new TableValidationServiceImpl();
    }

    // ==================== 正常情况测试 ====================

    @Test
    @DisplayName("正常CREATE TABLE语句 - 校验成功")
    @Description("测试标准的CREATE TABLE语句")
    @Severity(SeverityLevel.CRITICAL)
    public void testValidateDorisSqlSuccess() {
        ModuleInfo moduleInfo = createModuleInfo("test_module", "user_logs");
        String sql = "CREATE TABLE user_logs (id BIGINT, message TEXT, created_at DATETIME)";

        assertDoesNotThrow(() -> tableValidationService.validateDorisSql(moduleInfo, sql));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "CREATE TABLE user_logs (id BIGINT)",
                "create table user_logs (id BIGINT)",
                "Create Table user_logs (id BIGINT)",
                "CREATE table user_logs (id BIGINT)",
                "   CREATE   TABLE   user_logs   (id BIGINT)   ",
                "\tCREATE\tTABLE\tuser_logs\t(id BIGINT)\t",
                "\nCREATE\nTABLE\nuser_logs\n(id BIGINT)\n"
            })
    @DisplayName("CREATE TABLE大小写和空白字符测试")
    @Description("测试CREATE TABLE的大小写不敏感和各种空白字符")
    @Severity(SeverityLevel.NORMAL)
    public void testCreateTableCaseInsensitiveAndWhitespace(String sql) {
        ModuleInfo moduleInfo = createModuleInfo("test_module", "user_logs");
        assertDoesNotThrow(() -> tableValidationService.validateDorisSql(moduleInfo, sql));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "CREATE TABLE IF NOT EXISTS user_logs (id BIGINT)",
                "create table if not exists user_logs (id BIGINT)",
                "CREATE TABLE IF NOT EXISTS `user_logs` (id BIGINT)",
                "   CREATE   TABLE   IF   NOT   EXISTS   user_logs   (id BIGINT)   ",
                "CREATE TABLE\nIF NOT EXISTS\nuser_logs\n(id BIGINT)"
            })
    @DisplayName("CREATE TABLE IF NOT EXISTS语句测试")
    @Description("测试CREATE TABLE IF NOT EXISTS的各种格式")
    @Severity(SeverityLevel.NORMAL)
    public void testCreateTableIfNotExists(String sql) {
        ModuleInfo moduleInfo = createModuleInfo("test_module", "user_logs");
        assertDoesNotThrow(() -> tableValidationService.validateDorisSql(moduleInfo, sql));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "CREATE TABLE `user_logs` (id BIGINT)",
                "CREATE TABLE user_logs (id BIGINT)",
                "CREATE TABLE test_db.user_logs (id BIGINT)",
                "CREATE TABLE `test_db`.`user_logs` (id BIGINT)",
                "CREATE TABLE test_db.`user_logs` (id BIGINT)",
                "CREATE TABLE `test_db`.user_logs (id BIGINT)"
            })
    @DisplayName("表名格式测试")
    @Description("测试各种表名格式（反引号、数据库名等）")
    @Severity(SeverityLevel.NORMAL)
    public void testTableNameFormats(String sql) {
        ModuleInfo moduleInfo = createModuleInfo("test_module", "user_logs");
        assertDoesNotThrow(() -> tableValidationService.validateDorisSql(moduleInfo, sql));
    }

    @Test
    @DisplayName("复杂表名测试")
    @Description("测试包含下划线、数字等复杂表名")
    @Severity(SeverityLevel.NORMAL)
    public void testComplexTableNames() {
        String[] tableNames = {
            "user_logs_2024",
            "log_table_v1",
            "system_error_log",
            "user123",
            "table_with_123_numbers",
            "a",
            "table_a_b_c_d"
        };

        for (String tableName : tableNames) {
            ModuleInfo moduleInfo = createModuleInfo("test_module", tableName);
            String sql = "CREATE TABLE " + tableName + " (id BIGINT)";

            assertDoesNotThrow(
                    () -> tableValidationService.validateDorisSql(moduleInfo, sql),
                    "表名 " + tableName + " 应该校验成功");
        }
    }

    @Test
    @DisplayName("多行复杂SQL测试")
    @Description("测试复杂的多行CREATE TABLE语句")
    @Severity(SeverityLevel.NORMAL)
    public void testComplexMultiLineSQL() {
        ModuleInfo moduleInfo = createModuleInfo("test_module", "user_logs");
        String sql =
                """
            CREATE TABLE user_logs (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                message TEXT NOT NULL,
                level VARCHAR(10) DEFAULT 'INFO',
                service_name VARCHAR(100),
                trace_id VARCHAR(64),
                span_id VARCHAR(64),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_user_id (user_id),
                INDEX idx_created_at (created_at),
                INDEX idx_level (level)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

        assertDoesNotThrow(() -> tableValidationService.validateDorisSql(moduleInfo, sql));
    }

    // ==================== 异常情况测试 ====================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", "\r\n", "  \t  \n  "})
    @DisplayName("SQL为空或空白字符测试")
    @Description("测试SQL为null、空字符串或只包含空白字符的情况")
    @Severity(SeverityLevel.CRITICAL)
    public void testEmptyOrBlankSQL(String sql) {
        ModuleInfo moduleInfo = createModuleInfo("test_module", "user_logs");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> tableValidationService.validateDorisSql(moduleInfo, sql));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("SQL语句不能为空"));
    }

    @Test
    @DisplayName("模块信息为null测试")
    @Description("测试模块信息为null的情况")
    @Severity(SeverityLevel.CRITICAL)
    public void testNullModuleInfo() {
        String sql = "CREATE TABLE user_logs (id BIGINT)";

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> tableValidationService.validateDorisSql(null, sql));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("模块信息不能为空"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("模块表名为空测试")
    @Description("测试模块配置的表名为空的情况")
    @Severity(SeverityLevel.CRITICAL)
    public void testEmptyModuleTableName(String tableName) {
        ModuleInfo moduleInfo = createModuleInfo("test_module", tableName);
        String sql = "CREATE TABLE user_logs (id BIGINT)";

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> tableValidationService.validateDorisSql(moduleInfo, sql));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("模块的表名配置不能为空"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "SELECT * FROM user_logs",
                "INSERT INTO user_logs VALUES (1, 'test')",
                "UPDATE user_logs SET message = 'new'",
                "DELETE FROM user_logs WHERE id = 1",
                "DROP TABLE user_logs",
                "ALTER TABLE user_logs ADD COLUMN new_col VARCHAR(50)",
                "TRUNCATE TABLE user_logs",
                "SHOW TABLES",
                "DESCRIBE user_logs",
                "EXPLAIN SELECT * FROM user_logs",
                "CREATE INDEX idx_user ON user_logs(user_id)",
                "CREATE VIEW user_view AS SELECT * FROM user_logs",
                "GRANT SELECT ON user_logs TO user1",
                "REVOKE SELECT ON user_logs FROM user1"
            })
    @DisplayName("非CREATE TABLE语句测试")
    @Description("测试各种非CREATE TABLE语句，应该抛出SQL_NOT_CREATE_TABLE异常")
    @Severity(SeverityLevel.CRITICAL)
    public void testNonCreateTableStatements(String sql) {
        ModuleInfo moduleInfo = createModuleInfo("test_module", "user_logs");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> tableValidationService.validateDorisSql(moduleInfo, sql));

        assertEquals(ErrorCode.SQL_NOT_CREATE_TABLE, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("只允许执行CREATE TABLE语句"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "CREATE TABLE wrong_table (id BIGINT)",
                "CREATE TABLE user_log (id BIGINT)", // 少了s
                "CREATE TABLE user_logs_backup (id BIGINT)", // 多了后缀
                "CREATE TABLE USER_LOGS (id BIGINT)", // 大写
                "CREATE TABLE User_Logs (id BIGINT)", // 混合大小写
                "CREATE TABLE `wrong_table` (id BIGINT)",
                "CREATE TABLE test_db.wrong_table (id BIGINT)"
            })
    @DisplayName("表名不匹配测试")
    @Description("测试SQL中的表名与模块配置不一致的情况")
    @Severity(SeverityLevel.CRITICAL)
    public void testTableNameMismatch(String sql) {
        ModuleInfo moduleInfo = createModuleInfo("test_module", "user_logs");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> tableValidationService.validateDorisSql(moduleInfo, sql));

        assertEquals(ErrorCode.SQL_TABLE_NAME_MISMATCH, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("不一致"));
    }

    // ==================== 边界情况和异常SQL测试 ====================

    @ParameterizedTest
    @ValueSource(
            strings = {
                "CREATE TABLE", // 不完整
                "CREATE TABLE (id BIGINT)", // 缺少表名
                "CREATE TABLE  (id BIGINT)", // 表名为空
                "CREATE TABLE   \t   (id BIGINT)", // 表名只有空白字符
                "CREATE  user_logs (id BIGINT)", // 缺少TABLE关键字
                "CREATETABLE user_logs (id BIGINT)", // CREATE和TABLE连在一起
                "CREATE  TABLE", // 只有关键字
                "CREATE TABLE IF", // IF不完整
                "CREATE TABLE IF NOT", // IF NOT不完整
                "CREATE TABLE IF NOT EXIST user_logs (id BIGINT)", // EXIST拼写错误
                "CREATE TABLE .user_logs (id BIGINT)", // 数据库名为空
                "CREATE TABLE db. (id BIGINT)", // 表名为空
            })
    @DisplayName("异常SQL格式测试")
    @Description("测试各种格式异常的SQL语句")
    @Severity(SeverityLevel.NORMAL)
    public void testMalformedSQL(String sql) {
        ModuleInfo moduleInfo = createModuleInfo("test_module", "user_logs");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> tableValidationService.validateDorisSql(moduleInfo, sql));

        // 可能是格式错误或解析失败
        assertTrue(
                exception.getErrorCode() == ErrorCode.SQL_NOT_CREATE_TABLE
                        || exception.getErrorCode() == ErrorCode.VALIDATION_ERROR
                        || exception.getErrorCode() == ErrorCode.SQL_TABLE_NAME_MISMATCH);
    }

    @Test
    @DisplayName("SQL注入攻击测试")
    @Description("测试SQL注入攻击的防护")
    @Severity(SeverityLevel.CRITICAL)
    public void testSQLInjectionPrevention() {
        ModuleInfo moduleInfo = createModuleInfo("test_module", "user_logs");

        String[] maliciousSqls = {
            "SELECT * FROM user_logs WHERE 1=1",
            "INSERT INTO user_logs SELECT * FROM passwords",
            "UPDATE user_logs SET password = 'hacked'",
            "DELETE FROM user_logs; INSERT INTO admin VALUES ('hacker', 'password')",
            "DROP TABLE user_logs; CREATE TABLE malicious (data TEXT)"
        };

        for (String sql : maliciousSqls) {
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> tableValidationService.validateDorisSql(moduleInfo, sql),
                            "恶意SQL应该被拦截: " + sql);

            // 应该被识别为非CREATE TABLE语句或解析失败
            assertTrue(
                    exception.getErrorCode() == ErrorCode.SQL_NOT_CREATE_TABLE
                            || exception.getErrorCode() == ErrorCode.VALIDATION_ERROR
                            || exception.getErrorCode() == ErrorCode.SQL_TABLE_NAME_MISMATCH);
        }
    }

    @Test
    @DisplayName("特殊字符表名测试")
    @Description("测试包含特殊字符的表名")
    @Severity(SeverityLevel.MINOR)
    public void testSpecialCharacterTableNames() {
        // 这些表名在反引号中应该是合法的
        String[] specialTableNames = {
            "user-logs", // 连字符
            "user.logs", // 点号
            "user logs", // 空格
            "user@logs", // @符号
            "user#logs", // #符号
            "user$logs", // $符号
            "中文表名" // 中文字符
        };

        for (String tableName : specialTableNames) {
            ModuleInfo moduleInfo = createModuleInfo("test_module", tableName);
            String sql = "CREATE TABLE `" + tableName + "` (id BIGINT)";

            assertDoesNotThrow(
                    () -> tableValidationService.validateDorisSql(moduleInfo, sql),
                    "特殊字符表名应该支持: " + tableName);
        }
    }

    @Test
    @DisplayName("超长SQL性能测试")
    @Description("测试处理超长SQL的性能")
    @Severity(SeverityLevel.MINOR)
    public void testLongSQLPerformance() {
        ModuleInfo moduleInfo = createModuleInfo("test_module", "large_table");

        // 生成包含大量字段的SQL
        StringBuilder sqlBuilder = new StringBuilder("CREATE TABLE large_table (");
        for (int i = 0; i < 10000; i++) {
            if (i > 0) sqlBuilder.append(", ");
            sqlBuilder.append("field_").append(i).append(" VARCHAR(255)");
        }
        sqlBuilder.append(")");
        String sql = sqlBuilder.toString();

        long startTime = System.currentTimeMillis();

        assertDoesNotThrow(() -> tableValidationService.validateDorisSql(moduleInfo, sql));

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        assertTrue(executionTime < 5000, "处理10000个字段的SQL应在5秒内完成，实际耗时: " + executionTime + "ms");
    }

    @Test
    @DisplayName("超长表名测试")
    @Description("测试超长表名的处理")
    @Severity(SeverityLevel.MINOR)
    public void testVeryLongTableName() {
        // 生成一个很长的表名
        String longTableName = "a".repeat(1000);
        ModuleInfo moduleInfo = createModuleInfo("test_module", longTableName);
        String sql = "CREATE TABLE " + longTableName + " (id BIGINT)";

        assertDoesNotThrow(() -> tableValidationService.validateDorisSql(moduleInfo, sql));
    }

    @Test
    @DisplayName("Unicode字符测试")
    @Description("测试Unicode字符的处理")
    @Severity(SeverityLevel.MINOR)
    public void testUnicodeCharacters() {
        String[] unicodeTableNames = {
            "表名", // 中文
            "テーブル", // 日文
            "таблица", // 俄文
            "πίνακας", // 希腊文
            "جدول", // 阿拉伯文
            "테이블" // 韩文
        };

        for (String tableName : unicodeTableNames) {
            ModuleInfo moduleInfo = createModuleInfo("test_module", tableName);
            String sql = "CREATE TABLE `" + tableName + "` (id BIGINT)";

            assertDoesNotThrow(
                    () -> tableValidationService.validateDorisSql(moduleInfo, sql),
                    "Unicode表名应该支持: " + tableName);
        }
    }

    @Test
    @DisplayName("极端情况组合测试")
    @Description("测试各种极端情况的组合")
    @Severity(SeverityLevel.MINOR)
    public void testExtremeCombinations() {
        // 测试各种极端情况的组合

        // 1. 空白字符 + 有效SQL
        ModuleInfo moduleInfo = createModuleInfo("test_module", "user_logs");
        String sql = "   \t\n   CREATE TABLE user_logs (id BIGINT)   \t\n   ";
        assertDoesNotThrow(() -> tableValidationService.validateDorisSql(moduleInfo, sql));

        // 2. 模块表名为单个字符
        ModuleInfo singleCharModule = createModuleInfo("test_module", "a");
        String singleCharSql = "CREATE TABLE a (id BIGINT)";
        assertDoesNotThrow(
                () -> tableValidationService.validateDorisSql(singleCharModule, singleCharSql));

        // 3. 多重嵌套反引号（虽然语法上可能不正确，但应该能解析）
        ModuleInfo backtickModule = createModuleInfo("test_module", "user_logs");
        String backtickSql = "CREATE TABLE `user_logs` (id BIGINT)";
        assertDoesNotThrow(
                () -> tableValidationService.validateDorisSql(backtickModule, backtickSql));
    }

    @Test
    @DisplayName("并发安全测试")
    @Description("测试并发调用的安全性")
    @Severity(SeverityLevel.MINOR)
    public void testConcurrentSafety() {
        ModuleInfo moduleInfo = createModuleInfo("test_module", "user_logs");
        String sql = "CREATE TABLE user_logs (id BIGINT)";

        // 模拟并发调用
        Runnable task =
                () -> {
                    for (int i = 0; i < 100; i++) {
                        assertDoesNotThrow(
                                () -> tableValidationService.validateDorisSql(moduleInfo, sql));
                    }
                };

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(task);
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            assertDoesNotThrow(() -> thread.join());
        }
    }

    // ==================== 辅助方法 ====================

    /** 创建Mock的ModuleInfo对象 */
    private ModuleInfo createModuleInfo(String name, String tableName) {
        ModuleInfo moduleInfo = new ModuleInfo();
        moduleInfo.setId(1L);
        moduleInfo.setName(name);
        moduleInfo.setTableName(tableName);
        moduleInfo.setDatasourceId(1L);
        return moduleInfo;
    }
}
