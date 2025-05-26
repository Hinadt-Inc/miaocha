package com.hina.log.domain.mapper;

import com.hina.log.domain.entity.SqlQueryHistory;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL查询历史Mapper接口 */
@Mapper
public interface SqlQueryHistoryMapper {

    int insert(SqlQueryHistory history);

    SqlQueryHistory selectById(Long id);

    List<SqlQueryHistory> selectRecentByUserId(
            @Param("userId") Long userId, @Param("limit") int limit);

    int update(SqlQueryHistory history);

    /**
     * 分页查询SQL历史记录
     *
     * @param userId 用户ID
     * @param datasourceId 数据源ID，可选
     * @param tableName 表名关键字，可选
     * @param queryKeyword SQL查询关键字，可选
     * @param offset 偏移量
     * @param limit 每页数量
     * @return SQL查询历史记录列表
     */
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
     * @param userId 用户ID
     * @param datasourceId 数据源ID，可选
     * @param tableName 表名关键字，可选
     * @param queryKeyword SQL查询关键字，可选
     * @return 符合条件的记录总数
     */
    Long countTotal(
            @Param("userId") Long userId,
            @Param("datasourceId") Long datasourceId,
            @Param("tableName") String tableName,
            @Param("queryKeyword") String queryKeyword);
}
