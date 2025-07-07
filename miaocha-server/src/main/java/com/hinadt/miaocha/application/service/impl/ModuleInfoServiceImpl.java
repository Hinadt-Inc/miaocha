package com.hinadt.miaocha.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.application.service.ModuleInfoService;
import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.converter.ModuleInfoConverter;
import com.hinadt.miaocha.domain.converter.ModulePermissionConverter;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoCreateDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoUpdateDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoWithPermissionsDTO;
import com.hinadt.miaocha.domain.dto.module.QueryConfigDTO;
import com.hinadt.miaocha.domain.dto.permission.ModuleUsersPermissionDTO.UserPermissionInfoDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.entity.UserModulePermission;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.LogstashProcessMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import com.hinadt.miaocha.domain.mapper.UserModulePermissionMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 模块信息服务实现类 */
@Service
@Slf4j
public class ModuleInfoServiceImpl implements ModuleInfoService {

    private final ModuleInfoMapper moduleInfoMapper;
    private final DatasourceMapper datasourceMapper;
    private final JdbcQueryExecutor jdbcQueryExecutor;
    private final ModuleInfoConverter moduleInfoConverter;
    private final LogstashProcessMapper logstashProcessMapper;
    private final UserModulePermissionMapper userModulePermissionMapper;
    private final UserMapper userMapper;
    private final ModulePermissionConverter modulePermissionConverter;
    private final TableValidationService tableValidationService;
    private final ObjectMapper objectMapper;

    public ModuleInfoServiceImpl(
            ModuleInfoMapper moduleInfoMapper,
            DatasourceMapper datasourceMapper,
            JdbcQueryExecutor jdbcQueryExecutor,
            ModuleInfoConverter moduleInfoConverter,
            LogstashProcessMapper logstashProcessMapper,
            UserModulePermissionMapper userModulePermissionMapper,
            UserMapper userMapper,
            ModulePermissionConverter modulePermissionConverter,
            TableValidationService tableValidationService,
            ObjectMapper objectMapper) {
        this.moduleInfoMapper = moduleInfoMapper;
        this.datasourceMapper = datasourceMapper;
        this.jdbcQueryExecutor = jdbcQueryExecutor;
        this.moduleInfoConverter = moduleInfoConverter;
        this.logstashProcessMapper = logstashProcessMapper;
        this.userModulePermissionMapper = userModulePermissionMapper;
        this.userMapper = userMapper;
        this.modulePermissionConverter = modulePermissionConverter;
        this.tableValidationService = tableValidationService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ModuleInfoDTO createModule(ModuleInfoCreateDTO request) {
        // 验证模块名称唯一性
        validateModuleNameUnique(request.getName(), null);

        // 获取数据源信息
        DatasourceInfo datasourceInfo = getDatasourceOrThrow(request.getDatasourceId());

        // 创建并保存模块
        ModuleInfo moduleInfo = moduleInfoConverter.toEntity(request);
        insertModuleOrThrow(moduleInfo);

        return moduleInfoConverter.toDto(moduleInfo, datasourceInfo);
    }

    @Override
    @Transactional
    public ModuleInfoDTO updateModule(ModuleInfoUpdateDTO request) {
        // 获取现有模块
        ModuleInfo existingModule = getModuleOrThrow(request.getId());

        // 验证模块名称唯一性
        validateModuleNameUnique(request.getName(), request.getId());

        // 获取数据源信息
        DatasourceInfo datasourceInfo = getDatasourceOrThrow(request.getDatasourceId());

        // 更新模块
        ModuleInfo moduleInfo = moduleInfoConverter.updateEntity(existingModule, request);
        updateModuleOrThrow(moduleInfo);

        // 重新查询获取最新数据
        ModuleInfo updatedModule = getModuleOrThrow(request.getId());
        return moduleInfoConverter.toDto(updatedModule, datasourceInfo);
    }

    @Override
    @Transactional
    public void deleteModule(Long id, Boolean deleteDorisTable) {
        ModuleInfo moduleInfo = getModuleOrThrow(id);

        // 检查模块使用情况
        validateModuleNotInUse(id);

        // 删除模块
        deleteModuleInner(moduleInfo);

        // 根据参数决定是否删除Doris表
        if (Boolean.TRUE.equals(deleteDorisTable) && canDeleteDorisTable(moduleInfo)) {
            deleteDorisTable(moduleInfo);
        }
    }

    @Override
    public ModuleInfoDTO getModuleById(Long id) {
        ModuleInfo moduleInfo = getModuleOrThrow(id);
        return convertToModuleDTO(moduleInfo);
    }

    @Override
    public List<ModuleInfoDTO> getAllModules() {
        List<ModuleInfo> moduleInfos = moduleInfoMapper.selectAll();
        return moduleInfos.stream().map(this::convertToModuleDTO).collect(Collectors.toList());
    }

    @Override
    public List<ModuleInfoWithPermissionsDTO> getAllModulesWithPermissions() {
        List<ModuleInfo> moduleInfos = moduleInfoMapper.selectAll();
        if (moduleInfos.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建数据源映射
        Map<Long, DatasourceInfo> datasourceMap = buildDatasourceMap(moduleInfos);

        // 构建权限映射
        Map<String, List<UserPermissionInfoDTO>> permissionMap = buildPermissionMap();

        // 转换为DTO
        return moduleInfos.stream()
                .map(
                        moduleInfo -> {
                            DatasourceInfo datasourceInfo =
                                    datasourceMap.get(moduleInfo.getDatasourceId());
                            List<UserPermissionInfoDTO> users =
                                    permissionMap.getOrDefault(
                                            moduleInfo.getName(), new ArrayList<>());
                            return moduleInfoConverter.toWithPermissionsDto(
                                    moduleInfo, datasourceInfo, users);
                        })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ModuleInfoDTO executeDorisSql(Long id, String sql) {
        ModuleInfo moduleInfo = getModuleOrThrow(id);

        // 验证SQL
        validateSqlForExecution(moduleInfo, sql);

        // 获取数据源并执行SQL
        DatasourceInfo datasourceInfo = getDatasourceOrThrow(moduleInfo.getDatasourceId());
        executeSqlSafely(datasourceInfo, sql);

        // 更新模块SQL字段
        moduleInfo.setDorisSql(sql);
        updateModuleOrThrow(moduleInfo);

        return moduleInfoConverter.toDto(moduleInfo, datasourceInfo);
    }

    @Override
    @Transactional
    public ModuleInfoDTO configureQueryConfig(Long moduleId, QueryConfigDTO queryConfig) {
        ModuleInfo moduleInfo = getModuleOrThrow(moduleId);

        // 验证表和配置
        validateTableReady(moduleInfo);
        validateQueryConfig(moduleInfo, queryConfig);

        // 更新配置
        String queryConfigJson = serializeQueryConfig(queryConfig);
        moduleInfo.setQueryConfig(queryConfigJson);
        updateModuleOrThrow(moduleInfo);

        // 返回结果
        DatasourceInfo datasourceInfo = getDatasourceOrThrow(moduleInfo.getDatasourceId());
        return moduleInfoConverter.toDto(moduleInfo, datasourceInfo);
    }

    @Override
    public String getTableNameByModule(String module) {
        validateModuleName(module);

        ModuleInfo moduleInfo = getModuleByName(module);
        String tableName = moduleInfo.getTableName();

        if (!StringUtils.hasText(tableName)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块 " + module + " 对应的表名未配置");
        }

        return tableName;
    }

    @Override
    public QueryConfigDTO getQueryConfigByModule(String module) {
        if (!StringUtils.hasText(module)) {
            return null;
        }

        ModuleInfo moduleInfo = moduleInfoMapper.selectByName(module);
        if (moduleInfo == null) {
            return null;
        }

        return parseQueryConfig(moduleInfo.getQueryConfig(), module);
    }

    private void validateModuleName(String module) {
        if (!StringUtils.hasText(module)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块名称不能为空");
        }
    }

    private void validateModuleNameUnique(String name, Long excludeId) {
        if (moduleInfoMapper.existsByName(name, excludeId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块名称已存在");
        }
    }

    private void validateModuleNotInUse(Long moduleId) {
        int processCount = logstashProcessMapper.countByModuleId(moduleId);
        if (processCount > 0) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "该模块正在被 " + processCount + " 个进程使用，无法删除");
        }
    }

    private void validateSqlForExecution(ModuleInfo moduleInfo, String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SQL语句不能为空");
        }

        if (StringUtils.hasText(moduleInfo.getDorisSql())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "该模块已经执行过Doris SQL，不能重复执行");
        }

        tableValidationService.validateDorisSql(moduleInfo, sql);
    }

    private void validateTableReady(ModuleInfo moduleInfo) {
        if (!StringUtils.hasText(moduleInfo.getDorisSql())
                && !tableValidationService.isTableExists(moduleInfo)) {
            throw new BusinessException(ErrorCode.MODULE_DORIS_SQL_NOT_CONFIGURED);
        }
    }

    private void validateQueryConfig(ModuleInfo moduleInfo, QueryConfigDTO queryConfig) {
        validateExcludeFieldsNotContainTimeField(queryConfig);
        validateConfiguredFieldsExist(moduleInfo, queryConfig);
    }

    private void validateExcludeFieldsNotContainTimeField(QueryConfigDTO queryConfig) {
        if (queryConfig.getExcludeFields() != null
                && !queryConfig.getExcludeFields().isEmpty()
                && StringUtils.hasText(queryConfig.getTimeField())
                && queryConfig.getExcludeFields().contains(queryConfig.getTimeField())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "排除字段列表不能包含时间字段");
        }
    }

    private void validateConfiguredFieldsExist(ModuleInfo moduleInfo, QueryConfigDTO queryConfig) {
        List<String> configuredFields = collectConfiguredFields(queryConfig);
        if (!configuredFields.isEmpty()) {
            tableValidationService.validateQueryConfigFields(moduleInfo, configuredFields);
        }
    }

    private ModuleInfo getModuleOrThrow(Long id) {
        ModuleInfo moduleInfo = moduleInfoMapper.selectById(id);
        if (moduleInfo == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块不存在");
        }
        return moduleInfo;
    }

    private ModuleInfo getModuleByName(String module) {
        ModuleInfo moduleInfo = moduleInfoMapper.selectByName(module);
        if (moduleInfo == null) {
            throw new BusinessException(ErrorCode.MODULE_NOT_FOUND, "未找到模块: " + module);
        }
        return moduleInfo;
    }

    private DatasourceInfo getDatasourceOrThrow(Long datasourceId) {
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(datasourceId);
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }
        return datasourceInfo;
    }

    private void insertModuleOrThrow(ModuleInfo moduleInfo) {
        int result = moduleInfoMapper.insert(moduleInfo);
        if (result == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建模块失败");
        }
    }

    private void updateModuleOrThrow(ModuleInfo moduleInfo) {
        int result = moduleInfoMapper.update(moduleInfo);
        if (result == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "更新模块失败");
        }
    }

    private void deleteModuleInner(ModuleInfo moduleInfo) {
        int result = moduleInfoMapper.deleteById(moduleInfo.getId());
        if (result == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "删除模块失败");
        }

        userModulePermissionMapper.deleteByModuleName(moduleInfo.getName());
    }

    private void executeSqlSafely(DatasourceInfo datasourceInfo, String sql) {
        try {
            jdbcQueryExecutor.executeQuery(datasourceInfo, sql);
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.SQL_EXECUTION_FAILED, "SQL执行失败: " + e.getMessage(), e);
        }
    }

    private boolean canDeleteDorisTable(ModuleInfo moduleInfo) {
        return StringUtils.hasText(moduleInfo.getTableName())
                && StringUtils.hasText(moduleInfo.getDorisSql());
    }

    private void deleteDorisTable(ModuleInfo moduleInfo) {
        DatasourceInfo datasourceInfo = getDatasourceOrThrow(moduleInfo.getDatasourceId());
        String tableName = moduleInfo.getTableName();

        // 清空表数据
        executeSqlIgnoreError(datasourceInfo, "TRUNCATE TABLE " + tableName, "清空表数据");

        // 删除表
        executeSqlIgnoreError(datasourceInfo, "DROP TABLE IF EXISTS " + tableName, "删除表");
    }

    private void executeSqlIgnoreError(
            DatasourceInfo datasourceInfo, String sql, String operation) {
        try {
            jdbcQueryExecutor.executeQuery(datasourceInfo, sql);
        } catch (Exception e) {
            log.error("{}失败: {}, 错误: {}", operation, sql, e.getMessage());
        }
    }

    private ModuleInfoDTO convertToModuleDTO(ModuleInfo moduleInfo) {
        DatasourceInfo datasourceInfo = getDatasourceOrThrow(moduleInfo.getDatasourceId());
        return moduleInfoConverter.toDto(moduleInfo, datasourceInfo);
    }

    private Map<Long, DatasourceInfo> buildDatasourceMap(List<ModuleInfo> moduleInfos) {
        return moduleInfos.stream()
                .map(ModuleInfo::getDatasourceId)
                .distinct()
                .collect(Collectors.toMap(id -> id, this::getDatasourceOrThrow));
    }

    private Map<String, List<UserPermissionInfoDTO>> buildPermissionMap() {
        List<UserModulePermission> allPermissions = userModulePermissionMapper.selectAll();
        if (allPermissions.isEmpty()) {
            return new HashMap<>();
        }

        // 构建用户信息映射
        Map<Long, User> userMap = buildUserMap(allPermissions);

        // 按模块分组权限并转换为DTO
        return allPermissions.stream()
                .collect(
                        Collectors.groupingBy(
                                UserModulePermission::getModule,
                                Collectors.mapping(
                                        permission ->
                                                modulePermissionConverter
                                                        .createUserPermissionInfoDTO(
                                                                permission,
                                                                userMap.get(
                                                                        permission.getUserId())),
                                        Collectors.toList())));
    }

    private Map<Long, User> buildUserMap(List<UserModulePermission> allPermissions) {
        List<Long> userIds =
                allPermissions.stream()
                        .map(UserModulePermission::getUserId)
                        .distinct()
                        .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        return userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    private List<String> collectConfiguredFields(QueryConfigDTO queryConfig) {
        List<String> configuredFields = new ArrayList<>();

        // 收集关键词字段
        if (queryConfig.getKeywordFields() != null) {
            configuredFields.addAll(
                    queryConfig.getKeywordFields().stream()
                            .map(QueryConfigDTO.KeywordFieldConfigDTO::getFieldName)
                            .toList());
        }

        // 收集时间字段
        if (StringUtils.hasText(queryConfig.getTimeField())) {
            configuredFields.add(queryConfig.getTimeField());
        }

        // 收集排除字段
        if (queryConfig.getExcludeFields() != null && !queryConfig.getExcludeFields().isEmpty()) {
            configuredFields.addAll(queryConfig.getExcludeFields());
        }

        return configuredFields;
    }

    private String serializeQueryConfig(QueryConfigDTO queryConfig) {
        try {
            return objectMapper.writeValueAsString(queryConfig);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "查询配置序列化失败: " + e.getMessage());
        }
    }

    private QueryConfigDTO parseQueryConfig(String queryConfigJson, String module) {
        if (!StringUtils.hasText(queryConfigJson)) {
            return null;
        }

        try {
            return objectMapper.readValue(queryConfigJson, QueryConfigDTO.class);
        } catch (JsonProcessingException e) {
            log.warn("解析模块 {} 的查询配置JSON失败: {}", module, e.getMessage());
            return null;
        }
    }
}
