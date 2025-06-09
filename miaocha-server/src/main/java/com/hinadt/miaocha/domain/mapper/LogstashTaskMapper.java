package com.hinadt.miaocha.domain.mapper;

import com.hinadt.miaocha.domain.entity.LogstashTask;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Logstash任务Mapper接口 */
@Mapper
public interface LogstashTaskMapper {

    /** 插入任务记录 */
    void insert(LogstashTask task);

    /** 根据ID查询任务 */
    Optional<LogstashTask> findById(String id);

    /** 根据进程ID查询关联的任务 */
    List<LogstashTask> findByProcessId(Long processId);

    /** 更新任务状态 */
    void updateStatus(@Param("taskId") String taskId, @Param("status") String status);

    /** 更新任务开始时间 */
    void updateStartTime(
            @Param("taskId") String taskId, @Param("startTime") LocalDateTime startTime);

    /** 更新任务结束时间 */
    void updateEndTime(@Param("taskId") String taskId, @Param("endTime") LocalDateTime endTime);

    /** 更新任务错误信息 */
    void updateErrorMessage(
            @Param("taskId") String taskId, @Param("errorMessage") String errorMessage);

    /** 获取进程最近一次任务 */
    Optional<LogstashTask> findLatestByProcessId(Long processId);

    /** 获取进程在指定机器上的最近一次任务 (使用新增的machine_id字段) */
    Optional<LogstashTask> findLatestByProcessIdAndMachineId(
            @Param("processId") Long processId, @Param("machineId") Long machineId);

    /** 获取进程在指定机器上的所有任务 (使用新增的machine_id字段) */
    List<LogstashTask> findByProcessIdAndMachineId(
            @Param("processId") Long processId, @Param("machineId") Long machineId);

    /** 获取进程在指定机器上的所有任务ID */
    List<String> findTaskIdsByProcessIdAndMachineId(
            @Param("processId") Long processId, @Param("machineId") Long machineId);

    /** 获取进程相关的所有机器特定任务 */
    List<LogstashTask> findMachineTasksByProcessId(Long processId);

    /** 获取进程相关的所有全局任务 */
    List<LogstashTask> findGlobalTasksByProcessId(Long processId);

    /** 删除任务 */
    void deleteById(String id);
}
