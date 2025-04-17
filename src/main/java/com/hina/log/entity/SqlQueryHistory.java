package com.hina.log.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * SQL查询历史实体类
 */
@Data
public class SqlQueryHistory {
    private Long id;
    private Long userId;
    private Long datasourceId;
    private String tableName;
    private String sqlQuery;
    private LocalDateTime createTime;
}