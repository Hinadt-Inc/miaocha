package com.hinadt.miaocha.domain.dto.logstash;

import com.hinadt.miaocha.domain.entity.enums.LogTailResponseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 日志尾部跟踪响应DTO 包含批量的日志数据，用于减少网络传输频率 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "日志尾部跟踪响应数据")
public class LogTailResponseDTO {

    @Schema(description = "LogstashMachine实例ID")
    private Long logstashMachineId;

    @Schema(description = "日志内容列表（批量发送）")
    private List<String> logLines;

    @Schema(description = "数据生成时间")
    private LocalDateTime timestamp;

    @Schema(description = "连接状态")
    private LogTailResponseStatus status;

    @Schema(description = "错误信息（如果有）")
    private String errorMessage;
}
