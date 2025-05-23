package com.hina.log.application.service.sql.builder.condition;

import com.hina.log.domain.dto.LogSearchDTO;
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
        // 检查keywords列表
        if (dto.getKeywords() != null && !dto.getKeywords().isEmpty()) {
            for (String keyword : dto.getKeywords()) {
                if (StringUtils.isNotBlank(keyword)) {
                    String trimmedKeyword = keyword.trim();
                    // 排除复杂表达式和AND关系的表达式
                    if (!trimmedKeyword.contains(" && ") && !trimmedKeyword.contains("(") && !trimmedKeyword.contains(")")) {
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

                    // 排除复杂表达式和AND关系的表达式
                    if (!trimmedKeyword.contains(" && ") && !trimmedKeyword.contains("(") && !trimmedKeyword.contains(")")) {
                        StringBuilder keywordCondition = new StringBuilder();

                        if (trimmedKeyword.contains(" || ")) {
                            // 处理OR关系的关键字
                            String[] keywordParts = trimmedKeyword.split(" \\|\\| ");
                            StringBuilder matchAnyClause = new StringBuilder();
                            for (int i = 0; i < keywordParts.length; i++) {
                                String key = keywordParts[i].trim().replaceAll("^'|'$", ""); // 去除可能的引号
                                if (StringUtils.isNotBlank(key)) {
                                    if (i > 0) {
                                        matchAnyClause.append(" ");
                                    }
                                    matchAnyClause.append(key);
                                }
                            }
                            if (matchAnyClause.length() > 0) {
                                keywordCondition.append("message MATCH_ANY '").append(matchAnyClause).append("'");
                            }
                        } else {
                            // 处理单个关键字
                            String key = trimmedKeyword.replaceAll("^'|'$", ""); // 去除可能的引号
                            if (StringUtils.isNotBlank(key)) {
                                keywordCondition.append("message MATCH_ANY '").append(key).append("'");
                            }
                        }

                        if (keywordCondition.length() > 0) {
                            if (!isFirstCondition) {
                                condition.append(" AND ");
                            }
                            condition.append(keywordCondition);
                            isFirstCondition = false;
                        }
                    }
                }
            }
        }

        return condition.toString();
    }
}