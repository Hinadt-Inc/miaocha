package com.hinadt.miaocha.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.application.service.SystemCacheService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.util.UserContextUtil;
import com.hinadt.miaocha.domain.dto.cache.SystemCacheDTO;
import com.hinadt.miaocha.domain.entity.SystemCacheConfig;
import com.hinadt.miaocha.domain.enums.CacheGroup;
import com.hinadt.miaocha.infrastructure.mapper.SystemCacheConfigMapper;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 系统缓存服务实现类
 *
 * <p>提供系统缓存配置的管理功能，支持对象序列化/反序列化
 *
 * <p>用户信息通过 UserAuditInterceptor 和 UserContextUtil 自动处理
 */
@Slf4j
@Service
public class SystemCacheServiceImpl implements SystemCacheService {

    private static final String LOG_CACHE_OPERATION = "缓存操作: group={}, key={}, operation={}";
    private static final String LOG_USER_CACHE_OPERATION =
            "用户缓存操作: group={}, user={}, operation={}, count={}";

    private final SystemCacheConfigMapper systemCacheConfigMapper;
    private final ObjectMapper objectMapper;

    public SystemCacheServiceImpl(
            SystemCacheConfigMapper systemCacheConfigMapper, ObjectMapper objectMapper) {
        this.systemCacheConfigMapper = systemCacheConfigMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public <T> SystemCacheConfig saveCache(CacheGroup cacheGroup, String cacheKey, T data) {
        validateSaveParams(cacheGroup, cacheKey, data);

        String jsonContent = serializeToJson(data);
        String groupName = cacheGroup.name();

        return findExistingCache(groupName, cacheKey)
                .map(existing -> updateExistingCache(existing, jsonContent, cacheGroup, cacheKey))
                .orElseGet(() -> createAndInsertNewCache(cacheGroup, cacheKey, jsonContent));
    }

    @Override
    public <T> Optional<T> getCache(CacheGroup cacheGroup, String cacheKey) {
        validateGetParams(cacheGroup, cacheKey);

        return findExistingCache(cacheGroup.name(), cacheKey)
                .filter(cache -> StringUtils.hasText(cache.getContent()))
                .map(
                        cache -> {
                            @SuppressWarnings("unchecked")
                            Class<T> dataType = (Class<T>) cacheGroup.getDataType();
                            T data = deserializeFromJson(cache.getContent(), dataType);
                            log.debug(LOG_CACHE_OPERATION, cacheGroup.getName(), cacheKey, "获取");
                            return data;
                        })
                .or(
                        () -> {
                            log.debug(LOG_CACHE_OPERATION, cacheGroup.getName(), cacheKey, "未找到");
                            return Optional.empty();
                        });
    }

    @Override
    @Transactional
    public boolean deleteCache(CacheGroup cacheGroup, String cacheKey) {
        validateDeleteParams(cacheGroup, cacheKey);

        int deletedRows = systemCacheConfigMapper.deleteByGroupAndKey(cacheGroup.name(), cacheKey);
        boolean deleted = deletedRows > 0;

        String operation = deleted ? "删除" : "未找到";
        log.debug(LOG_CACHE_OPERATION, cacheGroup.getName(), cacheKey, operation);

        return deleted;
    }

    @Override
    public <T> List<SystemCacheDTO<T>> getUserCacheData(CacheGroup cacheGroup) {
        validateCacheGroup(cacheGroup);

        return getCurrentUserEmail()
                .map(
                        userEmail -> {
                            List<SystemCacheConfig> userCaches =
                                    systemCacheConfigMapper.selectByGroupAndCreateUser(
                                            cacheGroup.name(), userEmail);

                            // 转换为SystemCacheDTO并进行类型转换
                            @SuppressWarnings("unchecked")
                            Class<T> dataType = (Class<T>) cacheGroup.getDataType();

                            List<SystemCacheDTO<T>> result =
                                    userCaches.stream()
                                            .map(cache -> convertToSystemCacheDTO(cache, dataType))
                                            .toList();

                            log.debug(
                                    LOG_USER_CACHE_OPERATION,
                                    cacheGroup.getName(),
                                    userEmail,
                                    "获取数据",
                                    result.size());
                            return result;
                        })
                .orElseGet(
                        () -> {
                            log.warn("无法获取当前用户信息，返回空列表");
                            return Collections.emptyList();
                        });
    }

    @Override
    @Transactional
    public int batchDeleteCache(CacheGroup cacheGroup, List<String> cacheKeys) {
        validateBatchDeleteParams(cacheGroup, cacheKeys);

        int deletedRows =
                systemCacheConfigMapper.batchDeleteByGroupAndKeys(cacheGroup.name(), cacheKeys);

        log.debug(
                "批量删除缓存: group={}, keys={}, deleted={}",
                cacheGroup.getName(),
                cacheKeys.size(),
                deletedRows);

        return deletedRows;
    }

    // ==================== 私有辅助方法 ====================

    /** 序列化对象为JSON字符串 */
    private <T> String serializeToJson(T data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("对象序列化失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "缓存数据序列化失败: " + e.getMessage());
        }
    }

    /** 反序列化JSON字符串为对象 */
    private <T> T deserializeFromJson(String jsonContent, Class<T> clazz) {
        try {
            return objectMapper.readValue(jsonContent, clazz);
        } catch (JsonProcessingException e) {
            log.error(
                    "JSON反序列化失败: content={}, targetClass={}, error={}",
                    jsonContent,
                    clazz.getSimpleName(),
                    e.getMessage());
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "缓存数据反序列化失败: " + e.getMessage());
        }
    }

    /** 查找现有缓存配置 */
    private Optional<SystemCacheConfig> findExistingCache(String groupName, String cacheKey) {
        SystemCacheConfig existing =
                systemCacheConfigMapper.selectByGroupAndKey(groupName, cacheKey);
        return Optional.ofNullable(existing);
    }

    /** 更新现有缓存配置 */
    private SystemCacheConfig updateExistingCache(
            SystemCacheConfig existing,
            String jsonContent,
            CacheGroup cacheGroup,
            String cacheKey) {
        existing.setContent(jsonContent);
        systemCacheConfigMapper.updateContent(existing.getId(), jsonContent);
        log.debug(LOG_CACHE_OPERATION, cacheGroup.getName(), cacheKey, "更新");
        return existing;
    }

    /** 创建并插入新缓存配置 */
    private SystemCacheConfig createAndInsertNewCache(
            CacheGroup cacheGroup, String cacheKey, String jsonContent) {
        SystemCacheConfig newCache = createNewCache(cacheGroup, cacheKey, jsonContent);
        systemCacheConfigMapper.insert(newCache);
        log.debug(LOG_CACHE_OPERATION, cacheGroup.getName(), cacheKey, "创建");
        return newCache;
    }

    /** 获取当前用户邮箱 */
    private Optional<String> getCurrentUserEmail() {
        String userEmail = UserContextUtil.getCurrentUserEmail();
        return StringUtils.hasText(userEmail) ? Optional.of(userEmail) : Optional.empty();
    }

    /** 创建新的缓存配置实体 */
    private SystemCacheConfig createNewCache(
            CacheGroup cacheGroup, String cacheKey, String jsonContent) {
        SystemCacheConfig cache = new SystemCacheConfig();
        cache.setCacheGroup(cacheGroup.name());
        cache.setCacheKey(cacheKey);
        cache.setContent(jsonContent);
        return cache;
    }

    // ==================== 参数验证方法 ====================

    /** 验证保存缓存的参数 */
    private <T> void validateSaveParams(CacheGroup cacheGroup, String cacheKey, T data) {
        validateCacheGroup(cacheGroup);
        validateCacheKey(cacheKey);
        validateNotNull(data, "缓存数据不能为空");
        validateDataType(cacheGroup, data);
    }

    /** 验证获取缓存的参数 */
    private <T> void validateGetParams(CacheGroup cacheGroup, String cacheKey) {
        validateCacheGroup(cacheGroup);
        validateCacheKey(cacheKey);
    }

    /** 验证删除缓存的参数 */
    private void validateDeleteParams(CacheGroup cacheGroup, String cacheKey) {
        validateCacheGroup(cacheGroup);
        validateCacheKey(cacheKey);
    }

    /** 验证缓存组 */
    private void validateCacheGroup(CacheGroup cacheGroup) {
        validateNotNull(cacheGroup, "缓存组不能为空");
    }

    /** 验证缓存键 */
    private void validateCacheKey(String cacheKey) {
        if (!StringUtils.hasText(cacheKey)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "缓存键不能为空");
        }
    }

    /** 验证对象不为空 */
    private void validateNotNull(Object obj, String message) {
        if (obj == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
    }

    /** 验证数据类型是否与缓存组定义的类型匹配 */
    private <T> void validateDataType(CacheGroup cacheGroup, T data) {
        Class<?> expectedType = cacheGroup.getDataType();
        Class<?> actualType = data.getClass();

        if (!expectedType.isAssignableFrom(actualType)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    String.format(
                            "缓存数据类型不匹配，期望类型: %s，实际类型: %s",
                            expectedType.getSimpleName(), actualType.getSimpleName()));
        }
    }

    /** 转换SystemCacheConfig为SystemCacheDTO */
    private <T> SystemCacheDTO<T> convertToSystemCacheDTO(
            SystemCacheConfig cache, Class<T> dataType) {
        SystemCacheDTO<T> dto = new SystemCacheDTO<>();
        dto.setId(cache.getId());
        dto.setCacheGroup(cache.getCacheGroup());
        dto.setCacheKey(cache.getCacheKey());
        dto.setCreateTime(cache.getCreateTime());
        dto.setCreateUser(cache.getCreateUser());

        // 反序列化content字段为指定类型
        if (StringUtils.hasText(cache.getContent())) {
            T data = deserializeFromJson(cache.getContent(), dataType);
            dto.setData(data);
        }

        return dto;
    }

    /** 验证批量删除缓存的参数 */
    private void validateBatchDeleteParams(CacheGroup cacheGroup, List<String> cacheKeys) {
        validateCacheGroup(cacheGroup);
        validateNotNull(cacheKeys, "缓存键列表不能为空");
        if (cacheKeys.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "缓存键列表不能为空");
        }
        if (cacheKeys.size() > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "单次批量删除的缓存键数量不能超过100个");
        }
    }
}
