package com.hina.log.service.sql.builder.condition;

import com.hina.log.dto.LogSearchDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 关键字MATCH_ANY条件构建器
 * 处理单个关键字和多个关键字OR关系的情况
 */
@Component
@Order(20) // 中等优先级
public class KeywordMatchAnyConditionBuilder implements SearchConditionBuilder {

    @Override
    public boolean supports(LogSearchDTO dto) {
        if (StringUtils.isBlank(dto.getKeyword())) {
            return false;
        }
        String keyword = dto.getKeyword().trim();
        // 排除复杂表达式和AND关系的表达式
        return !keyword.contains(" && ") && !keyword.contains("(") && !keyword.contains(")");
    }

    @Override
    public String buildCondition(LogSearchDTO dto) {
        if (!supports(dto)) {
            return "";
        }

        String keyword = dto.getKeyword().trim();
        StringBuilder condition = new StringBuilder();

        if (keyword.contains(" || ")) {
            // 处理OR关系的关键字
            String[] keywords = keyword.split(" \\|\\| ");
            StringBuilder matchAnyClause = new StringBuilder();
            for (int i = 0; i < keywords.length; i++) {
                String key = keywords[i].trim().replaceAll("^'|'$", ""); // 去除可能的引号
                if (StringUtils.isNotBlank(key)) {
                    if (i > 0) {
                        matchAnyClause.append(" ");
                    }
                    matchAnyClause.append(key);
                }
            }
            if (matchAnyClause.length() > 0) {
                condition.append("message MATCH_ANY '").append(matchAnyClause).append("'");
            }
        } else {
            // 处理单个关键字
            String key = keyword.replaceAll("^'|'$", ""); // 去除可能的引号
            if (StringUtils.isNotBlank(key)) {
                condition.append("message MATCH_ANY '").append(key).append("'");
            }
        }

        return condition.toString();
    }
}