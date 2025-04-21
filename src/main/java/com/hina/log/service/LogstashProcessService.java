package com.hina.log.service;

import com.hina.log.dto.LogstashProcessCreateDTO;
import com.hina.log.dto.LogstashProcessDTO;
import com.hina.log.dto.TaskDetailDTO;

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
     * 更新Logstash进程
     *
     * @param id  Logstash进程ID
     * @param dto Logstash进程更新DTO
     * @return 更新后的Logstash进程信息
     */
    LogstashProcessDTO updateLogstashProcess(Long id, LogstashProcessCreateDTO dto);

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
     * 查询Logstash任务执行状态
     *
     * @param id Logstash进程ID
     * @return 任务状态信息
     */
    String getTaskStatus(Long id);

    /**
     * 重试失败的Logstash任务
     *
     * @param id Logstash进程ID
     * @return 重试操作结果
     */
    boolean retryTask(Long id);

    /**
     * 查询Logstash任务执行状态 - 返回详细信息
     *
     * @param id Logstash进程ID
     * @return 任务详情DTO
     */
    TaskDetailDTO getTaskDetailStatus(Long id);
}