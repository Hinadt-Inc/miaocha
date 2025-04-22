package com.hina.log.logstash.state;

import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.logstash.enums.LogstashProcessState;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.LogstashProcessMapper;
import com.hina.log.logstash.state.LogstashProcessStateHandler.OperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Logstash进程状态管理器
 * 使用状态模式管理Logstash进程的状态转换和操作执行
 */
@Component
public class LogstashProcessStateManager {
    private static final Logger logger = LoggerFactory.getLogger(LogstashProcessStateManager.class);

    private final Map<LogstashProcessState, LogstashProcessStateHandler> stateHandlers;
    private final LogstashProcessMapper logstashProcessMapper;

    public LogstashProcessStateManager(List<LogstashProcessStateHandler> handlers,
            LogstashProcessMapper logstashProcessMapper) {
        this.logstashProcessMapper = logstashProcessMapper;
        this.stateHandlers = new HashMap<>();

        // 注册所有状态处理器
        for (LogstashProcessStateHandler handler : handlers) {
            stateHandlers.put(handler.getState(), handler);
        }

        logger.info("Registered {} Logstash process state handlers", stateHandlers.size());
    }

    /**
     * 执行初始化操作，带任务ID
     */
    public CompletableFuture<Boolean> initialize(LogstashProcess process, List<Machine> machines, String taskId) {
        LogstashProcessState currentState = LogstashProcessState.valueOf(process.getState());

        // 检查当前状态是否允许初始化
        if (currentState != LogstashProcessState.INITIALIZING &&
                !stateHandlers.get(currentState).canInitialize()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    String.format("当前状态[%s]不允许执行初始化操作", currentState.getDescription()));
        }

        // 如果进程不是初始化中状态，更新为初始化中状态
        if (currentState != LogstashProcessState.INITIALIZING) {
            updateState(process.getId(), LogstashProcessState.INITIALIZING);
        }

        // 获取初始化中状态的处理器
        LogstashProcessStateHandler handler = stateHandlers.get(LogstashProcessState.INITIALIZING);
        if (handler == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "未找到初始化状态处理器");
        }

        // 执行初始化操作
        return handler.handleInitialize(process, machines, taskId)
                .thenApply(success -> {
                    // 根据操作结果更新状态
                    LogstashProcessState nextState = handler.getNextState(OperationType.INITIALIZE, success);
                    updateState(process.getId(), nextState);
                    return success;
                });
    }

    /**
     * 执行启动操作，带任务ID
     */
    public CompletableFuture<Boolean> start(LogstashProcess process, List<Machine> machines, String taskId) {
        LogstashProcessState currentState = LogstashProcessState.valueOf(process.getState());
        LogstashProcessStateHandler handler = getStateHandler(currentState);

        if (!handler.canStart()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    String.format("当前状态[%s]不允许执行启动操作", currentState.getDescription()));
        }

        // 更新状态为启动中
        updateState(process.getId(), LogstashProcessState.STARTING);

        // 执行启动操作
        return handler.handleStart(process, machines, taskId)
                .thenApply(success -> {
                    // 根据操作结果更新状态
                    LogstashProcessState nextState = handler.getNextState(OperationType.START, success);
                    updateState(process.getId(), nextState);
                    return success;
                });
    }

    /**
     * 执行停止操作，带任务ID
     */
    public CompletableFuture<Boolean> stop(LogstashProcess process, List<Machine> machines, String taskId) {
        LogstashProcessState currentState = LogstashProcessState.valueOf(process.getState());
        LogstashProcessStateHandler handler = getStateHandler(currentState);

        if (!handler.canStop()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    String.format("当前状态[%s]不允许执行停止操作", currentState.getDescription()));
        }

        // 更新状态为停止中
        updateState(process.getId(), LogstashProcessState.STOPPING);

        // 执行停止操作
        return handler.handleStop(process, machines, taskId)
                .thenApply(success -> {
                    // 根据操作结果更新状态
                    LogstashProcessState nextState = handler.getNextState(OperationType.STOP, success);
                    updateState(process.getId(), nextState);
                    return success;
                });
    }

    /**
     * 获取状态处理器
     */
    private LogstashProcessStateHandler getStateHandler(LogstashProcessState state) {
        LogstashProcessStateHandler handler = stateHandlers.get(state);
        if (handler == null) {
            // 如果没有找到对应的处理器，使用默认处理器
            handler = stateHandlers.get(LogstashProcessState.NOT_STARTED);
            if (handler == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "未找到合适的状态处理器");
            }
        }
        return handler;
    }

    /**
     * 更新进程状态
     */
    private void updateState(Long processId, LogstashProcessState state) {
        logger.info("更新进程 {} 状态: {}", processId, state.name());
        logstashProcessMapper.updateState(processId, state.name());
    }
}