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
        // 检查keywords列表
        if (dto.getKeywords() != null && !dto.getKeywords().isEmpty()) {
            for (String keyword : dto.getKeywords()) {
                if (StringUtils.isNotBlank(keyword)) {
                    String trimmedKeyword = keyword.trim();
                    // 处理简单的AND关系表达式，排除复杂表达式
                    if (trimmedKeyword.contains(" && ") && !trimmedKeyword.contains("(") && !trimmedKeyword.contains(")")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String buildCondition(LogSearchDTO dto) {
        if (!supports(dto)) {
            return "";
        }

        StringBuilder condition = new StringBuilder();
        boolean isFirstCondition = true;

        // 处理keywords列表
        if (dto.getKeywords() != null && !dto.getKeywords().isEmpty()) {
            for (String keyword : dto.getKeywords()) {
                if (StringUtils.isNotBlank(keyword)) {
                    String trimmedKeyword = keyword.trim();

                    // 处理简单的AND关系表达式，排除复杂表达式
                    if (trimmedKeyword.contains(" && ") && !trimmedKeyword.contains("(") && !trimmedKeyword.contains(")")) {
                        // 处理AND关系的关键字
                        String[] keywordParts = trimmedKeyword.split(" && ");
                        StringBuilder matchAllClause = new StringBuilder();
                        for (int i = 0; i < keywordParts.length; i++) {
                            String key = keywordParts[i].trim().replaceAll("^'|'$", ""); // 去除可能的引号
                            if (StringUtils.isNotBlank(key)) {
                                if (i > 0) {
                                    matchAllClause.append(" ");
                                }
                                matchAllClause.append(key);
                            }
                        }

                        if (matchAllClause.length() > 0) {
                            if (!isFirstCondition) {
                                condition.append(" AND ");
                            }
                            condition.append("message MATCH_ALL '").append(matchAllClause).append("'");
                            isFirstCondition = false;
                        }
                    }
                }
            }
        }

        return condition.toString();
    }
}