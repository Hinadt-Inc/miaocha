package com.hina.log.service.sql.builder.condition;

import com.hina.log.dto.LogSearchDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 关键字OR条件构建器
 * 处理单个关键字和多个关键字OR关系的情况，使用OR操作符和MATCH_ALL替代MATCH_ANY
 * 这种方法避免了分词器对连字符等特殊字符的错误处理问题
 */
@Component
@Order(20) // 中等优先级，与原match_any相同
public class KeywordOrConditionBuilder implements SearchConditionBuilder {

    private static final String MESSAGE_COLUMN = "message";

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

        if (keyword.contains(" || ")) {
            // 处理OR关系的关键字
            return buildOrCondition(keyword);
        } else {
            // 处理单个关键字
            return buildSingleKeywordCondition(keyword);
        }
    }

    /**
     * 构建单个关键字的条件
     */
    private String buildSingleKeywordCondition(String keyword) {
        String key = keyword.replaceAll("^'|'$", "").trim(); // 去除可能的引号
        if (StringUtils.isBlank(key)) {
            return "";
        }

        // 对单个关键字使用MATCH_ALL操作符
        return MESSAGE_COLUMN + " MATCH_ALL '" + key + "'";
    }

    /**
     * 构建OR关系的条件
     */
    private String buildOrCondition(String keyword) {
        String[] keywords = keyword.split(" \\|\\| ");
        List<String> conditions = new ArrayList<>();

        for (String key : keywords) {
            String trimmedKey = key.trim().replaceAll("^'|'$", ""); // 去除可能的引号
            if (StringUtils.isNotBlank(trimmedKey)) {
                // 为每个关键字构建MATCH_ALL条件
                conditions.add(MESSAGE_COLUMN + " MATCH_ALL '" + trimmedKey + "'");
            }
        }

        if (conditions.isEmpty()) {
            return "";
        } else if (conditions.size() == 1) {
            return conditions.get(0);
        } else {
            // 使用括号包围OR条件以保证优先级
            return "(" + String.join(" OR ", conditions) + ")";
        }
    }
}