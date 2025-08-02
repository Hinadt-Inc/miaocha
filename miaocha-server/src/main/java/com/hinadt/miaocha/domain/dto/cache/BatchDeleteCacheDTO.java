package com.hinadt.miaocha.domain.dto.cache;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * 批量删除缓存请求DTO
 *
 * <p>用于批量删除指定缓存组下的多个缓存键
 */
@Data
@Schema(description = "批量删除缓存请求对象")
public class BatchDeleteCacheDTO {

    @Schema(description = "缓存组,可以不设置,非必须")
    private String cacheGroup;

    @Schema(
            description = "缓存键列表",
            example = "[\"key1\", \"key2\", \"key3\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "缓存键列表不能为空")
    @Size(max = 100, message = "单次批量删除的缓存键数量不能超过100个")
    private List<String> cacheKeys;
}
