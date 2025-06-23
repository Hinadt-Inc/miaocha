package com.hinadt.miaocha.application.service.impl;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 模块信息服务实现类 */
@Service
@Slf4j
public class ModuleInfoServiceImpl implements ModuleInfoService {

    @Autowired private ModuleInfoMapper moduleInfoMapper;

    @Autowired private DatasourceMapper datasourceMapper;

    @Autowired private JdbcQueryExecutor jdbcQueryExecutor;

    @Autowired private ModuleInfoConverter moduleInfoConverter;

    @Autowired private LogstashProcessMapper logstashProcessMapper;

    @Autowired private UserModulePermissionMapper userModulePermissionMapper;

    @Autowired private UserMapper userMapper;

    @Autowired private ModulePermissionConverter modulePermissionConverter;

    @Autowired private TableValidationService tableValidationService;

    @Override
    @Transactional
    public ModuleInfoDTO createModule(ModuleInfoCreateDTO request) {
        // 检查模块名称是否已存在
        if (moduleInfoMapper.existsByName(request.getName(), null)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块名称已存在");
        }

        // 检查数据源是否存在
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(request.getDatasourceId());
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 创建模块实体 - 审计字段由MyBatis拦截器自动设置
        ModuleInfo moduleInfo = moduleInfoConverter.toEntity(request);

        // 插入数据库
        int result = moduleInfoMapper.insert(moduleInfo);
        if (result == 0) {
            throw new RuntimeException("创建模块失败");
        }

        // 返回响应DTO
        return moduleInfoConverter.toDto(moduleInfo, datasourceInfo);
    }

    @Override
    @Transactional
    public ModuleInfoDTO updateModule(ModuleInfoUpdateDTO request) {
        // 检查模块是否存在
        ModuleInfo existingModule = moduleInfoMapper.selectById(request.getId());
        if (existingModule == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块不存在");
        }

        // 检查模块名称是否与其他模块重复
        if (moduleInfoMapper.existsByName(request.getName(), request.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块名称已存在");
        }

        // 检查数据源是否存在
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(request.getDatasourceId());
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 更新模块实体 - 审计字段由MyBatis拦截器自动设置
        ModuleInfo moduleInfo = moduleInfoConverter.updateEntity(existingModule, request);

        int result = moduleInfoMapper.update(moduleInfo);
        if (result == 0) {
            throw new RuntimeException("更新模块失败");
        }

        // 重新查询以获取更新后的数据
        ModuleInfo updatedModule = moduleInfoMapper.selectById(request.getId());
        return moduleInfoConverter.toDto(updatedModule, datasourceInfo);
    }

    @Override
    public ModuleInfoDTO getModuleById(Long id) {
        ModuleInfo moduleInfo = moduleInfoMapper.selectById(id);
        if (moduleInfo == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块不存在");
        }

        DatasourceInfo datasourceInfo = datasourceMapper.selectById(moduleInfo.getDatasourceId());
        return moduleInfoConverter.toDto(moduleInfo, datasourceInfo);
    }

    @Override
    public List<ModuleInfoDTO> getAllModules() {
        List<ModuleInfo> moduleInfos = moduleInfoMapper.selectAll();
        return moduleInfos.stream()
                .map(
                        moduleInfo -> {
                            DatasourceInfo datasourceInfo =
                                    datasourceMapper.selectById(moduleInfo.getDatasourceId());
                            return moduleInfoConverter.toDto(moduleInfo, datasourceInfo);
                        })
                .collect(Collectors.toList());
    }

    @Override
    public List<ModuleInfoWithPermissionsDTO> getAllModulesWithPermissions() {
        // 获取所有模块
        List<ModuleInfo> moduleInfos = moduleInfoMapper.selectAll();
        if (moduleInfos.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有数据源信息
        Map<Long, DatasourceInfo> datasourceMap = new HashMap<>();
        for (ModuleInfo moduleInfo : moduleInfos) {
            if (!datasourceMap.containsKey(moduleInfo.getDatasourceId())) {
                DatasourceInfo datasourceInfo =
                        datasourceMapper.selectById(moduleInfo.getDatasourceId());
                if (datasourceInfo != null) {
                    datasourceMap.put(moduleInfo.getDatasourceId(), datasourceInfo);
                }
            }
        }

        // 获取所有用户模块权限
        List<UserModulePermission> allPermissions = userModulePermissionMapper.selectAll();

        // 按模块名称分组权限
        Map<String, List<UserModulePermission>> permissionsByModule =
                allPermissions.stream()
                        .collect(Collectors.groupingBy(UserModulePermission::getModule));

        // 获取所有用户信息
        List<Long> userIds =
                allPermissions.stream()
                        .map(UserModulePermission::getUserId)
                        .distinct()
                        .collect(Collectors.toList());

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectByIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, user -> user));
        }

        // 构建返回结果
        List<ModuleInfoWithPermissionsDTO> result = new ArrayList<>();
        for (ModuleInfo moduleInfo : moduleInfos) {
            DatasourceInfo datasourceInfo = datasourceMap.get(moduleInfo.getDatasourceId());

            // 获取该模块的权限用户列表
            List<UserModulePermission> modulePermissions =
                    permissionsByModule.getOrDefault(moduleInfo.getName(), new ArrayList<>());

            // 转换为UserPermissionInfoDTO列表
            List<UserPermissionInfoDTO> users = new ArrayList<>();
            for (UserModulePermission permission : modulePermissions) {
                User user = userMap.get(permission.getUserId());
                if (user != null) {
                    UserPermissionInfoDTO userInfo =
                            modulePermissionConverter.createUserPermissionInfoDTO(permission, user);
                    users.add(userInfo);
                }
            }

            // 创建包含权限的模块DTO
            ModuleInfoWithPermissionsDTO dto =
                    moduleInfoConverter.toWithPermissionsDto(moduleInfo, datasourceInfo, users);
            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public void deleteModule(Long id, Boolean deleteDorisTable) {
        ModuleInfo moduleInfo = moduleInfoMapper.selectById(id);
        if (moduleInfo == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块不存在");
        }

        // 检查是否有Logstash进程正在使用该模块
        int processCount = logstashProcessMapper.countByModuleId(id);
        if (processCount > 0) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "该模块正在被 " + processCount + " 个进程使用，无法删除");
        }

        // 根据参数决定是否删除关联的Doris表数据
        if (deleteDorisTable
                && StringUtils.hasText(moduleInfo.getTableName())
                && StringUtils.hasText(moduleInfo.getDorisSql())) {
            deleteDorisTable(moduleInfo);
        }

        moduleInfoMapper.deleteById(id);
    }

    /**
     * 删除Doris表数据
     *
     * @param moduleInfo 模块信息
     */
    private void deleteDorisTable(ModuleInfo moduleInfo) {
        // 获取数据源信息
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(moduleInfo.getDatasourceId());
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        String tableName = moduleInfo.getTableName();

        // 1. 先清空表数据
        String truncateSQL = "TRUNCATE TABLE " + tableName;
        try {
            jdbcQueryExecutor.executeQuery(datasourceInfo, truncateSQL);
        } catch (Exception e) {
            // 如果TRUNCATE失败，可能是表不存在，记录日志但不抛出异常
            log.error("清空表数据失败，表可能不存在: {}, 错误: {}", tableName, e.getMessage());
        }

        // 2. 删除表
        String dropSQL = "DROP TABLE IF EXISTS " + tableName;
        try {
            jdbcQueryExecutor.executeQuery(datasourceInfo, dropSQL);
        } catch (Exception e) {
            // 如果DROP失败，记录日志但不抛出异常，允许删除模块继续进行
            log.error("删除表失败，表可能不存在: {}, 错误: {}", tableName, e.getMessage());
        }
    }

    @Override
    @Transactional
    public ModuleInfoDTO executeDorisSql(Long id, String sql) {
        ModuleInfo moduleInfo = moduleInfoMapper.selectById(id);
        if (moduleInfo == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块不存在");
        }

        if (!StringUtils.hasText(sql)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SQL语句不能为空");
        }

        // 检查dorisSql字段是否已有值
        if (StringUtils.hasText(moduleInfo.getDorisSql())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "该模块已经执行过Doris SQL，不能重复执行");
        }

        // 校验Doris SQL语句
        tableValidationService.validateDorisSql(moduleInfo, sql);

        // 获取数据源
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(moduleInfo.getDatasourceId());
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 执行SQL
        try {
            jdbcQueryExecutor.executeQuery(datasourceInfo, sql);
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.SQL_EXECUTION_FAILED, "SQL执行失败: " + e.getMessage(), e);
        }

        // 更新模块的dorisSql字段 - 更新时间和更新人由MyBatis拦截器自动设置
        moduleInfo.setDorisSql(sql);
        int result = moduleInfoMapper.update(moduleInfo);
        if (result == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "更新模块Doris SQL失败");
        }

        return moduleInfoConverter.toDto(moduleInfo, datasourceInfo);
    }
}
