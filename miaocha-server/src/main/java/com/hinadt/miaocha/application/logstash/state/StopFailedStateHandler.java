package com.hinadt.miaocha.application.logstash.state;

import com.hinadt.miaocha.application.logstash.command.LogstashCommand;
import com.hinadt.miaocha.application.logstash.command.LogstashCommandFactory;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineStep;
import com.hinadt.miaocha.application.logstash.enums.StepStatus;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
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
    public boolean handleStop(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("重试停止机器 [{}] 上的LogstashMachine实例 [{}]", machineId, logstashMachineId);

        // 重置步骤状态
        taskService.resetStepStatuses(taskId, StepStatus.PENDING);

        taskService.updateStepStatus(
                taskId,
                logstashMachineId,
                LogstashMachineStep.STOP_PROCESS.getId(),
                StepStatus.RUNNING);
        LogstashCommand stopCommand = commandFactory.stopProcessCommand(logstashMachineId);

        try {
            boolean success = stopCommand.execute(machineInfo);
            StepStatus status = success ? StepStatus.COMPLETED : StepStatus.FAILED;
            String errorMessage = success ? null : "停止Logstash进程失败";
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.STOP_PROCESS.getId(),
                    status,
                    errorMessage);

            if (!success) {
                throw new RuntimeException("停止Logstash进程失败");
            }

            return success;
        } catch (Exception ex) {
            String errorMessage = ex.getMessage();
            // 异常已在命令层记录，这里只更新任务状态
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.STOP_PROCESS.getId(),
                    StepStatus.FAILED,
                    errorMessage);

            throw new RuntimeException("停止进程时发生异常: " + errorMessage, ex);
        }
    }

    @Override
    public boolean handleForceStop(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.warn(
                "强制停止机器 [{}] 上的LogstashMachine实例 [{}] (当前状态: 停止失败)", machineId, logstashMachineId);

        // 重置步骤状态
        taskService.resetStepStatuses(taskId, StepStatus.PENDING);

        taskService.updateStepStatus(
                taskId,
                logstashMachineId,
                LogstashMachineStep.STOP_PROCESS.getId(),
                StepStatus.RUNNING);
        LogstashCommand forceStopCommand =
                commandFactory.forceStopProcessCommand(logstashMachineId);

        try {
            forceStopCommand.execute(machineInfo);
            // 强制停止总是认为成功
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.STOP_PROCESS.getId(),
                    StepStatus.COMPLETED,
                    "强制停止完成");

            logger.info("强制停止机器 [{}] 上的LogstashMachine实例 [{}] 完成", machineId, logstashMachineId);
            return true;
        } catch (Exception ex) {
            // 即使发生异常，强制停止也认为成功
            logger.warn("强制停止过程中发生异常，但仍标记为成功: {}", ex.getMessage());
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.STOP_PROCESS.getId(),
                    StepStatus.COMPLETED,
                    "强制停止完成（忽略异常）");

            return true;
        }
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
