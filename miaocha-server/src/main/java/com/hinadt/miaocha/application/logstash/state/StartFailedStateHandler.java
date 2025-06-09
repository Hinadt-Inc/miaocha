package com.hinadt.miaocha.application.logstash.state;

import com.hinadt.miaocha.application.logstash.command.LogstashCommand;
import com.hinadt.miaocha.application.logstash.command.LogstashCommandFactory;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineStep;
import com.hinadt.miaocha.application.logstash.enums.StepStatus;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 启动失败状态处理器 */
@Component
public class StartFailedStateHandler extends AbstractLogstashMachineStateHandler {

    private static final Logger logger = LoggerFactory.getLogger(StartFailedStateHandler.class);

    public StartFailedStateHandler(TaskService taskService, LogstashCommandFactory commandFactory) {
        super(taskService, commandFactory);
    }

    @Override
    public LogstashMachineState getState() {
        return LogstashMachineState.START_FAILED;
    }

    @Override
    public CompletableFuture<Boolean> handleStart(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        Long processId = process.getId();
        Long machineId = machineInfo.getId();

        logger.info("重试启动机器 [{}] 上的Logstash进程 [{}]", machineId, processId);

        // 重置所有步骤的状态
        taskService.resetStepStatuses(taskId, StepStatus.PENDING);

        CompletableFuture<Boolean> result = CompletableFuture.completedFuture(true);

        // 1. 启动Logstash进程
        result =
                result.thenCompose(
                        success -> {
                            if (!success) return CompletableFuture.completedFuture(false);

                            taskService.updateStepStatus(
                                    taskId,
                                    machineId,
                                    LogstashMachineStep.START_PROCESS.getId(),
                                    StepStatus.RUNNING);
                            LogstashCommand startCommand =
                                    commandFactory.startProcessCommand(processId);

                            return startCommand
                                    .execute(machineInfo)
                                    .thenApply(
                                            startSuccess -> {
                                                StepStatus status =
                                                        startSuccess
                                                                ? StepStatus.COMPLETED
                                                                : StepStatus.FAILED;
                                                String errorMessage =
                                                        startSuccess ? null : "启动Logstash进程失败";
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        machineId,
                                                        LogstashMachineStep.START_PROCESS.getId(),
                                                        status,
                                                        errorMessage);

                                                if (!startSuccess) {
                                                    throw new RuntimeException("启动Logstash进程失败");
                                                }
                                                return startSuccess;
                                            })
                                    .exceptionally(
                                            ex -> {
                                                // 更新任务状态为失败，不记录详细日志（由外层处理）
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        machineId,
                                                        LogstashMachineStep.START_PROCESS.getId(),
                                                        StepStatus.FAILED,
                                                        ex.getMessage());

                                                // 重新抛出异常，确保异常传递到外层
                                                if (ex instanceof RuntimeException) {
                                                    throw (RuntimeException) ex;
                                                } else {
                                                    throw new RuntimeException(ex.getMessage(), ex);
                                                }
                                            });
                        });

        // 2. 验证进程状态
        result =
                result.thenCompose(
                        success -> {
                            if (!success) return CompletableFuture.completedFuture(false);

                            taskService.updateStepStatus(
                                    taskId,
                                    machineId,
                                    LogstashMachineStep.VERIFY_PROCESS.getId(),
                                    StepStatus.RUNNING);
                            LogstashCommand verifyCommand =
                                    commandFactory.verifyProcessCommand(processId);

                            return verifyCommand
                                    .execute(machineInfo)
                                    .thenApply(
                                            verifySuccess -> {
                                                StepStatus status =
                                                        verifySuccess
                                                                ? StepStatus.COMPLETED
                                                                : StepStatus.FAILED;
                                                String errorMessage =
                                                        verifySuccess
                                                                ? null
                                                                : "验证Logstash进程失败，进程可能未正常运行";
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        machineId,
                                                        LogstashMachineStep.VERIFY_PROCESS.getId(),
                                                        status,
                                                        errorMessage);

                                                if (!verifySuccess) {
                                                    throw new RuntimeException(
                                                            "验证Logstash进程失败，进程可能未正常运行");
                                                }
                                                return verifySuccess;
                                            })
                                    .exceptionally(
                                            ex -> {
                                                // 更新任务状态为失败，不记录详细日志（由外层处理）
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        machineId,
                                                        LogstashMachineStep.VERIFY_PROCESS.getId(),
                                                        StepStatus.FAILED,
                                                        ex.getMessage());

                                                // 重新抛出异常，确保异常传递到外层
                                                if (ex instanceof RuntimeException) {
                                                    throw (RuntimeException) ex;
                                                } else {
                                                    throw new RuntimeException(ex.getMessage(), ex);
                                                }
                                            });
                        });

        return result;
    }

    @Override
    public CompletableFuture<Boolean> handleUpdateConfig(
            LogstashProcess process,
            String configContent,
            String jvmOptions,
            String logstashYml,
            MachineInfo machineInfo,
            String taskId) {
        Long processId = process.getId();
        Long machineId = machineInfo.getId();

        logger.info("更新机器 [{}] 上的Logstash进程 [{}] 配置", machineId, processId);

        // 更新相关步骤的状态为运行中
        if (configContent != null) {
            taskService.updateStepStatus(
                    taskId,
                    machineId,
                    LogstashMachineStep.UPDATE_MAIN_CONFIG.getId(),
                    StepStatus.RUNNING);
        }
        if (jvmOptions != null) {
            taskService.updateStepStatus(
                    taskId,
                    machineId,
                    LogstashMachineStep.UPDATE_JVM_CONFIG.getId(),
                    StepStatus.RUNNING);
        }
        if (logstashYml != null) {
            taskService.updateStepStatus(
                    taskId,
                    machineId,
                    LogstashMachineStep.UPDATE_SYSTEM_CONFIG.getId(),
                    StepStatus.RUNNING);
        }

        // 创建一个命令来同时更新所有配置
        LogstashCommand updateCommand =
                commandFactory.updateConfigCommand(
                        processId, configContent, jvmOptions, logstashYml);

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
                                        machineId,
                                        LogstashMachineStep.UPDATE_MAIN_CONFIG.getId(),
                                        status,
                                        errorMessage);
                            }
                            if (jvmOptions != null) {
                                taskService.updateStepStatus(
                                        taskId,
                                        machineId,
                                        LogstashMachineStep.UPDATE_JVM_CONFIG.getId(),
                                        status,
                                        errorMessage);
                            }
                            if (logstashYml != null) {
                                taskService.updateStepStatus(
                                        taskId,
                                        machineId,
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
                            // 处理异常情况
                            String errorMessage = ex.getMessage();
                            // 异常已在命令层记录，这里只更新任务状态

                            if (configContent != null) {
                                taskService.updateStepStatus(
                                        taskId,
                                        machineId,
                                        LogstashMachineStep.UPDATE_MAIN_CONFIG.getId(),
                                        StepStatus.FAILED,
                                        errorMessage);
                            }
                            if (jvmOptions != null) {
                                taskService.updateStepStatus(
                                        taskId,
                                        machineId,
                                        LogstashMachineStep.UPDATE_JVM_CONFIG.getId(),
                                        StepStatus.FAILED,
                                        errorMessage);
                            }
                            if (logstashYml != null) {
                                taskService.updateStepStatus(
                                        taskId,
                                        machineId,
                                        LogstashMachineStep.UPDATE_SYSTEM_CONFIG.getId(),
                                        StepStatus.FAILED,
                                        errorMessage);
                            }

                            // 重新抛出异常，确保异常传递到外层
                            if (ex instanceof RuntimeException) {
                                throw (RuntimeException) ex;
                            } else {
                                throw new RuntimeException("更新配置时发生异常: " + errorMessage, ex);
                            }
                        });
    }

    @Override
    public CompletableFuture<Boolean> handleRefreshConfig(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        Long processId = process.getId();
        Long machineId = machineInfo.getId();

        logger.info("刷新机器 [{}] 上的Logstash进程 [{}] 配置", machineId, processId);

        taskService.updateStepStatus(
                taskId, machineId, LogstashMachineStep.REFRESH_CONFIG.getId(), StepStatus.RUNNING);

        // 使用支持所有配置类型的刷新命令
        // 传递null将从数据库中获取最新配置
        LogstashCommand refreshConfigCommand =
                commandFactory.refreshConfigCommand(processId, null, null, null);

        return refreshConfigCommand
                .execute(machineInfo)
                .thenApply(
                        success -> {
                            StepStatus status = success ? StepStatus.COMPLETED : StepStatus.FAILED;
                            String errorMessage = success ? null : "刷新配置失败";
                            taskService.updateStepStatus(
                                    taskId,
                                    machineId,
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
                            String errorMessage = ex.getMessage();
                            // 异常已在命令层记录，这里只更新任务状态
                            taskService.updateStepStatus(
                                    taskId,
                                    machineId,
                                    LogstashMachineStep.REFRESH_CONFIG.getId(),
                                    StepStatus.FAILED,
                                    errorMessage);

                            // 重新抛出异常，确保异常传递到外层
                            if (ex instanceof RuntimeException) {
                                throw (RuntimeException) ex;
                            } else {
                                throw new RuntimeException("刷新配置时发生异常: " + errorMessage, ex);
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
