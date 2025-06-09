package com.hinadt.miaocha.application.logstash.state;

import com.hinadt.miaocha.application.logstash.command.LogstashCommandFactory;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.domain.mapper.LogstashProcessMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logstash机器状态管理器 负责管理Logstash进程在特定机器上的状态
 *
 * <p>使用状态模式 + 上下文模式重构： 1. 状态模式：不同状态下的行为差异由各个状态处理器实现 2.
 * 上下文模式：LogstashMachineContext作为上下文，持有当前状态并委托操作给状态处理器
 */
@Component
public class LogstashMachineStateManager {
    private static final Logger logger = LoggerFactory.getLogger(LogstashMachineStateManager.class);

    private final LogstashMachineMapper logstashMachineMapper;
    private final Map<LogstashMachineState, LogstashMachineStateHandler> stateHandlers;
    private final LogstashCommandFactory commandFactory;
    private final LogstashProcessMapper logstashProcessMapper;

    public LogstashMachineStateManager(
            LogstashMachineMapper logstashMachineMapper,
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
     * 获取机器上下文 创建一个新的上下文对象，封装机器状态转换和操作
     *
     * @param process Logstash进程
     * @param machineInfo 目标机器
     * @return 机器上下文对象
     */
    public LogstashMachineContext getMachineContext(
            LogstashProcess process, MachineInfo machineInfo) {
        Long processId = process.getId();
        Long machineId = machineInfo.getId();

        // 获取机器状态
        LogstashMachine logstashMachine =
                logstashMachineMapper.selectByLogstashProcessIdAndMachineId(processId, machineId);
        if (logstashMachine == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "找不到进程与机器的关联记录");
        }

        LogstashMachineState currentState =
                LogstashMachineState.valueOf(logstashMachine.getState());
        LogstashMachineStateHandler currentHandler = getStateHandler(currentState);

        return new LogstashMachineContext(process, machineInfo, currentState, currentHandler, this);
    }

    /**
     * 更新机器上的进程状态
     *
     * @param logstashProcessId 进程ID
     * @param machineId 机器ID
     * @param state 新状态
     */
    public void updateMachineState(
            Long logstashProcessId, Long machineId, LogstashMachineState state) {
        logger.info("更新进程 {} 在机器 {} 上的状态: {}", logstashProcessId, machineId, state.name());
        logstashMachineMapper.updateState(logstashProcessId, machineId, state.name());

        // 如果状态变为停止状态，清除进程PID
        if (state == LogstashMachineState.NOT_STARTED
                || state == LogstashMachineState.STOP_FAILED
                || state == LogstashMachineState.STOPPING) {
            logstashMachineMapper.updateProcessPid(logstashProcessId, machineId, null);
            logger.info(
                    "进程 {} 在机器 {} 上状态变更为 {}，已清除PID", logstashProcessId, machineId, state.name());
        }
    }

    /**
     * 初始化机器状态
     *
     * @param logstashProcessId 进程ID
     * @param machineId 机器ID
     */
    public void initializeMachineState(Long logstashProcessId, Long machineId) {
        LogstashMachine machine =
                logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                        logstashProcessId, machineId);
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
     * @param machineInfo 机器
     * @param taskId 任务ID
     * @return 异步操作结果
     */
    public CompletableFuture<Boolean> initializeMachine(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        LogstashMachineContext context = getMachineContext(process, machineInfo);
        return context.initialize(taskId);
    }

    /**
     * 启动特定机器上的进程
     *
     * @param process 进程
     * @param machineInfo 机器
     * @param taskId 任务ID
     * @return 异步操作结果
     */
    public CompletableFuture<Boolean> startMachine(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        LogstashMachineContext context = getMachineContext(process, machineInfo);
        return context.start(taskId);
    }

    /**
     * 停止特定机器上的进程
     *
     * @param process 进程
     * @param machineInfo 机器
     * @param taskId 任务ID
     * @return 异步操作结果
     */
    public CompletableFuture<Boolean> stopMachine(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        LogstashMachineContext context = getMachineContext(process, machineInfo);
        return context.stop(taskId);
    }

    /**
     * 强制停止特定机器上的进程 应急停止功能：执行原有的停止逻辑，但无论命令成功与否，都强制将状态更改为未启动
     *
     * @param process 进程
     * @param machineInfo 机器
     * @param taskId 任务ID
     * @return 异步操作结果
     */
    public CompletableFuture<Boolean> forceStopMachine(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        LogstashMachineContext context = getMachineContext(process, machineInfo);
        return context.forceStop(taskId);
    }

    /**
     * 更新特定机器上的多种配置
     *
     * @param processId 进程ID
     * @param machineInfo 机器
     * @param configContent 主配置内容
     * @param jvmOptions JVM选项
     * @param logstashYml logstash.yml配置
     * @param taskId 任务ID
     * @return 异步操作结果
     */
    public CompletableFuture<Boolean> updateMachineConfig(
            Long processId,
            MachineInfo machineInfo,
            String configContent,
            String jvmOptions,
            String logstashYml,
            String taskId) {
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            logger.error("找不到指定的Logstash进程: {}", processId);
            return CompletableFuture.completedFuture(false);
        }
        LogstashMachineContext context = getMachineContext(process, machineInfo);
        // 执行更新操作
        return context.updateConfig(configContent, jvmOptions, logstashYml, taskId);
    }

    /**
     * 刷新特定机器上的配置
     *
     * @param process 进程
     * @param machineInfo 机器
     * @param taskId 任务ID
     * @return 异步操作结果
     */
    public CompletableFuture<Boolean> refreshMachineConfig(
            LogstashProcess process, MachineInfo machineInfo, String taskId) {
        LogstashMachineContext context = getMachineContext(process, machineInfo);
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

    /** Logstash机器状态上下文 持有当前状态，并将操作委托给对应的状态处理器 */
    public class LogstashMachineContext {
        /**
         * -- GETTER -- 获取进程信息
         *
         * @return 进程信息
         */
        @Getter private final LogstashProcess process;

        /**
         * -- GETTER -- 获取机器信息
         *
         * @return 机器信息
         */
        @Getter private final MachineInfo machineInfo;

        /**
         * -- GETTER -- 获取当前状态
         *
         * @return 当前状态
         */
        @Getter private LogstashMachineState currentState;

        /**
         * -- GETTER -- 获取当前状态处理器
         *
         * @return 当前状态处理器
         */
        @Getter private LogstashMachineStateHandler currentHandler;

        private final LogstashMachineStateManager stateManager;

        public LogstashMachineContext(
                LogstashProcess process,
                MachineInfo machineInfo,
                LogstashMachineState initialState,
                LogstashMachineStateHandler initialHandler,
                LogstashMachineStateManager stateManager) {
            this.process = process;
            this.machineInfo = machineInfo;
            this.currentState = initialState;
            this.currentHandler = initialHandler;
            this.stateManager = stateManager;
        }

        /**
         * 初始化机器上的进程
         *
         * @param taskId 任务ID
         * @return 异步操作结果
         */
        public CompletableFuture<Boolean> initialize(String taskId) {
            if (!currentHandler.canInitialize()) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        String.format("当前状态[%s]不允许执行初始化操作", currentState.getDescription()));
            }

            // 先获取当前状态处理器的引用
            LogstashMachineStateHandler handlerBeforeOperation = currentHandler;
            LogstashMachineState initialState = currentState;

            // 标记进入初始化中状态（仅DB更新）
            stateManager.updateMachineState(
                    process.getId(), machineInfo.getId(), LogstashMachineState.INITIALIZING);
            logger.info(
                    "机器 [{}] 上的进程 [{}] 开始初始化操作，状态从 [{}] 临时标记为 [INITIALIZING]",
                    machineInfo.getId(),
                    process.getId(),
                    initialState.name());

            // 执行初始化操作，使用初始状态的处理器
            return handlerBeforeOperation
                    .handleInitialize(process, machineInfo, taskId)
                    .thenApply(
                            success -> {
                                // 根据操作结果更新状态
                                LogstashMachineState nextState =
                                        handlerBeforeOperation.getNextState(
                                                LogstashMachineStateHandler.OperationType
                                                        .INITIALIZE,
                                                success);
                                // 直接更新数据库状态，不更新上下文（简化）
                                stateManager.updateMachineState(
                                        process.getId(), machineInfo.getId(), nextState);
                                logger.info(
                                        "机器 [{}] 上的进程 [{}] 初始化操作完成，最终状态设置为 [{}]",
                                        machineInfo.getId(),
                                        process.getId(),
                                        nextState.name());
                                return success;
                            })
                    .exceptionally(
                            e -> {
                                // 发生异常时直接更新状态为失败
                                stateManager.updateMachineState(
                                        process.getId(),
                                        machineInfo.getId(),
                                        LogstashMachineState.INITIALIZE_FAILED);
                                logger.info(
                                        "机器 [{}] 上的进程 [{}] 初始化操作异常，最终状态设置为 [INITIALIZE_FAILED]",
                                        machineInfo.getId(),
                                        process.getId());
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
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        String.format("当前状态[%s]不允许执行启动操作", currentState.getDescription()));
            }

            // 先获取当前状态处理器的引用
            LogstashMachineStateHandler handlerBeforeOperation = currentHandler;
            LogstashMachineState initialState = currentState;

            // 标记进入启动中状态（仅DB更新）
            stateManager.updateMachineState(
                    process.getId(), machineInfo.getId(), LogstashMachineState.STARTING);
            logger.info(
                    "机器 [{}] 上的进程 [{}] 开始启动操作，状态从 [{}] 临时标记为 [STARTING]",
                    machineInfo.getId(),
                    process.getId(),
                    initialState.name());

            // 执行启动操作，使用初始状态的处理器
            return handlerBeforeOperation
                    .handleStart(process, machineInfo, taskId)
                    .thenApply(
                            success -> {
                                // 根据操作结果更新状态
                                LogstashMachineState nextState =
                                        handlerBeforeOperation.getNextState(
                                                LogstashMachineStateHandler.OperationType.START,
                                                success);
                                // 直接更新数据库状态，不更新上下文（简化）
                                stateManager.updateMachineState(
                                        process.getId(), machineInfo.getId(), nextState);
                                logger.info(
                                        "机器 [{}] 上的进程 [{}] 启动操作完成，最终状态设置为 [{}]",
                                        machineInfo.getId(),
                                        process.getId(),
                                        nextState.name());
                                return success;
                            })
                    .exceptionally(
                            e -> {
                                // 发生异常时直接更新状态为失败
                                stateManager.updateMachineState(
                                        process.getId(),
                                        machineInfo.getId(),
                                        LogstashMachineState.START_FAILED);
                                logger.info(
                                        "机器 [{}] 上的进程 [{}] 启动操作异常，最终状态设置为 [START_FAILED]",
                                        machineInfo.getId(),
                                        process.getId());
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
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        String.format("当前状态[%s]不允许执行停止操作", currentState.getDescription()));
            }

            // 先获取当前状态处理器的引用
            LogstashMachineStateHandler handlerBeforeOperation = currentHandler;
            LogstashMachineState initialState = currentState;

            // 标记进入停止中状态（仅DB更新）
            stateManager.updateMachineState(
                    process.getId(), machineInfo.getId(), LogstashMachineState.STOPPING);
            logger.info(
                    "机器 [{}] 上的进程 [{}] 开始停止操作，状态从 [{}] 临时标记为 [STOPPING]",
                    machineInfo.getId(),
                    process.getId(),
                    initialState.name());

            // 执行停止操作，使用初始状态的处理器
            return handlerBeforeOperation
                    .handleStop(process, machineInfo, taskId)
                    .thenApply(
                            success -> {
                                // 根据操作结果更新状态
                                LogstashMachineState nextState =
                                        handlerBeforeOperation.getNextState(
                                                LogstashMachineStateHandler.OperationType.STOP,
                                                success);
                                // 直接更新数据库状态，不更新上下文（简化）
                                stateManager.updateMachineState(
                                        process.getId(), machineInfo.getId(), nextState);
                                logger.info(
                                        "机器 [{}] 上的进程 [{}] 停止操作完成，最终状态设置为 [{}]",
                                        machineInfo.getId(),
                                        process.getId(),
                                        nextState.name());
                                return success;
                            })
                    .exceptionally(
                            e -> {
                                // 发生异常时直接更新状态为失败
                                stateManager.updateMachineState(
                                        process.getId(),
                                        machineInfo.getId(),
                                        LogstashMachineState.STOP_FAILED);
                                logger.info(
                                        "机器 [{}] 上的进程 [{}] 停止操作异常，最终状态设置为 [STOP_FAILED]",
                                        machineInfo.getId(),
                                        process.getId());
                                // 继续传播异常以便外部任务系统能正确处理
                                throw new CompletionException(e);
                            });
        }

        /**
         * 强制停止机器上的进程 应急停止功能：执行原有的停止逻辑，但无论命令成功与否，都强制将状态更改为未启动
         *
         * @param taskId 任务ID
         * @return 异步操作结果
         */
        public CompletableFuture<Boolean> forceStop(String taskId) {
            // 强制停止可以在任何状态下执行
            if (!currentHandler.canForceStop()) {
                logger.warn("状态 [{}] 不支持强制停止操作，但仍将继续尝试", currentState.getDescription());
            }

            // 先获取当前状态处理器的引用
            LogstashMachineStateHandler handlerBeforeOperation = currentHandler;
            LogstashMachineState initialState = currentState;

            // 标记进入停止中状态（仅DB更新）
            stateManager.updateMachineState(
                    process.getId(), machineInfo.getId(), LogstashMachineState.STOPPING);
            logger.warn(
                    "机器 [{}] 上的进程 [{}] 开始强制停止操作，状态从 [{}] 临时标记为 [STOPPING]",
                    machineInfo.getId(),
                    process.getId(),
                    initialState.name());

            // 执行强制停止操作，使用初始状态的处理器
            return handlerBeforeOperation
                    .handleForceStop(process, machineInfo, taskId)
                    .thenApply(
                            success -> {
                                // 强制停止总是将状态设置为未启动
                                LogstashMachineState nextState =
                                        handlerBeforeOperation.getNextState(
                                                LogstashMachineStateHandler.OperationType
                                                        .FORCE_STOP,
                                                success);
                                // 直接更新数据库状态，不更新上下文（简化）
                                stateManager.updateMachineState(
                                        process.getId(), machineInfo.getId(), nextState);
                                logger.warn(
                                        "机器 [{}] 上的进程 [{}] 强制停止操作完成，最终状态强制设置为 [{}]",
                                        machineInfo.getId(),
                                        process.getId(),
                                        nextState.name());
                                return true; // 强制停止总是返回成功
                            })
                    .exceptionally(
                            e -> {
                                // 即使发生异常，强制停止也要设置为未启动状态
                                stateManager.updateMachineState(
                                        process.getId(),
                                        machineInfo.getId(),
                                        LogstashMachineState.NOT_STARTED);
                                logger.warn(
                                        "机器 [{}] 上的进程 [{}] 强制停止操作异常，但仍强制设置为 [NOT_STARTED]: {}",
                                        machineInfo.getId(),
                                        process.getId(),
                                        e.getMessage());
                                return true; // 强制停止总是返回成功
                            });
        }

        /**
         * 更新机器上的配置
         *
         * @param configContent 主配置内容，null表示不更新
         * @param jvmOptions JVM配置内容，null表示不更新
         * @param logstashYml 系统配置内容，null表示不更新
         * @param taskId 任务ID
         * @return 异步操作结果
         */
        public CompletableFuture<Boolean> updateConfig(
                String configContent, String jvmOptions, String logstashYml, String taskId) {
            if (!currentHandler.canUpdateConfig()) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        String.format("当前状态[%s]不允许执行更新配置操作", currentState.getDescription()));
            }

            // 执行更新配置操作，委托给当前状态处理器
            return currentHandler
                    .handleUpdateConfig(
                            process, configContent, jvmOptions, logstashYml, machineInfo, taskId)
                    .thenApply(
                            success -> {
                                // 根据操作结果更新状态
                                LogstashMachineState nextState =
                                        currentHandler.getNextState(
                                                LogstashMachineStateHandler.OperationType
                                                        .UPDATE_CONFIG,
                                                success);
                                if (nextState != currentState) {
                                    stateManager.updateMachineState(
                                            process.getId(), machineInfo.getId(), nextState);
                                }

                                return success;
                            })
                    .exceptionally(
                            e -> {
                                // 记录异常但保持当前状态（更新配置失败不改变机器状态）
                                logger.info(
                                        "机器 [{}] 上的进程 [{}] 更新配置操作异常，状态保持为 [{}]",
                                        machineInfo.getId(),
                                        process.getId(),
                                        currentState.name());
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
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        String.format("当前状态[%s]不允许执行刷新配置操作", currentState.getDescription()));
            }

            // 执行刷新配置操作
            return currentHandler
                    .handleRefreshConfig(process, machineInfo, taskId)
                    .thenApply(
                            success -> {
                                // 根据操作结果更新状态
                                LogstashMachineState nextState =
                                        currentHandler.getNextState(
                                                LogstashMachineStateHandler.OperationType
                                                        .REFRESH_CONFIG,
                                                success);
                                stateManager.updateMachineState(
                                        process.getId(), machineInfo.getId(), nextState);
                                return success;
                            })
                    .exceptionally(
                            e -> {
                                // 记录异常但保持当前状态（刷新配置失败不改变机器状态）
                                logger.info(
                                        "机器 [{}] 上的进程 [{}] 刷新配置操作异常，状态保持为 [{}]",
                                        machineInfo.getId(),
                                        process.getId(),
                                        currentState.name());
                                // 继续传播异常以便外部任务系统能正确处理
                                throw new CompletionException(e);
                            });
        }
    }
}
