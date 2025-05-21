package com.hina.log.dto.logstash;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Logstash配置更新DTO
 */
@Data
@Schema(description = "Logstash配置更新DTO")
public class LogstashConfigUpdateDTO {

    @NotBlank(message = "配置内容不能为空")
    @Schema(description = "Logstash配置文件内容", example = "input { ... } filter { ... } output { ... }")
    private String configContent;

    @Schema(description = "Doris表名, 选择性输入", example = "log_table_test_env")
    private String tableName;
}