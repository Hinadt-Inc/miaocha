package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.dto.module.ModuleInfoCreateDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoUpdateDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoWithPermissionsDTO;
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
     */
    void deleteModule(Long id);

    /**
     * 执行模块的Doris SQL
     *
     * @param id 模块ID
     * @param sql Doris SQL语句
     * @return 模块信息响应
     */
    ModuleInfoDTO executeDorisSql(Long id, String sql);
}
