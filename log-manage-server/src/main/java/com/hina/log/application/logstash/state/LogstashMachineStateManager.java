package com.hina.log.application.logstash.state;

import com.hina.log.domain.entity.LogstashMachine;
import com.hina.log.domain.entity.LogstashProcess;
import com.hina.log.domain.entity.Machine;
import com.hina.log.common.exception.BusinessException;
import com.hina.log.common.exception.ErrorCode;
import com.hina.log.application.logstash.command.LogstashCommandFactory;
import com.hina.log.application.logstash.enums.LogstashMachineState;
import com.hina.log.domain.mapper.LogstashMachineMapper;
import com.hina.log.domain.mapper.LogstashProcessMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
        
        // 如果状态变为停止状态，清除进程PID
        if (state == LogstashMachineState.NOT_STARTED || 
            state == LogstashMachineState.STOP_FAILED || 
            state == LogstashMachineState.STOPPING) {
            logstashMachineMapper.updateProcessPid(logstashProcessId, machineId, null);
            logger.info("进程 {} 在机器 {} 上状态变更为 {}，已清除PID", logstashProcessId, machineId, state.name());
        }
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

            // 先获取当前状态处理器的引用
            LogstashMachineStateHandler handlerBeforeOperation = currentHandler;
            LogstashMachineState initialState = currentState;
            
            // 标记进入初始化中状态（仅DB更新）
            stateManager.updateMachineState(process.getId(), machine.getId(), LogstashMachineState.INITIALIZING);
            logger.info("机器 [{}] 上的进程 [{}] 开始初始化操作，状态从 [{}] 临时标记为 [INITIALIZING]", 
                    machine.getId(), process.getId(), initialState.name());

            // 执行初始化操作，使用初始状态的处理器
            return handlerBeforeOperation.handleInitialize(process, machine, taskId)
                    .thenApply(success -> {
                        // 根据操作结果更新状态
                        LogstashMachineState nextState = handlerBeforeOperation.getNextState(
                                LogstashMachineStateHandler.OperationType.INITIALIZE, success);
                        // 直接更新数据库状态，不更新上下文（简化）
                        stateManager.updateMachineState(process.getId(), machine.getId(), nextState);
                        logger.info("机器 [{}] 上的进程 [{}] 初始化操作完成，最终状态设置为 [{}]", 
                                machine.getId(), process.getId(), nextState.name());
                        return success;
                    })
                    .exceptionally(e -> {
                        // 发生异常时直接更新状态为失败
                        logger.error("初始化过程中发生异常: {}", e.getMessage(), e);
                        stateManager.updateMachineState(process.getId(), machine.getId(), LogstashMachineState.INITIALIZE_FAILED);
                        logger.info("机器 [{}] 上的进程 [{}] 初始化操作异常，最终状态设置为 [INITIALIZE_FAILED]", 
                                machine.getId(), process.getId());
                        // 继续传播异常以便外部任务系统能正确处理
                        throw new CompletionException(e);
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

            // 先获取当前状态处理器的引用
            LogstashMachineStateHandler handlerBeforeOperation = currentHandler;
            LogstashMachineState initialState = currentState;
            
            // 标记进入启动中状态（仅DB更新）
            stateManager.updateMachineState(process.getId(), machine.getId(), LogstashMachineState.STARTING);
            logger.info("机器 [{}] 上的进程 [{}] 开始启动操作，状态从 [{}] 临时标记为 [STARTING]", 
                    machine.getId(), process.getId(), initialState.name());

            // 执行启动操作，使用初始状态的处理器
            return handlerBeforeOperation.handleStart(process, machine, taskId)
                    .thenApply(success -> {
                        // 根据操作结果更新状态
                        LogstashMachineState nextState = handlerBeforeOperation.getNextState(
                                LogstashMachineStateHandler.OperationType.START, success);
                        // 直接更新数据库状态，不更新上下文（简化）
                        stateManager.updateMachineState(process.getId(), machine.getId(), nextState);
                        logger.info("机器 [{}] 上的进程 [{}] 启动操作完成，最终状态设置为 [{}]", 
                                machine.getId(), process.getId(), nextState.name());
                        return success;
                    })
                    .exceptionally(e -> {
                        // 发生异常时直接更新状态为失败
                        logger.error("启动过程中发生异常: {}", e.getMessage(), e);
                        stateManager.updateMachineState(process.getId(), machine.getId(), LogstashMachineState.START_FAILED);
                        logger.info("机器 [{}] 上的进程 [{}] 启动操作异常，最终状态设置为 [START_FAILED]", 
                                machine.getId(), process.getId());
                        // 继续传播异常以便外部任务系统能正确处理
                        throw new CompletionException(e);
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

            // 先获取当前状态处理器的引用
            LogstashMachineStateHandler handlerBeforeOperation = currentHandler;
            LogstashMachineState initialState = currentState;
            
            // 标记进入停止中状态（仅DB更新）
            stateManager.updateMachineState(process.getId(), machine.getId(), LogstashMachineState.STOPPING);
            logger.info("机器 [{}] 上的进程 [{}] 开始停止操作，状态从 [{}] 临时标记为 [STOPPING]", 
                    machine.getId(), process.getId(), initialState.name());

            // 执行停止操作，使用初始状态的处理器
            return handlerBeforeOperation.handleStop(process, machine, taskId)
                    .thenApply(success -> {
                        // 根据操作结果更新状态
                        LogstashMachineState nextState = handlerBeforeOperation.getNextState(
                                LogstashMachineStateHandler.OperationType.STOP, success);
                        // 直接更新数据库状态，不更新上下文（简化）
                        stateManager.updateMachineState(process.getId(), machine.getId(), nextState);
                        logger.info("机器 [{}] 上的进程 [{}] 停止操作完成，最终状态设置为 [{}]", 
                                machine.getId(), process.getId(), nextState.name());
                        return success;
                    })
                    .exceptionally(e -> {
                        // 发生异常时直接更新状态为失败
                        logger.error("停止过程中发生异常: {}", e.getMessage(), e);
                        stateManager.updateMachineState(process.getId(), machine.getId(), LogstashMachineState.STOP_FAILED);
                        logger.info("机器 [{}] 上的进程 [{}] 停止操作异常，最终状态设置为 [STOP_FAILED]", 
                                machine.getId(), process.getId());
                        // 继续传播异常以便外部任务系统能正确处理
                        throw new CompletionException(e);
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
                            stateManager.updateMachineState(process.getId(), machine.getId(), nextState);
                        }

                        return success;
                    })
                    .exceptionally(e -> {
                        // 记录异常但保持当前状态
                        logger.error("更新配置过程中发生异常: {}", e.getMessage(), e);
                        // 继续传播异常以便外部任务系统能正确处理
                        throw new CompletionException(e);
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
                        stateManager.updateMachineState(process.getId(), machine.getId(), nextState);
                        return success;
                    })
                    .exceptionally(e -> {
                        // 记录异常但保持当前状态
                        logger.error("刷新配置过程中发生异常: {}", e.getMessage(), e);
                        // 继续传播异常以便外部任务系统能正确处理
                        throw new CompletionException(e);
                    });
        }

    }
}
