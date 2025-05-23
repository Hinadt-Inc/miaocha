package com.hina.log.application.service;

import com.hina.log.domain.dto.logstash.LogstashProcessConfigUpdateRequestDTO;
import com.hina.log.domain.dto.logstash.LogstashProcessCreateDTO;
import com.hina.log.domain.dto.logstash.LogstashProcessResponseDTO;

import java.util.List;

/**
 * Logstash进程管理服务接口
 */
public interface LogstashProcessService {

    /**
     * 创建Logstash进程
     * 创建后会自动初始化对应的LogstashMachine实例
     * 如果jvmOptions或logstashYml为空，将在初始化后异步同步这些配置
     *
     * @param dto Logstash进程创建DTO
     * @return 创建的Logstash进程信息
     */
    LogstashProcessResponseDTO createLogstashProcess(LogstashProcessCreateDTO dto);

    /**
     * 更新Logstash配置
     * 支持同时更新主配置、JVM配置、系统配置中的任意组合
     * 可以针对全部机器或指定机器
     *
     * @param id  Logstash进程ID
     * @param dto 配置更新请求DTO
     * @return 更新后的Logstash进程信息
     */
    LogstashProcessResponseDTO updateLogstashConfig(Long id, LogstashProcessConfigUpdateRequestDTO dto);

    /**
     * 手动刷新Logstash配置到目标机器
     * 将数据库中的配置刷新到目标机器（不更改配置内容）
     * 可以指定要刷新的机器ID，若不指定则刷新所有机器
     *
     * @param id  Logstash进程ID
     * @param dto 配置刷新请求DTO，其中machineIds可指定要刷新的机器
     * @return 刷新后的Logstash进程信息
     */
    LogstashProcessResponseDTO refreshLogstashConfig(Long id, LogstashProcessConfigUpdateRequestDTO dto);

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
    LogstashProcessResponseDTO getLogstashProcess(Long id);

    /**
     * 获取所有Logstash进程
     *
     * @return Logstash进程列表
     */
    List<LogstashProcessResponseDTO> getAllLogstashProcesses();

    /**
     * 启动Logstash进程 - 全局操作
     * 启动所有关联机器上的Logstash实例
     *
     * @param id Logstash进程ID
     * @return 启动后的Logstash进程信息
     */
    LogstashProcessResponseDTO startLogstashProcess(Long id);

    /**
     * 启动单台机器上的Logstash进程
     *
     * @param id        Logstash进程ID
     * @param machineId 机器ID
     * @return 启动后的Logstash进程信息
     */
    LogstashProcessResponseDTO startMachineProcess(Long id, Long machineId);

    /**
     * 停止Logstash进程 - 全局操作
     * 停止所有关联机器上的Logstash实例
     *
     * @param id Logstash进程ID
     * @return 停止后的Logstash进程信息
     */
    LogstashProcessResponseDTO stopLogstashProcess(Long id);

    /**
     * 停止单台机器上的Logstash进程
     *
     * @param id        Logstash进程ID
     * @param machineId 机器ID
     * @return 停止后的Logstash进程信息
     */
    LogstashProcessResponseDTO stopMachineProcess(Long id, Long machineId);


    /**
     * 在数据源上执行Doris SQL，并保存到进程的dorisSql字段
     * 只有当所有机器实例都处于未启动状态时才能执行
     * 每个进程只能执行一次
     *
     * @param id  Logstash进程ID
     * @param sql 要执行的SQL语句
     * @return 更新后的进程信息
     */
    LogstashProcessResponseDTO executeDorisSql(Long id, String sql);

    /**
     * 更新单个机器的Logstash配置
     *
     * @param id 进程ID
     * @param machineId 机器ID
     * @param configContent Logstash主配置内容
     * @param jvmOptions JVM选项
     * @param logstashYml Logstash系统配置内容
     * @return 更新后的进程信息
     */
    LogstashProcessResponseDTO updateSingleMachineConfig(Long id, Long machineId, String configContent, String jvmOptions, String logstashYml);
}