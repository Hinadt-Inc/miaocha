package com.hinadt.miaocha.domain.dto.logstash;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 日志尾部跟踪请求DTO 用于启动对指定Logstash实例的日志实时跟踪 */
@Data
@Schema(description = "日志尾部跟踪请求参数")
public class LogTailRequestDTO {

    @Schema(description = "LogstashMachine实例ID", example = "1", required = true)
    @NotNull(message = "LogstashMachine实例ID不能为空") @Positive(message = "LogstashMachine实例ID必须大于0") private Long logstashMachineId;

    @Schema(description = "从末尾开始读取的行数", example = "100", defaultValue = "100")
    private Integer tailLines = 100;
}
