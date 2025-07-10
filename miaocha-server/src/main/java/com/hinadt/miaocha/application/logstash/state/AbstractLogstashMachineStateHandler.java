package com.hinadt.miaocha.application.logstash.state;

import com.hinadt.miaocha.application.logstash.command.LogstashCommandFactory;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
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
    public boolean handleInitialize(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        logger.warn("状态 [{}] 不支持初始化操作", getState().name());
        return false;
    }

    @Override
    public boolean handleStart(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        logger.warn("状态 [{}] 不支持启动操作", getState().name());
        return false;
    }

    @Override
    public boolean handleStop(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        logger.warn("状态 [{}] 不支持停止操作", getState().name());
        return false;
    }

    @Override
    public boolean handleForceStop(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        logger.warn("状态 [{}] 不支持强制停止操作", getState().name());
        return false;
    }

    @Override
    public boolean handleUpdateConfig(
            LogstashMachine logstashMachine,
            String configContent,
            String jvmOptions,
            String logstashYml,
            MachineInfo machineInfo,
            String taskId) {
        logger.warn("状态 [{}] 不支持更新配置操作", getState().name());
        return false;
    }

    @Override
    public boolean handleRefreshConfig(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        // 默认实现直接返回成功
        return true;
    }

    @Override
    public boolean handleDelete(LogstashMachine logstashMachine, MachineInfo machineInfo) {
        logger.warn("状态 [{}] 不支持删除操作", getState().name());
        return false;
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
    public boolean canDelete() {
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
