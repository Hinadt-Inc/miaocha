package com.hinadt.miaocha.mock.service.sql.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hinadt.miaocha.application.service.sql.builder.WhereConditionBuilder;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * WhereConditionBuilder单元测试
 *
 * <p>验证用户自定义WHERE条件的构建逻辑和SQL注入防护
 */
@DisplayName("WHERE条件构建器测试")
class WhereConditionBuilderTest {

    private WhereConditionBuilder whereConditionBuilder;

    @BeforeEach
    void setUp() {
        whereConditionBuilder = new WhereConditionBuilder();
    }

    @Test
    @DisplayName("空条件处理")
    void testEmptyConditions() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setWhereSqls(null);
        assertEquals("", whereConditionBuilder.buildWhereConditions(dto));

        dto.setWhereSqls(Collections.emptyList());
        assertEquals("", whereConditionBuilder.buildWhereConditions(dto));

        dto.setWhereSqls(Arrays.asList("", "   ", null));
        assertEquals("", whereConditionBuilder.buildWhereConditions(dto));
    }

    @Test
    @DisplayName("单条件不添加括号")
    void testSingleCondition() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setWhereSqls(List.of("level = 'ERROR'"));

        String result = whereConditionBuilder.buildWhereConditions(dto);

        assertEquals("level = 'ERROR'", result);
    }

    @Test
    @DisplayName("多条件用AND连接且添加外层括号")
    void testMultipleConditions() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setWhereSqls(
                Arrays.asList(
                        "level = 'ERROR'", "service = 'user-service'", "host LIKE 'server-%'"));

        String result = whereConditionBuilder.buildWhereConditions(dto);

        String expected = "(level = 'ERROR' AND service = 'user-service' AND host LIKE 'server-%')";
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("过滤空白条件")
    void testFilterBlankConditions() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setWhereSqls(
                Arrays.asList("level = 'ERROR'", "", "   ", null, "service = 'user-service'"));

        String result = whereConditionBuilder.buildWhereConditions(dto);

        String expected = "(level = 'ERROR' AND service = 'user-service')";
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("SQL注入防护 - 危险关键字")
    void testSqlInjectionDangerousKeywords() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setWhereSqls(Arrays.asList("level = 'INFO'; DROP TABLE logs"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            whereConditionBuilder.buildWhereConditions(dto);
                        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("危险的SQL关键字"));
    }

    @Test
    @DisplayName("SQL注入防护 - SQL注释")
    void testSqlInjectionComments() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setWhereSqls(Arrays.asList("level = 'INFO' -- AND user_id = 1"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            whereConditionBuilder.buildWhereConditions(dto);
                        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("SQL注释"));
    }

    @Test
    @DisplayName("SQL注入防护 - 引号转义")
    void testSqlInjectionQuoteEscaping() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setWhereSqls(Arrays.asList("level = '''OR 1=1 OR'''"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            whereConditionBuilder.buildWhereConditions(dto);
                        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("可疑的引号模式"));
    }

    @Test
    @DisplayName("正常条件不被误拦截")
    void testValidConditionsNotBlocked() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setWhereSqls(
                Arrays.asList(
                        "level = 'ERROR'",
                        "service IN ('user', 'order')",
                        "message LIKE '%timeout%'",
                        "log_time > DATE_SUB(NOW(), INTERVAL 1 HOUR)",
                        "variant_field['meta']['type'] = 'request'"));

        String result = whereConditionBuilder.buildWhereConditions(dto);

        String expected =
                "(level = 'ERROR' AND service IN ('user', 'order') AND message LIKE '%timeout%' AND"
                        + " log_time > DATE_SUB(NOW(), INTERVAL 1 HOUR) AND"
                        + " variant_field['meta']['type'] = 'request')";
        assertEquals(expected, result);
    }
}
