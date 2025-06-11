package com.hinadt.miaocha.application.service.sql.builder.condition;

import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 搜索条件管理器 负责组合各种搜索条件 */
@Component
public class SearchConditionManager {

    private final List<SearchConditionBuilder> conditionBuilders;

    @Autowired
    public SearchConditionManager(List<SearchConditionBuilder> conditionBuilders) {
        this.conditionBuilders = conditionBuilders;
    }

    /**
     * 构建所有适用的搜索条件
     *
     * @param dto 日志搜索DTO
     * @return 完整的WHERE条件子句（不包含WHERE关键字）
     */
    public String buildSearchConditions(LogSearchDTO dto) {
        StringBuilder conditions = new StringBuilder();
        boolean isFirst = true;

        // 检查是否有KeywordPhraseConditionBuilder可用
        boolean hasKeywordPhraseBuilder = false;
        for (SearchConditionBuilder builder : conditionBuilders) {
            if (builder.getClass().getSimpleName().equals("KeywordPhraseConditionBuilder")
                    && builder.supports(dto)) {
                hasKeywordPhraseBuilder = true;
                break;
            }
        }

        for (SearchConditionBuilder builder : conditionBuilders) {
            if (builder.supports(dto)) {
                // 如果KeywordPhraseConditionBuilder可用，跳过其他关键字Builder
                if (hasKeywordPhraseBuilder && isOldKeywordBuilder(builder)) {
                    continue;
                }

                String condition = builder.buildCondition(dto);
                if (condition != null && !condition.isEmpty()) {
                    if (!isFirst) {
                        conditions.append(" AND ");
                    }
                    conditions.append(condition);
                    isFirst = false;
                }
            }
        }

        return conditions.toString();
    }

    /** 判断是否为关键字相关的Builder */
    private boolean isKeywordBuilder(SearchConditionBuilder builder) {
        String className = builder.getClass().getSimpleName();
        return className.contains("Keyword") && className.contains("ConditionBuilder");
    }

    /** 判断是否为旧的关键字Builder（需要被新Builder替代的） */
    private boolean isOldKeywordBuilder(SearchConditionBuilder builder) {
        String className = builder.getClass().getSimpleName();
        return className.equals("KeywordMatchAnyConditionBuilder")
                || className.equals("KeywordMatchAllConditionBuilder")
                || className.equals("KeywordComplexExpressionBuilder");
    }
}
