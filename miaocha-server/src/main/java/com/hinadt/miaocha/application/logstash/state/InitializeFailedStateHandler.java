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
import org.springframework.stereotype.Component;

/** 初始化失败状态处理器 允许重新初始化进程 */
@Component
public class InitializeFailedStateHandler extends AbstractLogstashMachineStateHandler {

    public InitializeFailedStateHandler(
            TaskService taskService, LogstashCommandFactory commandFactory) {
        super(taskService, commandFactory);
    }

    @Override
    public LogstashMachineState getState() {
        return LogstashMachineState.INITIALIZE_FAILED;
    }

    @Override
    public CompletableFuture<Boolean> handleInitialize(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("重新初始化机器 [{}] 上的LogstashMachine实例 [{}]", machineId, logstashMachineId);

        // 重置所有初始化步骤的状态
        taskService.resetStepStatuses(taskId, StepStatus.PENDING);

        CompletableFuture<Boolean> result = CompletableFuture.completedFuture(true);

        // 先删除进程目录（不计入步骤状态）
        result =
                result.thenCompose(
                        success -> {
                            if (!success) return CompletableFuture.completedFuture(false);

                            logger.info(
                                    "删除机器 [{}] 上的LogstashMachine实例 [{}] 目录",
                                    machineId,
                                    logstashMachineId);
                            LogstashCommand deleteCommand =
                                    commandFactory.deleteProcessDirectoryCommand(logstashMachineId);

                            return deleteCommand
                                    .execute(machineInfo)
                                    .exceptionally(
                                            ex -> {
                                                // 即使删除失败也继续执行（可能目录不存在）
                                                logger.warn(
                                                        "删除进程目录失败，将继续执行初始化: {}", ex.getMessage());
                                                return true;
                                            });
                        });

        // 1. 创建远程目录
        result =
                result.thenCompose(
                        success -> {
                            if (!success) return CompletableFuture.completedFuture(false);

                            taskService.updateStepStatus(
                                    taskId,
                                    logstashMachineId,
                                    LogstashMachineStep.CREATE_REMOTE_DIR.getId(),
                                    StepStatus.RUNNING);
                            LogstashCommand createDirCommand =
                                    commandFactory.createDirectoryCommand(logstashMachineId);

                            return createDirCommand
                                    .execute(machineInfo)
                                    .thenApply(
                                            createDirSuccess -> {
                                                StepStatus status =
                                                        createDirSuccess
                                                                ? StepStatus.COMPLETED
                                                                : StepStatus.FAILED;
                                                String errorMessage =
                                                        createDirSuccess ? null : "创建远程目录失败";
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.CREATE_REMOTE_DIR
                                                                .getId(),
                                                        status,
                                                        errorMessage);

                                                if (!createDirSuccess) {
                                                    throw new RuntimeException("创建远程目录失败");
                                                }

                                                return createDirSuccess;
                                            })
                                    .exceptionally(
                                            ex -> {
                                                // 更新任务状态为失败，不记录详细日志（由外层处理）
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.CREATE_REMOTE_DIR
                                                                .getId(),
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

        // 2. 上传Logstash安装包
        result =
                result.thenCompose(
                        success -> {
                            if (!success) return CompletableFuture.completedFuture(false);

                            taskService.updateStepStatus(
                                    taskId,
                                    logstashMachineId,
                                    LogstashMachineStep.UPLOAD_PACKAGE.getId(),
                                    StepStatus.RUNNING);
                            LogstashCommand uploadCommand =
                                    commandFactory.uploadPackageCommand(logstashMachineId);

                            return uploadCommand
                                    .execute(machineInfo)
                                    .thenApply(
                                            uploadSuccess -> {
                                                StepStatus status =
                                                        uploadSuccess
                                                                ? StepStatus.COMPLETED
                                                                : StepStatus.FAILED;
                                                String errorMessage =
                                                        uploadSuccess ? null : "上传Logstash安装包失败";
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.UPLOAD_PACKAGE.getId(),
                                                        status,
                                                        errorMessage);

                                                if (!uploadSuccess) {
                                                    throw new RuntimeException("上传Logstash安装包失败");
                                                }

                                                return uploadSuccess;
                                            })
                                    .exceptionally(
                                            ex -> {
                                                // 更新任务状态为失败，不记录详细日志（由外层处理）
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.UPLOAD_PACKAGE.getId(),
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

        // 3. 解压Logstash安装包
        result =
                result.thenCompose(
                        success -> {
                            if (!success) return CompletableFuture.completedFuture(false);

                            taskService.updateStepStatus(
                                    taskId,
                                    logstashMachineId,
                                    LogstashMachineStep.EXTRACT_PACKAGE.getId(),
                                    StepStatus.RUNNING);
                            LogstashCommand extractCommand =
                                    commandFactory.extractPackageCommand(logstashMachineId);

                            return extractCommand
                                    .execute(machineInfo)
                                    .thenApply(
                                            extractSuccess -> {
                                                StepStatus status =
                                                        extractSuccess
                                                                ? StepStatus.COMPLETED
                                                                : StepStatus.FAILED;
                                                String errorMessage =
                                                        extractSuccess ? null : "解压Logstash安装包失败";
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.EXTRACT_PACKAGE.getId(),
                                                        status,
                                                        errorMessage);

                                                if (!extractSuccess) {
                                                    throw new RuntimeException("解压Logstash安装包失败");
                                                }

                                                return extractSuccess;
                                            })
                                    .exceptionally(
                                            ex -> {
                                                // 更新任务状态为失败，不记录详细日志（由外层处理）
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.EXTRACT_PACKAGE.getId(),
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

        // 4. 创建配置文件
        result =
                result.thenCompose(
                        success -> {
                            if (!success) return CompletableFuture.completedFuture(false);

                            taskService.updateStepStatus(
                                    taskId,
                                    logstashMachineId,
                                    LogstashMachineStep.CREATE_CONFIG.getId(),
                                    StepStatus.RUNNING);
                            LogstashCommand configCommand =
                                    commandFactory.createConfigCommand(logstashMachine);

                            return configCommand
                                    .execute(machineInfo)
                                    .thenApply(
                                            configSuccess -> {
                                                StepStatus status =
                                                        configSuccess
                                                                ? StepStatus.COMPLETED
                                                                : StepStatus.FAILED;
                                                String errorMessage =
                                                        configSuccess ? null : "创建配置文件失败";
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.CREATE_CONFIG.getId(),
                                                        status,
                                                        errorMessage);

                                                if (!configSuccess) {
                                                    throw new RuntimeException("创建配置文件失败");
                                                }

                                                return configSuccess;
                                            })
                                    .exceptionally(
                                            ex -> {
                                                // 更新任务状态为失败，不记录详细日志（由外层处理）
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.CREATE_CONFIG.getId(),
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

        // 5. 修改系统配置
        result =
                result.thenCompose(
                        success -> {
                            if (!success) return CompletableFuture.completedFuture(false);

                            taskService.updateStepStatus(
                                    taskId,
                                    logstashMachineId,
                                    LogstashMachineStep.MODIFY_CONFIG.getId(),
                                    StepStatus.RUNNING);

                            // 获取JVM选项和系统配置，传递给增强的ModifySystemConfigCommand
                            String jvmOptions = logstashMachine.getJvmOptions();
                            String logstashYml = logstashMachine.getLogstashYml();
                            LogstashCommand modifyConfigCommand =
                                    commandFactory.modifySystemConfigCommand(
                                            logstashMachineId, jvmOptions, logstashYml);

                            return modifyConfigCommand
                                    .execute(machineInfo)
                                    .thenApply(
                                            modifyConfigSuccess -> {
                                                StepStatus status =
                                                        modifyConfigSuccess
                                                                ? StepStatus.COMPLETED
                                                                : StepStatus.FAILED;
                                                String errorMessage =
                                                        modifyConfigSuccess ? null : "修改系统配置失败";
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.MODIFY_CONFIG.getId(),
                                                        status,
                                                        errorMessage);

                                                if (!modifyConfigSuccess) {
                                                    throw new RuntimeException("修改系统配置失败");
                                                }

                                                return modifyConfigSuccess;
                                            })
                                    .exceptionally(
                                            ex -> {
                                                String errorMessage = ex.getMessage();
                                                // 异常已在命令层记录，这里只更新任务状态
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.MODIFY_CONFIG.getId(),
                                                        StepStatus.FAILED,
                                                        errorMessage);

                                                // 重新抛出异常，确保异常传递到外层
                                                if (ex instanceof RuntimeException) {
                                                    throw (RuntimeException) ex;
                                                } else {
                                                    throw new RuntimeException(
                                                            "修改系统配置时发生异常: " + errorMessage, ex);
                                                }
                                            });
                        });

        return result;
    }

    @Override
    public boolean canInitialize() {
        return true; // 允许重新初始化
    }

    @Override
    public LogstashMachineState getNextState(OperationType operationType, boolean success) {
        if (operationType == OperationType.INITIALIZE) {
            return success
                    ? LogstashMachineState.NOT_STARTED
                    : LogstashMachineState.INITIALIZE_FAILED;
        }
        return getState(); // 默认保持当前状态
    }

    @Override
    public CompletableFuture<Boolean> handleDelete(
            LogstashMachine logstashMachine, MachineInfo machineInfo) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info(
                "删除机器 [{}] 上的LogstashMachine实例 [{}] 目录 (初始化失败状态)", machineId, logstashMachineId);

        LogstashCommand deleteCommand =
                commandFactory.deleteProcessDirectoryCommand(logstashMachineId);

        return deleteCommand
                .execute(machineInfo)
                .thenApply(
                        success -> {
                            if (success) {
                                logger.info(
                                        "成功删除机器 [{}] 上的LogstashMachine实例 [{}] 目录",
                                        machineId,
                                        logstashMachineId);
                            } else {
                                logger.error(
                                        "删除机器 [{}] 上的LogstashMachine实例 [{}] 目录失败",
                                        machineId,
                                        logstashMachineId);
                            }
                            return success;
                        })
                .exceptionally(
                        ex -> {
                            logger.error(
                                    "删除机器 [{}] 上的LogstashMachine实例 [{}] 目录时发生异常: {}",
                                    machineId,
                                    logstashMachineId,
                                    ex.getMessage(),
                                    ex);
                            return false;
                        });
    }

    @Override
    public boolean canDelete() {
        return true;
    }
}
