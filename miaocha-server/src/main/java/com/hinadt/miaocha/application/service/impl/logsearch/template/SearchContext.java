package com.hinadt.miaocha.application.service.impl.logsearch.template;

import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import java.sql.Connection;
import lombok.Getter;

/**
 * 搜索执行上下文
 *
 * <p>包含执行搜索所需的所有上下文信息
 */
@Getter
public class SearchContext {

    private final Connection connection;
    private final LogSearchDTO dto;
    private final String tableName;
    private final String timeField;

    public SearchContext(
            Connection connection, LogSearchDTO dto, String tableName, String timeField) {
        this.connection = connection;
        this.dto = dto;
        this.tableName = tableName;
        this.timeField = timeField;
    }
}
