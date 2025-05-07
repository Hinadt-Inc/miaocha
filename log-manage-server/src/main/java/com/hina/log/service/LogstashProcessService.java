package com.hina.log.service;

import com.hina.log.dto.LogstashProcessCreateDTO;
import com.hina.log.dto.LogstashProcessDTO;
import com.hina.log.dto.TaskDetailDTO;
import com.hina.log.dto.TaskStepsGroupDTO;
import com.hina.log.dto.TaskSummaryDTO;

import java.util.List;

/**
 * Logstash进程管理服务接口
 */
public interface LogstashProcessService {

    /**
     * 创建Logstash进程
     *
     * @param dto Logstash进程创建DTO
     * @return 创建的Logstash进程信息
     */
    LogstashProcessDTO createLogstashProcess(LogstashProcessCreateDTO dto);

    /**
     * 更新Logstash配置
     *
     * @param id         Logstash进程ID
     * @param configContent 新的配置内容
     * @param tableName  手动指定的表名（可为空）
     * @return 更新后的Logstash进程信息
     */
    LogstashProcessDTO updateLogstashConfig(Long id, String configContent, String tableName);

    /**
     * 手动刷新Logstash配置到目标机器
     *
     * @param id Logstash进程ID
     * @return 刷新后的Logstash进程信息
     */
    LogstashProcessDTO refreshLogstashConfig(Long id);

    /**
     * 删除Logstash进程
     *
     * @param id Logstash进程ID
     */
    void deleteLogstashProcess(Long id);

    /**
     * 获取Logstash进程信息
     *
     * @param id Logstash进程ID
     * @return Logstash进程信息
     */
    LogstashProcessDTO getLogstashProcess(Long id);

    /**
     * 获取所有Logstash进程
     *
     * @return Logstash进程列表
     */
    List<LogstashProcessDTO> getAllLogstashProcesses();

    /**
     * 启动Logstash进程
     *
     * @param id Logstash进程ID
     * @return 启动后的Logstash进程信息
     */
    LogstashProcessDTO startLogstashProcess(Long id);

    /**
     * 停止Logstash进程
     *
     * @param id Logstash进程ID
     * @return 停止后的Logstash进程信息
     */
    LogstashProcessDTO stopLogstashProcess(Long id);

    /**
     * 重试失败的任务
     *
     * @param id 进程ID
     * @return 重试操作结果
     */
    LogstashProcessDTO retryLogstashProcessOps(Long id);

    /**
     * 查询Logstash任务执行状态 - 返回详细信息
     *
     * @param id Logstash进程ID
     * @return 任务详情DTO
     */
    TaskDetailDTO getTaskDetailStatus(Long id);

    /**
     * 获取进程关联的所有任务摘要信息
     *
     * @param id 进程ID
     * @return 任务摘要列表
     */
    List<TaskSummaryDTO> getProcessTaskSummaries(Long id);

    /**
     * 获取任务的步骤分组信息
     *
     * @param taskId 任务ID
     * @return 任务步骤分组信息
     */
    TaskStepsGroupDTO getTaskStepsGrouped(String taskId);
}