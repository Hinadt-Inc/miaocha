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
import org.springframework.stereotype.Component;

/** 停止失败状态处理器 */
@Component
public class StopFailedStateHandler extends AbstractLogstashMachineStateHandler {

    public StopFailedStateHandler(TaskService taskService, LogstashCommandFactory commandFactory) {
        super(taskService, commandFactory);
    }

    @Override
    public LogstashMachineState getState() {
        return LogstashMachineState.STOP_FAILED;
    }

    @Override
    public CompletableFuture<Boolean> handleStop(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        Long processId = process.getId();
        Long machineId = machineInfo.getId();

        logger.info("重试停止机器 [{}] 上的Logstash进程 [{}]", machineId, processId);

        // 重置步骤状态
        taskService.resetStepStatuses(taskId, StepStatus.PENDING);

        taskService.updateStepStatus(
                taskId, machineId, LogstashMachineStep.STOP_PROCESS.getId(), StepStatus.RUNNING);
        LogstashCommand stopCommand = commandFactory.stopProcessCommand(processId);

        return stopCommand
                .execute(machineInfo)
                .thenApply(
                        success -> {
                            StepStatus status = success ? StepStatus.COMPLETED : StepStatus.FAILED;
                            String errorMessage = success ? null : "停止Logstash进程失败";
                            taskService.updateStepStatus(
                                    taskId,
                                    machineId,
                                    LogstashMachineStep.STOP_PROCESS.getId(),
                                    status,
                                    errorMessage);

                            if (!success) {
                                throw new RuntimeException("停止Logstash进程失败");
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
                                    LogstashMachineStep.STOP_PROCESS.getId(),
                                    StepStatus.FAILED,
                                    errorMessage);

                            // 重新抛出异常，确保异常传递到外层
                            if (ex instanceof RuntimeException) {
                                throw (RuntimeException) ex;
                            } else {
                                throw new RuntimeException("停止进程时发生异常: " + errorMessage, ex);
                            }
                        });
    }

    @Override
    public CompletableFuture<Boolean> handleForceStop(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        Long processId = process.getId();
        Long machineId = machineInfo.getId();

        logger.warn("强制停止机器 [{}] 上的Logstash进程 [{}] (当前状态: 停止失败)", machineId, processId);

        // 重置步骤状态
        taskService.resetStepStatuses(taskId, StepStatus.PENDING);

        taskService.updateStepStatus(
                taskId, machineId, LogstashMachineStep.STOP_PROCESS.getId(), StepStatus.RUNNING);
        LogstashCommand forceStopCommand = commandFactory.forceStopProcessCommand(processId);

        return forceStopCommand
                .execute(machineInfo)
                .thenApply(
                        success -> {
                            // 强制停止总是认为成功
                            taskService.updateStepStatus(
                                    taskId,
                                    machineId,
                                    LogstashMachineStep.STOP_PROCESS.getId(),
                                    StepStatus.COMPLETED,
                                    "强制停止完成");

                            logger.info("强制停止机器 [{}] 上的Logstash进程 [{}] 完成", machineId, processId);
                            return true;
                        })
                .exceptionally(
                        ex -> {
                            // 即使发生异常，强制停止也认为成功
                            logger.warn("强制停止过程中发生异常，但仍标记为成功: {}", ex.getMessage());
                            taskService.updateStepStatus(
                                    taskId,
                                    machineId,
                                    LogstashMachineStep.STOP_PROCESS.getId(),
                                    StepStatus.COMPLETED,
                                    "强制停止完成（忽略异常）");

                            return true;
                        });
    }

    @Override
    public boolean canStop() {
        return true;
    }

    @Override
    public boolean canForceStop() {
        return true;
    }

    @Override
    public LogstashMachineState getNextState(OperationType operationType, boolean success) {
        if (operationType == OperationType.STOP) {
            return success ? LogstashMachineState.NOT_STARTED : LogstashMachineState.STOP_FAILED;
        } else if (operationType == OperationType.FORCE_STOP) {
            // 强制停止总是返回未启动状态，不管成功与否
            return LogstashMachineState.NOT_STARTED;
        }
        return getState(); // 默认保持当前状态
    }
}
