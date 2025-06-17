package com.hinadt.miaocha.domain.dto.logstash;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 更新Logstash进程元信息DTO */
@Data
@Schema(description = "更新Logstash进程元信息对象")
public class LogstashProcessUpdateDTO {

    @Schema(description = "进程名称", example = "Nginx日志收集")
    @NotBlank(message = "进程名称不能为空")
    private String name;

    @Schema(description = "关联的模块ID", example = "1")
    @NotNull(message = "模块ID不能为空") private Long moduleId;
}
