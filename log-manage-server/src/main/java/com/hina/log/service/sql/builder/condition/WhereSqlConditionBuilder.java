package com.hina.log.service.sql.builder.condition;

import com.hina.log.dto.LogSearchDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 自定义WHERE SQL条件构建器
 * 处理用户输入的自定义SQL条件
 */
@Component
@Order(30) // 较低优先级
public class WhereSqlConditionBuilder implements SearchConditionBuilder {

    @Override
    public boolean supports(LogSearchDTO dto) {
        // 支持新的whereSqls列表或旧的whereSql字段
        return (dto.getWhereSqls() != null && !dto.getWhereSqls().isEmpty()) ||
               StringUtils.isNotBlank(dto.getWhereSql());
    }

    @Override
    public String buildCondition(LogSearchDTO dto) {
        if (!supports(dto)) {
            return "";
        }

        StringBuilder condition = new StringBuilder();

        // 优先处理新的whereSqls列表
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

        // 向后兼容，处理旧的whereSql字段
        if (StringUtils.isNotBlank(dto.getWhereSql())) {
            return dto.getWhereSql().trim();
        }

        return "";
    }
}