package com.hinadt.miaocha.application.service.sql.builder.condition;

import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 自定义WHERE SQL条件构建器 处理用户输入的自定义SQL条件 */
@Component
@Order(30) // 较低优先级
public class WhereSqlConditionBuilder implements SearchConditionBuilder {

    @Override
    public boolean supports(LogSearchDTO dto) {
        // 支持whereSqls列表
        return dto.getWhereSqls() != null && !dto.getWhereSqls().isEmpty();
    }

    @Override
    public String buildCondition(LogSearchDTO dto) {
        if (!supports(dto)) {
            return "";
        }

        StringBuilder condition = new StringBuilder();

        // 处理whereSqls列表
        if (dto.getWhereSqls() != null && !dto.getWhereSqls().isEmpty()) {
            boolean isFirst = true;
            for (String whereSql : dto.getWhereSqls()) {
                if (StringUtils.isNotBlank(whereSql)) {
                    if (!isFirst) {
                        condition.append(" AND ");
                    }
                    condition.append("(").append(whereSql.trim()).append(")");
                    isFirst = false;
                }
            }
            return condition.toString();
        }

        return "";
    }
}
