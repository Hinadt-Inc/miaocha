package com.hina.log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Logstash配置更新DTO
 */
@Data
@Schema(description = "Logstash配置更新DTO")
public class LogstashConfigUpdateDTO {

    @NotBlank(message = "配置JSON不能为空")
    @Schema(description = "Logstash配置JSON", example = "{ \"input\": { ... }, \"filter\": { ... }, \"output\": { ... } }")
    private String configJson;
}