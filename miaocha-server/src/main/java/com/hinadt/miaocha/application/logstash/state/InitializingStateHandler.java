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
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("初始化机器 [{}] 上的LogstashMachine实例 [{}]", machineId, logstashMachineId);

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

                            logger.info(
                                    "创建机器 [{}] 上的LogstashMachine实例 [{}] 目录",
                                    machineId,
                                    logstashMachineId);
                            LogstashCommand createCommand =
                                    commandFactory.createDirectoryCommand(logstashMachineId);

                            return createCommand
                                    .execute(machineInfo)
                                    .thenApply(
                                            commandSuccess -> {
                                                StepStatus stepStatus =
                                                        commandSuccess
                                                                ? StepStatus.COMPLETED
                                                                : StepStatus.FAILED;
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.CREATE_REMOTE_DIR
                                                                .getId(),
                                                        stepStatus);
                                                return commandSuccess;
                                            });
                        });

        // 2. 上传Logstash压缩包
        result =
                result.thenCompose(
                        success -> {
                            if (!success) return CompletableFuture.completedFuture(false);

                            taskService.updateStepStatus(
                                    taskId,
                                    logstashMachineId,
                                    LogstashMachineStep.UPLOAD_PACKAGE.getId(),
                                    StepStatus.RUNNING);

                            logger.info(
                                    "上传Logstash包到机器 [{}] LogstashMachine实例 [{}]",
                                    machineId,
                                    logstashMachineId);
                            LogstashCommand uploadCommand =
                                    commandFactory.uploadPackageCommand(logstashMachineId);

                            return uploadCommand
                                    .execute(machineInfo)
                                    .thenApply(
                                            commandSuccess -> {
                                                StepStatus stepStatus =
                                                        commandSuccess
                                                                ? StepStatus.COMPLETED
                                                                : StepStatus.FAILED;
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.UPLOAD_PACKAGE.getId(),
                                                        stepStatus);
                                                return commandSuccess;
                                            });
                        });

        // 3. 解压Logstash包
        result =
                result.thenCompose(
                        success -> {
                            if (!success) return CompletableFuture.completedFuture(false);

                            taskService.updateStepStatus(
                                    taskId,
                                    logstashMachineId,
                                    LogstashMachineStep.EXTRACT_PACKAGE.getId(),
                                    StepStatus.RUNNING);

                            logger.info(
                                    "解压Logstash包在机器 [{}] LogstashMachine实例 [{}]",
                                    machineId,
                                    logstashMachineId);
                            LogstashCommand extractCommand =
                                    commandFactory.extractPackageCommand(logstashMachineId);

                            return extractCommand
                                    .execute(machineInfo)
                                    .thenApply(
                                            commandSuccess -> {
                                                StepStatus stepStatus =
                                                        commandSuccess
                                                                ? StepStatus.COMPLETED
                                                                : StepStatus.FAILED;
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.EXTRACT_PACKAGE.getId(),
                                                        stepStatus);
                                                return commandSuccess;
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

                            logger.info(
                                    "创建配置文件在机器 [{}] LogstashMachine实例 [{}]",
                                    machineId,
                                    logstashMachineId);
                            LogstashCommand configCommand =
                                    commandFactory.createConfigCommand(logstashMachine);

                            return configCommand
                                    .execute(machineInfo)
                                    .thenApply(
                                            commandSuccess -> {
                                                StepStatus stepStatus =
                                                        commandSuccess
                                                                ? StepStatus.COMPLETED
                                                                : StepStatus.FAILED;
                                                taskService.updateStepStatus(
                                                        taskId,
                                                        logstashMachineId,
                                                        LogstashMachineStep.CREATE_CONFIG.getId(),
                                                        stepStatus);
                                                return commandSuccess;
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
