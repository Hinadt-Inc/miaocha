package com.hina.log.mapper;

import com.hina.log.entity.LogstashProcess;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Logstash进程Mapper接口
 */
@Mapper
public interface LogstashProcessMapper {

        @Insert("INSERT INTO logstash_process (name, module, config_content, doris_sql, datasource_id, table_name, state, create_time, update_time) "
                        +
                        "VALUES (#{name}, #{module}, #{configContent}, #{dorisSql}, #{datasourceId}, #{tableName}, #{state}, NOW(), NOW())")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(LogstashProcess logstashProcess);

        @Update("UPDATE logstash_process SET name=#{name}, module=#{module}, " +
                        "config_content=#{configContent}, doris_sql=#{dorisSql}, datasource_id=#{datasourceId}, table_name=#{tableName}, update_time=NOW() WHERE id=#{id}")
        int update(LogstashProcess logstashProcess);

        @Update("UPDATE logstash_process SET state=#{state}, update_time=NOW() WHERE id=#{id}")
        int updateState(@Param("id") Long id, @Param("state") String state);

        @Delete("DELETE FROM logstash_process WHERE id=#{id}")
        int deleteById(Long id);

        @Select("SELECT * FROM logstash_process WHERE id=#{id}")
        LogstashProcess selectById(Long id);

        @Select("SELECT * FROM logstash_process WHERE name=#{name}")
        LogstashProcess selectByName(String name);

        @Select("SELECT * FROM logstash_process")
        List<LogstashProcess> selectAll();

        @Select("SELECT lp.* FROM logstash_process lp " +
                        "JOIN logstash_machine lm ON lp.id = lm.logstash_process_id " +
                        "WHERE lm.machine_id = #{machineId}")
        List<LogstashProcess> selectByMachineId(Long machineId);
}