package com.hinadt.miaocha.domain.dto.logsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 日志查询结果基类
 *
 * <p>所有日志查询结果DTO的公共基类，包含成功状态、错误信息和执行时间
 */
@Data
@Schema(description = "日志查询结果基类")
public abstract class LogSearchResultDTO {

    @Schema(description = "查询耗时（毫秒）", example = "123")
    private Long executionTimeMs;
}
