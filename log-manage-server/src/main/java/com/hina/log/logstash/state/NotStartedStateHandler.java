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
 * 未启动状态处理器
 */
@Component
public class NotStartedStateHandler extends AbstractLogstashProcessStateHandler {

    private final LogstashCommandFactory commandFactory;

    public NotStartedStateHandler(LogstashCommandFactory commandFactory, TaskService taskService) {
        super(taskService);
        this.commandFactory = commandFactory;
    }

    @Override
    public LogstashProcessState getState() {
        return LogstashProcessState.NOT_STARTED;
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
    public boolean canInitialize() {
        return false; // 初始化逻辑已移至InitializingStateHandler
    }

    @Override
    public boolean canStop() {
        return true; // 即使未启动，也支持停止操作（实际上是空操作）
    }

    @Override
    public CompletableFuture<Boolean> handleStart(LogstashProcess process, List<Machine> machines, String taskId) {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        Long processId = process.getId();

        // 1. 更新启动进程步骤状态为运行中
        if (taskId != null) {
            for (Machine machine : machines) {
                taskService.updateStepStatus(taskId, machine.getId(),
                        LogstashProcessStep.START_PROCESS.getId(), StepStatus.RUNNING);
            }
        }

        // 2. 启动进程
        executeCommandOnAllMachines(commandFactory.startProcessCommand(processId), machines)
                .thenCompose(startSuccess -> {
                    // 更新启动进程步骤状态
                    if (taskId != null) {
                        StepStatus status = startSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.START_PROCESS.getId(), status);
                        }
                    }

                    if (!startSuccess) {
                        logger.error("启动Logstash进程失败");
                        resultFuture.complete(false);
                        return CompletableFuture.completedFuture(false);
                    }
                    logger.info("Logstash进程启动命令执行成功");

                    // 3. 更新验证进程步骤状态为运行中
                    if (taskId != null) {
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.VERIFY_PROCESS.getId(), StepStatus.RUNNING);
                        }
                    }

                    // 4. 验证进程启动状态
                    return executeCommandOnAllMachines(commandFactory.verifyProcessCommand(processId), machines);
                })
                .thenAccept(verifySuccess -> {
                    // 更新验证进程步骤状态
                    if (taskId != null) {
                        StepStatus status = verifySuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.VERIFY_PROCESS.getId(), status);
                        }
                    }

                    logger.info("Logstash进程验证{}", verifySuccess ? "成功" : "失败");
                    resultFuture.complete(verifySuccess);
                })
                .exceptionally(e -> {
                    // 更新步骤状态为失败
                    if (taskId != null) {
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.VERIFY_PROCESS.getId(), StepStatus.FAILED);
                        }
                    }
                    logger.error("启动Logstash进程时发生错误", e);
                    resultFuture.complete(false);
                    return null;
                });

        return resultFuture;
    }

    @Override
    public CompletableFuture<Boolean> handleStop(LogstashProcess process, List<Machine> machines, String taskId) {
        // 未启动状态不需要停止，直接返回成功
        logger.info("未启动状态下无需执行停止操作");
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> handleUpdateConfig(LogstashProcess process, String configJson,
            List<Machine> machines, String taskId) {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        Long processId = process.getId();

        // 1. 更新配置步骤状态为运行中
        if (taskId != null) {
            for (Machine machine : machines) {
                taskService.updateStepStatus(taskId, machine.getId(),
                        LogstashProcessStep.CREATE_CONFIG.getId(), StepStatus.RUNNING);
            }
        }

        // 2. 执行配置更新命令
        LogstashCommand updateCommand = commandFactory.updateConfigCommand(processId, configJson);
        executeCommandOnAllMachines(updateCommand, machines)
                .thenAccept(updateSuccess -> {
                    // 更新配置步骤状态
                    if (taskId != null) {
                        StepStatus status = updateSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.CREATE_CONFIG.getId(), status);
                        }
                    }

                    logger.info("Logstash配置更新{}", updateSuccess ? "成功" : "失败");
                    resultFuture.complete(updateSuccess);
                })
                .exceptionally(e -> {
                    // 更新步骤状态为失败
                    if (taskId != null) {
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.CREATE_CONFIG.getId(), StepStatus.FAILED);
                        }
                    }

                    logger.error("更新Logstash配置时发生错误", e);
                    resultFuture.complete(false);
                    return null;
                });

        return resultFuture;
    }

    @Override
    public CompletableFuture<Boolean> handleRefreshConfig(LogstashProcess process, List<Machine> machines,
            String taskId) {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        Long processId = process.getId();

        // 1. 更新配置步骤状态为运行中
        if (taskId != null) {
            for (Machine machine : machines) {
                taskService.updateStepStatus(taskId, machine.getId(),
                        LogstashProcessStep.CREATE_CONFIG.getId(), StepStatus.RUNNING);
            }
        }

        // 2. 执行配置刷新命令
        LogstashCommand refreshCommand = commandFactory.refreshConfigCommand(processId);
        executeCommandOnAllMachines(refreshCommand, machines)
                .thenAccept(refreshSuccess -> {
                    // 更新配置步骤状态
                    if (taskId != null) {
                        StepStatus status = refreshSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.CREATE_CONFIG.getId(), status);
                        }
                    }

                    logger.info("Logstash配置刷新{}", refreshSuccess ? "成功" : "失败");
                    resultFuture.complete(refreshSuccess);
                })
                .exceptionally(e -> {
                    // 更新步骤状态为失败
                    if (taskId != null) {
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.CREATE_CONFIG.getId(), StepStatus.FAILED);
                        }
                    }

                    logger.error("刷新Logstash配置时发生错误", e);
                    resultFuture.complete(false);
                    return null;
                });

        return resultFuture;
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