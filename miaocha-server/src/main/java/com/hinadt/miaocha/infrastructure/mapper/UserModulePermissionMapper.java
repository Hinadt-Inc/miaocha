package com.hinadt.miaocha.infrastructure.mapper;

import com.hinadt.miaocha.domain.entity.UserModulePermission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 用户模块权限Mapper接口 */
@Mapper
public interface UserModulePermissionMapper {

    /**
     * 插入用户模块权限
     *
     * @param permission 权限实体
     * @return 影响的行数
     */
    int insert(UserModulePermission permission);

    /**
     * 删除用户模块权限
     *
     * @param userId 用户ID
     * @param datasourceId 数据源ID
     * @param module 模块名称
     * @return 影响的行数
     */
    int delete(
            @Param("userId") Long userId,
            @Param("datasourceId") Long datasourceId,
            @Param("module") String module);

    /**
     * 查询用户模块权限
     *
     * @param userId 用户ID
     * @param datasourceId 数据源ID
     * @param module 模块名称
     * @return 权限实体
     */
    UserModulePermission select(
            @Param("userId") Long userId,
            @Param("datasourceId") Long datasourceId,
            @Param("module") String module);

    /**
     * 查询用户在指定数据源的所有模块权限
     *
     * @param userId 用户ID
     * @param datasourceId 数据源ID
     * @return 权限实体列表
     */
    List<UserModulePermission> selectByUserAndDatasource(
            @Param("userId") Long userId, @Param("datasourceId") Long datasourceId);

    /**
     * 查询用户的所有模块权限
     *
     * @param userId 用户ID
     * @return 权限实体列表
     */
    List<UserModulePermission> selectByUser(@Param("userId") Long userId);

    /**
     * 查询所有用户的模块权限
     *
     * @return 权限实体列表
     */
    List<UserModulePermission> selectAll();

    /**
     * 删除用户的所有权限
     *
     * @param userId 用户ID
     * @return 影响的行数
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 删除权限
     *
     * @param id 权限ID
     * @return 影响的行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据模块名称删除所有相关权限
     *
     * @param moduleName 模块名称
     * @return 影响的行数
     */
    int deleteByModuleName(@Param("moduleName") String moduleName);

    /**
     * 批量更新模块名称 当模块名称发生变化时，同步更新权限表中的模块名称
     *
     * @param oldModuleName 旧模块名称
     * @param newModuleName 新模块名称
     * @param datasourceId 数据源ID
     * @return 影响的行数
     */
    int updateModuleName(
            @Param("oldModuleName") String oldModuleName,
            @Param("newModuleName") String newModuleName,
            @Param("datasourceId") Long datasourceId);

    /**
     * 获取用户有权限访问的所有表名 通过联查用户模块权限表和模块信息表，获取用户实际可以访问的表名列表
     *
     * @param userId 用户ID
     * @param datasourceId 数据源ID（可选，如果为null则查询所有数据源）
     * @return 用户有权限访问的表名列表
     */
    List<String> selectPermittedTableNames(
            @Param("userId") Long userId, @Param("datasourceId") Long datasourceId);
}
