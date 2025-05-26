package com.hina.log.application.service.sql.builder.condition;

import com.hina.log.domain.dto.LogSearchDTO;
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

        for (SearchConditionBuilder builder : conditionBuilders) {
            if (builder.supports(dto)) {
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
}
