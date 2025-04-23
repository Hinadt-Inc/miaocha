package com.hina.log.logstash.state;

import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.logstash.enums.LogstashProcessState;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Logstash进程状态处理器接口
 * 使用状态模式处理不同状态下的行为
 */
public interface LogstashProcessStateHandler {

    /**
     * 获取当前处理器能处理的状态
     * 
     * @return 状态枚举
     */
    LogstashProcessState getState();

    /**
     * 处理初始化操作
     * 
     * @param process  Logstash进程
     * @param machines 目标机器列表
     * @param taskId   任务ID，可以为null
     * @return 异步操作结果
     */
    CompletableFuture<Boolean> handleInitialize(LogstashProcess process, List<Machine> machines, String taskId);

    /**
     * 处理启动操作
     * 
     * @param process  Logstash进程
     * @param machines 目标机器列表
     * @param taskId   任务ID，可以为null
     * @return 异步操作结果
     */
    CompletableFuture<Boolean> handleStart(LogstashProcess process, List<Machine> machines, String taskId);

    /**
     * 处理停止操作
     * 
     * @param process  Logstash进程
     * @param machines 目标机器列表
     * @param taskId   任务ID，可以为null
     * @return 异步操作结果
     */
    CompletableFuture<Boolean> handleStop(LogstashProcess process, List<Machine> machines, String taskId);

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
     * 获取下一个操作完成后的状态
     * 
     * @param operationType 操作类型
     * @param success       操作是否成功
     * @return 下一个状态
     */
    LogstashProcessState getNextState(OperationType operationType, boolean success);

    /**
     * 判断状态是否是失败状态
     * 
     * @return 是否是失败状态
     */
    default boolean isFailedState() {
        return getState() == LogstashProcessState.START_FAILED
                || getState() == LogstashProcessState.STOP_FAILED;
    }

    /**
     * 操作类型枚举
     */
    enum OperationType {
        INITIALIZE,
        START,
        STOP
    }
}