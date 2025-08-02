package com.hinadt.miaocha.domain.dto.cache;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 系统缓存DTO
 *
 * <p>用于返回系统缓存配置信息，content字段根据CacheGroup的类型信息进行类型转换
 */
@Data
@Schema(description = "系统缓存配置对象")
public class SystemCacheDTO<T> {

    @Schema(description = "缓存ID", example = "1")
    private Long id;

    @Schema(description = "缓存组", example = "LOG_SEARCH_CONDITION")
    private String cacheGroup;

    @Schema(description = "缓存键", example = "search_condition_20241201_001")
    private String cacheKey;

    @Schema(description = "缓存数据", example = "反序列化后的具体对象")
    private T data;

    @Schema(description = "创建时间", example = "2024-12-01T10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "创建用户", example = "user@example.com")
    private String createUser;
}
