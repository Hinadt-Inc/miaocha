package com.hinadt.miaocha.application.logstash.state;

import com.hinadt.miaocha.application.logstash.command.LogstashCommand;
import com.hinadt.miaocha.application.logstash.command.LogstashCommandFactory;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineStep;
import com.hinadt.miaocha.application.logstash.enums.StepStatus;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 未启动状态处理器 */
@Component
public class NotStartedStateHandler extends AbstractLogstashMachineStateHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotStartedStateHandler.class);

    public NotStartedStateHandler(TaskService taskService, LogstashCommandFactory commandFactory) {
        super(taskService, commandFactory);
    }

    @Override
    public LogstashMachineState getState() {
        return LogstashMachineState.NOT_STARTED;
    }

    @Override
    public CompletableFuture<Boolean> handleStart(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("启动机器 [{}] 上的LogstashMachine实例 [{}]", machineId, logstashMachineId);

        CompletableFuture<Boolean> result = CompletableFuture.completedFuture(true);

        // 1. 启动Logstash进程
        result =
                result.thenCompose(
                        success -> {
                            if (!success) return CompletableFuture.completedFuture(false);

                            taskService.updateStepStatus(
                                    taskId,
                                    logstashMachineId,
                                    LogstashMachineStep.START_PROCESS.getId(),
                                    StepStatus.RUNNING);

                            LogstashCommand startCommand =
                                    commandFactory.startProcessCommand(logstashMachineId);

                            return startCommand
                                    .execute(machineInfo)
                                    .thenApply(
                                            startSuccess -> {
                                                StepStatus stepStatus =
                                                        startSuccess
                                                                ? StepStatus.COMPLETED
                                                                : StepStatus.FAILED;
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.START_PROCESS.getId(),
                                                        stepStatus);
                                                return startSuccess;
                                            });
                        });

        // 2. 验证进程状态
        result =
                result.thenCompose(
                        success -> {
                            if (!success) return CompletableFuture.completedFuture(false);

                            taskService.updateStepStatus(
                                    taskId,
                                    logstashMachineId,
                                    LogstashMachineStep.VERIFY_PROCESS.getId(),
                                    StepStatus.RUNNING);

                            LogstashCommand verifyCommand =
                                    commandFactory.verifyProcessCommand(logstashMachineId);

                            return verifyCommand
                                    .execute(machineInfo)
                                    .thenApply(
                                            verifySuccess -> {
                                                StepStatus stepStatus =
                                                        verifySuccess
                                                                ? StepStatus.COMPLETED
                                                                : StepStatus.FAILED;
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.VERIFY_PROCESS.getId(),
                                                        stepStatus);
                                                return verifySuccess;
                                            });
                        });

        return result;
    }

    @Override
    public CompletableFuture<Boolean> handleUpdateConfig(
            LogstashMachine logstashMachine,
            String configContent,
            String jvmOptions,
            String logstashYml,
            MachineInfo machineInfo,
            String taskId) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("更新机器 [{}] 上的LogstashMachine实例 [{}] 配置", machineId, logstashMachineId);

        // 更新相关步骤的状态为运行中
        if (configContent != null) {
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.UPDATE_MAIN_CONFIG.getId(),
                    StepStatus.RUNNING);
        }
        if (jvmOptions != null) {
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.UPDATE_JVM_CONFIG.getId(),
                    StepStatus.RUNNING);
        }
        if (logstashYml != null) {
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.UPDATE_SYSTEM_CONFIG.getId(),
                    StepStatus.RUNNING);
        }

        // 创建一个命令来同时更新所有配置
        LogstashCommand updateCommand =
                commandFactory.updateConfigCommand(
                        logstashMachineId, configContent, jvmOptions, logstashYml);

        // 执行更新命令
        return updateCommand
                .execute(machineInfo)
                .thenApply(
                        success -> {
                            // 更新所有相关步骤的状态
                            StepStatus status = success ? StepStatus.COMPLETED : StepStatus.FAILED;
                            String errorMessage = success ? null : "配置更新失败";

                            if (configContent != null) {
                                taskService.updateStepStatus(
                                        taskId,
                                        logstashMachineId,
                                        LogstashMachineStep.UPDATE_MAIN_CONFIG.getId(),
                                        status,
                                        errorMessage);
                            }
                            if (jvmOptions != null) {
                                taskService.updateStepStatus(
                                        taskId,
                                        logstashMachineId,
                                        LogstashMachineStep.UPDATE_JVM_CONFIG.getId(),
                                        status,
                                        errorMessage);
                            }
                            if (logstashYml != null) {
                                taskService.updateStepStatus(
                                        taskId,
                                        logstashMachineId,
                                        LogstashMachineStep.UPDATE_SYSTEM_CONFIG.getId(),
                                        status,
                                        errorMessage);
                            }

                            if (!success) {
                                throw new RuntimeException("配置更新失败");
                            }

                            return success;
                        })
                .exceptionally(
                        ex -> {
                            // 处理异常情况，不记录详细日志（由外层处理）
                            String errorMessage = ex.getMessage();

                            if (configContent != null) {
                                taskService.updateStepStatus(
                                        taskId,
                                        logstashMachineId,
                                        LogstashMachineStep.UPDATE_MAIN_CONFIG.getId(),
                                        StepStatus.FAILED,
                                        errorMessage);
                            }
                            if (jvmOptions != null) {
                                taskService.updateStepStatus(
                                        taskId,
                                        logstashMachineId,
                                        LogstashMachineStep.UPDATE_JVM_CONFIG.getId(),
                                        StepStatus.FAILED,
                                        errorMessage);
                            }
                            if (logstashYml != null) {
                                taskService.updateStepStatus(
                                        taskId,
                                        logstashMachineId,
                                        LogstashMachineStep.UPDATE_SYSTEM_CONFIG.getId(),
                                        StepStatus.FAILED,
                                        errorMessage);
                            }

                            // 重新抛出异常，确保异常传递到外层
                            if (ex instanceof RuntimeException) {
                                throw (RuntimeException) ex;
                            } else {
                                throw new RuntimeException(ex.getMessage(), ex);
                            }
                        });
    }

    @Override
    public CompletableFuture<Boolean> handleRefreshConfig(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("刷新机器 [{}] 上的LogstashMachine实例 [{}] 配置", machineId, logstashMachineId);

        taskService.updateStepStatus(
                taskId,
                logstashMachineId,
                LogstashMachineStep.REFRESH_CONFIG.getId(),
                StepStatus.RUNNING);

        // 使用支持所有配置类型的刷新命令
        // 传递null将从数据库中获取最新配置
        LogstashCommand refreshConfigCommand =
                commandFactory.refreshConfigCommand(logstashMachineId);

        return refreshConfigCommand
                .execute(machineInfo)
                .thenApply(
                        success -> {
                            StepStatus status = success ? StepStatus.COMPLETED : StepStatus.FAILED;
                            String errorMessage = success ? null : "刷新配置失败";
                            taskService.updateStepStatus(
                                    taskId,
                                    logstashMachineId,
                                    LogstashMachineStep.REFRESH_CONFIG.getId(),
                                    status,
                                    errorMessage);

                            if (!success) {
                                throw new RuntimeException("刷新配置失败");
                            }

                            return success;
                        })
                .exceptionally(
                        ex -> {
                            // 更新任务状态为失败，不记录详细日志（由外层处理）
                            taskService.updateStepStatus(
                                    taskId,
                                    logstashMachineId,
                                    LogstashMachineStep.REFRESH_CONFIG.getId(),
                                    StepStatus.FAILED,
                                    ex.getMessage());

                            // 重新抛出异常，确保异常传递到外层
                            if (ex instanceof RuntimeException) {
                                throw (RuntimeException) ex;
                            } else {
                                throw new RuntimeException(ex.getMessage(), ex);
                            }
                        });
    }

    @Override
    public boolean canStart() {
        return true;
    }

    @Override
    public boolean canUpdateConfig() {
        return true;
    }

    @Override
    public boolean canRefreshConfig() {
        return true;
    }

    @Override
    public LogstashMachineState getNextState(OperationType operationType, boolean success) {
        if (operationType == OperationType.START) {
            return success ? LogstashMachineState.RUNNING : LogstashMachineState.START_FAILED;
        }
        return getState(); // 默认保持当前状态
    }
}
