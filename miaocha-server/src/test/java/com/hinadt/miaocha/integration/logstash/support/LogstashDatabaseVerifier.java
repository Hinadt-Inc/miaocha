package com.hinadt.miaocha.integration.logstash.support;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineStep;
import com.hinadt.miaocha.application.logstash.enums.StepStatus;
import com.hinadt.miaocha.application.logstash.enums.TaskOperationType;
import com.hinadt.miaocha.application.logstash.enums.TaskStatus;
import com.hinadt.miaocha.domain.entity.LogstashTask;
import com.hinadt.miaocha.domain.entity.LogstashTaskMachineStep;
import com.hinadt.miaocha.infrastructure.mapper.LogstashTaskMachineStepMapper;
import com.hinadt.miaocha.infrastructure.mapper.LogstashTaskMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Logstash数据库验证器
 *
 * <p>基于真实代码流程的深度验证： 1.
 * 验证初始化任务包含5个步骤：DELETE_DIR(隐含)、CREATE_REMOTE_DIR、UPLOAD_PACKAGE、EXTRACT_PACKAGE、CREATE_CONFIG、MODIFY_CONFIG
 * 2. 验证启动任务包含2个步骤：START_PROCESS、VERIFY_PROCESS 3. 验证停止任务包含1个步骤：STOP_PROCESS 4. 验证任务操作类型与步骤的对应关系
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogstashDatabaseVerifier {

    private final LogstashTaskMapper logstashTaskMapper;
    private final LogstashTaskMachineStepMapper stepMapper;

    // 基于真实代码的步骤定义
    private static final List<String> INITIALIZE_STEP_IDS =
            Arrays.asList(
                    LogstashMachineStep.CREATE_REMOTE_DIR.getId(),
                    LogstashMachineStep.UPLOAD_PACKAGE.getId(),
                    LogstashMachineStep.EXTRACT_PACKAGE.getId(),
                    LogstashMachineStep.CREATE_CONFIG.getId(),
                    LogstashMachineStep.MODIFY_CONFIG.getId());

    private static final List<String> START_STEP_IDS =
            Arrays.asList(
                    LogstashMachineStep.START_PROCESS.getId(),
                    LogstashMachineStep.VERIFY_PROCESS.getId());

    private static final List<String> STOP_STEP_IDS =
            Arrays.asList(LogstashMachineStep.STOP_PROCESS.getId());

    /** 验证初始化任务和步骤的完整性 基于LogstashProcessDeployServiceImpl.initializeInstances的真实实现 */
    public boolean verifyInitializationTaskAndSteps(Long instanceId) {
        log.info("验证实例 {} 的初始化任务和步骤完整性", instanceId);

        // 1. 查找初始化任务
        Optional<LogstashTask> initTaskOpt =
                findTaskByOperationType(instanceId, TaskOperationType.INITIALIZE);
        if (!initTaskOpt.isPresent()) {
            log.error("实例 {} 没有找到初始化任务", instanceId);
            return false;
        }

        LogstashTask initTask = initTaskOpt.get();
        log.info("找到初始化任务: {} - {}", initTask.getId(), initTask.getName());

        // 2. 验证任务状态应该是COMPLETED
        if (!TaskStatus.COMPLETED.name().equals(initTask.getStatus())) {
            log.error("初始化任务状态不正确，期望: {}, 实际: {}", TaskStatus.COMPLETED, initTask.getStatus());
            return false;
        }

        // 3. 验证初始化步骤的数量和内容
        List<LogstashTaskMachineStep> initSteps = stepMapper.findByTaskId(initTask.getId());
        if (initSteps.size() != INITIALIZE_STEP_IDS.size()) {
            log.error("初始化步骤数量不正确，期望: {}, 实际: {}", INITIALIZE_STEP_IDS.size(), initSteps.size());
            logStepDetails(initSteps);
            return false;
        }

        // 4. 验证每个初始化步骤都存在且完成
        for (String expectedStepId : INITIALIZE_STEP_IDS) {
            boolean stepFound =
                    initSteps.stream()
                            .anyMatch(
                                    step ->
                                            expectedStepId.equals(step.getStepId())
                                                    && StepStatus.COMPLETED
                                                            .name()
                                                            .equals(step.getStatus())
                                                    && instanceId.equals(
                                                            step.getLogstashMachineId()));

            if (!stepFound) {
                log.error("初始化步骤 {} 未找到或未完成", expectedStepId);
                logStepDetails(initSteps);
                return false;
            }
        }

        log.info("✅ 实例 {} 初始化任务和步骤验证通过", instanceId);
        return true;
    }

    /** 验证启动任务和步骤的完整性 基于LogstashProcessDeployServiceImpl.startInstances的真实实现 */
    public boolean verifyStartTaskAndSteps(Long instanceId) {
        log.info("验证实例 {} 的启动任务和步骤完整性", instanceId);

        // 1. 查找启动任务
        Optional<LogstashTask> startTaskOpt =
                findTaskByOperationType(instanceId, TaskOperationType.START);
        if (!startTaskOpt.isPresent()) {
            log.error("实例 {} 没有找到启动任务", instanceId);
            return false;
        }

        LogstashTask startTask = startTaskOpt.get();
        log.info("找到启动任务: {} - {}", startTask.getId(), startTask.getName());

        // 2. 验证任务状态应该是COMPLETED
        if (!TaskStatus.COMPLETED.name().equals(startTask.getStatus())) {
            log.error("启动任务状态不正确，期望: {}, 实际: {}", TaskStatus.COMPLETED, startTask.getStatus());
            return false;
        }

        // 3. 验证启动步骤的数量和内容
        List<LogstashTaskMachineStep> startSteps = stepMapper.findByTaskId(startTask.getId());
        if (startSteps.size() != START_STEP_IDS.size()) {
            log.error("启动步骤数量不正确，期望: {}, 实际: {}", START_STEP_IDS.size(), startSteps.size());
            logStepDetails(startSteps);
            return false;
        }

        // 4. 验证每个启动步骤都存在且完成
        for (String expectedStepId : START_STEP_IDS) {
            boolean stepFound =
                    startSteps.stream()
                            .anyMatch(
                                    step ->
                                            expectedStepId.equals(step.getStepId())
                                                    && StepStatus.COMPLETED
                                                            .name()
                                                            .equals(step.getStatus())
                                                    && instanceId.equals(
                                                            step.getLogstashMachineId()));

            if (!stepFound) {
                log.error("启动步骤 {} 未找到或未完成", expectedStepId);
                logStepDetails(startSteps);
                return false;
            }
        }

        log.info("✅ 实例 {} 启动任务和步骤验证通过", instanceId);
        return true;
    }

    /** 验证停止任务和步骤的完整性 */
    public boolean verifyStopTaskAndSteps(Long instanceId) {
        log.info("验证实例 {} 的停止任务和步骤完整性", instanceId);

        // 1. 查找停止任务
        Optional<LogstashTask> stopTaskOpt =
                findTaskByOperationType(instanceId, TaskOperationType.STOP);
        if (!stopTaskOpt.isPresent()) {
            log.error("实例 {} 没有找到停止任务", instanceId);
            return false;
        }

        LogstashTask stopTask = stopTaskOpt.get();
        log.info("找到停止任务: {} - {}", stopTask.getId(), stopTask.getName());

        // 2. 验证任务状态应该是COMPLETED
        if (!TaskStatus.COMPLETED.name().equals(stopTask.getStatus())) {
            log.error("停止任务状态不正确，期望: {}, 实际: {}", TaskStatus.COMPLETED, stopTask.getStatus());
            return false;
        }

        // 3. 验证停止步骤的数量和内容
        List<LogstashTaskMachineStep> stopSteps = stepMapper.findByTaskId(stopTask.getId());
        if (stopSteps.size() != STOP_STEP_IDS.size()) {
            log.error("停止步骤数量不正确，期望: {}, 实际: {}", STOP_STEP_IDS.size(), stopSteps.size());
            logStepDetails(stopSteps);
            return false;
        }

        // 4. 验证停止步骤存在且完成
        for (String expectedStepId : STOP_STEP_IDS) {
            boolean stepFound =
                    stopSteps.stream()
                            .anyMatch(
                                    step ->
                                            expectedStepId.equals(step.getStepId())
                                                    && StepStatus.COMPLETED
                                                            .name()
                                                            .equals(step.getStatus())
                                                    && instanceId.equals(
                                                            step.getLogstashMachineId()));

            if (!stepFound) {
                log.error("停止步骤 {} 未找到或未完成", expectedStepId);
                logStepDetails(stopSteps);
                return false;
            }
        }

        log.info("✅ 实例 {} 停止任务和步骤验证通过", instanceId);
        return true;
    }

    /** 验证任务的时间戳字段正确设置 */
    public boolean verifyTaskTimestamps(Long instanceId, TaskOperationType operationType) {
        Optional<LogstashTask> taskOpt = findTaskByOperationType(instanceId, operationType);
        if (!taskOpt.isPresent()) {
            log.error("实例 {} 没有找到 {} 任务", instanceId, operationType);
            return false;
        }

        LogstashTask task = taskOpt.get();

        // 验证基本时间戳
        if (task.getCreateTime() == null) {
            log.error("任务 {} 缺少创建时间", task.getId());
            return false;
        }

        if (task.getUpdateTime() == null) {
            log.error("任务 {} 缺少更新时间", task.getId());
            return false;
        }

        // 对于已完成的任务，应该有开始和结束时间
        if (TaskStatus.COMPLETED.name().equals(task.getStatus())) {
            if (task.getStartTime() == null) {
                log.error("已完成任务 {} 缺少开始时间", task.getId());
                return false;
            }

            if (task.getEndTime() == null) {
                log.error("已完成任务 {} 缺少结束时间", task.getId());
                return false;
            }

            // 验证时间逻辑关系
            if (task.getStartTime().isAfter(task.getEndTime())) {
                log.error("任务 {} 开始时间晚于结束时间", task.getId());
                return false;
            }
        }

        log.info("✅ 任务 {} 时间戳验证通过", task.getId());
        return true;
    }

    /** 验证步骤的时间戳字段正确设置 */
    public boolean verifyStepTimestamps(Long instanceId, TaskOperationType operationType) {
        Optional<LogstashTask> taskOpt = findTaskByOperationType(instanceId, operationType);
        if (!taskOpt.isPresent()) {
            return false;
        }

        List<LogstashTaskMachineStep> steps = stepMapper.findByTaskId(taskOpt.get().getId());

        for (LogstashTaskMachineStep step : steps) {
            // 验证基本时间戳
            if (step.getCreateTime() == null) {
                log.error("步骤 {} 缺少创建时间", step.getId());
                return false;
            }

            if (step.getUpdateTime() == null) {
                log.error("步骤 {} 缺少更新时间", step.getId());
                return false;
            }

            // 对于已完成的步骤，验证时间逻辑
            if (StepStatus.COMPLETED.name().equals(step.getStatus())) {
                // 注意：根据代码实现，不是所有步骤都会设置开始/结束时间，这取决于具体实现
                log.debug(
                        "步骤 {} - stepId: {}, 开始时间: {}, 结束时间: {}",
                        step.getId(),
                        step.getStepId(),
                        step.getStartTime(),
                        step.getEndTime());
            }
        }

        log.info("✅ {} 操作的步骤时间戳验证通过", operationType);
        return true;
    }

    /** 验证任务和步骤记录被正确清理 */
    public boolean verifyTaskAndStepRecordsCleanedUp(Long instanceId) {
        List<String> taskIds = logstashTaskMapper.findTaskIdsByLogstashMachineId(instanceId);
        if (!taskIds.isEmpty()) {
            log.error("实例 {} 的任务记录未被清理，仍存在 {} 个任务", instanceId, taskIds.size());
            return false;
        }

        List<LogstashTaskMachineStep> steps = stepMapper.findByLogstashMachineId(instanceId);
        if (!steps.isEmpty()) {
            log.error("实例 {} 的步骤记录未被清理，仍存在 {} 个步骤", instanceId, steps.size());
            return false;
        }

        log.info("✅ 实例 {} 的任务和步骤记录已被正确清理", instanceId);
        return true;
    }

    /** 验证任务和步骤记录被正确保留 */
    public boolean verifyTaskAndStepRecordsPreserved(Long instanceId) {
        List<String> taskIds = logstashTaskMapper.findTaskIdsByLogstashMachineId(instanceId);
        if (taskIds.isEmpty()) {
            log.error("实例 {} 的任务记录被意外清理，预期应该保留", instanceId);
            return false;
        }

        List<LogstashTaskMachineStep> steps = stepMapper.findByLogstashMachineId(instanceId);
        if (steps.isEmpty()) {
            log.error("实例 {} 的步骤记录被意外清理，预期应该保留", instanceId);
            return false;
        }

        log.info(
                "✅ 实例 {} 的任务和步骤记录已被正确保留 - 任务数: {}, 步骤数: {}",
                instanceId,
                taskIds.size(),
                steps.size());
        return true;
    }

    /** 获取实例的详细任务和步骤信息（用于调试和故障排查） */
    public void logInstanceTasksAndStepsDetails(Long instanceId) {
        log.info("=== 实例 {} 的任务和步骤详情 ===", instanceId);

        List<String> taskIds = logstashTaskMapper.findTaskIdsByLogstashMachineId(instanceId);
        log.info("任务数量: {}", taskIds.size());

        for (String taskId : taskIds) {
            Optional<LogstashTask> taskOpt = logstashTaskMapper.findById(taskId);
            if (taskOpt.isPresent()) {
                LogstashTask task = taskOpt.get();
                log.info(
                        "任务 {}: 名称={}, 状态={}, 操作类型={}, 创建时间={}, 开始时间={}, 结束时间={}",
                        taskId,
                        task.getName(),
                        task.getStatus(),
                        task.getOperationType(),
                        task.getCreateTime(),
                        task.getStartTime(),
                        task.getEndTime());

                // 打印该任务的所有步骤
                List<LogstashTaskMachineStep> steps = stepMapper.findByTaskId(taskId);
                log.info("  任务 {} 包含 {} 个步骤", taskId, steps.size());
                for (LogstashTaskMachineStep step : steps) {
                    log.info(
                            "    步骤 {}: stepId={}, stepName={}, 状态={}, 实例ID={}, 创建时间={}, 开始时间={},"
                                    + " 结束时间={}",
                            step.getId(),
                            step.getStepId(),
                            step.getStepName(),
                            step.getStatus(),
                            step.getLogstashMachineId(),
                            step.getCreateTime(),
                            step.getStartTime(),
                            step.getEndTime());
                }
            }
        }

        log.info("=== 实例 {} 详情结束 ===", instanceId);
    }

    // ==================== 私有辅助方法 ====================

    /** 根据操作类型查找任务 */
    private Optional<LogstashTask> findTaskByOperationType(
            Long instanceId, TaskOperationType operationType) {
        List<String> taskIds = logstashTaskMapper.findTaskIdsByLogstashMachineId(instanceId);

        for (String taskId : taskIds) {
            Optional<LogstashTask> taskOpt = logstashTaskMapper.findById(taskId);
            if (taskOpt.isPresent()) {
                LogstashTask task = taskOpt.get();
                if (operationType.name().equals(task.getOperationType())) {
                    return Optional.of(task);
                }
            }
        }

        return Optional.empty();
    }

    /** 打印步骤详情（用于调试） */
    private void logStepDetails(List<LogstashTaskMachineStep> steps) {
        log.info("步骤详情：");
        for (LogstashTaskMachineStep step : steps) {
            log.info(
                    "  步骤ID: {}, 步骤名称: {}, 状态: {}, 实例ID: {}",
                    step.getStepId(),
                    step.getStepName(),
                    step.getStatus(),
                    step.getLogstashMachineId());
        }
    }
}
