package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.dto.logstash.LogstashMachineDetailDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessConfigUpdateRequestDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessCreateDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessResponseDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessScaleRequestDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessUpdateDTO;
import java.util.List;

/** Logstash进程管理服务接口 - 重构支持多实例，基于logstashMachineId */
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
     * 更新Logstash配置 支持同时更新主配置、JVM配置、系统配置中的任意组合 可以针对全部实例或指定实例
     *
     * @param id Logstash进程ID
     * @param dto 配置更新请求DTO
     * @return 更新后的Logstash进程信息
     */
    LogstashProcessResponseDTO updateLogstashConfig(
            Long id, LogstashProcessConfigUpdateRequestDTO dto);

    /**
     * 刷新Logstash配置到指定实例 将数据库中保存的配置同步到目标实例，不更新数据库
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
     * 启动Logstash进程（启动所有关联的LogstashMachine实例）
     *
     * @param id Logstash进程ID
     * @return 启动后的Logstash进程信息
     */
    LogstashProcessResponseDTO startLogstashProcess(Long id);

    /**
     * 停止Logstash进程（停止所有关联的LogstashMachine实例）
     *
     * @param id Logstash进程ID
     * @return 停止后的Logstash进程信息
     */
    LogstashProcessResponseDTO stopLogstashProcess(Long id);

    /**
     * 强制停止Logstash进程（强制停止所有关联的LogstashMachine实例） 应急停止功能：执行原有的停止逻辑，但无论命令成功与否，都强制将状态更改为未启动
     *
     * @param id Logstash进程ID
     * @return 停止后的Logstash进程信息
     */
    LogstashProcessResponseDTO forceStopLogstashProcess(Long id);

    /**
     * 伸缩Logstash进程 支持扩容（添加机器）和缩容（删除实例）
     *
     * @param id Logstash进程ID
     * @param dto 伸缩请求DTO
     * @return 伸缩后的Logstash进程信息
     */
    LogstashProcessResponseDTO scaleLogstashProcess(Long id, LogstashProcessScaleRequestDTO dto);

    // ==================== 单个LogstashMachine实例操作方法 ====================

    /**
     * 启动单个LogstashMachine实例
     *
     * @param instanceId LogstashMachine实例ID
     */
    void startLogstashInstance(Long instanceId);

    /**
     * 停止单个LogstashMachine实例
     *
     * @param instanceId LogstashMachine实例ID
     */
    void stopLogstashInstance(Long instanceId);

    /**
     * 强制停止单个LogstashMachine实例 应急停止功能：执行原有的停止逻辑，但无论命令成功与否，都强制将状态更改为未启动
     *
     * @param instanceId LogstashMachine实例ID
     */
    void forceStopLogstashInstance(Long instanceId);

    /**
     * 重新初始化单个LogstashMachine实例 用于初始化失败或需要重新部署的实例
     *
     * @param instanceId LogstashMachine实例ID
     */
    void reinitializeLogstashInstance(Long instanceId);

    /**
     * 根据LogstashMachine实例ID获取详细信息
     *
     * @param logstashMachineId LogstashMachine实例ID
     * @return LogstashMachine实例详细信息
     */
    LogstashMachineDetailDTO getLogstashMachineDetail(Long logstashMachineId);
}
