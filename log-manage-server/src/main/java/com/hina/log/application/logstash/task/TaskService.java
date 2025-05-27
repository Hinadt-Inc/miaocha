package com.hina.log.application.logstash.task;

import com.hina.log.application.logstash.enums.StepStatus;
import com.hina.log.application.logstash.enums.TaskOperationType;
import com.hina.log.application.logstash.enums.TaskStatus;
import com.hina.log.domain.dto.logstash.TaskDetailDTO;
import com.hina.log.domain.dto.logstash.TaskStepsGroupDTO;
import com.hina.log.domain.entity.MachineInfo;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 任务服务接口 */
public interface TaskService {

    /**
     * 创建新任务（全局任务，不针对特定机器）
     *
     * @param processId 进程ID
     * @param name 任务名称
     * @param description 任务描述
     * @param operationType 操作类型
     * @param stepIds 步骤ID列表
     * @return 任务ID
     */
    String createGlobalTask(
            Long processId,
            String name,
            String description,
            TaskOperationType operationType,
            List<String> stepIds);

    /**
     * 创建新任务（针对单一机器）
     *
     * @param processId 进程ID
     * @param machineId 机器ID
     * @param name 任务名称
     * @param description 任务描述
     * @param operationType 操作类型
     * @param stepIds 步骤ID列表
     * @return 任务ID
     */
    String createMachineTask(
            Long processId,
            Long machineId,
            String name,
            String description,
            TaskOperationType operationType,
            List<String> stepIds);

    /**
     * 创建新任务（批量创建多个机器任务）
     *
     * @param processId 进程ID
     * @param name 任务名称
     * @param description 任务描述
     * @param operationType 操作类型
     * @param machineInfos 关联机器列表
     * @param stepIds 步骤ID列表
     * @return 机器ID到任务ID的映射
     */
    Map<Long, String> createMachineTasks(
            Long processId,
            String name,
            String description,
            TaskOperationType operationType,
            List<MachineInfo> machineInfos,
            List<String> stepIds);

    /**
     * 获取任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    Optional<TaskDetailDTO> getTaskDetail(String taskId);

    /**
     * 获取与进程关联的所有任务ID
     *
     * @param processId 进程ID
     * @return 任务ID列表
     */
    List<String> getAllProcessTaskIds(Long processId);

    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 任务状态
     */
    void updateTaskStatus(String taskId, TaskStatus status);

    /**
     * 更新步骤状态
     *
     * @param taskId 任务ID
     * @param machineId 机器ID
     * @param stepId 步骤ID
     * @param status 步骤状态
     */
    void updateStepStatus(String taskId, Long machineId, String stepId, StepStatus status);

    /**
     * 更新步骤状态，同时设置错误信息
     *
     * @param taskId 任务ID
     * @param machineId 机器ID
     * @param stepId 步骤ID
     * @param status 步骤状态
     * @param errorMessage 错误信息，如果状态是成功的则可以为null
     */
    void updateStepStatus(
            String taskId, Long machineId, String stepId, StepStatus status, String errorMessage);

    /**
     * 执行任务
     *
     * @param taskId 任务ID
     * @param action 任务执行操作
     * @param callback 执行完成回调
     */
    void executeAsync(String taskId, Runnable action, Runnable callback);

    /**
     * 获取任务机器步骤执行状态统计
     *
     * @param taskId 任务ID
     * @return 各机器步骤执行状态统计 (机器名称 -> 状态统计)
     */
    Map<String, Map<String, Integer>> getTaskMachineStepStatusStats(String taskId);

    /**
     * 删除任务记录
     *
     * @param taskId 任务ID
     */
    void deleteTask(String taskId);

    /**
     * 删除任务步骤记录
     *
     * @param taskId 任务ID
     */
    void deleteTaskSteps(String taskId);

    /**
     * 获取任务步骤分组信息（按步骤分组）
     *
     * @param taskId 任务ID
     * @return 任务步骤分组信息
     */
    TaskStepsGroupDTO getTaskStepsGrouped(String taskId);

    /**
     * 获取指定机器上的进程最新任务详情
     *
     * @param processId 进程ID
     * @param machineId 机器ID
     * @return 任务详情
     */
    Optional<TaskDetailDTO> getLatestMachineTaskDetail(Long processId, Long machineId);

    /**
     * 获取与指定机器上的进程关联的所有任务ID
     *
     * @param processId 进程ID
     * @param machineId 机器ID
     * @return 任务ID列表
     */
    List<String> getAllMachineTaskIds(Long processId, Long machineId);

    /**
     * 重置任务所有步骤的状态
     *
     * @param taskId 任务ID
     * @param status 重置后的步骤状态
     */
    void resetStepStatuses(String taskId, StepStatus status);
}
