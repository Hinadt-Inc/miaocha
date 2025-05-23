package com.hina.log.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * SQL查询历史分页响应DTO
 */
@Data
@Schema(description = "SQL查询历史分页响应对象")
public class SqlHistoryResponseDTO {

    @Schema(description = "当前页码", example = "1")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "总页数", example = "10")
    private Integer pages;

    @Schema(description = "查询历史记录列表")
    private List<SqlHistoryItemDTO> records;

    /**
     * SQL查询历史记录项
     */
    @Data
    @Schema(description = "SQL查询历史记录项")
    public static class SqlHistoryItemDTO {

        @Schema(description = "查询历史ID", example = "1")
        private Long id;

        @Schema(description = "用户ID", example = "1")
        private Long userId;

        @Schema(description = "用户邮箱", example = "user@example.com")
        private String userEmail;

        @Schema(description = "数据源ID", example = "1")
        private Long datasourceId;

        @Schema(description = "表名", example = "users")
        private String tableName;

        @Schema(description = "SQL查询语句", example = "SELECT * FROM users LIMIT 10")
        private String sqlQuery;

        @Schema(description = "是否有结果文件", example = "true")
        private Boolean hasResultFile;

        @Schema(description = "结果文件下载链接", example = "/api/sql/result/1")
        private String downloadUrl;

        @Schema(description = "创建时间", example = "2023-06-01 10:00:00")
        private String createTime;
    }
}