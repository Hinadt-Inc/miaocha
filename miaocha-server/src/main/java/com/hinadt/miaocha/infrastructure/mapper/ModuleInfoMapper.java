package com.hinadt.miaocha.infrastructure.mapper;

import com.hinadt.miaocha.domain.entity.ModuleInfo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 模块信息Mapper接口 */
@Mapper
public interface ModuleInfoMapper {

    /**
     * 插入模块信息
     *
     * @param moduleInfo 模块信息
     * @return 影响行数
     */
    int insert(ModuleInfo moduleInfo);

    /**
     * 根据ID查询模块信息
     *
     * @param id 模块ID
     * @return 模块信息
     */
    ModuleInfo selectById(Long id);

    /**
     * 根据ID查询模块信息（包含用户昵称）
     *
     * @param id 模块ID
     * @return 模块信息（包含用户昵称）
     */
    ModuleInfo selectByIdWithUserNames(Long id);

    /**
     * 根据名称查询模块信息
     *
     * @param name 模块名称
     * @return 模块信息
     */
    ModuleInfo selectByName(String name);

    /**
     * 查询所有模块信息
     *
     * @return 模块信息列表
     */
    List<ModuleInfo> selectAll();

    /**
     * 查询所有模块信息（包含用户昵称）
     *
     * @return 模块信息列表（包含用户昵称）
     */
    List<ModuleInfo> selectAllWithUserNames();

    /**
     * 根据数据源ID查询模块信息列表
     *
     * @param datasourceId 数据源ID
     * @return 模块信息列表
     */
    List<ModuleInfo> selectByDatasourceId(Long datasourceId);

    /**
     * 更新模块信息
     *
     * @param moduleInfo 模块信息
     * @return 影响行数
     */
    int update(ModuleInfo moduleInfo);

    /**
     * 根据ID删除模块信息
     *
     * @param id 模块ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 删除所有模块信息
     *
     * @return 影响行数
     */
    int deleteAll();

    /**
     * 查询所有启用的模块信息
     *
     * @return 启用的模块信息列表
     */
    List<ModuleInfo> selectAllEnabled();

    /**
     * 检查模块名称是否存在
     *
     * @param name 模块名称
     * @param excludeId 排除的ID（用于更新时检查）
     * @return 是否存在
     */
    boolean existsByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 检查数据源是否被模块使用
     *
     * @param datasourceId 数据源ID
     * @return 是否被使用
     */
    boolean existsByDatasourceId(Long datasourceId);

    /**
     * 获取指定数据源下所有启用模块的表名
     *
     * @param datasourceId 数据源ID（可选，如果为null则查询所有数据源）
     * @return 启用模块的表名列表
     */
    List<String> selectEnabledModuleTableNames(@Param("datasourceId") Long datasourceId);
}
