package com.hina.log.logstash.state;

import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.logstash.enums.LogstashProcessState;
import com.hina.log.logstash.task.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Logstash进程状态处理器抽象基类
 * 实现通用方法，提供模板方法模式
 */
public abstract class AbstractLogstashProcessStateHandler implements LogstashProcessStateHandler {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final TaskService taskService;

    protected AbstractLogstashProcessStateHandler(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 获取处理器状态
     */
    @Override
    public abstract LogstashProcessState getState();

    /**
     * 处理初始化操作的默认实现
     */
    @Override
    public CompletableFuture<Boolean> handleInitialize(LogstashProcess process, List<Machine> machines, String taskId) {
        logger.warn("状态 [{}] 不支持初始化操作", getState().name());
        return CompletableFuture.completedFuture(false);
    }

    /**
     * 处理启动操作的默认实现
     */
    @Override
    public CompletableFuture<Boolean> handleStart(LogstashProcess process, List<Machine> machines, String taskId) {
        logger.warn("状态 [{}] 不支持启动操作", getState().name());
        return CompletableFuture.completedFuture(false);
    }

    /**
     * 处理停止操作的默认实现
     */
    @Override
    public CompletableFuture<Boolean> handleStop(LogstashProcess process, List<Machine> machines, String taskId) {
        logger.warn("状态 [{}] 不支持停止操作", getState().name());
        return CompletableFuture.completedFuture(false);
    }

    /**
     * 判断当前状态是否可以执行初始化操作
     */
    @Override
    public boolean canInitialize() {
        return false;
    }

    /**
     * 判断当前状态是否可以执行启动操作
     */
    @Override
    public boolean canStart() {
        return false;
    }

    /**
     * 判断当前状态是否可以执行停止操作
     */
    @Override
    public boolean canStop() {
        return false;
    }

    /**
     * 获取操作完成后的下一个状态
     */
    @Override
    public LogstashProcessState getNextState(OperationType operationType, boolean success) {
        switch (operationType) {
            case INITIALIZE:
                return success ? LogstashProcessState.NOT_STARTED : getState();
            case START:
                return success ? LogstashProcessState.RUNNING : LogstashProcessState.START_FAILED;
            case STOP:
                return success ? LogstashProcessState.NOT_STARTED : LogstashProcessState.STOP_FAILED;
            default:
                return getState();
        }
    }
}