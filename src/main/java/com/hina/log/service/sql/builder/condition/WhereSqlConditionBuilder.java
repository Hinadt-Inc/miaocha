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
        return StringUtils.isNotBlank(dto.getWhereSql());
    }

    @Override
    public String buildCondition(LogSearchDTO dto) {
        if (!supports(dto)) {
            return "";
        }

        // 直接返回用户输入的SQL条件
        return dto.getWhereSql().trim();
    }
}