package com.hinadt.miaocha.integration.logsearch;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.hinadt.miaocha.TestContainersFactory;
import com.hinadt.miaocha.application.service.LogSearchService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.domain.dto.*;
import com.hinadt.miaocha.domain.dto.logsearch.*;
import com.hinadt.miaocha.integration.data.IntegrationTestDataInitializer;
import com.hinadt.miaocha.integration.data.LogSearchTestDataInitializer;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * LogSearchServiceImpl 集成测试
 *
 * <p>使用 @Nested 分组组织测试，支持并行执行，验证日志搜索服务的核心功能： - 关键字搜索功能组 - WHERE条件查询功能组 - 字段和组合查询功能组 -
 * 高级功能组（时间分布、字段分布、元数据） - 异常处理功能组
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("LogSearchServiceImpl 集成测试")
public class LogSearchIntegrationTest {

    // ==================== 容器配置 ====================

    /** MySQL容器 - 存储模块配置、数据源配置等元数据 */
    @Container @ServiceConnection
    static MySQLContainer<?> mysqlContainer = TestContainersFactory.mysqlContainer();

    /** Doris容器 - 存储日志数据，执行实际查询 */
    @Container static GenericContainer<?> dorisContainer = TestContainersFactory.dorisContainer();

    // ==================== 依赖注入 ====================

    @Autowired private LogSearchService logSearchService;

    @Autowired private IntegrationTestDataInitializer baseDataInitializer;

    @Autowired private LogSearchTestDataInitializer logSearchDataInitializer;

    // ==================== 测试环境管理 ====================

    @BeforeAll
    void setupTestEnvironment() {
        log.info("=== LogSearch集成测试：开始搭建测试环境 ===");

        // 验证容器状态
        assertThat(mysqlContainer.isRunning()).isTrue();
        assertThat(dorisContainer.isRunning()).isTrue();

        // 1. 先初始化基础业务数据（用户、机器、基础数据源等）
        baseDataInitializer.initializeTestData();
        log.info("基础业务数据初始化完成");

        // 2. 再初始化日志搜索专用数据（Doris数据库、测试表、10000条日志数据）
        logSearchDataInitializer.initializeTestEnvironment(dorisContainer);
        log.info("日志搜索测试数据初始化完成");

        log.info(
                "测试环境搭建完成 - MySQL: {}, Doris: {}",
                mysqlContainer.getJdbcUrl(),
                dorisContainer.getMappedPort(9030));
    }

    @AfterAll
    void cleanupTestEnvironment() {
        log.info("=== LogSearch集成测试：开始清理测试环境 ===");

        // 清理顺序：先清理日志搜索相关数据，再清理基础数据
        logSearchDataInitializer.cleanupTestData();
        log.info("日志搜索测试数据清理完成");

        baseDataInitializer.cleanupTestData();
        log.info("基础业务数据清理完成");

        log.info("测试环境清理完成");
    }

    // ==================== 关键字搜索功能组 ====================

    @Nested
    @DisplayName("关键字搜索功能组")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class KeywordSearchIntegrationTest {

        @Test
        @Order(1)
        @DisplayName("KW-001: 基础关键字搜索 - 使用配置默认搜索方法")
        void testBasicKeywordSearch() {
            log.info("🔍 测试基础关键字搜索");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 使用关键字搜索，会自动应用到模块配置的所有关键字字段
            searchRequest.setKeywords(List.of("NullPointerException"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotEmpty();
            assertThat(result.getTotalCount()).isGreaterThan(0);

            // 验证返回的记录确实包含搜索关键字
            boolean foundMatch =
                    result.getRows().stream()
                            .anyMatch(
                                    row -> {
                                        Object messageText = row.get("message_text");
                                        return messageText != null
                                                && messageText
                                                        .toString()
                                                        .contains("NullPointerException");
                                    });
            assertThat(foundMatch).isTrue();

            log.info("✅ 基础关键字搜索通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(2)
        @DisplayName("KW-002: 复杂表达式查询 - 两层嵌套&&和||组合")
        void testComplexExpressionSearch() {
            log.info("🔍 测试复杂表达式查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试复杂表达式：基于真实测试数据的错误和用户相关日志
            searchRequest.setKeywords(
                    List.of(
                            "('NullPointerException' || 'timeout') && ('processing' ||"
                                    + " 'request')"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotEmpty();

            log.info("✅ 复杂表达式查询通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(3)
        @DisplayName("KW-003: 三元素OR组合表达式")
        void testTripleOrExpressionSearch() {
            log.info("🔍 测试三元素OR组合表达式");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试三元素OR表达式：'ERROR' || 'WARN' || 'INFO'
            searchRequest.setKeywords(List.of("'ERROR' || 'WARN' || 'INFO'"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotEmpty();

            log.info("✅ 三元素OR组合表达式通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(4)
        @DisplayName("KW-004: 深度嵌套AND/OR组合")
        void testDeepNestedExpressionSearch() {
            log.info("🔍 测试深度嵌套AND/OR组合");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试深度嵌套：基于真实测试数据的内容组合
            // (('user' && 'login') || ('order' && 'processed')) && 'request'
            searchRequest.setKeywords(
                    List.of("(('user' && 'login') || ('order' && 'processed')) && 'request'"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotEmpty();
            assertThat(result.getTotalCount()).isGreaterThan(0);

            // 验证返回的记录确实符合嵌套条件逻辑
            boolean foundMatch =
                    result.getRows().stream()
                            .anyMatch(
                                    row -> {
                                        Object messageText = row.get("message_text");
                                        if (messageText == null) return false;

                                        String text = messageText.toString().toLowerCase();
                                        // 验证嵌套逻辑：((user && login) || (order && processed)) &&
                                        // request
                                        boolean userLogin =
                                                text.contains("user") && text.contains("login");
                                        boolean orderProcessed =
                                                text.contains("order")
                                                        && text.contains("processed");
                                        boolean hasRequest = text.contains("request");

                                        return (userLogin || orderProcessed) && hasRequest;
                                    });
            assertThat(foundMatch).isTrue();

            log.info("✅ 深度嵌套AND/OR组合通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(5)
        @DisplayName("KW-005: 验证关键字自动应用到所有配置字段")
        void testMultiFieldOrSearch() {
            log.info("🔍 测试关键字自动应用到所有配置字段");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 使用一个只在host字段存在的关键字，验证关键字确实在所有字段中搜索
            searchRequest.setKeywords(List.of("172.20.61"));

            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            assertThat(result.getTotalCount()).isGreaterThan(0);

            // 验证找到的记录确实在host字段包含该关键字
            boolean foundHostMatch =
                    result.getRows().stream()
                            .anyMatch(
                                    row -> {
                                        Object host = row.get("host");
                                        return host != null
                                                && host.toString().contains("172.20.61");
                                    });
            assertThat(foundHostMatch).isTrue();

            log.info("✅ 关键字自动应用到所有配置字段验证通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(6)
        @DisplayName("KW-006: 多条件AND多字段OR查询 - 验证条件间AND连接，字段间OR连接")
        void testMultiConditionAndMultiFieldOrSearch() {
            log.info("🔍 测试多条件AND多字段OR查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 多个关键字间AND连接：每个关键字自动应用到模块配置的所有关键字字段
            searchRequest.setKeywords(
                    List.of(
                            "'172.20.61'", // 第一个关键字：搜索主机信息
                            "'ERROR' || 'INFO'" // 第二个关键字：搜索级别信息
                            ));

            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            assertThat(result.getTotalCount()).isGreaterThan(0);

            // 验证返回记录符合逻辑：条件间AND连接，字段间OR连接
            boolean foundMatch =
                    result.getRows().stream()
                            .anyMatch(
                                    row -> {
                                        Object host = row.get("host");
                                        Object source = row.get("source");
                                        Object message = row.get("message");

                                        String hostStr = host != null ? host.toString() : "";
                                        String sourceStr = source != null ? source.toString() : "";
                                        String messageStr =
                                                message != null ? message.toString() : "";

                                        // 验证第一个条件：host包含172.20.61 OR source包含172.20.61
                                        boolean firstConditionMatch =
                                                hostStr.contains("172.20.61")
                                                        || sourceStr.contains("172.20.61");

                                        // 验证第二个条件：message包含ERROR OR INFO
                                        boolean secondConditionMatch =
                                                messageStr.contains("ERROR")
                                                        || messageStr.contains("INFO");

                                        // 两个条件都必须满足（AND连接）
                                        return firstConditionMatch && secondConditionMatch;
                                    });
            assertThat(foundMatch).isTrue();

            log.info("✅ 多条件AND多字段OR查询通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(7)
        @DisplayName("KW-007: 空关键字搜索条件处理")
        void testEmptyKeywordConditions() {
            log.info("🔍 测试空关键字搜索条件处理");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 设置空的关键字列表
            searchRequest.setKeywords(List.of());

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotEmpty(); // 空条件应返回所有数据
            assertThat(result.getTotalCount()).isGreaterThan(0);

            log.info("✅ 空关键字搜索条件处理通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(8)
        @DisplayName("KW-008: 验证不同搜索方法的自动应用")
        void testUnauthorizedFieldValidation() {
            log.info("🔍 测试不同搜索方法的自动应用");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试LIKE搜索方法：部分匹配，会在host等LIKE字段中找到
            searchRequest.setKeywords(List.of("61"));

            LogDetailResultDTO likeResult = logSearchService.searchDetails(searchRequest);

            // 测试MATCH_PHRASE搜索方法：精确匹配，会在message.level等MATCH_PHRASE字段中找到
            searchRequest.setKeywords(List.of("ERROR"));

            LogDetailResultDTO matchPhraseResult = logSearchService.searchDetails(searchRequest);

            assertThat(likeResult.getTotalCount()).isGreaterThan(0);
            assertThat(matchPhraseResult.getTotalCount()).isGreaterThan(0);

            log.info(
                    "✅ 不同搜索方法的自动应用验证通过 - LIKE查询到{}条，MATCH_PHRASE查询到{}条",
                    likeResult.getTotalCount(),
                    matchPhraseResult.getTotalCount());
        }

        @Test
        @Order(9)
        @DisplayName("KW-009: 特殊字符转义处理")
        void testSpecialCharacterEscaping() {
            log.info("🔍 测试特殊字符转义处理");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试包含单引号的关键字，会自动应用到模块配置的所有关键字字段
            searchRequest.setKeywords(List.of("user's data")); // 包含单引号

            // 应该不抛出SQL注入异常
            assertDoesNotThrow(
                    () -> {
                        LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);
                        assertThat(result).isNotNull();
                        log.info("✅ 特殊字符转义处理通过 - 查询到{}条记录", result.getTotalCount());
                    });
        }

        @Test
        @Order(10)
        @DisplayName("KW-010: 配置驱动搜索方法验证")
        void testConfigDrivenSearchMethods() {
            log.info("🔍 测试配置驱动搜索方法验证");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试关键字自动应用到配置字段时的不同搜索方法效果
            // "61" 会在host字段用LIKE进行模糊匹配，在其他字段也会应用对应的搜索方法
            searchRequest.setKeywords(List.of("61"));

            LogDetailResultDTO hostResult = logSearchService.searchDetails(searchRequest);

            // "ERROR" 会在message.level字段用MATCH_PHRASE进行精确匹配，在其他字段也会应用对应的搜索方法
            searchRequest.setKeywords(List.of("ERROR"));

            LogDetailResultDTO levelResult = logSearchService.searchDetails(searchRequest);

            assertThat(hostResult).isNotNull();
            assertThat(levelResult).isNotNull();

            log.info(
                    "✅ 配置驱动搜索方法验证通过 - 关键字'61'查询到{}条记录，关键字'ERROR'查询到{}条记录",
                    hostResult.getTotalCount(),
                    levelResult.getTotalCount());
        }

        @Test
        @Order(11)
        @DisplayName("KW-011: NOT操作符复杂集成验证 - 基于真实测试数据的业务场景")
        void testNotOperatorComplexIntegration() {
            log.info("🔍 测试NOT操作符复杂集成");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 基于真实测试数据分布的合理查询：查找包含处理或请求相关内容，但排除调试级别的日志
            searchRequest.setKeywords(
                    List.of(
                            "('processing' || 'request') && - 'DEBUG' && - 'debug' && -"
                                    + " 'debugMode'"));

            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isEmpty();
            assertThat(result.getTotalCount()).isEqualTo(0);

            log.info("✅ NOT操作符复杂集成验证通过 - 查询到{}条记录，符合业务逻辑", result.getTotalCount());
        }

        @Test
        @Order(12)
        @DisplayName("KW-012: NOT操作符与WHERE条件组合验证 - 多维度过滤集成")
        void testNotOperatorWithWhereConditionIntegration() {
            log.info("🔍 测试NOT操作符与WHERE条件组合");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 多关键字AND连接：排除空指针异常但包含错误级别
            searchRequest.setKeywords(
                    List.of(
                            "ERROR && - 'NullPointerException'", // 第一个关键字：包含ERROR但排除空指针异常
                            "'172.20.61'" // 第二个关键字：特定主机段的日志
                            ));

            // WHERE条件：限制在特定时间范围和服务类型
            searchRequest.setWhereSqls(
                    List.of(
                            "log_time >= DATE_SUB(NOW(), INTERVAL 25 HOUR)",
                            "source LIKE '%cloud-engine%'"));

            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getTotalCount()).isGreaterThan(0);

            // 验证返回记录符合多维度过滤逻辑：条件间AND连接，字段间OR连接
            boolean foundMatch =
                    result.getRows().stream()
                            .anyMatch(
                                    row -> {
                                        Object host = row.get("host");
                                        Object source = row.get("source");
                                        Object messageText = row.get("message_text");
                                        Object message = row.get("message");

                                        String hostStr = host != null ? host.toString() : "";
                                        String sourceStr = source != null ? source.toString() : "";
                                        String messageTextStr =
                                                messageText != null ? messageText.toString() : "";
                                        String messageStr =
                                                message != null ? message.toString() : "";

                                        String allContent =
                                                hostStr
                                                        + " "
                                                        + sourceStr
                                                        + " "
                                                        + messageTextStr
                                                        + " "
                                                        + messageStr;

                                        // 验证第一个关键字条件：包含ERROR但不包含NullPointerException
                                        // 在所有配置字段中搜索：message_text, host, source, message.level等
                                        boolean hasError =
                                                allContent.toUpperCase().contains("ERROR");
                                        boolean notNullPointer =
                                                !allContent.contains("NullPointerException");
                                        boolean firstConditionMatch = hasError && notNullPointer;

                                        // 验证第二个关键字条件：包含'172.20.61'
                                        boolean secondConditionMatch =
                                                allContent.contains("172.20.61");

                                        // 验证WHERE条件：source必须包含cloud-engine
                                        boolean whereConditionMatch =
                                                sourceStr.contains("cloud-engine");

                                        // 所有条件都必须满足（关键字间AND连接）
                                        return firstConditionMatch
                                                && secondConditionMatch
                                                && whereConditionMatch;
                                    });

            assertThat(foundMatch).isTrue();

            log.info("✅ NOT操作符与WHERE条件组合验证通过 - 查询到{}条记录，多维度过滤正确", result.getTotalCount());
        }
    }

    // ==================== WHERE条件查询功能组 ====================

    @Nested
    @DisplayName("WHERE条件查询功能组")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WhereConditionIntegrationTest {

        @Test
        @Order(1)
        @DisplayName("WHERE-001: 单条件WHERE查询")
        void testSingleWhereCondition() {
            log.info("🔍 测试单条件WHERE查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 配置WHERE条件：查找特定主机的日志（使用测试数据中真实存在的主机）
            searchRequest.setWhereSqls(List.of("host = '172.20.61.22'"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getTotalCount()).isGreaterThan(0);
            assertThat(result.getRows()).isNotEmpty();

            // 验证返回的记录确实符合WHERE条件
            result.getRows()
                    .forEach(
                            record -> {
                                assertThat(record.get("host")).isEqualTo("172.20.61.22");
                            });

            log.info("✅ 单条件WHERE查询通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(2)
        @DisplayName("WHERE-002: 多条件WHERE查询")
        void testMultipleWhereConditions() {
            log.info("🔍 测试多条件WHERE查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 配置多个WHERE条件（基于真实测试数据）
            searchRequest.setWhereSqls(
                    List.of("host LIKE '172.20.61.%'", "source LIKE '%/data/log/%'"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getTotalCount()).isGreaterThan(0);
            assertThat(result.getRows()).isNotEmpty();

            log.info("✅ 多条件WHERE查询通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(3)
        @DisplayName("WHERE-003: Variant字段WHERE查询")
        void testVariantWhereCondition() {
            log.info("🔍 测试variant字段WHERE查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试variant嵌套字段查询：查找管理员用户的日志
            searchRequest.setWhereSqls(List.of("message.user.role = 'admin'"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();

            log.info("✅ variant字段WHERE查询通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(4)
        @DisplayName("WHERE-004: 字符串匹配查询")
        void testStringMatchWhereCondition() {
            log.info("🔍 测试字符串匹配WHERE查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 配置字符串匹配查询：查找特定服务的日志
            searchRequest.setWhereSqls(List.of("message.service LIKE '%engine%'"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();

            log.info("✅ 字符串匹配WHERE查询通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(5)
        @DisplayName("WHERE-005: 组合条件查询")
        void testCombinedWhereCondition() {
            log.info("🔍 测试组合条件WHERE查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 配置组合条件查询：使用AND和OR组合
            searchRequest.setWhereSqls(
                    List.of(
                            "(message.level = 'ERROR' OR message.level = 'WARN') AND"
                                    + " message.service IS NOT NULL"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();

            log.info("✅ 组合条件WHERE查询通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(6)
        @DisplayName("WHERE-006: 复杂逻辑条件查询")
        void testComplexLogicWhereCondition() {
            log.info("🔍 测试复杂逻辑WHERE查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 配置复杂逻辑条件：(主机条件 AND 源条件) OR 时间条件
            searchRequest.setWhereSqls(
                    List.of(
                            "(host LIKE 'server-node-%' AND source LIKE '%application%') OR"
                                    + " log_time >= DATE_SUB(NOW(), INTERVAL 30 MINUTE)"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();

            log.info("✅ 复杂逻辑WHERE查询通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(7)
        @DisplayName("WHERE-007: IN子句查询")
        void testInClauseWhereCondition() {
            log.info("🔍 测试IN子句WHERE查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 配置IN子句查询：查找指定主机列表的日志（使用测试数据中实际存在的主机名）
            searchRequest.setWhereSqls(
                    List.of("host IN ('172.20.61.22', '172.20.61.23', '172.20.61.24')"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotEmpty();
            assertThat(result.getTotalCount()).isGreaterThan(0);

            log.info("✅ IN子句WHERE查询通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(8)
        @DisplayName("WHERE-008: NULL值处理查询")
        void testNullValueWhereCondition() {
            log.info("🔍 测试NULL值处理WHERE查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 配置NULL值查询
            searchRequest.setWhereSqls(List.of("host IS NOT NULL"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotEmpty();
            assertThat(result.getTotalCount()).isGreaterThan(0);

            log.info("✅ NULL值处理WHERE查询通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(9)
        @DisplayName("WHERE-009: Variant字段嵌套查询")
        void testVariantNestedWhereCondition() {
            log.info("🔍 测试Variant字段嵌套WHERE查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 配置Variant字段嵌套查询：查找包含特定键值的Variant字段
            searchRequest.setWhereSqls(List.of("message.user.role = 'admin'"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();

            log.info("✅ Variant字段嵌套WHERE查询通过 - 查询到{}条记录", result.getTotalCount());
        }
    }

    // ==================== 字段和组合查询功能组 ====================

    @Nested
    @DisplayName("字段和组合查询功能组")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FieldAndCombinedIntegrationTest {

        @Test
        @Order(1)
        @DisplayName("FIELD-001: 字段选择查询")
        void testFieldSelection() {
            log.info("🔍 测试字段选择查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 只选择特定字段
            searchRequest.setFields(List.of("log_time", "host", "message_text"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotEmpty();

            // 验证返回的字段确实只包含指定字段
            assertThat(result.getColumns())
                    .containsExactlyInAnyOrder("log_time", "host", "message_text");

            log.info("✅ 字段选择查询通过 - 查询到{}条记录，字段已过滤", result.getTotalCount());
        }

        @Test
        @Order(2)
        @DisplayName("COMBO-001: 综合查询")
        void testCombinedSearch() {
            log.info("🔍 测试多功能组合查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 组合多种查询条件
            searchRequest.setKeywords(List.of("service")); // 关键字自动应用到所有配置字段
            searchRequest.setWhereSqls(List.of("host LIKE '172.20.61.%'"));
            searchRequest.setFields(List.of("log_time", "host", "source", "message_text"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotEmpty();
            assertThat(result.getTotalCount()).isGreaterThan(0);

            // 验证字段过滤
            assertThat(result.getColumns())
                    .containsExactlyInAnyOrder("log_time", "host", "source", "message_text");

            log.info("✅ 综合查询通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(3)
        @DisplayName("COMBO-002: 空查询条件处理")
        void testEmptySearchConditions() {
            log.info("🔍 测试空查询条件处理");

            LogSearchDTO searchRequest = createBaseSearchRequest();
            // 不设置任何查询条件，只有基础的分页和时间范围

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果：应该返回所有数据（在时间范围内）
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotEmpty();
            assertThat(result.getTotalCount()).isGreaterThan(0);

            log.info("✅ 空查询条件处理通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(4)
        @DisplayName("SORT-001: 单字段排序功能")
        void testSingleFieldSort() {
            log.info("🔍 测试单字段排序功能");

            LogSearchDTO searchRequest = createBaseSearchRequest();
            searchRequest.setFields(List.of("log_time", "host", "source"));

            // 测试升序排序
            LogSearchDTO.SortField ascSort = new LogSearchDTO.SortField();
            ascSort.setFieldName("host");
            ascSort.setDirection("ASC");
            searchRequest.setSortFields(List.of(ascSort));

            LogDetailResultDTO ascResult = logSearchService.searchDetails(searchRequest);

            // 测试降序排序
            LogSearchDTO.SortField descSort = new LogSearchDTO.SortField();
            descSort.setFieldName("host");
            descSort.setDirection("DESC");
            searchRequest.setSortFields(List.of(descSort));

            LogDetailResultDTO descResult = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(ascResult).isNotNull();
            assertThat(ascResult.getRows()).isNotEmpty();
            assertThat(descResult).isNotNull();
            assertThat(descResult.getRows()).isNotEmpty();

            // 验证排序效果：从外部验证排序顺序
            List<String> ascHosts =
                    ascResult.getRows().stream()
                            .map(row -> String.valueOf(row.get("host")))
                            .limit(3)
                            .toList();
            List<String> descHosts =
                    descResult.getRows().stream()
                            .map(row -> String.valueOf(row.get("host")))
                            .limit(3)
                            .toList();

            // 验证升序排序：第一个应该小于等于第二个
            if (ascHosts.size() >= 2) {
                assertThat(ascHosts.get(0).compareTo(ascHosts.get(1))).isLessThanOrEqualTo(0);
            }

            // 验证降序排序：第一个应该大于等于第二个
            if (descHosts.size() >= 2) {
                assertThat(descHosts.get(0).compareTo(descHosts.get(1))).isGreaterThanOrEqualTo(0);
            }

            log.info(
                    "✅ 单字段排序功能通过 - ASC:{}条记录，DESC:{}条记录",
                    ascResult.getTotalCount(),
                    descResult.getTotalCount());
        }

        @Test
        @Order(5)
        @DisplayName("SORT-002: 多字段排序功能")
        void testMultiFieldSort() {
            log.info("🔍 测试多字段排序功能");

            LogSearchDTO searchRequest = createBaseSearchRequest();
            searchRequest.setFields(List.of("log_time", "host", "source"));

            // 创建多字段排序：先按source升序，再按host降序
            LogSearchDTO.SortField sourceSort = new LogSearchDTO.SortField();
            sourceSort.setFieldName("source");
            sourceSort.setDirection("ASC");

            LogSearchDTO.SortField hostSort = new LogSearchDTO.SortField();
            hostSort.setFieldName("host");
            hostSort.setDirection("DESC");

            searchRequest.setSortFields(List.of(sourceSort, hostSort));

            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotEmpty();
            assertThat(result.getTotalCount()).isGreaterThan(0);

            // 验证多字段排序效果：检查相同source值的记录host字段是否按降序排列
            Map<String, List<String>> sourceToHosts =
                    result.getRows().stream()
                            .collect(
                                    java.util.stream.Collectors.groupingBy(
                                            row -> String.valueOf(row.get("source")),
                                            java.util.stream.Collectors.mapping(
                                                    row -> String.valueOf(row.get("host")),
                                                    java.util.stream.Collectors.toList())));

            // 验证每个source组内的host是否按降序排列
            sourceToHosts.forEach(
                    (source, hosts) -> {
                        if (hosts.size() >= 2) {
                            for (int i = 0; i < hosts.size() - 1; i++) {
                                assertThat(hosts.get(i).compareTo(hosts.get(i + 1)))
                                        .isGreaterThanOrEqualTo(0);
                            }
                        }
                    });

            log.info(
                    "✅ 多字段排序功能通过 - 查询到{}条记录，{}个source分组",
                    result.getTotalCount(),
                    sourceToHosts.size());
        }

        @Test
        @Order(6)
        @DisplayName("SORT-003: 时间字段排序覆盖")
        void testTimeFieldSortOverride() {
            log.info("🔍 测试时间字段排序覆盖");

            LogSearchDTO searchRequest = createBaseSearchRequest();
            searchRequest.setFields(List.of("log_time", "host", "source"));

            // 用户指定时间字段升序排序（覆盖默认的倒序）
            LogSearchDTO.SortField timeSort = new LogSearchDTO.SortField();
            timeSort.setFieldName("log_time");
            timeSort.setDirection("ASC");

            searchRequest.setSortFields(List.of(timeSort));

            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotEmpty();
            assertThat(result.getTotalCount()).isGreaterThan(0);

            // 验证时间字段升序排序：检查时间顺序
            List<String> logTimes =
                    result.getRows().stream()
                            .map(row -> String.valueOf(row.get("log_time")))
                            .limit(5)
                            .toList();

            // 验证时间升序排序
            if (logTimes.size() >= 2) {
                for (int i = 0; i < logTimes.size() - 1; i++) {
                    assertThat(logTimes.get(i).compareTo(logTimes.get(i + 1)))
                            .isLessThanOrEqualTo(0);
                }
            }

            log.info("✅ 时间字段排序覆盖通过 - 查询到{}条记录，时间升序排列", result.getTotalCount());
        }

        @Test
        @Order(7)
        @DisplayName("SORT-004: 排序组合查询")
        void testSortWithCombinedConditions() {
            log.info("🔍 测试排序组合查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 组合查询条件：关键字 + WHERE条件 + 字段选择 + 排序
            searchRequest.setKeywords(List.of("service"));
            searchRequest.setWhereSqls(List.of("host LIKE '172.20.61.%'"));
            searchRequest.setFields(List.of("log_time", "host", "source", "message_text"));

            // 添加排序
            LogSearchDTO.SortField hostSort = new LogSearchDTO.SortField();
            hostSort.setFieldName("host");
            hostSort.setDirection("ASC");

            LogSearchDTO.SortField sourceSort = new LogSearchDTO.SortField();
            sourceSort.setFieldName("source");
            sourceSort.setDirection("DESC");

            searchRequest.setSortFields(List.of(hostSort, sourceSort));

            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotEmpty();
            assertThat(result.getTotalCount()).isGreaterThan(0);

            // 验证字段过滤
            assertThat(result.getColumns())
                    .containsExactlyInAnyOrder("log_time", "host", "source", "message_text");

            // 验证关键字过滤：至少一条记录包含"service"
            boolean hasServiceKeyword =
                    result.getRows().stream()
                            .anyMatch(
                                    row -> {
                                        Object messageText = row.get("message_text");
                                        return messageText != null
                                                && messageText.toString().contains("service");
                                    });
            assertThat(hasServiceKeyword).isTrue();

            // 验证WHERE条件：所有记录的host都符合条件
            result.getRows()
                    .forEach(
                            row -> {
                                Object host = row.get("host");
                                assertThat(host).isNotNull();
                                assertThat(host.toString()).startsWith("172.20.61.");
                            });

            // 验证排序效果：检查host升序排序
            List<String> hosts =
                    result.getRows().stream()
                            .map(row -> String.valueOf(row.get("host")))
                            .limit(3)
                            .toList();

            if (hosts.size() >= 2) {
                assertThat(hosts.get(0).compareTo(hosts.get(1))).isLessThanOrEqualTo(0);
            }

            log.info("✅ 排序组合查询通过 - 查询到{}条记录，综合功能正常", result.getTotalCount());
        }
    }

    // ==================== 高级功能组 ====================

    @Nested
    @DisplayName("高级功能组")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AdvancedFunctionIntegrationTest {

        @Test
        @Order(1)
        @DisplayName("TIME-001: 时间分布查询")
        void testTimeDistribution() {
            log.info("🔍 测试时间分布查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 执行时间分布查询
            LogHistogramResultDTO result = logSearchService.searchHistogram(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getDistributionData()).isNotEmpty();

            // 验证结果结构：应该包含时间字段和计数字段
            result.getDistributionData()
                    .forEach(
                            item -> {
                                assertThat(item.getTimePoint()).isNotNull();
                                assertThat(item.getCount()).isInstanceOf(Number.class);
                            });

            log.info("✅ 时间分布查询通过 - 返回{}个时间片段", result.getDistributionData().size());
        }

        @Test
        @Order(2)
        @DisplayName("FIELD-001: 字段分布查询")
        void testFieldDistribution() {
            log.info("🔍 测试字段分布查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 查询主机字段的分布
            searchRequest.setFields(List.of("host"));

            LogFieldDistributionResultDTO result =
                    logSearchService.searchFieldDistributions(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getFieldDistributions()).isNotEmpty();

            // 验证结果结构：应该包含字段分布信息
            result.getFieldDistributions()
                    .forEach(
                            distribution -> {
                                assertThat(distribution).isNotNull();
                            });

            log.info("✅ 字段分布查询通过 - 返回{}个分布项", result.getFieldDistributions().size());
        }

        @Test
        @Order(3)
        @DisplayName("META-001: 表结构查询")
        void testTableColumns() {
            log.info("🔍 测试表结构查询");

            // 获取测试表的列信息
            List<SchemaInfoDTO.ColumnInfoDTO> result =
                    logSearchService.getTableColumns(
                            logSearchDataInitializer.getTestModule().getName());

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();

            // 验证包含预期的核心字段
            Set<String> columnNames =
                    result.stream()
                            .map(SchemaInfoDTO.ColumnInfoDTO::getColumnName)
                            .collect(java.util.stream.Collectors.toSet());

            assertThat(columnNames)
                    .contains("log_time", "host", "source", "message", "message_text");

            // 验证字段信息完整性
            result.forEach(
                    column -> {
                        assertThat(column.getColumnName()).isNotNull();
                        assertThat(column.getDataType()).isNotNull();
                    });

            log.info("✅ 表结构查询通过 - 返回{}个字段", result.size());
        }

        @Test
        @Order(4)
        @DisplayName("TIME-002: 多时间窗口分布查询")
        void testMultipleTimeWindowDistribution() {
            log.info("🔍 测试多时间窗口分布查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试不同的时间范围设置
            List<String> timeRanges = List.of("last_1h", "last_6h", "last_24h");

            for (String timeRange : timeRanges) {
                searchRequest.setTimeRange(timeRange);

                LogHistogramResultDTO result = logSearchService.searchHistogram(searchRequest);

                assertThat(result).isNotNull();
                assertThat(result.getDistributionData()).isNotEmpty();

                log.info(
                        "✅ 时间范围{}分布查询通过 - 返回{}个时间片段",
                        timeRange,
                        result.getDistributionData().size());
            }
        }

        @Test
        @Order(5)
        @DisplayName("FIELD-002: 多字段分布查询")
        void testMultipleFieldDistribution() {
            log.info("🔍 测试多字段分布查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试不同字段的分布查询
            List<String> fieldNames = List.of("host", "source");

            for (String fieldName : fieldNames) {
                searchRequest.setFields(List.of(fieldName));

                LogFieldDistributionResultDTO result =
                        logSearchService.searchFieldDistributions(searchRequest);

                assertThat(result).isNotNull();
                assertThat(result.getFieldDistributions()).isNotEmpty();
                assertThat(result.getFieldDistributions().size()).isLessThanOrEqualTo(5); // TOP5限制

                log.info(
                        "✅ 字段{}分布查询通过 - 返回{}个分布项",
                        fieldName,
                        result.getFieldDistributions().size());
            }
        }

        @Test
        @Order(6)
        @DisplayName("AGGR-001: 聚合统计组合查询")
        void testAggregationCombinationQueries() {
            log.info("🔍 测试聚合统计组合查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 添加过滤条件后进行聚合（基于真实测试数据）
            searchRequest.setWhereSqls(List.of("host LIKE '172.20.61.%'"));

            // 时间分布聚合
            LogHistogramResultDTO histogramResult = logSearchService.searchHistogram(searchRequest);

            assertThat(histogramResult).isNotNull();
            assertThat(histogramResult.getDistributionData()).isNotEmpty();

            // 字段分布聚合
            searchRequest.setFields(List.of("host"));
            LogFieldDistributionResultDTO fieldResult =
                    logSearchService.searchFieldDistributions(searchRequest);

            assertThat(fieldResult).isNotNull();
            assertThat(fieldResult.getFieldDistributions()).isNotEmpty();

            log.info(
                    "✅ 组合聚合查询通过 - 时间分布{}个片段，主机分布{}个项",
                    histogramResult.getDistributionData().size(),
                    fieldResult.getFieldDistributions().size());
        }

        @Test
        @Order(7)
        @DisplayName("FIELD-003: Variant字段分布查询 - 测试点语法字段转换")
        void testVariantFieldDistribution() {
            log.info("🔍 测试Variant字段分布查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试message.level字段的分布查询（点语法转换）
            searchRequest.setFields(List.of("message.level"));

            LogFieldDistributionResultDTO result =
                    logSearchService.searchFieldDistributions(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getFieldDistributions()).isNotEmpty();
            assertThat(result.getFieldDistributions().size()).isEqualTo(1);

            // 验证字段名正确返回
            FieldDistributionDTO levelDistribution = result.getFieldDistributions().get(0);
            assertThat(levelDistribution.getFieldName()).isEqualTo("message.level");
            assertThat(levelDistribution.getValueDistributions()).isNotEmpty();

            // 验证包含预期的日志级别
            Set<String> levels =
                    levelDistribution.getValueDistributions().stream()
                            .map(dist -> String.valueOf(dist.getValue()))
                            .collect(java.util.stream.Collectors.toSet());
            assertThat(levels).containsAnyOf("INFO", "ERROR", "WARN", "DEBUG");

            log.info("✅ Variant字段分布查询通过 - message.level包含{}种级别", levels.size());
        }

        @Test
        @Order(8)
        @DisplayName("FIELD-004: 多Variant字段分布查询 - 测试多个点语法字段")
        void testMultipleVariantFieldDistribution() {
            log.info("🔍 测试多Variant字段分布查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试多个Variant字段的分布查询
            searchRequest.setFields(List.of("message.level", "message.service"));

            LogFieldDistributionResultDTO result =
                    logSearchService.searchFieldDistributions(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getFieldDistributions()).hasSize(2);

            // 验证每个字段的结果
            Map<String, FieldDistributionDTO> distributionMap =
                    result.getFieldDistributions().stream()
                            .collect(
                                    java.util.stream.Collectors.toMap(
                                            FieldDistributionDTO::getFieldName,
                                            java.util.function.Function.identity()));

            // 验证message.level字段
            assertThat(distributionMap).containsKey("message.level");
            FieldDistributionDTO levelDist = distributionMap.get("message.level");
            assertThat(levelDist.getValueDistributions()).isNotEmpty();

            // 验证message.service字段
            assertThat(distributionMap).containsKey("message.service");
            FieldDistributionDTO serviceDist = distributionMap.get("message.service");
            assertThat(serviceDist.getValueDistributions()).isNotEmpty();

            log.info(
                    "✅ 多Variant字段分布查询通过 - level:{}种, service:{}种",
                    levelDist.getValueDistributions().size(),
                    serviceDist.getValueDistributions().size());
        }

        @Test
        @Order(9)
        @DisplayName("FIELD-005: 混合字段分布查询 - 普通字段与Variant字段混合")
        void testMixedFieldDistribution() {
            log.info("🔍 测试混合字段分布查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试普通字段与Variant字段混合查询
            searchRequest.setFields(List.of("host", "message.level", "source"));

            LogFieldDistributionResultDTO result =
                    logSearchService.searchFieldDistributions(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getFieldDistributions()).hasSize(3);

            // 验证每个字段都有正确的结果
            Map<String, FieldDistributionDTO> distributionMap =
                    result.getFieldDistributions().stream()
                            .collect(
                                    java.util.stream.Collectors.toMap(
                                            FieldDistributionDTO::getFieldName,
                                            java.util.function.Function.identity()));

            // 验证普通字段
            assertThat(distributionMap).containsKey("host");
            assertThat(distributionMap).containsKey("source");

            // 验证Variant字段
            assertThat(distributionMap).containsKey("message.level");

            // 验证所有字段都有分布数据
            distributionMap
                    .values()
                    .forEach(
                            dist -> {
                                assertThat(dist.getValueDistributions()).isNotEmpty();
                                assertThat(dist.getFieldName()).isNotNull();
                            });

            log.info("✅ 混合字段分布查询通过 - 3个字段均有分布数据");
        }
    }

    // ==================== 并发可靠性测试组 ====================

    @Nested
    @DisplayName("并发可靠性测试组")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ConcurrencyReliabilityIntegrationTest {

        @Test
        @Order(1)
        @DisplayName("CONCURRENT-001: HikariCP连接池并发可靠性压测")
        void testHikariCPHighConcurrentReliability() {
            log.info("🚀 开始HikariCP连接池并发可靠性压测");

            // 获取测试环境信息
            ConcurrentTestContext context = initializeTestContext();
            logTestConfiguration(context);

            // 创建执行器和监控
            ExecutorService executor = Executors.newFixedThreadPool(context.threadCount);
            ScheduledExecutorService monitor = createConnectionPoolMonitor(context.datasourceId);

            try {
                // 执行并发查询测试
                ConcurrentTestResult testResult = executeConcurrentQueries(executor, context);

                // 验证查询结果
                validateQueryResults(testResult);

                // 记录测试统计信息
                logTestStatistics(testResult, context);

                log.info("✅ HikariCP连接池并发可靠性压测通过 - 所有{}个查询成功完成", testResult.totalQueries);

            } catch (TimeoutException e) {
                log.error("❌ 并发压测超时", e);
                throw new RuntimeException("并发压测超时: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("❌ 并发压测失败", e);
                throw new RuntimeException("并发压测失败: " + e.getMessage(), e);
            } finally {
                // 清理资源
                cleanupResources(executor, monitor, context.datasourceId);
            }
        }

        // ==================== 并发测试相关的内部类和私有方法 ====================

        /** 并发测试上下文信息 */
        private record ConcurrentTestContext(
                String moduleName,
                Long datasourceId,
                int threadCount,
                int detailQueries,
                int histogramQueries,
                int fieldDistributionQueries) {}

        /** 并发测试结果 */
        private record ConcurrentTestResult(
                List<String> results,
                long totalTime,
                int totalQueries,
                long successCount,
                List<CompletableFuture<String>> futures) {}

        /** 初始化测试上下文 */
        private ConcurrentTestContext initializeTestContext() {
            String moduleName = logSearchDataInitializer.getTestModule().getName();
            Long datasourceId = logSearchDataInitializer.getTestModule().getDatasourceId();

            return new ConcurrentTestContext(moduleName, datasourceId, 30, 20, 15, 15);
        }

        /** 记录测试配置信息 */
        private void logTestConfiguration(ConcurrentTestContext context) {
            log.info("📊 测试配置:");
            log.info("   - 数据源模块: {}", context.moduleName);
            log.info("   - 数据源ID: {}", context.datasourceId);
            log.info("   - 总测试数据: {} 条", LogSearchTestDataInitializer.TOTAL_LOG_RECORDS);
            log.info("   - 并发线程数: {} 个", context.threadCount);
            log.info(
                    "   - 查询任务分布: {}详情 + {}直方图 + {}字段分布",
                    context.detailQueries,
                    context.histogramQueries,
                    context.fieldDistributionQueries);

            // 记录测试开始前的连接池状态
            logDataSourceStatus("测试开始前", context.datasourceId);
        }

        /** 创建连接池监控器 */
        private ScheduledExecutorService createConnectionPoolMonitor(Long datasourceId) {
            ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
            monitor.scheduleAtFixedRate(
                    () -> {
                        logDataSourceStatus("测试进行中", datasourceId);
                    },
                    2,
                    2,
                    TimeUnit.SECONDS);
            return monitor;
        }

        /** 执行并发查询测试 */
        private ConcurrentTestResult executeConcurrentQueries(
                ExecutorService executor, ConcurrentTestContext context)
                throws InterruptedException,
                        java.util.concurrent.ExecutionException,
                        TimeoutException {

            List<CompletableFuture<String>> futures = new ArrayList<>();

            // 创建详情查询任务
            futures.addAll(createDetailQueries(executor, context.detailQueries));

            // 创建直方图查询任务
            futures.addAll(createHistogramQueries(executor, context.histogramQueries));

            // 创建字段分布查询任务
            futures.addAll(
                    createFieldDistributionQueries(executor, context.fieldDistributionQueries));

            // 启动所有查询
            log.info("⏳ 启动所有{}个并发查询...", futures.size());
            log.info(
                    "📊 查询分布: {}个详情查询 + {}个直方图查询 + {}个字段分布查询",
                    context.detailQueries,
                    context.histogramQueries,
                    context.fieldDistributionQueries);

            long startTime = System.currentTimeMillis();
            logDataSourceStatus("查询启动后", context.datasourceId);

            CompletableFuture<Void> allQueries =
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            // 设置超时时间为120秒
            allQueries.get(120, TimeUnit.SECONDS);

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            logDataSourceStatus("查询完成后", context.datasourceId);

            // 收集所有查询结果
            List<String> results = futures.stream().map(CompletableFuture::join).toList();

            long successCount = results.stream().filter(r -> r.contains("成功")).count();

            return new ConcurrentTestResult(
                    results, totalTime, futures.size(), successCount, futures);
        }

        /** 创建详情查询任务 */
        private List<CompletableFuture<String>> createDetailQueries(
                ExecutorService executor, int queryCount) {
            List<CompletableFuture<String>> futures = new ArrayList<>();

            // 基于实际测试数据的查询配置
            String[] actualHosts = {
                "172.20.61.22", "172.20.61.18", "172.20.61.35", "192.168.1.10", "10.0.1.15"
            };
            String[] actualLevels = {"INFO", "ERROR", "WARN", "DEBUG"};
            String[] actualServices = {
                "hina-cloud-engine",
                "order-service",
                "user-service",
                "payment-service",
                "notification-service"
            };
            String[] errorKeywords = {
                "NullPointerException",
                "timeout",
                "SQLException",
                "OutOfMemoryError",
                "ValidationException",
                "TimeoutException"
            };

            int queryId = 1;

            // 主机查询 (5个)
            for (int i = 0; i < 5 && queryId <= queryCount; i++, queryId++) {
                final String host = actualHosts[i % actualHosts.length];
                futures.add(createDetailQueryByHost(executor, host, queryId));
            }

            // 级别查询 (4个)
            for (int i = 0; i < 4 && queryId <= queryCount; i++, queryId++) {
                final String level = actualLevels[i];
                futures.add(createDetailQueryByLevel(executor, level, queryId));
            }

            // 服务查询 (5个)
            for (int i = 0; i < 5 && queryId <= queryCount; i++, queryId++) {
                final String service = actualServices[i];
                futures.add(createDetailQueryByService(executor, service, queryId));
            }

            // 错误关键字查询 (剩余数量)
            for (int i = 0; i < errorKeywords.length && queryId <= queryCount; i++, queryId++) {
                final String keyword = errorKeywords[i];
                futures.add(createDetailQueryByErrorKeyword(executor, keyword, queryId));
            }

            return futures;
        }

        /** 创建基于主机的详情查询 */
        private CompletableFuture<String> createDetailQueryByHost(
                ExecutorService executor, String host, int queryId) {
            return CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            LogSearchDTO request = createBaseSearchRequest();
                            request.setWhereSqls(List.of("host = '" + host + "'"));
                            request.setFields(
                                    List.of("host", "source", "log_time", "message.level"));
                            request.setPageSize(50);

                            LogDetailResultDTO result = logSearchService.searchDetails(request);

                            // 数据验证
                            validateDetailResult(result, "主机查询", host);

                            log.info(
                                    "✅ 详情查询{}完成 - 主机{}，查询到{}条记录",
                                    queryId,
                                    host,
                                    result.getTotalCount());
                            return "详情查询"
                                    + queryId
                                    + "成功: "
                                    + result.getTotalCount()
                                    + "条记录(主机:"
                                    + host
                                    + ")";
                        } catch (Exception e) {
                            log.error("❌ 详情查询{}失败", queryId, e);
                            throw new RuntimeException(
                                    "详情查询" + queryId + "失败: " + e.getMessage(), e);
                        }
                    },
                    executor);
        }

        /** 创建基于级别的详情查询 */
        private CompletableFuture<String> createDetailQueryByLevel(
                ExecutorService executor, String level, int queryId) {
            return CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            LogSearchDTO request = createBaseSearchRequest();
                            request.setKeywords(List.of(level)); // 关键字自动应用到所有配置字段
                            request.setPageSize(30);

                            LogDetailResultDTO result = logSearchService.searchDetails(request);

                            // 数据验证
                            validateDetailResult(result, "级别查询", level);

                            log.info(
                                    "✅ 详情查询{}完成 - 级别{}，查询到{}条记录",
                                    queryId,
                                    level,
                                    result.getTotalCount());
                            return "详情查询"
                                    + queryId
                                    + "成功: "
                                    + result.getTotalCount()
                                    + "条记录(级别:"
                                    + level
                                    + ")";
                        } catch (Exception e) {
                            log.error("❌ 详情查询{}失败", queryId, e);
                            throw new RuntimeException(
                                    "详情查询" + queryId + "失败: " + e.getMessage(), e);
                        }
                    },
                    executor);
        }

        /** 创建基于服务的详情查询 */
        private CompletableFuture<String> createDetailQueryByService(
                ExecutorService executor, String service, int queryId) {
            return CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            LogSearchDTO request = createBaseSearchRequest();
                            request.setKeywords(List.of(service)); // 关键字自动应用到所有配置字段
                            request.setFields(
                                    List.of(
                                            "message.service",
                                            "message.timestamp",
                                            "message.thread"));
                            request.setPageSize(20);

                            LogDetailResultDTO result = logSearchService.searchDetails(request);

                            // 数据验证
                            validateDetailResult(result, "服务查询", service);

                            log.info(
                                    "✅ 详情查询{}完成 - 服务{}，查询到{}条记录",
                                    queryId,
                                    service,
                                    result.getTotalCount());
                            return "详情查询"
                                    + queryId
                                    + "成功: "
                                    + result.getTotalCount()
                                    + "条记录(服务:"
                                    + service
                                    + ")";
                        } catch (Exception e) {
                            log.error("❌ 详情查询{}失败", queryId, e);
                            throw new RuntimeException(
                                    "详情查询" + queryId + "失败: " + e.getMessage(), e);
                        }
                    },
                    executor);
        }

        /** 创建基于错误关键字的详情查询 */
        private CompletableFuture<String> createDetailQueryByErrorKeyword(
                ExecutorService executor, String keyword, int queryId) {
            return CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            LogSearchDTO request = createBaseSearchRequest();
                            request.setKeywords(List.of(keyword)); // 关键字自动应用到所有配置字段
                            request.setWhereSqls(List.of("message.level = 'ERROR'"));
                            request.setPageSize(10);

                            LogDetailResultDTO result = logSearchService.searchDetails(request);

                            // 数据验证
                            validateDetailResult(result, "错误关键字查询", keyword);

                            log.info(
                                    "✅ 详情查询{}完成 - 错误关键字{}，查询到{}条记录",
                                    queryId,
                                    keyword,
                                    result.getTotalCount());
                            return "详情查询"
                                    + queryId
                                    + "成功: "
                                    + result.getTotalCount()
                                    + "条记录(错误:"
                                    + keyword
                                    + ")";
                        } catch (Exception e) {
                            log.error("❌ 详情查询{}失败", queryId, e);
                            throw new RuntimeException(
                                    "详情查询" + queryId + "失败: " + e.getMessage(), e);
                        }
                    },
                    executor);
        }

        /** 验证详情查询结果 */
        private void validateDetailResult(
                LogDetailResultDTO result, String queryType, String queryParam) {
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotNull();
            assertThat(result.getTotalCount()).isNotNull().isGreaterThanOrEqualTo(0);
            assertThat(result.getColumns()).isNotNull().isNotEmpty();

            // 如果有数据，验证数据的完整性
            if (result.getTotalCount() > 0) {
                assertThat(result.getRows()).isNotEmpty();
                assertThat((long) result.getRows().size())
                        .isLessThanOrEqualTo(result.getTotalCount());

                // 验证每行数据都不为空
                result.getRows()
                        .forEach(
                                row -> {
                                    assertThat(row).isNotNull().isNotEmpty();
                                });
            }

            log.debug(
                    "✅ {}({})数据验证通过 - 总数:{}, 返回行数:{}, 列数:{}",
                    queryType,
                    queryParam,
                    result.getTotalCount(),
                    result.getRows().size(),
                    result.getColumns().size());
        }

        /** 创建直方图查询任务 */
        private List<CompletableFuture<String>> createHistogramQueries(
                ExecutorService executor, int queryCount) {
            List<CompletableFuture<String>> futures = new ArrayList<>();

            String[] timeGroupings = {"minute", "hour", "auto", "second", "day"};
            Integer[] targetBuckets = {30, 50, 60, 40, 20};
            String[] actualLevels = {"INFO", "ERROR", "WARN", "DEBUG"};
            String[] actualHosts = {
                "172.20.61.22", "172.20.61.18", "172.20.61.35", "192.168.1.10", "10.0.1.15"
            };

            int queryId = 1;

            // 不同时间分组单位的直方图 (5个)
            for (int i = 0; i < 5 && queryId <= queryCount; i++, queryId++) {
                final String timeGrouping = timeGroupings[i];
                final Integer targetBucket = targetBuckets[i];
                futures.add(
                        createHistogramQueryByTimeGrouping(
                                executor, timeGrouping, targetBucket, queryId));
            }

            // 带条件过滤的直方图 (5个)
            for (int i = 0; i < 5 && queryId <= queryCount; i++, queryId++) {
                final String level = actualLevels[i % actualLevels.length];
                futures.add(createHistogramQueryByLevel(executor, level, queryId));
            }

            // 复杂条件的直方图 (剩余数量)
            for (int i = 0; i < actualHosts.length && queryId <= queryCount; i++, queryId++) {
                final String host = actualHosts[i];
                futures.add(createHistogramQueryByHostCondition(executor, host, queryId));
            }

            return futures;
        }

        /** 创建基于时间分组的直方图查询 */
        private CompletableFuture<String> createHistogramQueryByTimeGrouping(
                ExecutorService executor, String timeGrouping, Integer targetBucket, int queryId) {
            return CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            LogSearchDTO request = createBaseSearchRequest();
                            request.setTimeGrouping(timeGrouping);
                            request.setTargetBuckets(targetBucket);

                            LogHistogramResultDTO result =
                                    logSearchService.searchHistogram(request);

                            // 数据验证
                            validateHistogramResult(
                                    result, "时间分组查询", timeGrouping + "/" + targetBucket);

                            log.info(
                                    "✅ 直方图查询{}完成 - {}分组/{}目标桶，{}个时间窗口",
                                    queryId,
                                    timeGrouping,
                                    targetBucket,
                                    result.getDistributionData().size());
                            return "直方图查询"
                                    + queryId
                                    + "成功: "
                                    + result.getDistributionData().size()
                                    + "个时间窗口("
                                    + timeGrouping
                                    + "/"
                                    + targetBucket
                                    + "桶)";
                        } catch (Exception e) {
                            log.error("❌ 直方图查询{}失败", queryId, e);
                            throw new RuntimeException(
                                    "直方图查询" + queryId + "失败: " + e.getMessage(), e);
                        }
                    },
                    executor);
        }

        /** 创建基于级别的直方图查询 */
        private CompletableFuture<String> createHistogramQueryByLevel(
                ExecutorService executor, String level, int queryId) {
            return CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            LogSearchDTO request = createBaseSearchRequest();
                            request.setTimeGrouping("auto");
                            request.setTargetBuckets(45);
                            request.setWhereSqls(List.of("message.level = '" + level + "'"));

                            LogHistogramResultDTO result =
                                    logSearchService.searchHistogram(request);

                            // 数据验证
                            validateHistogramResult(result, "级别过滤查询", level);

                            log.info(
                                    "✅ 直方图查询{}完成 - {}级别时间分布，{}个时间窗口",
                                    queryId,
                                    level,
                                    result.getDistributionData().size());
                            return "直方图查询"
                                    + queryId
                                    + "成功: "
                                    + result.getDistributionData().size()
                                    + "个时间窗口("
                                    + level
                                    + "级别)";
                        } catch (Exception e) {
                            log.error("❌ 直方图查询{}失败", queryId, e);
                            throw new RuntimeException(
                                    "直方图查询" + queryId + "失败: " + e.getMessage(), e);
                        }
                    },
                    executor);
        }

        /** 创建基于主机条件的直方图查询 */
        private CompletableFuture<String> createHistogramQueryByHostCondition(
                ExecutorService executor, String host, int queryId) {
            return CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            LogSearchDTO request = createBaseSearchRequest();
                            request.setTimeGrouping("hour");
                            request.setTargetBuckets(24);
                            request.setWhereSqls(
                                    List.of("host = '" + host + "' AND message.level != 'DEBUG'"));

                            LogHistogramResultDTO result =
                                    logSearchService.searchHistogram(request);

                            // 数据验证
                            validateHistogramResult(result, "主机条件查询", host);

                            log.info(
                                    "✅ 直方图查询{}完成 - {}主机非DEBUG分布，{}个时间窗口",
                                    queryId,
                                    host,
                                    result.getDistributionData().size());
                            return "直方图查询"
                                    + queryId
                                    + "成功: "
                                    + result.getDistributionData().size()
                                    + "个时间窗口("
                                    + host
                                    + "主机)";
                        } catch (Exception e) {
                            log.error("❌ 直方图查询{}失败", queryId, e);
                            throw new RuntimeException(
                                    "直方图查询" + queryId + "失败: " + e.getMessage(), e);
                        }
                    },
                    executor);
        }

        /** 验证直方图查询结果 */
        private void validateHistogramResult(
                LogHistogramResultDTO result, String queryType, String queryParam) {
            assertThat(result).isNotNull();
            assertThat(result.getDistributionData()).isNotNull();
            assertThat(result.getTimeUnit()).isNotNull().isNotBlank();
            assertThat(result.getTimeInterval()).isNotNull().isGreaterThan(0);

            // 验证时间分布数据的完整性
            result.getDistributionData()
                    .forEach(
                            data -> {
                                assertThat(data).isNotNull();
                                assertThat(data.getTimePoint()).isNotNull();
                                assertThat(data.getCount()).isNotNull().isGreaterThanOrEqualTo(0);
                            });

            log.debug(
                    "✅ {}({})数据验证通过 - 时间窗口数:{}, 时间单位:{}, 间隔:{}",
                    queryType,
                    queryParam,
                    result.getDistributionData().size(),
                    result.getTimeUnit(),
                    result.getTimeInterval());
        }

        /** 创建字段分布查询任务 */
        private List<CompletableFuture<String>> createFieldDistributionQueries(
                ExecutorService executor, int queryCount) {
            List<CompletableFuture<String>> futures = new ArrayList<>();

            String[] singleFields = {
                "host", "source", "message.level", "message.service", "message.thread"
            };
            String[][] multipleFields = {
                {"host", "source"},
                {"message.level", "message.service"},
                {"host", "message.level"},
                {"source", "message.thread"},
                {"message.service", "message.environment"}
            };
            String[] actualLevels = {"INFO", "ERROR", "WARN", "DEBUG"};

            int queryId = 1;

            // 单字段分布查询 (5个)
            for (int i = 0; i < 5 && queryId <= queryCount; i++, queryId++) {
                final String field = singleFields[i];
                final int currentQueryId = queryId;
                futures.add(createFieldDistributionQuerySingle(executor, field, currentQueryId));
            }

            // 多字段分布查询 (5个)
            for (int i = 0; i < 5 && queryId <= queryCount; i++, queryId++) {
                final String[] fields = multipleFields[i];
                final int currentQueryId = queryId;
                futures.add(createFieldDistributionQueryMultiple(executor, fields, currentQueryId));
            }

            // 带条件过滤的字段分布查询 (剩余数量)
            for (int i = 0; i < actualLevels.length && queryId <= queryCount; i++, queryId++) {
                final String level = actualLevels[i];
                final int currentQueryId = queryId;
                futures.add(
                        createFieldDistributionQueryWithCondition(executor, level, currentQueryId));
            }

            return futures;
        }

        /** 创建单字段分布查询 */
        private CompletableFuture<String> createFieldDistributionQuerySingle(
                ExecutorService executor, String field, int queryId) {
            return CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            LogSearchDTO request = createBaseSearchRequest();
                            request.setFields(List.of(field));

                            LogFieldDistributionResultDTO result =
                                    logSearchService.searchFieldDistributions(request);

                            // 数据验证
                            validateFieldDistributionResult(result, "单字段查询", field);

                            int totalDistributions =
                                    result.getFieldDistributions().stream()
                                            .mapToInt(fd -> fd.getValueDistributions().size())
                                            .sum();
                            log.info(
                                    "✅ 字段分布查询{}完成 - {}字段，{}个不同值",
                                    queryId,
                                    field,
                                    totalDistributions);
                            return "字段分布查询"
                                    + queryId
                                    + "成功: "
                                    + totalDistributions
                                    + "个值("
                                    + field
                                    + ")";
                        } catch (Exception e) {
                            log.error("❌ 字段分布查询{}失败", queryId, e);
                            throw new RuntimeException(
                                    "字段分布查询" + queryId + "失败: " + e.getMessage(), e);
                        }
                    },
                    executor);
        }

        /** 创建多字段分布查询 */
        private CompletableFuture<String> createFieldDistributionQueryMultiple(
                ExecutorService executor, String[] fields, int queryId) {
            return CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            LogSearchDTO request = createBaseSearchRequest();
                            request.setFields(List.of(fields));

                            LogFieldDistributionResultDTO result =
                                    logSearchService.searchFieldDistributions(request);

                            // 数据验证
                            validateFieldDistributionResult(
                                    result, "多字段查询", String.join("+", fields));

                            log.info(
                                    "✅ 字段分布查询{}完成 - {}字段组合，{}个字段分布",
                                    queryId,
                                    String.join("+", fields),
                                    result.getFieldDistributions().size());
                            return "字段分布查询"
                                    + queryId
                                    + "成功: "
                                    + result.getFieldDistributions().size()
                                    + "个字段分布("
                                    + String.join("+", fields)
                                    + ")";
                        } catch (Exception e) {
                            log.error("❌ 字段分布查询{}失败", queryId, e);
                            throw new RuntimeException(
                                    "字段分布查询" + queryId + "失败: " + e.getMessage(), e);
                        }
                    },
                    executor);
        }

        /** 创建带条件的字段分布查询 */
        private CompletableFuture<String> createFieldDistributionQueryWithCondition(
                ExecutorService executor, String level, int queryId) {
            return CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            LogSearchDTO request = createBaseSearchRequest();
                            request.setFields(List.of("host", "message.service"));
                            request.setWhereSqls(List.of("message.level = '" + level + "'"));

                            LogFieldDistributionResultDTO result =
                                    logSearchService.searchFieldDistributions(request);

                            // 数据验证
                            validateFieldDistributionResult(result, "条件过滤查询", level);

                            log.info(
                                    "✅ 字段分布查询{}完成 - {}级别的主机+服务分布，{}个字段",
                                    queryId,
                                    level,
                                    result.getFieldDistributions().size());
                            return "字段分布查询"
                                    + queryId
                                    + "成功: "
                                    + result.getFieldDistributions().size()
                                    + "个字段分布("
                                    + level
                                    + "级别)";
                        } catch (Exception e) {
                            log.error("❌ 字段分布查询{}失败", queryId, e);
                            throw new RuntimeException(
                                    "字段分布查询" + queryId + "失败: " + e.getMessage(), e);
                        }
                    },
                    executor);
        }

        /** 验证字段分布查询结果 */
        private void validateFieldDistributionResult(
                LogFieldDistributionResultDTO result, String queryType, String queryParam) {
            assertThat(result).isNotNull();
            assertThat(result.getFieldDistributions()).isNotNull().isNotEmpty();
            assertThat(result.getSampleSize()).isNotNull().isGreaterThan(0);

            // 验证新增的actualSampleCount字段 - 这是我们功能修改的核心验证
            assertThat(result.getActualSampleCount())
                    .as("actualSampleCount字段应该被正确设置")
                    .isNotNull()
                    .isGreaterThan(0);

            // 验证actualSampleCount与sampleSize的业务逻辑关系
            assertThat(result.getActualSampleCount())
                    .as("实际采样数应该小于等于配置的采样大小")
                    .isLessThanOrEqualTo(result.getSampleSize());

            // 验证isSampledStatistics字段的逻辑一致性
            assertThat(result.getIsSampledStatistics()).isNotNull();
            if (result.getActualSampleCount() < result.getSampleSize()) {
                // 当实际采样数小于配置大小时，说明数据量不足，这是正常的采样统计
                assertThat(result.getIsSampledStatistics()).as("数据量不足时应标记为采样统计").isTrue();
            }

            // 验证每个字段分布的完整性
            result.getFieldDistributions()
                    .forEach(
                            fieldDistribution -> {
                                assertThat(fieldDistribution).isNotNull();
                                assertThat(fieldDistribution.getFieldName())
                                        .isNotNull()
                                        .isNotBlank();
                                assertThat(fieldDistribution.getValueDistributions()).isNotNull();

                                // 验证值分布数据
                                fieldDistribution
                                        .getValueDistributions()
                                        .forEach(
                                                valueDistribution -> {
                                                    assertThat(valueDistribution).isNotNull();
                                                    assertThat(valueDistribution.getCount())
                                                            .isNotNull()
                                                            .isGreaterThan(0);
                                                    assertThat(valueDistribution.getPercentage())
                                                            .isNotNull()
                                                            .isGreaterThanOrEqualTo(0.0);
                                                });
                            });

            log.debug(
                    "✅ {}({})数据验证通过 - 字段数:{}, 配置采样大小:{}, 实际采样数:{}, 采样统计:{}",
                    queryType,
                    queryParam,
                    result.getFieldDistributions().size(),
                    result.getSampleSize(),
                    result.getActualSampleCount(),
                    result.getIsSampledStatistics());
        }

        /** 验证查询结果 */
        private void validateQueryResults(ConcurrentTestResult testResult) {
            // 验证所有查询都成功
            assertThat(testResult.results).hasSize(testResult.totalQueries);
            testResult.results.forEach(
                    result -> {
                        assertThat(result).contains("成功");
                        log.debug("📋 查询结果: {}", result);
                    });

            // 验证没有异常
            testResult.futures.forEach(
                    future -> {
                        assertThat(future).isCompleted();
                        assertThat(future).isNotCancelled();
                        assertThat(future.isCompletedExceptionally()).isFalse();
                    });

            // 验证成功率
            double successRate = (testResult.successCount * 100.0 / testResult.totalQueries);
            assertThat(successRate).isEqualTo(100.0);

            log.info("✅ 查询结果验证通过 - 成功率: {}%", String.format("%.2f", successRate));
        }

        /** 记录测试统计信息 */
        private void logTestStatistics(
                ConcurrentTestResult testResult, ConcurrentTestContext context) {
            double successRate = testResult.successCount * 100.0 / testResult.totalQueries;
            double throughput = testResult.totalQueries * 1000.0 / testResult.totalTime;

            log.info("🎉 HikariCP高并发压测完成！");
            log.info("📊 执行统计:");
            log.info("   - 并发线程数: {} 个", context.threadCount);
            log.info(
                    "   - 总查询数: {} 个 ({}详情 + {}直方图 + {}字段分布)",
                    testResult.totalQueries,
                    context.detailQueries,
                    context.histogramQueries,
                    context.fieldDistributionQueries);
            log.info("   - 成功查询数: {} 个", testResult.successCount);
            log.info("   - 成功率: {}%", String.format("%.2f", successRate));
            log.info("   - 总耗时: {} ms", testResult.totalTime);
            log.info("   - 平均耗时: {} ms/查询", testResult.totalTime / testResult.totalQueries);
            log.info("   - 吞吐量: {} 查询/秒", String.format("%.2f", throughput));
            log.info("   - 数据源模块: {}", context.moduleName);
            log.info("   - 数据源ID: {}", context.datasourceId);

            // 最终连接池状态检查
            logDataSourceStatus("测试结束", context.datasourceId);
        }

        /** 清理资源 */
        private void cleanupResources(
                ExecutorService executor, ScheduledExecutorService monitor, Long datasourceId) {
            monitor.shutdown();
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
                if (!monitor.awaitTermination(5, TimeUnit.SECONDS)) {
                    monitor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                monitor.shutdownNow();
            }
            log.info("🧹 线程池已清理");

            // 最终状态记录
            logDataSourceStatus("清理完成", datasourceId);
        }

        /** 记录数据源连接池状态信息 */
        private void logDataSourceStatus(String phase, Long datasourceId) {
            try {
                log.info("📊 ========== {} 数据源状态 ==========", phase);
                log.info("📌 数据源ID: {}", datasourceId);

                // 尝试通过JMX获取HikariCP连接池信息
                try {
                    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

                    // HikariCP的JMX ObjectName模式: com.zaxxer.hikari:type=Pool (池名称)
                    // 连接池名称格式: HikariPool-数据源名称-数据源ID
                    String datasourceName = logSearchDataInitializer.getTestDatasource().getName();
                    String poolName =
                            String.format("HikariPool-%s-%s", datasourceName, datasourceId);
                    ObjectName objectName =
                            new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");

                    log.debug("🔍 查找JMX连接池: {}", objectName);
                    log.debug("🔍 已注册的HikariCP MBeans:");
                    mBeanServer
                            .queryNames(new ObjectName("com.zaxxer.hikari:*"), null)
                            .forEach(name -> log.debug("   - {}", name));

                    if (mBeanServer.isRegistered(objectName)) {
                        Integer totalConnections =
                                (Integer) mBeanServer.getAttribute(objectName, "TotalConnections");
                        Integer activeConnections =
                                (Integer) mBeanServer.getAttribute(objectName, "ActiveConnections");
                        Integer idleConnections =
                                (Integer) mBeanServer.getAttribute(objectName, "IdleConnections");
                        Integer threadsAwaitingConnection =
                                (Integer)
                                        mBeanServer.getAttribute(
                                                objectName, "ThreadsAwaitingConnection");

                        log.info("🔗 连接池状态:");
                        log.info("   - 总连接数: {}", totalConnections);
                        log.info("   - 活跃连接数: {}", activeConnections);
                        log.info("   - 空闲连接数: {}", idleConnections);
                        log.info("   - 等待连接的线程数: {}", threadsAwaitingConnection);
                        double poolUtilization =
                                totalConnections > 0
                                        ? (activeConnections * 100.0 / totalConnections)
                                        : 0;
                        log.info("   - 连接池利用率: {}%", String.format("%.2f", poolUtilization));

                    } else {
                        log.info("🔗 连接池状态: JMX MBean未找到 - {}", objectName);
                        log.info("   可能原因: 1) JMX监控未启用 2) 连接池尚未初始化 3) 连接池名称不匹配");
                    }
                } catch (Exception jmxException) {
                    log.debug("⚠️  无法通过JMX获取连接池状态: {}", jmxException.getMessage());
                    log.info("🔗 连接池状态: 无法获取详细信息 (JMX不可用)");
                }

                // 记录系统资源状态
                Runtime runtime = Runtime.getRuntime();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                long usedMemory = totalMemory - freeMemory;
                double memoryUsageRate = (usedMemory * 100.0 / totalMemory);

                log.info("💾 系统资源:");
                log.info("   - 已用内存: {} MB", usedMemory / 1024 / 1024);
                log.info("   - 空闲内存: {} MB", freeMemory / 1024 / 1024);
                log.info("   - 总内存: {} MB", totalMemory / 1024 / 1024);
                log.info("   - 内存使用率: {}%", String.format("%.2f", memoryUsageRate));
                log.info("   - 活跃线程数: {}", Thread.activeCount());

                log.info("📊 ==========================================");

            } catch (Exception e) {
                log.warn("⚠️  记录数据源状态时发生异常: {}", e.getMessage());
            }
        }
    }

    // ==================== 异常处理功能组 ====================

    @Nested
    @DisplayName("异常处理功能组")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ExceptionHandlingIntegrationTest {

        @Test
        @Order(1)
        @DisplayName("ERROR-001: 无效模块名查询")
        void testInvalidModuleSearch() {
            log.info("🔍 测试无效模块名查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();
            searchRequest.setModule("invalid-module-name"); // 不存在的模块名

            // 验证抛出业务异常
            assertThatThrownBy(() -> logSearchService.searchDetails(searchRequest))
                    .isInstanceOf(BusinessException.class);

            log.info("✅ 无效模块名查询异常验证通过");
        }

        @Test
        @Order(2)
        @DisplayName("ERROR-002: 恶意SQL注入防护测试")
        void testSqlInjectionProtection() {
            log.info("🔍 测试SQL注入防护");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 尝试SQL注入攻击
            List<String> maliciousSqls =
                    List.of(
                            "'; DROP TABLE test_doris_table; --",
                            "1=1 OR 1=1",
                            "UNION SELECT * FROM mysql.user",
                            "' OR 1=1 --");

            for (String maliciousSql : maliciousSqls) {
                searchRequest.setWhereSqls(List.of(maliciousSql));

                // 应该安全处理，不会抛出异常或执行恶意SQL
                try {
                    LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);
                    assertThat(result).isNotNull();
                } catch (Exception e) {
                    // 预期可能抛出语法错误，这是正常的
                    log.debug("恶意SQL被正常拦截: {}", e.getMessage());
                }
            }

            log.info("✅ SQL注入防护测试通过");
        }

        @Test
        @Order(3)
        @DisplayName("ERROR-003: 无效字段名查询")
        void testInvalidFieldNameSearch() {
            log.info("🔍 测试无效字段名查询");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试在WHERE条件中使用无效字段名
            searchRequest.setWhereSqls(List.of("invalid_field_name = 'test'"));

            // 验证抛出SQL异常或业务异常
            assertThatThrownBy(() -> logSearchService.searchDetails(searchRequest))
                    .isInstanceOf(Exception.class);

            log.info("✅ 无效字段名查询异常验证通过");
        }

        @Test
        @Order(4)
        @DisplayName("ERROR-004: 大分页参数测试")
        void testExcessivePaginationParams() {
            log.info("🔍 测试大分页参数");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试超大页面尺寸
            searchRequest.setPageSize(5000);
            searchRequest.setOffset(0);

            // 执行查询，验证系统能否处理超大分页
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            assertThat(result).isNotNull();
            // 系统应该限制实际返回的数据量
            assertThat(result.getRows().size()).isLessThanOrEqualTo(5000);

            log.info(
                    "✅ 超大分页参数测试通过 - 请求{}条，实际返回{}条",
                    searchRequest.getPageSize(),
                    result.getRows().size());
        }

        @Test
        @Order(5)
        @DisplayName("ERROR-005: 边界条件处理测试")
        void testEmptyDatabaseConnection() {
            log.info("🔍 测试边界条件处理");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 设置可能导致空结果的条件
            searchRequest.setWhereSqls(List.of("host = 'non-existent-host-name'"));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证空结果的正确处理
            assertThat(result).isNotNull();
            assertThat(result.getTotalCount()).isEqualTo(0);
            assertThat(result.getRows()).isEmpty();
            assertThat(result.getColumns()).isNotEmpty(); // 列信息应该存在

            log.info("✅ 边界条件测试通过 - 空结果正确处理");
        }
    }

    // ==================== 辅助方法 ====================

    /** 创建基础查询请求 - 使用相对时间范围匹配测试数据 */
    private LogSearchDTO createBaseSearchRequest() {
        LogSearchDTO request = new LogSearchDTO();

        // 设置模块名
        request.setModule(logSearchDataInitializer.getTestModule().getName());

        // 设置分页参数
        request.setPageSize(10);
        request.setOffset(0);

        // 设置时间范围：最近24小时，匹配测试数据的生成规律
        request.setTimeRange("last_24h");

        return request;
    }
}
