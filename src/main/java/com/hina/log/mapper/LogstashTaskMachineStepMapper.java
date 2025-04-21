package com.hina.log.mapper;

import com.hina.log.entity.LogstashTaskMachineStep;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Logstash任务机器步骤Mapper接口
 */
@Mapper
public interface LogstashTaskMachineStepMapper {

    /**
     * 插入步骤记录
     */
    @Insert("INSERT INTO logstash_task_machine_step (task_id, machine_id, step_id, step_name, status) " +
            "VALUES (#{taskId}, #{machineId}, #{stepId}, #{stepName}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(LogstashTaskMachineStep step);

    /**
     * 批量插入步骤记录
     */
    @Insert("<script>" +
            "INSERT INTO logstash_task_machine_step (task_id, machine_id, step_id, step_name, status) VALUES " +
            "<foreach collection='steps' item='step' separator=','>" +
            "(#{step.taskId}, #{step.machineId}, #{step.stepId}, #{step.stepName}, #{step.status})" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("steps") List<LogstashTaskMachineStep> steps);

    /**
     * 根据任务ID查询所有步骤
     */
    @Select("SELECT * FROM logstash_task_machine_step WHERE task_id = #{taskId} ORDER BY machine_id, id")
    List<LogstashTaskMachineStep> findByTaskId(String taskId);

    /**
     * 根据任务ID和机器ID查询所有步骤
     */
    @Select("SELECT * FROM logstash_task_machine_step WHERE task_id = #{taskId} AND machine_id = #{machineId} ORDER BY id")
    List<LogstashTaskMachineStep> findByTaskIdAndMachineId(@Param("taskId") String taskId,
            @Param("machineId") Long machineId);

    /**
     * 更新步骤状态
     */
    @Update("UPDATE logstash_task_machine_step SET status = #{status}, update_time = NOW() " +
            "WHERE task_id = #{taskId} AND machine_id = #{machineId} AND step_id = #{stepId}")
    void updateStatus(@Param("taskId") String taskId, @Param("machineId") Long machineId,
            @Param("stepId") String stepId, @Param("status") String status);

    /**
     * 更新步骤开始时间
     */
    @Update("UPDATE logstash_task_machine_step SET start_time = #{startTime}, update_time = NOW() " +
            "WHERE task_id = #{taskId} AND machine_id = #{machineId} AND step_id = #{stepId}")
    void updateStartTime(@Param("taskId") String taskId, @Param("machineId") Long machineId,
            @Param("stepId") String stepId, @Param("startTime") LocalDateTime startTime);

    /**
     * 更新步骤结束时间
     */
    @Update("UPDATE logstash_task_machine_step SET end_time = #{endTime}, update_time = NOW() " +
            "WHERE task_id = #{taskId} AND machine_id = #{machineId} AND step_id = #{stepId}")
    void updateEndTime(@Param("taskId") String taskId, @Param("machineId") Long machineId,
            @Param("stepId") String stepId, @Param("endTime") LocalDateTime endTime);

    /**
     * 更新步骤错误信息
     */
    @Update("UPDATE logstash_task_machine_step SET error_message = #{errorMessage}, update_time = NOW() " +
            "WHERE task_id = #{taskId} AND machine_id = #{machineId} AND step_id = #{stepId}")
    void updateErrorMessage(@Param("taskId") String taskId, @Param("machineId") Long machineId,
            @Param("stepId") String stepId, @Param("errorMessage") String errorMessage);

    /**
     * 根据任务ID统计各状态步骤数量
     */
    @Select("SELECT status, COUNT(*) as count FROM logstash_task_machine_step " +
            "WHERE task_id = #{taskId} GROUP BY status")
    List<Object[]> countStepStatusByTaskId(String taskId);
}