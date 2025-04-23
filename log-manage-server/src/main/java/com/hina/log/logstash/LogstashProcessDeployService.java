package com.hina.log.logstash;

import com.hina.log.dto.TaskDetailDTO;
import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Logstash进程服务接口 - 负责Logstash进程的部署、启动和停止
 */
public interface LogstashProcessDeployService {

    /**
     * 异步初始化Logstash进程环境
     *
     * @param process  Logstash进程
     * @param machines 目标机器列表
     */
    void initializeProcessAsync(LogstashProcess process, List<Machine> machines);

    /**
     * 异步启动Logstash进程
     *
     * @param process  Logstash进程
     * @param machines 目标机器列表
     */
    void startProcessAsync(LogstashProcess process, List<Machine> machines);

    /**
     * 异步停止指定进程ID的Logstash进程
     *
     * @param processId 进程ID
     * @param machines  目标机器列表
     */
    void stopProcessAsync(Long processId, List<Machine> machines);

    /**
     * 删除进程目录
     *
     * @param processId 进程ID
     * @param machines  目标机器列表
     * @return 删除结果
     */
    CompletableFuture<Boolean> deleteProcessDirectory(Long processId, List<Machine> machines);

    /**
     * 查询Logstash进程任务详情
     *
     * @param processId Logstash进程ID
     * @return 任务详情
     */
    Optional<TaskDetailDTO> getLatestProcessTaskDetail(Long processId);

    /**
     * 获取与进程关联的所有任务ID
     * 
     * @param processId 进程ID
     * @return 任务ID列表
     */
    List<String> getAllProcessTaskIds(Long processId);

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