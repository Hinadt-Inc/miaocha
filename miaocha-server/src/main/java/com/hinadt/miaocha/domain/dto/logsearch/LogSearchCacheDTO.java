package com.hinadt.miaocha.domain.dto.logsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 日志搜索条件缓存DTO
 *
 * <p>扩展自LogSearchDTO，增加了名称和描述字段，用于用户个性化的日志搜索条件缓存
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "日志搜索条件缓存对象")
public class LogSearchCacheDTO extends LogSearchDTO {

    @Schema(
            description = "搜索缓存条件名称",
            example = "错误日志查询",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "搜索条件名称不能为空")
    @Size(max = 100, message = "搜索条件名称长度不能超过100个字符")
    private String name;

    @Schema(description = "搜索条件描述", example = "查询最近24小时内的错误级别日志")
    @Size(max = 500, message = "搜索条件描述长度不能超过500个字符")
    private String description;
}
