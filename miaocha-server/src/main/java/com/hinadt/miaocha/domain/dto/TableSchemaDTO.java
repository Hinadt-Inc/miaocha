package com.hinadt.miaocha.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/** 表字段信息DTO */
@Data
@Schema(description = "表字段信息对象")
public class TableSchemaDTO {
    @Schema(description = "数据库名称")
    private String databaseName;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "表注释")
    private String tableComment;

    @Schema(description = "字段列表")
    private List<ColumnInfoDTO> columns;

    /** 字段信息DTO */
    @Data
    @Schema(description = "字段信息对象")
    public static class ColumnInfoDTO {
        @Schema(description = "字段名")
        private String columnName;

        @Schema(description = "字段类型")
        private String dataType;

        @Schema(description = "是否主键")
        private Boolean isPrimaryKey;

        @Schema(description = "是否可为空")
        private Boolean isNullable;
    }
}
