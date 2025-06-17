package com.hinadt.miaocha.domain.mapper;

import com.hinadt.miaocha.domain.entity.LogstashTaskMachineStep;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Logstash任务机器步骤Mapper接口 */
@Mapper
public interface LogstashTaskMachineStepMapper {

    /** 插入步骤记录 */
    void insert(LogstashTaskMachineStep step);

    /** 批量插入步骤记录 */
    void batchInsert(@Param("steps") List<LogstashTaskMachineStep> steps);

    /** 根据任务ID查询所有步骤 */
    List<LogstashTaskMachineStep> findByTaskId(String taskId);

    /** 根据LogstashMachine实例ID查询所有步骤 */
    List<LogstashTaskMachineStep> findByLogstashMachineId(Long logstashMachineId);

    /** 更新步骤状态 */
    void updateStatus(
            @Param("taskId") String taskId,
            @Param("machineId") Long machineId,
            @Param("stepId") String stepId,
            @Param("status") String status);

    /** 根据LogstashMachine实例ID更新步骤状态 */
    void updateStatusByLogstashMachineId(
            @Param("taskId") String taskId,
            @Param("logstashMachineId") Long logstashMachineId,
            @Param("stepId") String stepId,
            @Param("status") String status);

    /** 更新步骤开始时间 */
    void updateStartTime(
            @Param("taskId") String taskId,
            @Param("machineId") Long machineId,
            @Param("stepId") String stepId,
            @Param("startTime") LocalDateTime startTime);

    /** 根据LogstashMachine实例ID更新步骤开始时间 */
    void updateStartTimeByLogstashMachineId(
            @Param("taskId") String taskId,
            @Param("logstashMachineId") Long logstashMachineId,
            @Param("stepId") String stepId,
            @Param("startTime") LocalDateTime startTime);

    /** 根据LogstashMachine实例ID更新步骤结束时间 */
    void updateEndTimeByLogstashMachineId(
            @Param("taskId") String taskId,
            @Param("logstashMachineId") Long logstashMachineId,
            @Param("stepId") String stepId,
            @Param("endTime") LocalDateTime endTime);

    /** 根据LogstashMachine实例ID更新步骤错误信息 */
    void updateErrorMessageByLogstashMachineId(
            @Param("taskId") String taskId,
            @Param("logstashMachineId") Long logstashMachineId,
            @Param("stepId") String stepId,
            @Param("errorMessage") String errorMessage);

    /**
     * 重置任务所有步骤的状态
     *
     * @param taskId 任务ID
     * @param status 重置后的状态
     */
    void resetStepStatuses(@Param("taskId") String taskId, @Param("status") String status);

    /**
     * 删除任务所有步骤
     *
     * @param taskId 任务ID
     */
    void deleteByTaskId(String taskId);
}
