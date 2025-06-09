package com.hinadt.miaocha.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** SQL查询历史实体类 */
@Data
public class SqlQueryHistory {
    private Long id;
    private Long userId;
    private Long datasourceId;
    private String tableName;
    private String sqlQuery;
    private String resultFilePath;
    private LocalDateTime createTime;
}
