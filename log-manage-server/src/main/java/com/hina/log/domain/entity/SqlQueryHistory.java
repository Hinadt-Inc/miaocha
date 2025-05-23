package com.hina.log.domain.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Date;

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
    private String resultFilePath;
    private LocalDateTime createTime;
    private Date updateTime;
}