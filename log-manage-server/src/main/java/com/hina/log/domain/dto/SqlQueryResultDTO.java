package com.hina.log.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** SQL查询结果DTO */
@Data
@Schema(description = "SQL查询结果对象")
public class SqlQueryResultDTO {
    @Schema(description = "列名列表")
    private List<String> columns;

    @Schema(description = "数据行列表，每行是一个键值对，键为列名，值为单元格数据")
    private List<Map<String, Object>> rows;

    @Schema(description = "查询执行时间(毫秒)")
    private Long executionTimeMs;

    @Schema(description = "影响的行数(仅适用于更新操作)")
    private Integer affectedRows;

    @Schema(description = "文件下载URL")
    private String downloadUrl;
}
