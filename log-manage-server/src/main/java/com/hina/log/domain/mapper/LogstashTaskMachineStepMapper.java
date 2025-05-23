package com.hina.log.domain.mapper;

import com.hina.log.domain.entity.LogstashTaskMachineStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
        void insert(LogstashTaskMachineStep step);

        /**
         * 批量插入步骤记录
         */
        void batchInsert(@Param("steps") List<LogstashTaskMachineStep> steps);

        /**
         * 根据任务ID查询所有步骤
         */
        List<LogstashTaskMachineStep> findByTaskId(String taskId);

        /**
         * 根据任务ID和机器ID查询所有步骤
         */
        List<LogstashTaskMachineStep> findByTaskIdAndMachineId(@Param("taskId") String taskId,
                        @Param("machineId") Long machineId);

        /**
         * 更新步骤状态
         */
        void updateStatus(@Param("taskId") String taskId, @Param("machineId") Long machineId,
                        @Param("stepId") String stepId, @Param("status") String status);

        /**
         * 更新步骤开始时间
         */
        void updateStartTime(@Param("taskId") String taskId, @Param("machineId") Long machineId,
                        @Param("stepId") String stepId, @Param("startTime") LocalDateTime startTime);

        /**
         * 更新步骤结束时间
         */
        void updateEndTime(@Param("taskId") String taskId, @Param("machineId") Long machineId,
                        @Param("stepId") String stepId, @Param("endTime") LocalDateTime endTime);

        /**
         * 更新步骤错误信息
         */
        void updateErrorMessage(@Param("taskId") String taskId, @Param("machineId") Long machineId,
                        @Param("stepId") String stepId, @Param("errorMessage") String errorMessage);

        /**
         * 根据任务ID统计各状态步骤数量
         */
        List<Object[]> countStepStatusByTaskId(String taskId);

        /**
         * 重置任务所有步骤的状态
         *
         * @param taskId 任务ID
         * @param status 重置后的状态
         */
        void resetStepStatuses(@Param("taskId") String taskId, @Param("status") String status);

        /**
         * 清除任务所有步骤的错误信息
         *
         * @param taskId 任务ID
         */
        void clearStepErrorMessages(String taskId);

        /**
         * 删除任务所有步骤
         *
         * @param taskId 任务ID
         */
        void deleteByTaskId(String taskId);
}