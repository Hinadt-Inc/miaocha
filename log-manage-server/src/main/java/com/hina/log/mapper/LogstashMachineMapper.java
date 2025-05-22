package com.hina.log.mapper;

import com.hina.log.entity.LogstashMachine;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Logstash机器关联Mapper接口
 */
@Mapper
public interface LogstashMachineMapper {

        @Insert("INSERT INTO logstash_machine (logstash_process_id, machine_id, state, config_content, jvm_options, logstash_yml, create_time, update_time) " +
                        "VALUES (#{logstashProcessId}, #{machineId}, #{state}, #{configContent}, #{jvmOptions}, #{logstashYml}, NOW(), NOW())")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(LogstashMachine logstashMachine);

        @Update("UPDATE logstash_machine SET process_pid = #{processPid} " +
                        "WHERE logstash_process_id = #{logstashProcessId} AND machine_id = #{machineId}")
        int updateProcessPid(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId,
                        @Param("processPid") String processPid);

        @Update("UPDATE logstash_machine SET state = #{state} " +
                        "WHERE logstash_process_id = #{logstashProcessId} AND machine_id = #{machineId}")
        int updateState(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId,
                        @Param("state") String state);

        @Update("UPDATE logstash_machine SET config_content = #{configContent} " +
                        "WHERE logstash_process_id = #{logstashProcessId} AND machine_id = #{machineId}")
        int updateConfigContent(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId,
                        @Param("configContent") String configContent);

        @Update("UPDATE logstash_machine SET jvm_options = #{jvmOptions} " +
                        "WHERE logstash_process_id = #{logstashProcessId} AND machine_id = #{machineId}")
        int updateJvmOptions(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId,
                        @Param("jvmOptions") String jvmOptions);

        @Update("UPDATE logstash_machine SET logstash_yml = #{logstashYml} " +
                        "WHERE logstash_process_id = #{logstashProcessId} AND machine_id = #{machineId}")
        int updateLogstashYml(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId,
                        @Param("logstashYml") String logstashYml);

        @Update("UPDATE logstash_machine SET " +
                        "process_pid = #{processPid}, " +
                        "state = #{state}, " +
                        "config_content = #{configContent}, " +
                        "jvm_options = #{jvmOptions}, " +
                        "logstash_yml = #{logstashYml} " +
                        "WHERE id = #{id}")
        int update(LogstashMachine logstashMachine);

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

        @Select("SELECT * FROM logstash_machine WHERE state = #{state}")
        List<LogstashMachine> selectByState(String state);

        @Select("SELECT * FROM logstash_machine WHERE logstash_process_id = #{logstashProcessId} AND state = #{state}")
        List<LogstashMachine> selectByLogstashProcessIdAndState(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("state") String state);

        @Select("SELECT COUNT(*) FROM logstash_machine WHERE logstash_process_id=#{logstashProcessId} AND machine_id=#{machineId}")
        int countByLogstashProcessIdAndMachineId(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId);
}