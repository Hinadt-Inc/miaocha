package com.hina.log.logstash.state;

import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.logstash.command.LogstashCommand;
import com.hina.log.logstash.command.LogstashCommandFactory;
import com.hina.log.logstash.enums.LogstashMachineState;
import com.hina.log.logstash.enums.LogstashMachineStep;
import com.hina.log.logstash.enums.StepStatus;
import com.hina.log.logstash.task.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 启动失败状态处理器
 */
@Component
public class StartFailedStateHandler extends AbstractLogstashMachineStateHandler {

    private static final Logger logger = LoggerFactory.getLogger(StartFailedStateHandler.class);

    public StartFailedStateHandler(TaskService taskService, LogstashCommandFactory commandFactory) {
        super(taskService, commandFactory);
    }

    @Override
    public LogstashMachineState getState() {
        return LogstashMachineState.START_FAILED;
    }

    @Override
    public CompletableFuture<Boolean> handleStart(LogstashProcess process, Machine machine, String taskId) {
        Long processId = process.getId();
        Long machineId = machine.getId();
        
        logger.info("重试启动机器 [{}] 上的Logstash进程 [{}]", machineId, processId);
        
        // 重置所有步骤的状态
        taskService.resetStepStatuses(taskId, StepStatus.PENDING);
        
        CompletableFuture<Boolean> result = CompletableFuture.completedFuture(true);
        
        // 1. 启动Logstash进程
        result = result.thenCompose(success -> {
            if (!success) return CompletableFuture.completedFuture(false);
            
            taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.START_PROCESS.getId(), StepStatus.RUNNING);
            LogstashCommand startCommand = commandFactory.startProcessCommand(processId);
            
            return startCommand.execute(machine)
                    .thenApply(startSuccess -> {
                        StepStatus status = startSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.START_PROCESS.getId(), status);
                        return startSuccess;
                    });
        });
        
        // 2. 验证进程状态
        result = result.thenCompose(success -> {
            if (!success) return CompletableFuture.completedFuture(false);
            
            taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.VERIFY_PROCESS.getId(), StepStatus.RUNNING);
            LogstashCommand verifyCommand = commandFactory.verifyProcessCommand(processId);
            
            return verifyCommand.execute(machine)
                    .thenApply(verifySuccess -> {
                        StepStatus status = verifySuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.VERIFY_PROCESS.getId(), status);
                        return verifySuccess;
                    });
        });
        
        return result;
    }

    @Override
    public CompletableFuture<Boolean> handleUpdateConfig(LogstashProcess process, String configContent, Machine machine, String taskId) {
        Long processId = process.getId();
        Long machineId = machine.getId();
        
        logger.info("更新机器 [{}] 上的Logstash进程 [{}] 配置", machineId, processId);
        
        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.UPDATE_MAIN_CONFIG.getId(), StepStatus.RUNNING);
        LogstashCommand updateConfigCommand = commandFactory.updateConfigCommand(processId, configContent);
        
        return updateConfigCommand.execute(machine)
                .thenApply(success -> {
                    StepStatus status = success ? StepStatus.COMPLETED : StepStatus.FAILED;
                    taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.UPDATE_MAIN_CONFIG.getId(), status);
                    return success;
                });
    }

    @Override
    public CompletableFuture<Boolean> handleRefreshConfig(LogstashProcess process, Machine machine, String taskId) {
        Long processId = process.getId();
        Long machineId = machine.getId();
        
        logger.info("刷新机器 [{}] 上的Logstash进程 [{}] 配置", machineId, processId);
        
        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.REFRESH_CONFIG.getId(), StepStatus.RUNNING);
        
        // 使用支持所有配置类型的刷新命令
        // 传递null将从数据库中获取最新配置
        LogstashCommand refreshConfigCommand = commandFactory.refreshConfigCommand(processId, null, null, null);
        
        return refreshConfigCommand.execute(machine)
                .thenApply(success -> {
                    StepStatus status = success ? StepStatus.COMPLETED : StepStatus.FAILED;
                    taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.REFRESH_CONFIG.getId(), status);
                    return success;
                });
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
    public LogstashMachineState getNextState(OperationType operationType, boolean success) {
        if (operationType == OperationType.START) {
            return success ? LogstashMachineState.RUNNING : LogstashMachineState.START_FAILED;
        }
        return getState(); // 默认保持当前状态
    }
}
