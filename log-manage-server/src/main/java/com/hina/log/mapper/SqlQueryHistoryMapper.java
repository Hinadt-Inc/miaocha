package com.hina.log.mapper;

import com.hina.log.entity.SqlQueryHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

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

    /**
     * 分页查询SQL历史记录
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID，可选
     * @param tableName    表名关键字，可选
     * @param queryKeyword SQL查询关键字，可选
     * @param offset       偏移量
     * @param limit        每页数量
     * @return SQL查询历史记录列表
     */
    @Select({ "<script>",
            "SELECT * FROM sql_query_history",
            "WHERE user_id=#{userId}",
            "<if test='datasourceId != null'>",
            "  AND datasource_id=#{datasourceId}",
            "</if>",
            "<if test='tableName != null and tableName != \"\"'>",
            "  AND table_name LIKE CONCAT('%', #{tableName}, '%')",
            "</if>",
            "<if test='queryKeyword != null and queryKeyword != \"\"'>",
            "  AND sql_query LIKE CONCAT('%', #{queryKeyword}, '%')",
            "</if>",
            "ORDER BY create_time DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>" })
    List<SqlQueryHistory> selectByPage(
            @Param("userId") Long userId,
            @Param("datasourceId") Long datasourceId,
            @Param("tableName") String tableName,
            @Param("queryKeyword") String queryKeyword,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 查询符合条件的记录总数
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID，可选
     * @param tableName    表名关键字，可选
     * @param queryKeyword SQL查询关键字，可选
     * @return 符合条件的记录总数
     */
    @Select({ "<script>",
            "SELECT COUNT(*) FROM sql_query_history",
            "WHERE user_id=#{userId}",
            "<if test='datasourceId != null'>",
            "  AND datasource_id=#{datasourceId}",
            "</if>",
            "<if test='tableName != null and tableName != \"\"'>",
            "  AND table_name LIKE CONCAT('%', #{tableName}, '%')",
            "</if>",
            "<if test='queryKeyword != null and queryKeyword != \"\"'>",
            "  AND sql_query LIKE CONCAT('%', #{queryKeyword}, '%')",
            "</if>",
            "</script>" })
    Long countTotal(
            @Param("userId") Long userId,
            @Param("datasourceId") Long datasourceId,
            @Param("tableName") String tableName,
            @Param("queryKeyword") String queryKeyword);
}