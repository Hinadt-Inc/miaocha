package com.hinadt.miaocha.domain.dto.logstash;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "Logstash实例批量操作请求DTO")
public class LogstashInstanceBatchOperationRequestDTO {

    @NotEmpty(message = "实例ID列表不能为空")
    @Schema(description = "要操作的LogstashMachine实例ID列表", required = true)
    private List<Long> instanceIds;
}
