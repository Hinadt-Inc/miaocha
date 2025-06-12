package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.dto.logstash.LogstashMachineDetailDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessConfigUpdateRequestDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessCreateDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessResponseDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessScaleRequestDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessUpdateDTO;
import java.util.List;

/** Logstash进程管理服务接口 */
public interface LogstashProcessService {

    /**
     * 创建Logstash进程 创建后会自动初始化对应的LogstashMachine实例 如果jvmOptions或logstashYml为空，将在初始化后异步同步这些配置
     *
     * @param dto Logstash进程创建DTO
     * @return 创建的Logstash进程信息
     */
    LogstashProcessResponseDTO createLogstashProcess(LogstashProcessCreateDTO dto);

    /**
     * 更新Logstash进程元信息 只能更新name和moduleId字段
     *
     * @param id Logstash进程ID
     * @param dto 更新请求DTO
     * @return 更新后的Logstash进程信息
     */
    LogstashProcessResponseDTO updateLogstashProcessMetadata(Long id, LogstashProcessUpdateDTO dto);

    /**
     * 更新Logstash配置 支持同时更新主配置、JVM配置、系统配置中的任意组合 可以针对全部机器或指定机器
     *
     * @param id Logstash进程ID
     * @param dto 配置更新请求DTO
     * @return 更新后的Logstash进程信息
     */
    LogstashProcessResponseDTO updateLogstashConfig(
            Long id, LogstashProcessConfigUpdateRequestDTO dto);

    /**
     * 刷新Logstash配置到指定机器 将数据库中保存的配置同步到目标机器，不更新数据库
     *
     * @param id Logstash进程ID
     * @param dto 刷新配置请求DTO
     * @return 刷新后的Logstash进程信息
     */
    LogstashProcessResponseDTO refreshLogstashConfig(
            Long id, LogstashProcessConfigUpdateRequestDTO dto);

    /**
     * 删除Logstash进程 删除前会检查进程是否正在运行，运行中的进程不能删除
     *
     * @param id Logstash进程ID
     */
    void deleteLogstashProcess(Long id);

    /**
     * 根据ID获取Logstash进程信息
     *
     * @param id Logstash进程ID
     * @return Logstash进程信息
     */
    LogstashProcessResponseDTO getLogstashProcess(Long id);

    /**
     * 获取所有Logstash进程信息
     *
     * @return Logstash进程信息列表
     */
    List<LogstashProcessResponseDTO> getAllLogstashProcesses();

    /**
     * 启动Logstash进程（在所有关联的机器上）
     *
     * @param id Logstash进程ID
     * @return 启动后的Logstash进程信息
     */
    LogstashProcessResponseDTO startLogstashProcess(Long id);

    /**
     * 启动单台机器上的Logstash进程
     *
     * @param id Logstash进程ID
     * @param machineId 机器ID
     * @return 启动后的Logstash进程信息
     */
    LogstashProcessResponseDTO startMachineProcess(Long id, Long machineId);

    /**
     * 停止Logstash进程（在所有关联的机器上）
     *
     * @param id Logstash进程ID
     * @return 停止后的Logstash进程信息
     */
    LogstashProcessResponseDTO stopLogstashProcess(Long id);

    /**
     * 停止单台机器上的Logstash进程
     *
     * @param id Logstash进程ID
     * @param machineId 机器ID
     * @return 停止后的Logstash进程信息
     */
    LogstashProcessResponseDTO stopMachineProcess(Long id, Long machineId);

    /**
     * 强制停止Logstash进程（在所有关联的机器上） 应急停止功能：执行原有的停止逻辑，但无论命令成功与否，都强制将状态更改为未启动
     *
     * @param id Logstash进程ID
     * @return 停止后的Logstash进程信息
     */
    LogstashProcessResponseDTO forceStopLogstashProcess(Long id);

    /**
     * 强制停止单台机器上的Logstash进程 应急停止功能：执行原有的停止逻辑，但无论命令成功与否，都强制将状态更改为未启动
     *
     * @param id Logstash进程ID
     * @param machineId 机器ID
     * @return 停止后的Logstash进程信息
     */
    LogstashProcessResponseDTO forceStopMachineProcess(Long id, Long machineId);

    /**
     * 更新单台机器的配置
     *
     * @param id Logstash进程ID
     * @param machineId 机器ID
     * @param configContent 配置内容
     * @param jvmOptions JVM选项
     * @param logstashYml Logstash YML配置
     * @return 更新后的Logstash进程信息
     */
    LogstashProcessResponseDTO updateSingleMachineConfig(
            Long id, Long machineId, String configContent, String jvmOptions, String logstashYml);

    /**
     * 重新初始化Logstash机器 主要用于初始化失败后的重试
     *
     * @param id Logstash进程ID
     * @param machineId 机器ID (如果为null则重新初始化所有失败的机器)
     * @return 重新初始化后的Logstash进程信息
     */
    LogstashProcessResponseDTO reinitializeLogstashMachine(Long id, Long machineId);

    /**
     * 获取Logstash机器详情
     *
     * @param id Logstash进程ID
     * @param machineId 机器ID
     * @return Logstash机器详情
     */
    LogstashMachineDetailDTO getLogstashMachineDetail(Long id, Long machineId);

    /**
     * 伸缩Logstash进程 支持扩容（添加机器）和缩容（删除机器）
     *
     * @param id Logstash进程ID
     * @param dto 伸缩请求DTO
     * @return 伸缩后的Logstash进程信息
     */
    LogstashProcessResponseDTO scaleLogstashProcess(Long id, LogstashProcessScaleRequestDTO dto);
}
