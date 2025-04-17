package com.hina.log.mapper;

import com.hina.log.entity.UserDatasourcePermission;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户数据源权限Mapper接口
 */
@Mapper
public interface UserDatasourcePermissionMapper {

    @Insert("INSERT INTO user_datasource_permission (user_id, datasource_id, table_name, create_time, update_time) " +
            "VALUES (#{userId}, #{datasourceId}, #{tableName}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserDatasourcePermission permission);

    @Delete("DELETE FROM user_datasource_permission WHERE id=#{id}")
    int deleteById(Long id);

    @Select("SELECT * FROM user_datasource_permission WHERE user_id=#{userId} AND datasource_id=#{datasourceId}")
    List<UserDatasourcePermission> selectByUserAndDatasource(Long userId, Long datasourceId);

    @Select("SELECT * FROM user_datasource_permission " +
            "WHERE user_id=#{userId} AND datasource_id=#{datasourceId} AND table_name=#{tableName}")
    UserDatasourcePermission selectByUserDatasourceAndTable(Long userId, Long datasourceId, String tableName);

    @Select("SELECT * FROM user_datasource_permission " +
            "WHERE user_id=#{userId} AND datasource_id=#{datasourceId} AND table_name='*'")
    UserDatasourcePermission selectAllTablesPermission(Long userId, Long datasourceId);
}