package com.hina.log.logstash.state;

import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.logstash.enums.LogstashProcessState;
import com.hina.log.logstash.enums.LogstashProcessStep;
import com.hina.log.logstash.enums.StepStatus;
import com.hina.log.logstash.command.LogstashCommand;
import com.hina.log.logstash.command.LogstashCommandFactory;
import com.hina.log.logstash.task.TaskService;
import com.hina.log.mapper.LogstashMachineMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 运行中状态处理器
 */
@Component
public class RunningStateHandler extends AbstractLogstashProcessStateHandler {

    private final LogstashCommandFactory commandFactory;
    private final LogstashMachineMapper logstashMachineMapper;

    public RunningStateHandler(LogstashCommandFactory commandFactory, TaskService taskService, LogstashMachineMapper logstashMachineMapper) {
        super(taskService);
        this.commandFactory = commandFactory;
        this.logstashMachineMapper = logstashMachineMapper;
    }

    @Override
    public LogstashProcessState getState() {
        return LogstashProcessState.RUNNING;
    }

    @Override
    public boolean canStop() {
        return true;
    }

    @Override
    public CompletableFuture<Boolean> handleInitialize(LogstashProcess process, List<Machine> machines, String taskId) {
        // 运行中状态不支持初始化，返回失败
        logger.warn("运行中状态不支持初始化操作");
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> handleStart(LogstashProcess process, List<Machine> machines, String taskId) {
        // 运行中状态不支持再次启动，返回失败
        logger.warn("运行中状态不支持再次启动操作");
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> handleStop(LogstashProcess process, List<Machine> machines, String taskId) {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        Long processId = process.getId();

        // 更新停止进程步骤状态为运行中
        if (taskId != null) {
            for (Machine machine : machines) {
                taskService.updateStepStatus(taskId, machine.getId(),
                        LogstashProcessStep.STOP_PROCESS.getId(), StepStatus.RUNNING);
            }
        }

        // 停止进程
        executeCommandOnAllMachines(commandFactory.stopProcessCommand(processId), machines)
                .thenAccept(stopSuccess -> {
                    if (!stopSuccess) {
                        logger.error("停止Logstash进程失败");

                        // 更新停止进程步骤状态为失败
                        if (taskId != null) {
                            for (Machine machine : machines) {
                                taskService.updateStepStatus(taskId, machine.getId(),
                                        LogstashProcessStep.STOP_PROCESS.getId(), StepStatus.FAILED);
                            }
                        }
                        resultFuture.complete(false);
                        return;
                    }


                    logger.info("成功停止Logstash进程");
                    // 更新停止进程步骤状态
                    if (taskId != null) {
                        StepStatus status = StepStatus.COMPLETED;
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.STOP_PROCESS.getId(), status);

                            // 更新 进程-机器-进程pid表
                            logstashMachineMapper.updateProcessPid(processId, machine.getId(), null);
                        }
                    }
                    resultFuture.complete(true);
                })
                .exceptionally(e -> {
                    logger.error("停止Logstash进程时发生错误", e);

                    // 更新停止进程步骤状态为失败
                    if (taskId != null) {
                        for (Machine machine : machines) {
                            taskService.updateStepStatus(taskId, machine.getId(),
                                    LogstashProcessStep.STOP_PROCESS.getId(), StepStatus.FAILED);
                        }
                    }

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