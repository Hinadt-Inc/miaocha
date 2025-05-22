package com.hina.log.logstash.state;

import com.hina.log.entity.LogstashMachine;
import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.logstash.command.LogstashCommandFactory;
import com.hina.log.logstash.enums.LogstashMachineState;
import com.hina.log.mapper.LogstashMachineMapper;
import com.hina.log.mapper.LogstashProcessMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Logstash机器状态管理器
 * 负责管理Logstash进程在特定机器上的状态
 * <p>
 * 使用状态模式 + 上下文模式重构：
 * 1. 状态模式：不同状态下的行为差异由各个状态处理器实现
 * 2. 上下文模式：LogstashMachineContext作为上下文，持有当前状态并委托操作给状态处理器
 */
@Component
public class LogstashMachineStateManager {
    private static final Logger logger = LoggerFactory.getLogger(LogstashMachineStateManager.class);

    private final LogstashMachineMapper logstashMachineMapper;
    private final Map<LogstashMachineState, LogstashMachineStateHandler> stateHandlers;
    private final LogstashCommandFactory commandFactory;
    private final LogstashProcessMapper logstashProcessMapper;

    public LogstashMachineStateManager(LogstashMachineMapper logstashMachineMapper,
                                       List<LogstashMachineStateHandler> handlers,
                                       LogstashCommandFactory commandFactory,
                                       LogstashProcessMapper logstashProcessMapper) {
        this.logstashMachineMapper = logstashMachineMapper;
        this.commandFactory = commandFactory;
        this.stateHandlers = new HashMap<>();
        this.logstashProcessMapper = logstashProcessMapper;

        // 注册所有状态处理器
        for (LogstashMachineStateHandler handler : handlers) {
            stateHandlers.put(handler.getState(), handler);
        }

        logger.info("Registered {} Logstash machine state handlers", stateHandlers.size());
    }

    /**
     * 获取机器上下文
     * 创建一个新的上下文对象，封装机器状态转换和操作
     *
     * @param process Logstash进程
     * @param machine 目标机器
     * @return 机器上下文对象
     */
    public LogstashMachineContext getMachineContext(LogstashProcess process, Machine machine) {
        Long processId = process.getId();
        Long machineId = machine.getId();

        // 获取机器状态
        LogstashMachine logstashMachine = logstashMachineMapper.selectByLogstashProcessIdAndMachineId(processId, machineId);
        if (logstashMachine == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "找不到进程与机器的关联记录");
        }

        LogstashMachineState currentState = LogstashMachineState.valueOf(logstashMachine.getState());
        LogstashMachineStateHandler currentHandler = getStateHandler(currentState);

        return new LogstashMachineContext(process, machine, currentState, currentHandler, this);
    }

    /**
     * 更新机器上的进程状态
     *
     * @param logstashProcessId 进程ID
     * @param machineId         机器ID
     * @param state             新状态
     */
    public void updateMachineState(Long logstashProcessId, Long machineId, LogstashMachineState state) {
        logger.info("更新进程 {} 在机器 {} 上的状态: {}", logstashProcessId, machineId, state.name());
        logstashMachineMapper.updateState(logstashProcessId, machineId, state.name());
    }

    /**
     * 初始化机器状态
     *
     * @param logstashProcessId 进程ID
     * @param machineId         机器ID
     */
    public void initializeMachineState(Long logstashProcessId, Long machineId) {
        LogstashMachine machine = logstashMachineMapper.selectByLogstashProcessIdAndMachineId(logstashProcessId, machineId);
        if (machine == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "找不到进程与机器的关联记录");
        }

        // 设置初始状态为INITIALIZING
        machine.setState(LogstashMachineState.INITIALIZING.name());
        logstashMachineMapper.update(machine);
    }

    /**
     * 获取状态处理器
     *
     * @param state 状态
     * @return 状态处理器
     */
    public LogstashMachineStateHandler getStateHandler(LogstashMachineState state) {
        LogstashMachineStateHandler handler = stateHandlers.get(state);
        if (handler == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "找不到状态处理器: " + state);
        }
        return handler;
    }

    /**
     * 初始化特定机器上的进程环境
     *
     * @param process 进程
     * @param machine 机器
     * @param taskId  任务ID
     * @return 异步操作结果
     */
    public CompletableFuture<Boolean> initializeMachine(LogstashProcess process, Machine machine, String taskId) {
        LogstashMachineContext context = getMachineContext(process, machine);
        return context.initialize(taskId);
    }

    /**
     * 启动特定机器上的进程
     *
     * @param process 进程
     * @param machine 机器
     * @param taskId  任务ID
     * @return 异步操作结果
     */
    public CompletableFuture<Boolean> startMachine(LogstashProcess process, Machine machine, String taskId) {
        LogstashMachineContext context = getMachineContext(process, machine);
        return context.start(taskId);
    }

    /**
     * 停止特定机器上的进程
     *
     * @param process 进程
     * @param machine 机器
     * @param taskId  任务ID
     * @return 异步操作结果
     */
    public CompletableFuture<Boolean> stopMachine(LogstashProcess process, Machine machine, String taskId) {
        LogstashMachineContext context = getMachineContext(process, machine);
        return context.stop(taskId);
    }

    /**
     * 更新特定机器上的多种配置
     *
     * @param processId     进程ID
     * @param machine       机器
     * @param configContent 主配置内容
     * @param jvmOptions    JVM选项
     * @param logstashYml   logstash.yml配置
     * @param taskId        任务ID
     * @return 异步操作结果
     */
    public CompletableFuture<Boolean> updateMachineConfig(Long processId, Machine machine,
                                                          String configContent, String jvmOptions,
                                                          String logstashYml, String taskId) {
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            logger.error("找不到指定的Logstash进程: {}", processId);
            return CompletableFuture.completedFuture(false);
        }
        LogstashMachineContext context = getMachineContext(process, machine);
        // 执行更新操作
        return context.updateConfig(configContent, jvmOptions, logstashYml, taskId);
    }

    /**
     * 刷新特定机器上的配置
     *
     * @param process 进程
     * @param machine 机器
     * @param taskId  任务ID
     * @return 异步操作结果
     */
    public CompletableFuture<Boolean> refreshMachineConfig(LogstashProcess process, Machine machine, String taskId) {
        LogstashMachineContext context = getMachineContext(process, machine);
        return context.refreshConfig(taskId);
    }

    /**
     * 获取命令工厂
     *
     * @return 命令工厂
     */
    public LogstashCommandFactory getCommandFactory() {
        return commandFactory;
    }

    /**
     * Logstash机器状态上下文
     * 持有当前状态，并将操作委托给对应的状态处理器
     */
    public class LogstashMachineContext {
        /**
         * -- GETTER --
         * 获取进程信息
         *
         * @return 进程信息
         */
        @Getter
        private final LogstashProcess process;
        /**
         * -- GETTER --
         * 获取机器信息
         *
         * @return 机器信息
         */
        @Getter
        private final Machine machine;
        /**
         * -- GETTER --
         * 获取当前状态
         *
         * @return 当前状态
         */
        @Getter
        private LogstashMachineState currentState;
        /**
         * -- GETTER --
         * 获取当前状态处理器
         *
         * @return 当前状态处理器
         */
        @Getter
        private LogstashMachineStateHandler currentHandler;
        private final LogstashMachineStateManager stateManager;

        public LogstashMachineContext(LogstashProcess process, Machine machine,
                                      LogstashMachineState initialState,
                                      LogstashMachineStateHandler initialHandler,
                                      LogstashMachineStateManager stateManager) {
            this.process = process;
            this.machine = machine;
            this.currentState = initialState;
            this.currentHandler = initialHandler;
            this.stateManager = stateManager;
        }

        /**
         * 转换到新状态
         *
         * @param newState 新状态
         */
        public void transitionTo(LogstashMachineState newState) {
            stateManager.updateMachineState(process.getId(), machine.getId(), newState);
            this.currentState = newState;
            this.currentHandler = stateManager.getStateHandler(newState);
        }

        /**
         * 初始化机器上的进程环境
         *
         * @param taskId 任务ID
         * @return 异步操作结果
         */
        public CompletableFuture<Boolean> initialize(String taskId) {
            if (!currentHandler.canInitialize()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        String.format("当前状态[%s]不允许执行初始化操作", currentState.getDescription()));
            }

            // 更新状态为初始化中
            transitionTo(LogstashMachineState.INITIALIZING);

            // 执行初始化操作
            return currentHandler.handleInitialize(process, machine, taskId)
                    .thenApply(success -> {
                        // 根据操作结果更新状态
                        LogstashMachineState nextState = currentHandler.getNextState(
                                LogstashMachineStateHandler.OperationType.INITIALIZE, success);
                        transitionTo(nextState);
                        return success;
                    });
        }

        /**
         * 启动机器上的进程
         *
         * @param taskId 任务ID
         * @return 异步操作结果
         */
        public CompletableFuture<Boolean> start(String taskId) {
            if (!currentHandler.canStart()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        String.format("当前状态[%s]不允许执行启动操作", currentState.getDescription()));
            }

            // 更新状态为启动中
            transitionTo(LogstashMachineState.STARTING);

            // 执行启动操作
            return currentHandler.handleStart(process, machine, taskId)
                    .thenApply(success -> {
                        // 根据操作结果更新状态
                        LogstashMachineState nextState = currentHandler.getNextState(
                                LogstashMachineStateHandler.OperationType.START, success);
                        transitionTo(nextState);
                        return success;
                    });
        }

        /**
         * 停止机器上的进程
         *
         * @param taskId 任务ID
         * @return 异步操作结果
         */
        public CompletableFuture<Boolean> stop(String taskId) {
            if (!currentHandler.canStop()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        String.format("当前状态[%s]不允许执行停止操作", currentState.getDescription()));
            }

            // 更新状态为停止中
            transitionTo(LogstashMachineState.STOPPING);

            // 执行停止操作
            return currentHandler.handleStop(process, machine, taskId)
                    .thenApply(success -> {
                        // 根据操作结果更新状态
                        LogstashMachineState nextState = currentHandler.getNextState(
                                LogstashMachineStateHandler.OperationType.STOP, success);
                        transitionTo(nextState);
                        return success;
                    });
        }

        /**
         * 更新机器上的配置
         *
         * @param configContent 主配置内容，null表示不更新
         * @param jvmOptions    JVM配置内容，null表示不更新
         * @param logstashYml   系统配置内容，null表示不更新
         * @param taskId        任务ID
         * @return 异步操作结果
         */
        public CompletableFuture<Boolean> updateConfig(String configContent, String jvmOptions, String logstashYml, String taskId) {
            if (!currentHandler.canUpdateConfig()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        String.format("当前状态[%s]不允许执行更新配置操作", currentState.getDescription()));
            }

            // 执行更新配置操作，委托给当前状态处理器
            return currentHandler.handleUpdateConfig(process, configContent, jvmOptions, logstashYml, machine, taskId)
                    .thenApply(success -> {
                        // 根据操作结果更新状态
                        LogstashMachineState nextState = currentHandler.getNextState(
                                LogstashMachineStateHandler.OperationType.UPDATE_CONFIG, success);
                        if (nextState != currentState) {
                            transitionTo(nextState);
                        }

                        return success;
                    });
        }

        /**
         * 刷新机器上的配置
         *
         * @param taskId 任务ID
         * @return 异步操作结果
         */
        public CompletableFuture<Boolean> refreshConfig(String taskId) {
            if (!currentHandler.canRefreshConfig()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        String.format("当前状态[%s]不允许执行刷新配置操作", currentState.getDescription()));
            }

            // 执行刷新配置操作
            return currentHandler.handleRefreshConfig(process, machine, taskId)
                    .thenApply(success -> {
                        // 根据操作结果更新状态
                        LogstashMachineState nextState = currentHandler.getNextState(
                                LogstashMachineStateHandler.OperationType.REFRESH_CONFIG, success);
                        transitionTo(nextState);
                        return success;
                    });
        }

    }
}
