package com.hina.log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 数据库结构信息DTO
 */
@Data
@Schema(description = "数据库结构信息对象")
public class SchemaInfoDTO {
    @Schema(description = "数据库名称")
    private String databaseName;

    @Schema(description = "表列表")
    private List<TableInfoDTO> tables;

    /**
     * 表信息DTO
     */
    @Data
    @Schema(description = "表信息对象")
    public static class TableInfoDTO {
        @Schema(description = "表名")
        private String tableName;

        @Schema(description = "表注释")
        private String tableComment;

        @Schema(description = "字段列表")
        private List<ColumnInfoDTO> columns;
    }

    /**
     * 字段信息DTO
     */
    @Data
    @Schema(description = "字段信息对象")
    public static class ColumnInfoDTO {
        @Schema(description = "字段名")
        private String columnName;

        @Schema(description = "字段类型")
        private String dataType;

        @Schema(description = "字段注释")
        private String columnComment;

        @Schema(description = "是否主键")
        private Boolean isPrimaryKey;

        @Schema(description = "是否可为空")
        private Boolean isNullable;
    }
}