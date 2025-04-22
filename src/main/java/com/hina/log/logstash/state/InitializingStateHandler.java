package com.hina.log.logstash.state;

import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.logstash.enums.LogstashProcessState;
import com.hina.log.logstash.enums.LogstashProcessStep;
import com.hina.log.logstash.enums.StepStatus;
import com.hina.log.logstash.command.LogstashCommand;
import com.hina.log.logstash.command.LogstashCommandFactory;
import com.hina.log.logstash.task.TaskService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 初始化中状态处理器
 * 负责处理Logstash进程的初始化过程
 */
@Component
public class InitializingStateHandler extends AbstractLogstashProcessStateHandler {

    private final LogstashCommandFactory commandFactory;

    public InitializingStateHandler(LogstashCommandFactory commandFactory, TaskService taskService) {
        super(taskService);
        this.commandFactory = commandFactory;
    }

    @Override
    public LogstashProcessState getState() {
        return LogstashProcessState.INITIALIZING;
    }

    @Override
    public boolean canInitialize() {
        return true;
    }

    @Override
    public CompletableFuture<Boolean> handleInitialize(LogstashProcess process, List<Machine> machines, String taskId) {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        Long processId = process.getId();

        // 1. 更新创建目录步骤状态为运行中
        if (taskId != null) {
            for (Machine machine : machines) {
                taskService.updateStepStatus(taskId, machine.getId(),
                        LogstashProcessStep.CREATE_REMOTE_DIR.getId(), StepStatus.RUNNING);
            }
        }

        // 2. 创建目录
        executeCommandOnAllMachines(commandFactory.createDirectoryCommand(processId), machines)
                .thenCompose(dirSuccess -> {
                    // 更新创建目录步骤状态
                    if (taskId != null) {
                        StepStatus status = dirSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.CREATE_REMOTE_DIR.getId(), status);
                        }
                    }

                    if (!dirSuccess) {
                        logger.error("创建Logstash进程目录失败");
                        resultFuture.complete(false);
                        return CompletableFuture.completedFuture(false);
                    }
                    logger.info("成功创建Logstash进程目录");

                    // 3. 更新上传安装包步骤状态为运行中
                    if (taskId != null) {
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.UPLOAD_PACKAGE.getId(), StepStatus.RUNNING);
                        }
                    }

                    // 4. 上传安装包
                    return executeCommandOnAllMachines(commandFactory.uploadPackageCommand(processId), machines);
                })
                .thenCompose(uploadSuccess -> {
                    // 更新上传安装包步骤状态
                    if (taskId != null) {
                        StepStatus status = uploadSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.UPLOAD_PACKAGE.getId(), status);
                        }
                    }

                    if (!uploadSuccess) {
                        logger.error("上传Logstash安装包失败");
                        resultFuture.complete(false);
                        return CompletableFuture.completedFuture(false);
                    }
                    logger.info("成功上传Logstash安装包");

                    // 5. 更新解压安装包步骤状态为运行中
                    if (taskId != null) {
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.EXTRACT_PACKAGE.getId(), StepStatus.RUNNING);
                        }
                    }

                    // 6. 解压安装包
                    return executeCommandOnAllMachines(commandFactory.extractPackageCommand(processId), machines);
                })
                .thenCompose(extractSuccess -> {
                    // 更新解压安装包步骤状态
                    if (taskId != null) {
                        StepStatus status = extractSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.EXTRACT_PACKAGE.getId(), status);
                        }
                    }

                    if (!extractSuccess) {
                        logger.error("解压Logstash安装包失败");
                        resultFuture.complete(false);
                        return CompletableFuture.completedFuture(false);
                    }
                    logger.info("成功解压Logstash安装包");

                    // 7. 更新创建配置文件步骤状态为运行中
                    if (taskId != null) {
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.CREATE_CONFIG.getId(), StepStatus.RUNNING);
                        }
                    }

                    // 8. 创建配置文件
                    return executeCommandOnAllMachines(commandFactory.createConfigCommand(process), machines);
                })
                .thenCompose(configSuccess -> {
                    // 更新创建配置文件步骤状态
                    if (taskId != null) {
                        StepStatus status = configSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.CREATE_CONFIG.getId(), status);
                        }
                    }

                    if (!configSuccess) {
                        logger.error("创建Logstash配置文件失败");
                        resultFuture.complete(false);
                        return CompletableFuture.completedFuture(false);
                    }
                    logger.info("成功创建Logstash配置文件");

                    // 9. 更新修改系统配置步骤状态为运行中
                    if (taskId != null) {
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.MODIFY_CONFIG.getId(), StepStatus.RUNNING);
                        }
                    }

                    // 10. 修改系统配置
                    return executeCommandOnAllMachines(commandFactory.modifySystemConfigCommand(processId),
                            machines);
                })
                .thenAccept(sysConfigSuccess -> {
                    // 更新修改系统配置步骤状态
                    if (taskId != null) {
                        StepStatus status = sysConfigSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.MODIFY_CONFIG.getId(), status);
                        }
                    }

                    logger.info("Logstash系统配置修改{}", sysConfigSuccess ? "成功" : "失败");
                    resultFuture.complete(sysConfigSuccess);
                })
                .exceptionally(e -> {
                    logger.error("初始化Logstash进程时发生错误", e);
                    resultFuture.complete(false);
                    return null;
                });

        return resultFuture;
    }

    @Override
    public CompletableFuture<Boolean> handleStart(LogstashProcess process, List<Machine> machines, String taskId) {
        // 初始化中状态不支持启动，返回失败
        logger.warn("初始化中状态不支持启动操作");
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> handleStop(LogstashProcess process, List<Machine> machines, String taskId) {
        // 初始化中状态支持停止（取消初始化）
        logger.info("初始化中状态下执行停止操作，将取消初始化过程");
        return CompletableFuture.completedFuture(true);
    }

    /**
     * 在所有机器上执行单一命令
     */
    private CompletableFuture<Boolean> executeCommandOnAllMachines(LogstashCommand command, List<Machine> machines) {
        // 在每台机器上执行命令，收集futures
        List<CompletableFuture<Boolean>> machineFutures = machines.stream()
                .map(command::execute)
                .collect(Collectors.toList());

        // 包装成一个整体结果
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

        // 等待所有命令完成，只有全部成功才返回true
        CompletableFuture.allOf(machineFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    try {
                        // 只有所有机器都成功执行，才算整体成功
                        boolean allSucceeded = machineFutures.stream()
                                .map(future -> {
                                    try {
                                        return future.get();
                                    } catch (Exception e) {
                                        logger.warn("获取命令执行结果时发生异常", e);
                                        return false;
                                    }
                                })
                                .allMatch(success -> success);

                        resultFuture.complete(allSucceeded);
                    } catch (Exception e) {
                        logger.error("处理命令执行结果时发生错误", e);
                        resultFuture.complete(false);
                    }
                })
                .exceptionally(e -> {
                    logger.error("等待命令执行完成时发生错误", e);
                    resultFuture.complete(false);
                    return null;
                });

        return resultFuture;
    }
}