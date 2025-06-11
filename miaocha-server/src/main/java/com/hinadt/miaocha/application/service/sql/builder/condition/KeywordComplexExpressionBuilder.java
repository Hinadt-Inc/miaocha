package com.hinadt.miaocha.application.service.sql.builder.condition;

import com.hinadt.miaocha.application.service.sql.builder.condition.parser.KeywordExpressionParser;
import com.hinadt.miaocha.application.service.sql.builder.condition.parser.KeywordExpressionParser.ParseResult;
import com.hinadt.miaocha.common.exception.KeywordSyntaxException;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 复杂关键字表达式条件构建器 处理包含括号和复合逻辑的复杂表达式 比如: ('error' || 'warning') && ('timeout' || 'failure')
 *
 * @deprecated 该类已被废弃，请使用 {@link KeywordPhraseConditionBuilder} 替代。
 *     新实现使用MATCH_PHRASE提供更简单的关键字搜索，不再使用MATCH_ANY/MATCH_ALL的复杂优化。 该类保留是为了可能的未来迁移需求。
 */
@Deprecated
@Component
@Order(20) // 降低优先级，让新的KeywordPhraseConditionBuilder优先执行
public class KeywordComplexExpressionBuilder implements SearchConditionBuilder {

    private static final Logger logger =
            LoggerFactory.getLogger(KeywordComplexExpressionBuilder.class);

    @Override
    public boolean supports(LogSearchDTO dto) {
        // 新的KeywordPhraseConditionBuilder已经启用，该Builder已废弃
        return false;
    }

    @Override
    public String buildCondition(LogSearchDTO dto) {
        if (!supports(dto)) {
            return "";
        }

        StringBuilder condition = new StringBuilder();
        boolean isFirst = true;

        // 处理keywords列表
        if (dto.getKeywords() != null && !dto.getKeywords().isEmpty()) {
            for (String keyword : dto.getKeywords()) {
                if (StringUtils.isNotBlank(keyword)) {
                    String trimmedKeyword = keyword.trim();

                    // 检查是否是复杂表达式
                    boolean isComplex =
                            (trimmedKeyword.contains("(") && trimmedKeyword.contains(")"))
                                    || (trimmedKeyword.contains("&&")
                                            && trimmedKeyword.contains("||"))
                                    || trimmedKeyword.contains("(")
                                    || trimmedKeyword.contains(")");

                    if (isComplex) {
                        // 使用表达式解析器解析复杂表达式
                        ParseResult parseResult = KeywordExpressionParser.parse(trimmedKeyword);

                        // 如果语法错误，抛出异常
                        if (!parseResult.isSuccess()) {
                            // 记录错误日志
                            logger.warn(
                                    "关键字表达式语法错误: {}, 表达式: {}",
                                    parseResult.getErrorMessage(),
                                    trimmedKeyword);
                            // 抛出自定义异常
                            throw new KeywordSyntaxException(
                                    parseResult.getErrorMessage(), trimmedKeyword);
                        }

                        if (!isFirst) {
                            condition.append(" AND ");
                        }
                        condition.append(parseResult.getResult());
                        isFirst = false;
                    }
                }
            }
        }

        return condition.length() > 0 ? condition.toString() : "";
    }
}
