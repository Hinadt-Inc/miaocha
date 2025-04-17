package com.hina.log.mapper;

import com.hina.log.entity.LogSearchHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 日志检索历史Mapper接口
 */
@Repository
public interface LogSearchHistoryMapper {
    @Insert("INSERT INTO log_search_history (user_id, datasource_id, table_name, keyword, start_time, end_time, result_file_path, create_time) "
            +
            "VALUES (#{userId}, #{datasourceId}, #{tableName}, #{keyword}, #{startTime}, #{endTime}, #{resultFilePath}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LogSearchHistory history);

    @Select("SELECT * FROM log_search_history WHERE id=#{id}")
    LogSearchHistory selectById(Long id);

    @Select("SELECT * FROM log_search_history WHERE user_id=#{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<LogSearchHistory> selectRecentByUserId(Long userId, int limit);

    @Update("UPDATE log_search_history SET result_file_path=#{resultFilePath} WHERE id=#{id}")
    int update(LogSearchHistory history);
}