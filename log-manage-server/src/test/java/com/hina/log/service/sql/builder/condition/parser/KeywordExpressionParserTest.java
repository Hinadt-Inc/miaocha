package com.hina.log.service.sql.builder.condition.parser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KeywordExpressionParserTest {

    @Test
    void testSimpleOrExpression() {
        String input = "'error' || 'warning'";
        KeywordExpressionParser.ParseResult result = KeywordExpressionParser.parse(input);

        assertTrue(result.isSuccess());
        assertEquals("(message MATCH_ALL 'error' OR message MATCH_ALL 'warning')", result.getResult());
    }

    @Test
    void testSimpleAndExpression() {
        String input = "'error' && 'warning'";
        KeywordExpressionParser.ParseResult result = KeywordExpressionParser.parse(input);

        assertTrue(result.isSuccess());
        assertEquals("message MATCH_ALL 'error warning'", result.getResult());
    }

    @Test
    void testNestedOrAndExpression() {
        String input = "('error' || 'warning') && 'critical'";
        KeywordExpressionParser.ParseResult result = KeywordExpressionParser.parse(input);

        assertTrue(result.isSuccess());
        assertEquals("((message MATCH_ALL 'error' OR message MATCH_ALL 'warning') AND message MATCH_ALL 'critical')",
                result.getResult());
    }

    @Test
    void testNestedAndOrExpression() {
        String input = "'fatal' || ('error' && 'warning')";
        KeywordExpressionParser.ParseResult result = KeywordExpressionParser.parse(input);

        assertTrue(result.isSuccess());
        assertEquals("(message MATCH_ALL 'fatal' OR message MATCH_ALL 'error warning')", result.getResult());
    }

    @Test
    void testDoubleLevelNesting() {
        String input = "('error' || 'warning') && ('timeout' || 'failure')";
        KeywordExpressionParser.ParseResult result = KeywordExpressionParser.parse(input);

        assertTrue(result.isSuccess());
        assertEquals(
                "((message MATCH_ALL 'error' OR message MATCH_ALL 'warning') AND (message MATCH_ALL 'timeout' OR message MATCH_ALL 'failure'))",
                result.getResult());
    }

    @Test
    void testHyphenatedText() {
        String input = "'cddda693-7ee6-45ac-97ce-e03db01e8e22' || 'another-hyphenated-value'";
        KeywordExpressionParser.ParseResult result = KeywordExpressionParser.parse(input);

        assertTrue(result.isSuccess());
        assertEquals(
                "(message MATCH_ALL 'cddda693-7ee6-45ac-97ce-e03db01e8e22' OR message MATCH_ALL 'another-hyphenated-value')",
                result.getResult());
    }

    @Test
    void testComplexNestedAndWithOr() {
        String input = "('term1' && 'term2') && ('term3' || 'term4')";
        KeywordExpressionParser.ParseResult result = KeywordExpressionParser.parse(input);

        assertTrue(result.isSuccess());
        assertEquals("(message MATCH_ALL 'term1 term2' AND (message MATCH_ALL 'term3' OR message MATCH_ALL 'term4'))",
                result.getResult());
    }

    @Test
    void testMixedNestedExpressionsWithHyphens() {
        String input = "('abc-123' || 'def-456') && ('error-code' && 'debug-output')";
        KeywordExpressionParser.ParseResult result = KeywordExpressionParser.parse(input);

        assertTrue(result.isSuccess());
        assertEquals(
                "((message MATCH_ALL 'abc-123' OR message MATCH_ALL 'def-456') AND message MATCH_ALL 'error-code debug-output')",
                result.getResult());
    }
}