package com.hina.log.logstash;

import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Logstash进程服务接口 - 负责Logstash进程的部署、启动和停止
 * 所有批量机器操作默认并行执行
 */
public interface LogstashProcessDeployService {

    /**
     * 初始化Logstash进程环境（在多台机器上并行执行）
     *
     * @param process  Logstash进程
     * @param machines 目标机器列表
     */
    void initializeProcess(LogstashProcess process, List<Machine> machines);

    /**
     * 启动Logstash进程（在多台机器上并行执行）
     *
     * @param process  Logstash进程
     * @param machines 目标机器列表
     */
    void startProcess(LogstashProcess process, List<Machine> machines);


    /**
     * 停止Logstash进程（在多台机器上并行执行）
     *
     * @param processId 进程ID
     * @param machines  目标机器列表
     */
    void stopProcess(Long processId, List<Machine> machines);


    /**
     * 更新多种Logstash配置并部署到目标机器（在多台机器上并行执行）
     * 可以同时更新主配置、JVM配置和系统配置
     *
     * @param processId      进程ID
     * @param machines       目标机器列表
     * @param configContent  主配置内容 (可为null)
     * @param jvmOptions     JVM配置内容 (可为null)
     * @param logstashYml    系统配置内容 (可为null)
     */
    void updateMultipleConfigs(Long processId, List<Machine> machines, String configContent, String jvmOptions, String logstashYml);

    /**
     * 刷新Logstash配置到目标机器（在多台机器上并行执行）
     *
     * @param processId 进程ID
     * @param machines  目标机器列表
     */
    void refreshConfig(Long processId, List<Machine> machines);

    /**
     * 在单台机器上启动Logstash进程
     *
     * @param process Logstash进程
     * @param machine 目标机器
     */
    void startMachine(LogstashProcess process, Machine machine);

    /**
     * 在单台机器上停止Logstash进程
     *
     * @param processId 进程ID
     * @param machine   目标机器
     */
    void stopMachine(Long processId, Machine machine);

    /**
     * 重启单台机器上的Logstash进程
     *
     * @param processId 进程ID
     * @param machine   目标机器
     */
    void restartMachine(Long processId, Machine machine);


    /**
     * 删除进程目录（清理）
     *
     * @param processId 进程ID
     * @param machines  目标机器列表
     * @return 删除结果
     */
    CompletableFuture<Boolean> deleteProcessDirectory(Long processId, List<Machine> machines);

    /**
     * 获取Logstash进程部署的基础目录
     * 
     * @return 基础目录路径
     */
    String getDeployBaseDir();

    /**
     * 获取配置服务实例
     *
     * @return LogstashProcessConfigService 实例
     */
    LogstashProcessConfigService getConfigService();
}