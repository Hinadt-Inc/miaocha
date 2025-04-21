package com.hina.log.service.task;

import com.hina.log.dto.TaskDetailDTO;
import com.hina.log.entity.Machine;
import com.hina.log.enums.StepStatus;
import com.hina.log.enums.TaskOperationType;
import com.hina.log.enums.TaskStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 任务服务接口
 */
public interface TaskService {

    /**
     * 创建新任务
     *
     * @param processId     进程ID
     * @param name          任务名称
     * @param description   任务描述
     * @param operationType 操作类型
     * @param machines      关联机器列表
     * @param stepIds       步骤ID列表
     * @return 任务ID
     */
    String createTask(Long processId, String name, String description,
            TaskOperationType operationType, List<Machine> machines,
            List<String> stepIds);

    /**
     * 获取任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    Optional<TaskDetailDTO> getTaskDetail(String taskId);

    /**
     * 获取进程最新任务详情
     *
     * @param processId 进程ID
     * @return 任务详情
     */
    Optional<TaskDetailDTO> getLatestProcessTaskDetail(Long processId);

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
     * @param taskId    任务ID
     * @param machineId 机器ID
     * @param stepId    步骤ID
     * @param status    步骤状态
     */
    void updateStepStatus(String taskId, Long machineId, String stepId, StepStatus status);

    /**
     * 更新步骤错误信息
     *
     * @param taskId       任务ID
     * @param machineId    机器ID
     * @param stepId       步骤ID
     * @param errorMessage 错误信息
     */
    void updateStepErrorMessage(String taskId, Long machineId, String stepId, String errorMessage);

    /**
     * 执行任务
     *
     * @param taskId   任务ID
     * @param action   任务执行操作
     * @param callback 执行完成回调
     */
    void executeAsync(String taskId, Runnable action, Runnable callback);

    /**
     * 获取任务摘要信息
     *
     * @param taskId 任务ID
     * @return 任务摘要信息
     */
    String getTaskSummary(String taskId);

    /**
     * 获取任务机器步骤执行状态统计
     *
     * @param taskId 任务ID
     * @return 各机器步骤执行状态统计
     */
    Map<Long, Map<String, Integer>> getTaskMachineStepStatusStats(String taskId);

    /**
     * 重置进程关联的任务状态
     * 将进程ID关联的所有任务及步骤状态设置为未执行状态，便于重试
     * 
     * @deprecated 任务应当被视为历史记录，不应重置状态，新的操作应创建新的任务记录
     * @param processId 进程ID
     * @return 是否成功重置
     */
    @Deprecated
    boolean resetTasksForBusiness(Long processId);

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
}