package com.hinadt.miaocha.domain.mapper;

import com.hinadt.miaocha.domain.entity.LogstashProcess;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Logstash进程Mapper接口 */
@Mapper
public interface LogstashProcessMapper {

    int insert(LogstashProcess logstashProcess);

    int update(LogstashProcess logstashProcess);

    int updateMetadataOnly(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("moduleId") Long moduleId,
            @Param("updateUser") String updateUser);

    int updateConfigOnly(
            @Param("id") Long id,
            @Param("configContent") String configContent,
            @Param("jvmOptions") String jvmOptions,
            @Param("logstashYml") String logstashYml);

    int deleteById(Long id);

    LogstashProcess selectById(Long id);

    LogstashProcess selectByName(String name);

    int countByModuleId(Long moduleId);

    List<LogstashProcess> selectAll();
}
