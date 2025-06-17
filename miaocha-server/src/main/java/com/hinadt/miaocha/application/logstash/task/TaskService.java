package com.hinadt.miaocha.application.logstash.task;

import com.hinadt.miaocha.application.logstash.enums.StepStatus;
import com.hinadt.miaocha.application.logstash.enums.TaskOperationType;
import com.hinadt.miaocha.application.logstash.enums.TaskStatus;
import com.hinadt.miaocha.domain.dto.logstash.TaskDetailDTO;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 任务服务接口 - 重构支持多实例，基于logstashMachineId */
public interface TaskService {

    /**
     * 创建新任务（全局任务，不针对特定实例）
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
     * 创建新任务（针对单一实例）
     *
     * @param logstashMachineId LogstashMachine实例ID
     * @param name 任务名称
     * @param description 任务描述
     * @param operationType 操作类型
     * @param stepIds 步骤ID列表
     * @return 任务ID
     */
    String createInstanceTask(
            Long logstashMachineId,
            String name,
            String description,
            TaskOperationType operationType,
            List<String> stepIds);

    /**
     * 创建新任务（批量创建多个实例任务）
     *
     * @param logstashMachines LogstashMachine实例列表
     * @param name 任务名称
     * @param description 任务描述
     * @param operationType 操作类型
     * @param stepIds 步骤ID列表
     * @return LogstashMachine实例ID到任务ID的映射
     */
    Map<Long, String> createInstanceTasks(
            List<LogstashMachine> logstashMachines,
            String name,
            String description,
            TaskOperationType operationType,
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
     * 获取与实例关联的所有任务ID
     *
     * @param logstashMachineId LogstashMachine实例ID
     * @return 任务ID列表
     */
    List<String> getAllInstanceTaskIds(Long logstashMachineId);

    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 任务状态
     */
    void updateTaskStatus(String taskId, TaskStatus status);

    /**
     * 更新步骤状态 - 基于logstashMachineId
     *
     * @param taskId 任务ID
     * @param logstashMachineId LogstashMachine实例ID
     * @param stepId 步骤ID
     * @param status 步骤状态
     */
    void updateStepStatus(String taskId, Long logstashMachineId, String stepId, StepStatus status);

    /**
     * 更新步骤状态，同时设置错误信息 - 基于logstashMachineId
     *
     * @param taskId 任务ID
     * @param logstashMachineId LogstashMachine实例ID
     * @param stepId 步骤ID
     * @param status 步骤状态
     * @param errorMessage 错误信息，如果状态是成功的则可以为null
     */
    void updateStepStatus(
            String taskId,
            Long logstashMachineId,
            String stepId,
            StepStatus status,
            String errorMessage);

    /**
     * 执行任务
     *
     * @param taskId 任务ID
     * @param action 任务执行操作
     * @param callback 执行完成回调
     */
    void executeAsync(String taskId, Runnable action, Runnable callback);

    /**
     * 重置任务所有步骤状态
     *
     * @param taskId 任务ID
     * @param newStatus 新状态
     */
    void resetStepStatuses(String taskId, StepStatus newStatus);

    /**
     * 删除任务及其所有相关步骤
     *
     * @param taskId 任务ID
     */
    void deleteTask(String taskId);
}
