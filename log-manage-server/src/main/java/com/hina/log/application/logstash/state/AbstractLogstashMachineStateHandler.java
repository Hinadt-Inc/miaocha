package com.hina.log.application.logstash.state;

import com.hina.log.application.logstash.command.LogstashCommandFactory;
import com.hina.log.application.logstash.enums.LogstashMachineState;
import com.hina.log.application.logstash.task.TaskService;
import com.hina.log.domain.entity.LogstashProcess;
import com.hina.log.domain.entity.MachineInfo;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logstash机器状态处理器抽象基类 */
public abstract class AbstractLogstashMachineStateHandler implements LogstashMachineStateHandler {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final TaskService taskService;
    protected final LogstashCommandFactory commandFactory;

    protected AbstractLogstashMachineStateHandler(
            TaskService taskService, LogstashCommandFactory commandFactory) {
        this.taskService = taskService;
        this.commandFactory = commandFactory;
    }

    @Override
    public CompletableFuture<Boolean> handleInitialize(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        logger.warn("状态 [{}] 不支持初始化操作", getState().name());
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> handleStart(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        logger.warn("状态 [{}] 不支持启动操作", getState().name());
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> handleStop(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        logger.warn("状态 [{}] 不支持停止操作", getState().name());
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> handleForceStop(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        logger.warn("状态 [{}] 不支持强制停止操作", getState().name());
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> handleUpdateConfig(
            LogstashProcess process,
            String configContent,
            String jvmOptions,
            String logstashYml,
            MachineInfo machineInfo,
            String taskId) {
        logger.warn("状态 [{}] 不支持更新配置操作", getState().name());
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> handleRefreshConfig(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        // 默认实现直接返回成功
        return CompletableFuture.completedFuture(true);
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

    @Override
    public TaskService getTaskService() {
        return taskService;
    }

    @Override
    public LogstashMachineState getNextState(OperationType operationType, boolean success) {
        // 默认实现保持当前状态
        return getState();
    }
}
