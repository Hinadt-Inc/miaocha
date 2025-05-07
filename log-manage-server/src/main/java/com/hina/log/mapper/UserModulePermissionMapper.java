package com.hina.log.mapper;

import com.hina.log.entity.UserModulePermission;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户模块权限Mapper接口
 */
@Mapper
public interface UserModulePermissionMapper {
    
    /**
     * 插入用户模块权限
     *
     * @param permission 权限实体
     * @return 影响的行数
     */
    @Insert("INSERT INTO user_module_permission (user_id, datasource_id, module, create_time, update_time) " +
            "VALUES (#{userId}, #{datasourceId}, #{module}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserModulePermission permission);
    
    /**
     * 删除用户模块权限
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID
     * @param module       模块名称
     * @return 影响的行数
     */
    @Delete("DELETE FROM user_module_permission WHERE user_id = #{userId} AND datasource_id = #{datasourceId} AND module = #{module}")
    int delete(@Param("userId") Long userId, @Param("datasourceId") Long datasourceId, @Param("module") String module);
    
    /**
     * 查询用户模块权限
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID
     * @param module       模块名称
     * @return 权限实体
     */
    @Select("SELECT * FROM user_module_permission WHERE user_id = #{userId} AND datasource_id = #{datasourceId} AND module = #{module}")
    UserModulePermission select(@Param("userId") Long userId, @Param("datasourceId") Long datasourceId, @Param("module") String module);
    
    /**
     * 查询用户在指定数据源的所有模块权限
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID
     * @return 权限实体列表
     */
    @Select("SELECT * FROM user_module_permission WHERE user_id = #{userId} AND datasource_id = #{datasourceId}")
    List<UserModulePermission> selectByUserAndDatasource(@Param("userId") Long userId, @Param("datasourceId") Long datasourceId);
    
    /**
     * 查询用户的所有模块权限
     *
     * @param userId 用户ID
     * @return 权限实体列表
     */
    @Select("SELECT * FROM user_module_permission WHERE user_id = #{userId}")
    List<UserModulePermission> selectByUser(@Param("userId") Long userId);
    
    /**
     * 查询所有用户的模块权限
     *
     * @return 权限实体列表
     */
    @Select("SELECT * FROM user_module_permission")
    List<UserModulePermission> selectAll();
}
