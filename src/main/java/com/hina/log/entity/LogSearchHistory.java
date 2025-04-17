package com.hina.log.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志检索历史实体类
 */
@Data
@Schema(description = "日志检索历史实体")
public class LogSearchHistory {
    @Schema(description = "历史记录ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "数据源ID", example = "1")
    private Long datasourceId;

    @Schema(description = "日志表名", example = "log_db.log_table")
    private String tableName;

    @Schema(description = "搜索关键字", example = "error")
    private String keyword;

    @Schema(description = "开始时间", example = "2023-06-01 10:00:00")
    private String startTime;

    @Schema(description = "结束时间", example = "2023-06-01 11:00:00")
    private String endTime;

    @Schema(description = "结果文件路径", example = "/tmp/log-exports/123.csv")
    private String resultFilePath;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}