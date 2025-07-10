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
import com.hinadt.miaocha.domain.mapper.MachineMapper;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Logstash机器状态管理器 负责管理LogstashMachine实例的状态转换和操作执行 */
@Slf4j
@Component
public class LogstashMachineStateManager {

    private final LogstashMachineMapper logstashMachineMapper;
    private final Map<LogstashMachineState, LogstashMachineStateHandler> stateHandlers;
    private final LogstashCommandFactory commandFactory;
    private final LogstashProcessMapper logstashProcessMapper;
    private final MachineMapper machineMapper;

    /** 构造函数，初始化状态处理器映射 */
    public LogstashMachineStateManager(
            LogstashMachineMapper logstashMachineMapper,
            List<LogstashMachineStateHandler> handlers,
            LogstashCommandFactory commandFactory,
            LogstashProcessMapper logstashProcessMapper,
            MachineMapper machineMapper) {

        this.logstashMachineMapper = logstashMachineMapper;
        this.commandFactory = commandFactory;
        this.logstashProcessMapper = logstashProcessMapper;
        this.machineMapper = machineMapper;

        // 将处理器列表转换为状态映射
        this.stateHandlers =
                handlers.stream()
                        .collect(
                                Collectors.toMap(
                                        LogstashMachineStateHandler::getState,
                                        Function.identity()));

        log.info("初始化LogstashMachineStateManager，支持状态: {}", stateHandlers.keySet());
    }

    // ==================== 实例上下文获取 ====================

    /** 获取LogstashMachine实例的上下文 */
    public LogstashMachineContext getInstanceContext(Long logstashMachineId) {
        LogstashMachine logstashMachine = logstashMachineMapper.selectById(logstashMachineId);
        if (logstashMachine == null) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "找不到LogstashMachine实例: " + logstashMachineId);
        }

        LogstashProcess process =
                logstashProcessMapper.selectById(logstashMachine.getLogstashProcessId());
        if (process == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "找不到关联的Logstash进程");
        }

        MachineInfo machineInfo = machineMapper.selectById(logstashMachine.getMachineId());
        if (machineInfo == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "找不到关联的机器信息");
        }

        LogstashMachineState currentState =
                LogstashMachineState.valueOf(logstashMachine.getState());
        LogstashMachineStateHandler currentHandler = getStateHandler(currentState);

        return new LogstashMachineContext(
                process, machineInfo, logstashMachine, currentState, currentHandler, this);
    }

    // ==================== 实例操作方法 ====================

    /** 部署LogstashMachine实例 */
    public boolean deployInstance(Long logstashMachineId, String taskId) {
        log.info("开始部署LogstashMachine实例: {}", logstashMachineId);
        LogstashMachineContext context = getInstanceContext(logstashMachineId);
        return context.initialize(taskId);
    }

    /** 启动LogstashMachine实例 */
    public boolean startInstance(Long logstashMachineId, String taskId) {
        log.info("开始启动LogstashMachine实例: {}", logstashMachineId);
        LogstashMachineContext context = getInstanceContext(logstashMachineId);
        return context.start(taskId);
    }

    /** 停止LogstashMachine实例 */
    public boolean stopInstance(Long logstashMachineId, String taskId) {
        log.info("开始停止LogstashMachine实例: {}", logstashMachineId);
        LogstashMachineContext context = getInstanceContext(logstashMachineId);
        return context.stop(taskId);
    }

    /** 强制停止LogstashMachine实例 */
    public boolean forceStopInstance(Long logstashMachineId, String taskId) {
        log.info("开始强制停止LogstashMachine实例: {}", logstashMachineId);
        LogstashMachineContext context = getInstanceContext(logstashMachineId);
        return context.forceStop(taskId);
    }

    /** 删除LogstashMachine实例 */
    public boolean deleteInstance(Long logstashMachineId) {
        log.info("开始删除LogstashMachine实例: {}", logstashMachineId);
        LogstashMachineContext context = getInstanceContext(logstashMachineId);
        return context.delete();
    }

    /** 更新LogstashMachine实例配置（直接写入配置内容） */
    public boolean updateInstanceConfig(
            Long logstashMachineId,
            String configContent,
            String jvmOptions,
            String logstashYml,
            String taskId) {
        log.info("开始更新LogstashMachine实例配置: {}", logstashMachineId);
        LogstashMachineContext context = getInstanceContext(logstashMachineId);
        return context.updateConfig(configContent, jvmOptions, logstashYml, taskId);
    }

    /** 刷新LogstashMachine实例配置（从数据库查询配置内容） */
    public boolean refreshInstanceConfig(Long logstashMachineId, String taskId) {
        log.info("开始刷新LogstashMachine实例配置: {}", logstashMachineId);
        LogstashMachineContext context = getInstanceContext(logstashMachineId);
        return context.refreshConfig(taskId);
    }

    // ==================== 状态管理方法 ====================

    /** 更新LogstashMachine实例状态 */
    public void updateInstanceState(Long logstashMachineId, LogstashMachineState state) {
        log.info("更新LogstashMachine实例 {} 状态: {}", logstashMachineId, state.name());
        logstashMachineMapper.updateStateById(logstashMachineId, state.name());

        // 如果状态变为停止状态，清除进程PID
        if (shouldClearPid(state)) {
            logstashMachineMapper.updateProcessPidById(logstashMachineId, null);
            log.info("LogstashMachine实例 {} 状态变更为 {}，已清除PID", logstashMachineId, state.name());
        }
    }

    /** 获取状态处理器 */
    public LogstashMachineStateHandler getStateHandler(LogstashMachineState state) {
        LogstashMachineStateHandler handler = stateHandlers.get(state);
        if (handler == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "找不到状态处理器: " + state);
        }
        return handler;
    }

    public LogstashCommandFactory getCommandFactory() {
        return commandFactory;
    }

    // ==================== 私有辅助方法 ====================

    /** 判断是否应该清除PID */
    private boolean shouldClearPid(LogstashMachineState state) {
        return state == LogstashMachineState.NOT_STARTED
                || state == LogstashMachineState.STOP_FAILED
                || state == LogstashMachineState.STOPPING;
    }

    /** 安全执行状态转换操作 */
    private boolean safeStateTransition(
            Long instanceId,
            LogstashMachineState initialState,
            LogstashMachineState transitionState,
            LogstashMachineState successState,
            LogstashMachineState failureState,
            LogstashMachineStateHandler.OperationType operationType,
            Function<LogstashMachineStateHandler, Boolean> operation) {

        // 获取初始状态处理器
        LogstashMachineStateHandler handler = getStateHandler(initialState);

        // 设置过渡状态
        updateInstanceState(instanceId, transitionState);
        log.info(
                "LogstashMachine实例 [{}] 开始{}操作，状态从 [{}] 临时标记为 [{}]",
                instanceId,
                operationType.name(),
                initialState.name(),
                transitionState.name());

        try {
            boolean success = operation.apply(handler);
            LogstashMachineState nextState = handler.getNextState(operationType, success);
            updateInstanceState(instanceId, nextState);
            log.info(
                    "LogstashMachine实例 [{}] {}操作完成，最终状态设置为 [{}]",
                    instanceId,
                    operationType.name(),
                    nextState.name());
            return success;
        } catch (Exception e) {
            updateInstanceState(instanceId, failureState);
            log.error(
                    "LogstashMachine实例 [{}] {}操作异常，最终状态设置为 [{}]",
                    instanceId,
                    operationType.name(),
                    failureState.name());
            throw e;
        }
    }

    /** Logstash机器状态上下文 持有当前状态，并将操作委托给对应的状态处理器 */
    @Getter
    public class LogstashMachineContext {
        private final LogstashProcess process;
        private final MachineInfo machineInfo;
        private final LogstashMachine logstashMachine;
        private final LogstashMachineState currentState;
        private final LogstashMachineStateHandler currentHandler;
        private final LogstashMachineStateManager stateManager;

        public LogstashMachineContext(
                LogstashProcess process,
                MachineInfo machineInfo,
                LogstashMachine logstashMachine,
                LogstashMachineState currentState,
                LogstashMachineStateHandler currentHandler,
                LogstashMachineStateManager stateManager) {
            this.process = process;
            this.machineInfo = machineInfo;
            this.logstashMachine = logstashMachine;
            this.currentState = currentState;
            this.currentHandler = currentHandler;
            this.stateManager = stateManager;
        }

        /** 初始化机器上的进程 */
        public boolean initialize(String taskId) {
            validateOperation(currentHandler::canInitialize, "初始化");

            return safeStateTransition(
                    logstashMachine.getId(),
                    currentState,
                    LogstashMachineState.INITIALIZING,
                    LogstashMachineState.NOT_STARTED,
                    LogstashMachineState.INITIALIZE_FAILED,
                    LogstashMachineStateHandler.OperationType.INITIALIZE,
                    handler -> handler.handleInitialize(logstashMachine, machineInfo, taskId));
        }

        /** 启动机器上的进程 */
        public boolean start(String taskId) {
            validateOperation(currentHandler::canStart, "启动");

            return safeStateTransition(
                    logstashMachine.getId(),
                    currentState,
                    LogstashMachineState.STARTING,
                    LogstashMachineState.RUNNING,
                    LogstashMachineState.START_FAILED,
                    LogstashMachineStateHandler.OperationType.START,
                    handler -> handler.handleStart(logstashMachine, machineInfo, taskId));
        }

        /** 停止机器上的进程 */
        public boolean stop(String taskId) {
            validateOperation(currentHandler::canStop, "停止");

            return safeStateTransition(
                    logstashMachine.getId(),
                    currentState,
                    LogstashMachineState.STOPPING,
                    LogstashMachineState.NOT_STARTED,
                    LogstashMachineState.STOP_FAILED,
                    LogstashMachineStateHandler.OperationType.STOP,
                    handler -> handler.handleStop(logstashMachine, machineInfo, taskId));
        }

        /** 强制停止机器上的进程 */
        public boolean forceStop(String taskId) {
            // 强制停止不检查状态
            stateManager.updateInstanceState(
                    logstashMachine.getId(), LogstashMachineState.STOPPING);
            log.warn(
                    "LogstashMachine实例 [{}] 开始强制停止操作，状态从 [{}] 临时标记为 [STOPPING]",
                    logstashMachine.getId(),
                    currentState.name());

            try {
                boolean success =
                        currentHandler.handleForceStop(logstashMachine, machineInfo, taskId);
                LogstashMachineState nextState =
                        currentHandler.getNextState(
                                LogstashMachineStateHandler.OperationType.FORCE_STOP, success);
                stateManager.updateInstanceState(logstashMachine.getId(), nextState);
                log.warn(
                        "LogstashMachine实例 [{}] 强制停止操作完成，最终状态强制设置为 [{}]",
                        logstashMachine.getId(),
                        nextState.name());
                return true; // 强制停止总是返回成功
            } catch (Exception e) {
                stateManager.updateInstanceState(
                        logstashMachine.getId(), LogstashMachineState.NOT_STARTED);
                log.warn(
                        "LogstashMachine实例 [{}] 强制停止操作异常，但仍强制设置为 [NOT_STARTED]: {}",
                        logstashMachine.getId(),
                        e.getMessage());
                return true; // 强制停止总是返回成功
            }
        }

        /** 更新机器上的配置（直接写入提供的配置内容） */
        public boolean updateConfig(
                String configContent, String jvmOptions, String logstashYml, String taskId) {
            validateOperation(currentHandler::canUpdateConfig, "配置更新");

            try {
                boolean success =
                        currentHandler.handleUpdateConfig(
                                logstashMachine,
                                configContent,
                                jvmOptions,
                                logstashYml,
                                machineInfo,
                                taskId);
                log.info(
                        "LogstashMachine实例 [{}] 配置更新操作完成，状态保持为 [{}]",
                        logstashMachine.getId(),
                        currentState.name());
                return success;
            } catch (Exception e) {
                log.error(
                        "LogstashMachine实例 [{}] 配置更新操作异常，状态保持为 [{}]",
                        logstashMachine.getId(),
                        currentState.name());
                throw e;
            }
        }

        /** 刷新机器上的配置（从数据库查询配置内容） */
        public boolean refreshConfig(String taskId) {
            validateOperation(currentHandler::canRefreshConfig, "配置刷新");

            try {
                boolean success =
                        currentHandler.handleRefreshConfig(logstashMachine, machineInfo, taskId);
                log.info(
                        "LogstashMachine实例 [{}] 配置刷新操作完成，状态保持为 [{}]",
                        logstashMachine.getId(),
                        currentState.name());
                return success;
            } catch (Exception e) {
                log.error(
                        "LogstashMachine实例 [{}] 配置刷新操作异常，状态保持为 [{}]",
                        logstashMachine.getId(),
                        currentState.name());
                throw e;
            }
        }

        /** 删除LogstashMachine实例 */
        public boolean delete() {
            validateOperation(currentHandler::canDelete, "删除");

            log.info(
                    "LogstashMachine实例 [{}] 开始删除操作，当前状态: [{}]",
                    logstashMachine.getId(),
                    currentState.name());

            try {
                boolean success = currentHandler.handleDelete(logstashMachine, machineInfo);
                log.info(
                        "LogstashMachine实例 [{}] 删除操作完成，结果: {}",
                        logstashMachine.getId(),
                        success ? "成功" : "失败");
                // 回调删除
                logstashMachineMapper.deleteById(logstashMachine.getId());
                return success;
            } catch (Exception e) {
                log.error(
                        "LogstashMachine实例 [{}] 删除操作异常: {}",
                        logstashMachine.getId(),
                        e.getMessage(),
                        e);
                throw e;
            }
        }

        /** 验证操作是否被允许 */
        private void validateOperation(
                java.util.function.BooleanSupplier canOperate, String operationName) {
            if (!canOperate.getAsBoolean()) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        String.format(
                                "当前状态[%s]不允许执行%s操作", currentState.getDescription(), operationName));
            }
        }
    }
}
