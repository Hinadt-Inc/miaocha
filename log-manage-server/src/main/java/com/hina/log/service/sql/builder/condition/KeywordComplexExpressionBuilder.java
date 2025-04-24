package com.hina.log.service.sql.builder.condition;

import com.hina.log.dto.LogSearchDTO;
import com.hina.log.exception.KeywordSyntaxException;
import com.hina.log.service.sql.builder.condition.parser.KeywordExpressionParser;
import com.hina.log.service.sql.builder.condition.parser.KeywordExpressionParser.ParseResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 复杂关键字表达式条件构建器
 * 处理包含括号和复合逻辑的复杂表达式
 * 比如: ('error' || 'warning') && ('timeout' || 'failure')
 * 
 * 说明：
 * 1. 对于OR关系（||），使用SQL的OR操作符和LIKE条件，而不是MATCH_ANY
 * 2. 对于AND关系（&&），使用MATCH_ALL或SQL的AND操作符
 * 3. 最多支持两层嵌套，符合大多数查询需求
 */
@Component
@Order(10) // 高优先级，确保在简单表达式处理器之前执行
public class KeywordComplexExpressionBuilder implements SearchConditionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(KeywordComplexExpressionBuilder.class);

    @Override
    public boolean supports(LogSearchDTO dto) {
        if (StringUtils.isBlank(dto.getKeyword())) {
            return false;
        }
        String keyword = dto.getKeyword().trim();

        // 检查是否包含括号或复杂运算符组合
        boolean containsParentheses = keyword.contains("(") && keyword.contains(")");
        boolean containsComplexOperators = (keyword.contains("&&") && keyword.contains("||")) ||
                keyword.contains("(") || keyword.contains(")");

        return containsParentheses || containsComplexOperators;
    }

    @Override
    public String buildCondition(LogSearchDTO dto) {
        if (!supports(dto)) {
            return "";
        }

        // 使用表达式解析器解析复杂表达式
        ParseResult parseResult = KeywordExpressionParser.parse(dto.getKeyword().trim());

        // 如果语法错误，抛出异常
        if (!parseResult.isSuccess()) {
            // 记录错误日志
            logger.warn("关键字表达式语法错误: {}, 表达式: {}", parseResult.getErrorMessage(), dto.getKeyword());
            // 抛出自定义异常
            throw new KeywordSyntaxException(parseResult.getErrorMessage(), dto.getKeyword());
        }

        return parseResult.getResult();
    }
}