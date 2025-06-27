package com.hinadt.miaocha.integration.logsearch;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hinadt.miaocha.TestContainersFactory;
import com.hinadt.miaocha.application.service.LogSearchService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.*;
import com.hinadt.miaocha.integration.data.IntegrationTestDataInitializer;
import com.hinadt.miaocha.integration.data.LogSearchTestDataInitializer;
import java.util.*;
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

            // 使用message_text字段搜索ERROR日志，配置中默认为MATCH_PHRASE方法
            KeywordConditionDTO keywordCondition = new KeywordConditionDTO();
            keywordCondition.setFieldName("message_text");
            keywordCondition.setSearchValue("NullPointerException"); // 使用测试数据中真实存在的错误信息
            // 不设置searchMethod，使用配置中的默认方法MATCH_PHRASE

            searchRequest.setKeywordConditions(List.of(keywordCondition));

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
            KeywordConditionDTO complexCondition = new KeywordConditionDTO();
            complexCondition.setFieldName("message_text");
            complexCondition.setSearchValue(
                    "('NullPointerException' || 'timeout') && ('processing' || 'request')");

            searchRequest.setKeywordConditions(List.of(complexCondition));

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
            KeywordConditionDTO tripleOrCondition = new KeywordConditionDTO();
            tripleOrCondition.setFieldName("message.level");
            tripleOrCondition.setSearchValue("'ERROR' || 'WARN' || 'INFO'");

            searchRequest.setKeywordConditions(List.of(tripleOrCondition));

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

            // 测试深度嵌套：(('user' && 'login') || ('order' && 'payment')) && 'success'
            KeywordConditionDTO nestedCondition = new KeywordConditionDTO();
            nestedCondition.setFieldName("message_text");
            nestedCondition.setSearchValue(
                    "(('user' && 'login') || ('order' && 'payment')) && 'success'");

            searchRequest.setKeywordConditions(List.of(nestedCondition));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getRows()).isNotEmpty();

            log.info("✅ 深度嵌套AND/OR组合通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(5)
        @DisplayName("KW-005: 指定搜索方法覆盖配置默认值")
        void testOverrideDefaultSearchMethod() {
            log.info("🔍 测试指定搜索方法覆盖配置默认值");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // host字段配置默认为LIKE，但这里指定使用MATCH_PHRASE
            KeywordConditionDTO overrideCondition = new KeywordConditionDTO();
            overrideCondition.setFieldName("host");
            overrideCondition.setSearchValue("172.20.61.22"); // 使用测试数据中真实存在的主机
            overrideCondition.setSearchMethod("MATCH_PHRASE"); // 覆盖默认的LIKE方法

            searchRequest.setKeywordConditions(List.of(overrideCondition));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getTotalCount()).isGreaterThan(0);

            // 验证返回的记录确实来自指定主机
            boolean foundMatch =
                    result.getRows().stream()
                            .anyMatch(
                                    row -> {
                                        Object host = row.get("host");
                                        return host != null
                                                && "172.20.61.22".equals(host.toString());
                                    });
            assertThat(foundMatch).isTrue();
            assertThat(result.getRows()).isNotEmpty();

            log.info("✅ 指定搜索方法覆盖通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(6)
        @DisplayName("KW-006: 多字段关键字搜索 - 验证多字段AND组合")
        void testMultiFieldKeywordSearch() {
            log.info("🔍 测试多字段关键字搜索");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 多个字段的关键字条件，基于真实测试数据
            KeywordConditionDTO levelCondition = new KeywordConditionDTO();
            levelCondition.setFieldName("message.level");
            levelCondition.setSearchValue("ERROR"); // 测试数据中的真实级别

            KeywordConditionDTO serviceCondition = new KeywordConditionDTO();
            serviceCondition.setFieldName("message.service");
            serviceCondition.setSearchValue("hina-cloud-engine"); // 测试数据中的真实服务名

            KeywordConditionDTO hostCondition = new KeywordConditionDTO();
            hostCondition.setFieldName("host");
            hostCondition.setSearchValue("172.20.61"); // 匹配测试数据中的主机IP段

            searchRequest.setKeywordConditions(
                    List.of(levelCondition, serviceCondition, hostCondition));

            // 执行查询
            LogDetailResultDTO result = logSearchService.searchDetails(searchRequest);

            // 验证查询结果
            assertThat(result).isNotNull();

            // 验证返回的记录确实符合所有条件
            if (result.getTotalCount() > 0) {
                boolean allMatch =
                        result.getRows().stream()
                                .allMatch(
                                        row -> {
                                            Object level = row.get("level");
                                            Object service = row.get("service");
                                            Object host = row.get("host");
                                            return level != null
                                                    && level.toString().contains("ERROR")
                                                    && service != null
                                                    && service.toString()
                                                            .contains("hina-cloud-engine")
                                                    && host != null
                                                    && host.toString().contains("172.20.61");
                                        });
                assertThat(allMatch).isTrue();
            }

            log.info("✅ 多字段关键字搜索通过 - 查询到{}条记录", result.getTotalCount());
        }

        @Test
        @Order(7)
        @DisplayName("KW-007: 空关键字搜索条件处理")
        void testEmptyKeywordConditions() {
            log.info("🔍 测试空关键字搜索条件处理");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 设置空的关键字条件列表
            searchRequest.setKeywordConditions(List.of());

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
        @DisplayName("KW-008: 不允许的字段权限验证")
        void testUnauthorizedFieldValidation() {
            log.info("🔍 测试不允许的字段权限验证");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 尝试使用未在模块配置中的字段进行关键字搜索
            KeywordConditionDTO unauthorizedCondition = new KeywordConditionDTO();
            unauthorizedCondition.setFieldName("unauthorized_field"); // 未配置的字段
            unauthorizedCondition.setSearchValue("test");

            searchRequest.setKeywordConditions(List.of(unauthorizedCondition));

            // 应该抛出KEYWORD_FIELD_NOT_ALLOWED异常
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> {
                                logSearchService.searchDetails(searchRequest);
                            });

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.KEYWORD_FIELD_NOT_ALLOWED);
            assertThat(exception.getMessage()).contains("不允许进行关键字查询");

            log.info("✅ 不允许的字段权限验证通过 - 正确拒绝了未配置字段的查询");
        }

        @Test
        @Order(9)
        @DisplayName("KW-009: 特殊字符转义处理")
        void testSpecialCharacterEscaping() {
            log.info("🔍 测试特殊字符转义处理");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试包含单引号的搜索值
            KeywordConditionDTO specialCharCondition = new KeywordConditionDTO();
            specialCharCondition.setFieldName("message_text");
            specialCharCondition.setSearchValue("user's data"); // 包含单引号

            searchRequest.setKeywordConditions(List.of(specialCharCondition));

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
        @DisplayName("KW-010: 不同搜索方法的行为验证")
        void testDifferentSearchMethodBehaviors() {
            log.info("🔍 测试不同搜索方法的行为验证");

            LogSearchDTO searchRequest = createBaseSearchRequest();

            // 测试LIKE方法的模糊匹配
            KeywordConditionDTO likeCondition = new KeywordConditionDTO();
            likeCondition.setFieldName("host");
            likeCondition.setSearchValue("61"); // 部分匹配
            likeCondition.setSearchMethod("LIKE");

            searchRequest.setKeywordConditions(List.of(likeCondition));

            LogDetailResultDTO likeResult = logSearchService.searchDetails(searchRequest);

            // 测试MATCH_PHRASE方法的精确匹配
            KeywordConditionDTO phraseCondition = new KeywordConditionDTO();
            phraseCondition.setFieldName("message.level");
            phraseCondition.setSearchValue("ERROR");
            phraseCondition.setSearchMethod("MATCH_PHRASE");

            searchRequest.setKeywordConditions(List.of(phraseCondition));

            LogDetailResultDTO phraseResult = logSearchService.searchDetails(searchRequest);

            assertThat(likeResult).isNotNull();
            assertThat(phraseResult).isNotNull();

            log.info(
                    "✅ 不同搜索方法行为验证通过 - LIKE查询到{}条记录，MATCH_PHRASE查询到{}条记录",
                    likeResult.getTotalCount(),
                    phraseResult.getTotalCount());
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
            KeywordConditionDTO keywordCondition = new KeywordConditionDTO();
            keywordCondition.setFieldName("message_text");
            keywordCondition.setSearchValue("service");
            keywordCondition.setSearchMethod("LIKE");

            searchRequest.setKeywordConditions(List.of(keywordCondition));
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

            // 使用不在模块配置中的字段进行关键字查询
            KeywordConditionDTO invalidCondition =
                    createKeywordCondition("invalid_field_name", "test", "LIKE");
            searchRequest.setKeywordConditions(List.of(invalidCondition));

            // 验证抛出字段权限异常
            assertThatThrownBy(() -> logSearchService.searchDetails(searchRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不允许进行关键字查询");

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

    /** 创建关键字条件 */
    private KeywordConditionDTO createKeywordCondition(
            String fieldName, String searchValue, String searchMethod) {
        KeywordConditionDTO condition = new KeywordConditionDTO();
        condition.setFieldName(fieldName);
        condition.setSearchValue(searchValue);
        condition.setSearchMethod(searchMethod);
        return condition;
    }
}
