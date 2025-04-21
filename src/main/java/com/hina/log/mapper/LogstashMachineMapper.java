package com.hina.log.mapper;

import com.hina.log.entity.LogstashMachine;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Logstash机器关联Mapper接口
 */
@Mapper
public interface LogstashMachineMapper {

        @Insert("INSERT INTO logstash_machine (logstash_process_id, machine_id, create_time) " +
                        "VALUES (#{logstashProcessId}, #{machineId}, NOW())")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(LogstashMachine logstashMachine);

        @Update("UPDATE logstash_machine SET process_pid = #{processPid} " +
                        "WHERE logstash_process_id = #{logstashProcessId} AND machine_id = #{machineId}")
        int updateProcessPid(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId,
                        @Param("processPid") String processPid);

        @Delete("DELETE FROM logstash_machine WHERE id=#{id}")
        int deleteById(Long id);

        @Delete("DELETE FROM logstash_machine WHERE logstash_process_id=#{logstashProcessId}")
        int deleteByLogstashProcessId(Long logstashProcessId);

        @Delete("DELETE FROM logstash_machine WHERE machine_id=#{machineId}")
        int deleteByMachineId(Long machineId);

        @Select("SELECT * FROM logstash_machine WHERE logstash_process_id=#{logstashProcessId}")
        List<LogstashMachine> selectByLogstashProcessId(Long logstashProcessId);

        @Select("SELECT * FROM logstash_machine WHERE machine_id=#{machineId}")
        List<LogstashMachine> selectByMachineId(Long machineId);

        @Select("SELECT * FROM logstash_machine WHERE logstash_process_id=#{logstashProcessId} AND machine_id=#{machineId}")
        LogstashMachine selectByLogstashProcessIdAndMachineId(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId);

        @Select("SELECT * FROM logstash_machine WHERE process_pid IS NOT NULL")
        List<LogstashMachine> selectAllWithProcessPid();

        @Select("SELECT COUNT(*) FROM logstash_machine WHERE logstash_process_id=#{logstashProcessId} AND machine_id=#{machineId}")
        int countByLogstashProcessIdAndMachineId(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId);
}