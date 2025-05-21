package com.hina.log.mapper;

import com.hina.log.entity.LogstashTask;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Logstash任务Mapper接口
 */
@Mapper
public interface LogstashTaskMapper {

    /**
     * 插入任务记录
     */
    @Insert("INSERT INTO logstash_task (id, process_id, machine_id, name, description, status, operation_type) " +
            "VALUES (#{id}, #{processId}, #{machineId}, #{name}, #{description}, #{status}, #{operationType})")
    void insert(LogstashTask task);

    /**
     * 根据ID查询任务
     */
    @Select("SELECT * FROM logstash_task WHERE id = #{id}")
    Optional<LogstashTask> findById(String id);

    /**
     * 根据进程ID查询关联的任务
     */
    @Select("SELECT * FROM logstash_task WHERE process_id = #{processId} ORDER BY create_time DESC")
    List<LogstashTask> findByProcessId(Long processId);

    /**
     * 更新任务状态
     */
    @Update("UPDATE logstash_task SET status = #{status}, update_time = NOW() WHERE id = #{taskId}")
    void updateStatus(@Param("taskId") String taskId, @Param("status") String status);

    /**
     * 更新任务开始时间
     */
    @Update("UPDATE logstash_task SET start_time = #{startTime}, update_time = NOW() WHERE id = #{taskId}")
    void updateStartTime(@Param("taskId") String taskId, @Param("startTime") LocalDateTime startTime);

    /**
     * 更新任务结束时间
     */
    @Update("UPDATE logstash_task SET end_time = #{endTime}, update_time = NOW() WHERE id = #{taskId}")
    void updateEndTime(@Param("taskId") String taskId, @Param("endTime") LocalDateTime endTime);

    /**
     * 更新任务错误信息
     */
    @Update("UPDATE logstash_task SET error_message = #{errorMessage}, update_time = NOW() WHERE id = #{taskId}")
    void updateErrorMessage(@Param("taskId") String taskId, @Param("errorMessage") String errorMessage);

    /**
     * 获取进程最近一次任务
     */
    @Select("SELECT * FROM logstash_task WHERE process_id = #{processId} ORDER BY create_time DESC LIMIT 1")
    Optional<LogstashTask> findLatestByProcessId(Long processId);

    /**
     * 获取进程在指定机器上的最近一次任务 (使用新增的machine_id字段)
     */
    @Select("SELECT * FROM logstash_task " +
           "WHERE process_id = #{processId} AND machine_id = #{machineId} " +
           "ORDER BY create_time DESC LIMIT 1")
    Optional<LogstashTask> findLatestByProcessIdAndMachineId(@Param("processId") Long processId, @Param("machineId") Long machineId);

    /**
     * 获取进程在指定机器上的所有任务 (使用新增的machine_id字段)
     */
    @Select("SELECT * FROM logstash_task " +
           "WHERE process_id = #{processId} AND machine_id = #{machineId} " +
           "ORDER BY create_time DESC")
    List<LogstashTask> findByProcessIdAndMachineId(@Param("processId") Long processId, @Param("machineId") Long machineId);
    
    /**
     * 获取进程在指定机器上的所有任务ID
     */
    @Select("SELECT id FROM logstash_task " +
           "WHERE process_id = #{processId} AND machine_id = #{machineId} " +
           "ORDER BY create_time DESC")
    List<String> findTaskIdsByProcessIdAndMachineId(@Param("processId") Long processId, @Param("machineId") Long machineId);

    /**
     * 获取进程相关的所有机器特定任务
     */
    @Select("SELECT * FROM logstash_task " +
           "WHERE process_id = #{processId} AND machine_id IS NOT NULL " +
           "ORDER BY create_time DESC")
    List<LogstashTask> findMachineTasksByProcessId(Long processId);

    /**
     * 获取进程相关的所有全局任务
     */
    @Select("SELECT * FROM logstash_task " +
           "WHERE process_id = #{processId} AND machine_id IS NULL " +
           "ORDER BY create_time DESC")
    List<LogstashTask> findGlobalTasksByProcessId(Long processId);

    /**
     * 删除任务
     */
    @Delete("DELETE FROM logstash_task WHERE id = #{id}")
    void deleteById(String id);
}