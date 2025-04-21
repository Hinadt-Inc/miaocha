package com.hina.log.service.sql.builder.condition;

import com.hina.log.dto.LogSearchDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 关键字MATCH_ALL条件构建器
 * 处理多个关键字AND关系的情况
 */
@Component
@Order(20) // 中等优先级
public class KeywordMatchAllConditionBuilder implements SearchConditionBuilder {

    @Override
    public boolean supports(LogSearchDTO dto) {
        if (StringUtils.isBlank(dto.getKeyword())) {
            return false;
        }
        String keyword = dto.getKeyword().trim();
        // 处理简单的AND关系表达式，排除复杂表达式
        return keyword.contains(" && ") && !keyword.contains("(") && !keyword.contains(")");
    }

    @Override
    public String buildCondition(LogSearchDTO dto) {
        if (!supports(dto)) {
            return "";
        }

        String keyword = dto.getKeyword().trim();
        StringBuilder condition = new StringBuilder();

        // 处理AND关系的关键字
        String[] keywords = keyword.split(" && ");
        StringBuilder matchAllClause = new StringBuilder();
        for (int i = 0; i < keywords.length; i++) {
            String key = keywords[i].trim().replaceAll("^'|'$", ""); // 去除可能的引号
            if (StringUtils.isNotBlank(key)) {
                if (i > 0) {
                    matchAllClause.append(" ");
                }
                matchAllClause.append(key);
            }
        }
        if (matchAllClause.length() > 0) {
            condition.append("message MATCH_ALL '").append(matchAllClause).append("'");
        }

        return condition.toString();
    }
}