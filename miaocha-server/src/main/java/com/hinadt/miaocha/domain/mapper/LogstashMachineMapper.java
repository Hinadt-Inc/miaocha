package com.hinadt.miaocha.domain.mapper;

import com.hinadt.miaocha.domain.entity.LogstashMachine;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Logstash机器关联Mapper接口 - 重构支持多实例 */
@Mapper
public interface LogstashMachineMapper {

    int insert(LogstashMachine logstashMachine);

    int update(LogstashMachine logstashMachine);

    int deleteById(Long id);

    LogstashMachine selectById(Long id);

    List<LogstashMachine> selectByIds(List<Long> ids);

    List<LogstashMachine> selectByLogstashProcessId(Long logstashProcessId);

    List<LogstashMachine> selectByMachineId(Long machineId);

    /**
     * 统计机器被Logstash实例使用的数量
     *
     * @param machineId 机器ID
     * @return 使用数量
     */
    int countByMachineId(Long machineId);

    List<LogstashMachine> selectAll();

    List<LogstashMachine> selectAllWithProcessPid();

    /** 基于实例ID更新进程PID */
    int updateProcessPidById(@Param("id") Long id, @Param("processPid") String processPid);

    /** 基于实例ID更新状态 */
    int updateStateById(@Param("id") Long id, @Param("state") String state);

    /** 基于实例ID更新配置内容 */
    int updateConfigContentById(@Param("id") Long id, @Param("configContent") String configContent);

    /** 基于实例ID更新JVM配置 */
    int updateJvmOptionsById(@Param("id") Long id, @Param("jvmOptions") String jvmOptions);

    /** 基于实例ID更新Logstash系统配置 */
    int updateLogstashYmlById(@Param("id") Long id, @Param("logstashYml") String logstashYml);

    /** 根据机器ID和部署路径查询实例（用于检查路径冲突） */
    LogstashMachine selectByMachineAndPath(
            @Param("machineId") Long machineId, @Param("deployPath") String deployPath);
}
