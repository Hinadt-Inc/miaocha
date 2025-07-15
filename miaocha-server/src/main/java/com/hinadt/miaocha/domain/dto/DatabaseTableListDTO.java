package com.hinadt.miaocha.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/** 数据库表列表DTO */
@Data
@Schema(description = "数据库表列表对象")
public class DatabaseTableListDTO {
    @Schema(description = "数据库名称")
    private String databaseName;

    @Schema(description = "表列表")
    private List<TableBasicInfoDTO> tables;

    /** 表基本信息DTO */
    @Data
    @Schema(description = "表基本信息对象")
    public static class TableBasicInfoDTO {
        @Schema(description = "表名")
        private String tableName;

        @Schema(description = "表注释")
        private String tableComment;
    }
}
