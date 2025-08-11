package com.hinadt.miaocha.integration.logstash.support;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineStep;
import com.hinadt.miaocha.application.logstash.enums.StepStatus;
import com.hinadt.miaocha.application.logstash.enums.TaskStatus;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.infrastructure.mapper.LogstashMachineMapper;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Logstash实例状态验证器
 *
 * <p>职责： 1. 等待和验证实例状态转换 2. 验证任务和步骤的执行状态 3. 提供状态相关的断言方法
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogstashInstanceStateVerifier {

    private final LogstashMachineMapper logstashMachineMapper;
    private final TaskService taskService;

    /** 等待实例状态变化到目标状态之一 */
    public boolean waitForInstanceState(
            Long instanceId,
            List<LogstashMachineState> targetStates,
            long timeout,
            TimeUnit timeUnit)
            throws InterruptedException {
        long timeoutMs = timeUnit.toMillis(timeout);
        long startTime = System.currentTimeMillis();
        long pollingInterval = 1000; // 1秒轮询间隔

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            LogstashMachine instance = logstashMachineMapper.selectById(instanceId);
            if (instance != null) {
                LogstashMachineState currentState =
                        LogstashMachineState.valueOf(instance.getState());
                if (targetStates.contains(currentState)) {
                    log.info("实例 {} 状态变更为: {}", instanceId, currentState);
                    return true;
                }
                log.debug(
                        "等待实例 {} 状态变化，当前状态: {}, 目标状态: {}", instanceId, currentState, targetStates);
            }
            Thread.sleep(pollingInterval);
        }

        LogstashMachine instance = logstashMachineMapper.selectById(instanceId);
        LogstashMachineState finalState =
                instance != null ? LogstashMachineState.valueOf(instance.getState()) : null;
        log.warn("等待实例状态超时，实例: {}, 最终状态: {}, 期望状态: {}", instanceId, finalState, targetStates);
        return false;
    }

    /** 验证实例当前状态 */
    public LogstashMachineState getCurrentState(Long instanceId) {
        LogstashMachine instance = logstashMachineMapper.selectById(instanceId);
        return instance != null ? LogstashMachineState.valueOf(instance.getState()) : null;
    }

    /** 验证初始化相关步骤都已完成 */
    public boolean verifyInitializationStepsCompleted(Long instanceId) {
        return verifyStepCompleted(instanceId, LogstashMachineStep.CREATE_REMOTE_DIR)
                && verifyStepCompleted(instanceId, LogstashMachineStep.UPLOAD_PACKAGE)
                && verifyStepCompleted(instanceId, LogstashMachineStep.EXTRACT_PACKAGE)
                && verifyStepCompleted(instanceId, LogstashMachineStep.CREATE_CONFIG);
    }

    /** 验证启动相关步骤都已完成 */
    public boolean verifyStartupStepsCompleted(Long instanceId) {
        return verifyStepCompleted(instanceId, LogstashMachineStep.START_PROCESS)
                && verifyStepCompleted(instanceId, LogstashMachineStep.VERIFY_PROCESS);
    }

    /** 验证特定步骤已完成 */
    public boolean verifyStepCompleted(Long instanceId, LogstashMachineStep step) {
        List<String> taskIds = taskService.getAllInstanceTaskIds(instanceId);

        return taskIds.stream()
                .anyMatch(
                        taskId -> {
                            var taskDetail = taskService.getTaskDetail(taskId);
                            if (taskDetail.isPresent()) {
                                var instanceSteps = taskDetail.get().getInstanceSteps();
                                return instanceSteps.values().stream()
                                        .anyMatch(
                                                steps ->
                                                        steps.stream()
                                                                .anyMatch(
                                                                        stepDto ->
                                                                                step.getId()
                                                                                                .equals(
                                                                                                        stepDto
                                                                                                                .getStepId())
                                                                                        && StepStatus
                                                                                                .COMPLETED
                                                                                                .name()
                                                                                                .equals(
                                                                                                        stepDto
                                                                                                                .getStatus())));
                            }
                            return false;
                        });
    }

    /** 验证是否有成功完成的初始化任务 */
    public boolean hasCompletedInitializationTask(Long instanceId) {
        List<String> taskIds = taskService.getAllInstanceTaskIds(instanceId);

        return taskIds.stream()
                .anyMatch(
                        taskId -> {
                            var taskDetail = taskService.getTaskDetail(taskId);
                            return taskDetail.isPresent()
                                    && TaskStatus.COMPLETED
                                            .name()
                                            .equals(taskDetail.get().getStatus());
                        });
    }

    /** 验证实例的PID是否已清理 */
    public boolean isPidCleared(Long instanceId) {
        LogstashMachine instance = logstashMachineMapper.selectById(instanceId);
        return instance != null
                && (instance.getProcessPid() == null || instance.getProcessPid().isEmpty());
    }

    /** 获取实例的当前PID */
    public String getCurrentPid(Long instanceId) {
        LogstashMachine instance = logstashMachineMapper.selectById(instanceId);
        return instance != null ? instance.getProcessPid() : null;
    }
}
