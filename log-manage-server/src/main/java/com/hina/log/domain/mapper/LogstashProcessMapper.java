package com.hina.log.domain.mapper;

import com.hina.log.domain.entity.LogstashProcess;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Logstash进程Mapper接口 */
@Mapper
public interface LogstashProcessMapper {

    int insert(LogstashProcess logstashProcess);

    int update(LogstashProcess logstashProcess);

    int updateConfigOnly(
            @Param("id") Long id,
            @Param("configContent") String configContent,
            @Param("jvmOptions") String jvmOptions,
            @Param("logstashYml") String logstashYml);

    int deleteById(Long id);

    LogstashProcess selectById(Long id);

    LogstashProcess selectByName(String name);

    LogstashProcess selectByModule(String module);

    String selectTableNameByModule(String module);

    int countByModule(String module);

    List<LogstashProcess> selectAll();

    List<LogstashProcess> selectByMachineId(Long machineId);
}
