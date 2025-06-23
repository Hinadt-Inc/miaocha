package com.hinadt.miaocha.application.logstash.state;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import java.util.concurrent.CompletableFuture;

/** Logstash机器状态处理器接口 使用状态模式处理不同状态下的行为 */
public interface LogstashMachineStateHandler {

    /**
     * 获取当前处理器能处理的状态
     *
     * @return 状态枚举
     */
    LogstashMachineState getState();

    /**
     * 处理初始化操作
     *
     * @param logstashMachine LogstashMachine实例
     * @param machineInfo 目标机器
     * @param taskId 任务ID，可以为null
     * @return 异步操作结果
     */
    CompletableFuture<Boolean> handleInitialize(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId);

    /**
     * 处理启动操作
     *
     * @param logstashMachine LogstashMachine实例
     * @param machineInfo 目标机器
     * @param taskId 任务ID，可以为null
     * @return 异步操作结果
     */
    CompletableFuture<Boolean> handleStart(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId);

    /**
     * 处理停止操作
     *
     * @param logstashMachine LogstashMachine实例
     * @param machineInfo 目标机器
     * @param taskId 任务ID，可以为null
     * @return 异步操作结果
     */
    CompletableFuture<Boolean> handleStop(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId);

    /**
     * 处理强制停止操作 应急停止功能：执行原有的停止逻辑，但无论命令成功与否，都强制将状态更改为未启动 用于应急情况下确保进程状态的一致性
     *
     * @param logstashMachine LogstashMachine实例
     * @param machineInfo 目标机器
     * @param taskId 任务ID，可以为null
     * @return 异步操作结果
     */
    CompletableFuture<Boolean> handleForceStop(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId);

    /**
     * 处理更新配置操作
     *
     * @param logstashMachine LogstashMachine实例
     * @param configContent 主配置内容，null表示不更新
     * @param jvmOptions JVM选项内容，null表示不更新
     * @param logstashYml logstash.yml配置内容，null表示不更新
     * @param machineInfo 目标机器
     * @param taskId 任务ID，可以为null
     * @return 异步操作结果
     */
    CompletableFuture<Boolean> handleUpdateConfig(
            LogstashMachine logstashMachine,
            String configContent,
            String jvmOptions,
            String logstashYml,
            MachineInfo machineInfo,
            String taskId);

    /**
     * 处理刷新配置操作
     *
     * @param logstashMachine LogstashMachine实例
     * @param machineInfo 目标机器
     * @param taskId 任务ID，可以为null
     * @return 异步操作结果
     */
    CompletableFuture<Boolean> handleRefreshConfig(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId);

    /**
     * 处理删除操作
     *
     * @param logstashMachine LogstashMachine实例
     * @param machineInfo 目标机器
     * @return 异步操作结果
     */
    CompletableFuture<Boolean> handleDelete(
            LogstashMachine logstashMachine, MachineInfo machineInfo);

    /**
     * 判断当前状态是否可以执行初始化操作
     *
     * @return 是否可以初始化
     */
    boolean canInitialize();

    /**
     * 判断当前状态是否可以执行启动操作
     *
     * @return 是否可以启动
     */
    boolean canStart();

    /**
     * 判断当前状态是否可以执行停止操作
     *
     * @return 是否可以停止
     */
    boolean canStop();

    /**
     * 判断当前状态是否可以执行强制停止操作 默认情况下，能停止的状态也能强制停止
     *
     * @return 是否可以强制停止
     */
    default boolean canForceStop() {
        return canStop();
    }

    /**
     * 判断当前状态是否可以执行更新配置操作
     *
     * @return 是否可以更新配置
     */
    boolean canUpdateConfig();

    /**
     * 判断当前状态是否可以执行刷新配置操作
     *
     * @return 是否可以刷新配置
     */
    boolean canRefreshConfig();

    /**
     * 判断当前状态是否可以执行删除操作
     *
     * @return 是否可以删除
     */
    boolean canDelete();

    /**
     * 获取任务服务实例
     *
     * @return 任务服务实例
     */
    TaskService getTaskService();

    /**
     * 获取下一个操作完成后的状态
     *
     * @param operationType 操作类型
     * @param success 操作是否成功
     * @return 下一个状态
     */
    LogstashMachineState getNextState(OperationType operationType, boolean success);

    /** 操作类型枚举 */
    enum OperationType {
        INITIALIZE,
        START,
        STOP,
        FORCE_STOP,
        UPDATE_CONFIG,
        REFRESH_CONFIG,
        UPDATE_JVM_OPTIONS,
        UPDATE_LOGSTASH_YML,
        DELETE
    }
}
