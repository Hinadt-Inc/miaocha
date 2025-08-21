package com.hinadt.miaocha.infrastructure.mapper;

import com.hinadt.miaocha.domain.entity.LogstashProcess;
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
            @Param("logstashYml") String logstashYml,
            @Param("updateUser") String updateUser);

    /** 更新进程的完整元信息（包括基础信息和配置信息） 只更新数据库，不同步到实例 */
    int updateMetadataAndConfig(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("moduleId") Long moduleId,
            @Param("configContent") String configContent,
            @Param("jvmOptions") String jvmOptions,
            @Param("logstashYml") String logstashYml,
            @Param("updateUser") String updateUser);

    int deleteById(Long id);

    int deleteAll();

    LogstashProcess selectById(Long id);

    LogstashProcess selectByName(String name);

    int countByModuleId(Long moduleId);

    List<LogstashProcess> selectAll();
}
