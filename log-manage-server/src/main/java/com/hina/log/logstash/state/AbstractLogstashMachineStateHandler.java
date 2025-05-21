package com.hina.log.logstash.state;

import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.logstash.command.LogstashCommand;
import com.hina.log.logstash.command.LogstashCommandFactory;
import com.hina.log.logstash.enums.LogstashMachineStep;
import com.hina.log.logstash.enums.StepStatus;
import com.hina.log.logstash.task.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Logstash机器状态处理器抽象基类
 */
public abstract class AbstractLogstashMachineStateHandler implements LogstashMachineStateHandler {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final TaskService taskService;
    protected final LogstashCommandFactory commandFactory;

    protected AbstractLogstashMachineStateHandler(TaskService taskService, LogstashCommandFactory commandFactory) {
        this.taskService = taskService;
        this.commandFactory = commandFactory;
    }

    @Override
    public CompletableFuture<Boolean> handleInitialize(LogstashProcess process, Machine machine, String taskId) {
        logger.warn("状态 [{}] 不支持初始化操作", getState().name());
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> handleStart(LogstashProcess process, Machine machine, String taskId) {
        logger.warn("状态 [{}] 不支持启动操作", getState().name());
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> handleStop(LogstashProcess process, Machine machine, String taskId) {
        logger.warn("状态 [{}] 不支持停止操作", getState().name());
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> handleUpdateConfig(LogstashProcess process, String configContent, Machine machine, String taskId) {
        logger.warn("状态 [{}] 不支持更新配置操作", getState().name());
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> handleRefreshConfig(LogstashProcess process, Machine machine, String taskId) {
        // 默认实现直接返回成功
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> handleUpdateJvmOptions(LogstashProcess process, Machine machine, 
                                                          String jvmOptions, String taskId) {
        Long processId = process.getId();
        Long machineId = machine.getId();
        
        logger.info("更新机器 [{}] 上的Logstash进程 [{}] JVM选项", machineId, processId);
        
        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.UPDATE_JVM_CONFIG.getId(), StepStatus.RUNNING);
        
        // 使用更新配置命令，只更新JVM选项
        LogstashCommand updateCommand = commandFactory.updateConfigCommand(processId, null, jvmOptions, null);
        
        return updateCommand.execute(machine)
                .thenApply(success -> {
                    StepStatus status = success ? StepStatus.COMPLETED : StepStatus.FAILED;
                    taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.UPDATE_JVM_CONFIG.getId(), status);
                    return success;
                });
    }

    @Override
    public CompletableFuture<Boolean> handleUpdateLogstashYml(LogstashProcess process, Machine machine, 
                                                           String logstashYml, String taskId) {
        Long processId = process.getId();
        Long machineId = machine.getId();
        
        logger.info("更新机器 [{}] 上的Logstash进程 [{}] 系统配置", machineId, processId);
        
        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.UPDATE_SYSTEM_CONFIG.getId(), StepStatus.RUNNING);
        
        // 使用更新配置命令，只更新系统配置
        LogstashCommand updateCommand = commandFactory.updateConfigCommand(processId, null, null, logstashYml);
        
        return updateCommand.execute(machine)
                .thenApply(success -> {
                    StepStatus status = success ? StepStatus.COMPLETED : StepStatus.FAILED;
                    taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.UPDATE_SYSTEM_CONFIG.getId(), status);
                    return success;
                });
    }

    @Override
    public boolean canInitialize() {
        return false;
    }

    @Override
    public boolean canStart() {
        return false;
    }

    @Override
    public boolean canStop() {
        return false;
    }

    @Override
    public boolean canUpdateConfig() {
        return false;
    }

    @Override
    public boolean canRefreshConfig() {
        return false;
    }
}
