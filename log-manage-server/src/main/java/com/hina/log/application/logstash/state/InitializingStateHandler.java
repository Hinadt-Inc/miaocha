package com.hina.log.application.logstash.state;

import com.hina.log.application.logstash.command.LogstashCommand;
import com.hina.log.application.logstash.command.LogstashCommandFactory;
import com.hina.log.application.logstash.enums.LogstashMachineState;
import com.hina.log.application.logstash.enums.LogstashMachineStep;
import com.hina.log.application.logstash.enums.StepStatus;
import com.hina.log.application.logstash.task.TaskService;
import com.hina.log.domain.entity.LogstashProcess;
import com.hina.log.domain.entity.MachineInfo;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/** 初始化中状态处理器 */
@Component
public class InitializingStateHandler extends AbstractLogstashMachineStateHandler {

    public InitializingStateHandler(
            TaskService taskService, LogstashCommandFactory commandFactory) {
        super(taskService, commandFactory);
    }

    @Override
    public LogstashMachineState getState() {
        return LogstashMachineState.INITIALIZING;
    }

    @Override
    public CompletableFuture<Boolean> handleInitialize(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        Long processId = process.getId();
        Long machineId = machineInfo.getId();

        logger.info("初始化机器 [{}] 上的Logstash进程 [{}]", machineId, processId);

        CompletableFuture<Boolean> result = CompletableFuture.completedFuture(true);

        // 先删除进程目录（不计入步骤状态）
        result =
                result.thenCompose(
                        success -> {
                            if (!success) return CompletableFuture.completedFuture(false);

                            logger.info("删除机器 [{}] 上的Logstash进程 [{}] 目录", machineId, processId);
                            LogstashCommand deleteCommand =
                                    commandFactory.deleteProcessDirectoryCommand(processId);

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
                                    machineId,
                                    LogstashMachineStep.CREATE_REMOTE_DIR.getId(),
                                    StepStatus.RUNNING);
                            LogstashCommand createDirCommand =
                                    commandFactory.createDirectoryCommand(processId);

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
                                                        machineId,
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
                                                        machineId,
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
                                    machineId,
                                    LogstashMachineStep.UPLOAD_PACKAGE.getId(),
                                    StepStatus.RUNNING);
                            LogstashCommand uploadCommand =
                                    commandFactory.uploadPackageCommand(processId);

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
                                                        machineId,
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
                                                        machineId,
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
                                    machineId,
                                    LogstashMachineStep.EXTRACT_PACKAGE.getId(),
                                    StepStatus.RUNNING);
                            LogstashCommand extractCommand =
                                    commandFactory.extractPackageCommand(processId);

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
                                                        machineId,
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
                                                        machineId,
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
                                    machineId,
                                    LogstashMachineStep.CREATE_CONFIG.getId(),
                                    StepStatus.RUNNING);
                            LogstashCommand configCommand =
                                    commandFactory.createConfigCommand(process);

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
                                                        machineId,
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
                                                        machineId,
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
                                    machineId,
                                    LogstashMachineStep.MODIFY_CONFIG.getId(),
                                    StepStatus.RUNNING);

                            // 获取JVM选项和系统配置，传递给增强的ModifySystemConfigCommand
                            String jvmOptions = process.getJvmOptions();
                            String logstashYml = process.getLogstashYml();
                            LogstashCommand modifyConfigCommand =
                                    commandFactory.modifySystemConfigCommand(
                                            processId, jvmOptions, logstashYml);

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
                                                        machineId,
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
                                                // 更新任务状态为失败，不记录详细日志（由外层处理）
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        machineId,
                                                        LogstashMachineStep.MODIFY_CONFIG.getId(),
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
    public boolean canInitialize() {
        return true;
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
}
