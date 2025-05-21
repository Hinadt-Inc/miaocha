package com.hina.log.logstash.state;

import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.logstash.command.LogstashCommand;
import com.hina.log.logstash.command.LogstashCommandFactory;
import com.hina.log.logstash.enums.LogstashMachineState;
import com.hina.log.logstash.enums.LogstashMachineStep;
import com.hina.log.logstash.enums.StepStatus;
import com.hina.log.logstash.task.TaskService;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 停止失败状态处理器
 */
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
    public CompletableFuture<Boolean> handleStop(LogstashProcess process, Machine machine, String taskId) {
        Long processId = process.getId();
        Long machineId = machine.getId();
        
        logger.info("重试停止机器 [{}] 上的Logstash进程 [{}]", machineId, processId);
        
        // 重置步骤状态
        taskService.resetStepStatuses(taskId, StepStatus.PENDING);
        
        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.STOP_PROCESS.getId(), StepStatus.RUNNING);
        LogstashCommand stopCommand = commandFactory.stopProcessCommand(processId);
        
        return stopCommand.execute(machine)
                .thenApply(success -> {
                    StepStatus status = success ? StepStatus.COMPLETED : StepStatus.FAILED;
                    taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.STOP_PROCESS.getId(), status);
                    return success;
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
