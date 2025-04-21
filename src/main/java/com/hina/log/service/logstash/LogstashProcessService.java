package com.hina.log.service.logstash;

import com.hina.log.dto.TaskDetailDTO;
import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Logstash进程服务接口 - 负责Logstash进程的部署、启动和停止
 */
public interface LogstashProcessService {

    /**
     * 异步部署和启动Logstash进程
     *
     * @param process  Logstash进程
     * @param machines 目标机器列表
     * @return 异步操作结果
     */
    CompletableFuture<Boolean> deployAndStartAsync(LogstashProcess process, List<Machine> machines);

    /**
     * 异步停止Logstash进程
     *
     * @param machines 目标机器列表
     * @return 异步操作结果
     */
    CompletableFuture<Boolean> stopAsync(List<Machine> machines);

    /**
     * 查询Logstash进程任务状态
     *
     * @param processId Logstash进程ID
     * @return 任务执行详情
     */
    String getProcessTaskStatus(Long processId);

    /**
     * 查询Logstash进程任务详情
     *
     * @param processId Logstash进程ID
     * @return 任务详情
     */
    Optional<TaskDetailDTO> getLatestProcessTaskDetail(Long processId);

    /**
     * 删除任务记录
     *
     * @param taskId 任务ID
     */
    void deleteTask(String taskId);

    /**
     * 删除任务所有步骤记录
     *
     * @param taskId 任务ID
     */
    void deleteTaskSteps(String taskId);
}