package com.hina.log.domain.mapper;

import com.hina.log.domain.entity.UserModulePermission;
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
}
