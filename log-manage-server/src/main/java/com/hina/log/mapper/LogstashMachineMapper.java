package com.hina.log.mapper;

import com.hina.log.entity.LogstashMachine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Logstash机器关联Mapper接口
 */
@Mapper
public interface LogstashMachineMapper {

        int insert(LogstashMachine logstashMachine);

        int updateProcessPid(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId,
                        @Param("processPid") String processPid);

        int updateState(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId,
                        @Param("state") String state);

        int updateConfigContent(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId,
                        @Param("configContent") String configContent);

        int updateJvmOptions(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId,
                        @Param("jvmOptions") String jvmOptions);

        int updateLogstashYml(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId,
                        @Param("logstashYml") String logstashYml);

        int update(LogstashMachine logstashMachine);

        int deleteById(Long id);

        int deleteByLogstashProcessId(Long logstashProcessId);

        int deleteByMachineId(Long machineId);

        List<LogstashMachine> selectByLogstashProcessId(Long logstashProcessId);

        List<LogstashMachine> selectByMachineId(Long machineId);

        LogstashMachine selectByLogstashProcessIdAndMachineId(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId);

        List<LogstashMachine> selectAllWithProcessPid();

        List<LogstashMachine> selectByState(String state);

        List<LogstashMachine> selectByLogstashProcessIdAndState(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("state") String state);

        int countByLogstashProcessIdAndMachineId(@Param("logstashProcessId") Long logstashProcessId,
                        @Param("machineId") Long machineId);
}