package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.dto.module.*;
import java.util.List;

/** 模块信息服务接口 */
public interface ModuleInfoService {

    /**
     * 创建模块信息
     *
     * @param request 创建请求
     * @return 模块信息响应
     */
    ModuleInfoDTO createModule(ModuleInfoCreateDTO request);

    /**
     * 更新模块信息
     *
     * @param request 更新请求
     * @return 模块信息响应
     */
    ModuleInfoDTO updateModule(ModuleInfoUpdateDTO request);

    /**
     * 根据ID查询模块信息
     *
     * @param id 模块ID
     * @return 模块信息响应
     */
    ModuleInfoDTO getModuleById(Long id);

    /**
     * 查询所有模块信息
     *
     * @return 模块信息列表
     */
    List<ModuleInfoDTO> getAllModules();

    /**
     * 查询所有模块信息（包含用户权限）
     *
     * @return 包含权限信息的模块信息列表
     */
    List<ModuleInfoWithPermissionsDTO> getAllModulesWithPermissions();

    /**
     * 删除模块信息
     *
     * @param id 模块ID
     * @param deleteDorisTable 是否删除底层Doris表数据
     */
    void deleteModule(Long id, Boolean deleteDorisTable);

    /**
     * 执行模块的Doris SQL
     *
     * @param id 模块ID
     * @param sql Doris SQL语句
     * @return 模块信息响应
     */
    ModuleInfoDTO executeDorisSql(Long id, String sql);

    /**
     * 配置模块的查询配置
     *
     * @param moduleId 模块ID
     * @param queryConfig 查询配置
     * @return 模块信息响应
     */
    ModuleInfoDTO configureQueryConfig(Long moduleId, QueryConfigDTO queryConfig);

    /**
     * 根据模块名称获取对应的表名
     *
     * @param module 模块名称
     * @return 表名
     */
    String getTableNameByModule(String module);

    /**
     * 根据模块名称获取查询配置
     *
     * @param module 模块名称
     * @return 查询配置DTO，如果模块不存在或未配置则返回null
     */
    QueryConfigDTO getQueryConfigByModule(String module);

    /**
     * 更新模块状态（启用/禁用）
     *
     * @param request 状态更新请求
     * @return 模块信息响应
     */
    ModuleInfoDTO updateModuleStatus(ModuleStatusUpdateDTO request);
}
