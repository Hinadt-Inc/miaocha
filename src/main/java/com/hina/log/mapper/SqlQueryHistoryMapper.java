package com.hina.log.mapper;

import com.hina.log.entity.SqlQueryHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * SQL查询历史Mapper接口
 */
@Mapper
public interface SqlQueryHistoryMapper {

    @Insert("INSERT INTO sql_query_history (user_id, datasource_id, table_name, sql_query, result_file_path, create_time) "
            +
            "VALUES (#{userId}, #{datasourceId}, #{tableName}, #{sqlQuery}, #{resultFilePath}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SqlQueryHistory history);

    @Select("SELECT * FROM sql_query_history WHERE id=#{id}")
    SqlQueryHistory selectById(Long id);

    @Select("SELECT * FROM sql_query_history WHERE user_id=#{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<SqlQueryHistory> selectRecentByUserId(Long userId, int limit);

    @Update("UPDATE sql_query_history SET result_file_path=#{resultFilePath} WHERE id=#{id}")
    int update(SqlQueryHistory history);
}