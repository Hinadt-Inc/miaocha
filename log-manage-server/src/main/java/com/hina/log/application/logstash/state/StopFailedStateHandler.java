package com.hina.log.application.logstash.state;

import com.hina.log.application.logstash.command.LogstashCommand;
import com.hina.log.application.logstash.command.LogstashCommandFactory;
import com.hina.log.application.logstash.enums.LogstashMachineState;
import com.hina.log.application.logstash.enums.LogstashMachineStep;
import com.hina.log.application.logstash.enums.StepStatus;
import com.hina.log.application.logstash.task.TaskService;
import com.hina.log.domain.entity.LogstashProcess;
import com.hina.log.domain.entity.Machine;
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
            LogstashProcess process, Machine machine, String taskId) {
        Long processId = process.getId();
        Long machineId = machine.getId();

        logger.info("重试停止机器 [{}] 上的Logstash进程 [{}]", machineId, processId);

        // 重置步骤状态
        taskService.resetStepStatuses(taskId, StepStatus.PENDING);

        taskService.updateStepStatus(
                taskId, machineId, LogstashMachineStep.STOP_PROCESS.getId(), StepStatus.RUNNING);
        LogstashCommand stopCommand = commandFactory.stopProcessCommand(processId);

        return stopCommand
                .execute(machine)
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
    public boolean canStop() {
        return true;
    }

    @Override
    public LogstashMachineState getNextState(OperationType operationType, boolean success) {
        if (operationType == OperationType.STOP) {
            return success ? LogstashMachineState.NOT_STARTED : LogstashMachineState.STOP_FAILED;
        }
        return getState(); // 默认保持当前状态
    }
}
