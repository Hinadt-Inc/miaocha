package com.hina.log.application.logstash;

import com.hina.log.domain.entity.LogstashProcess;
import com.hina.log.domain.entity.MachineInfo;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Logstash进程服务接口 - 负责Logstash进程的部署、启动和停止 所有批量机器操作默认并行执行 */
public interface LogstashProcessDeployService {

    /**
     * 初始化Logstash进程环境（在多台机器上并行执行）
     *
     * @param process Logstash进程
     * @param machineInfos 目标机器列表
     */
    void initializeProcess(LogstashProcess process, List<MachineInfo> machineInfos);

    /**
     * 启动Logstash进程（在多台机器上并行执行）
     *
     * @param process Logstash进程
     * @param machineInfos 目标机器列表
     */
    void startProcess(LogstashProcess process, List<MachineInfo> machineInfos);

    /**
     * 停止Logstash进程（在多台机器上并行执行）
     *
     * @param processId 进程ID
     * @param machineInfos 目标机器列表
     */
    void stopProcess(Long processId, List<MachineInfo> machineInfos);

    /**
     * 强制停止Logstash进程（在多台机器上并行执行） 应急停止功能：执行原有的停止逻辑，但无论命令成功与否，都强制将状态更改为未启动
     *
     * @param processId 进程ID
     * @param machineInfos 目标机器列表
     */
    void forceStopProcess(Long processId, List<MachineInfo> machineInfos);

    /**
     * 更新多种Logstash配置并部署到目标机器（在多台机器上并行执行） 可以同时更新主配置、JVM配置和系统配置
     *
     * @param processId 进程ID
     * @param machineInfos 目标机器列表
     * @param configContent 主配置内容 (可为null)
     * @param jvmOptions JVM配置内容 (可为null)
     * @param logstashYml 系统配置内容 (可为null)
     */
    void updateMultipleConfigs(
            Long processId,
            List<MachineInfo> machineInfos,
            String configContent,
            String jvmOptions,
            String logstashYml);

    /**
     * 刷新Logstash配置到目标机器（在多台机器上并行执行）
     *
     * @param processId 进程ID
     * @param machineInfos 目标机器列表
     */
    void refreshConfig(Long processId, List<MachineInfo> machineInfos);

    /**
     * 在单台机器上启动Logstash进程
     *
     * @param process Logstash进程
     * @param machineInfo 目标机器
     */
    void startMachine(LogstashProcess process, MachineInfo machineInfo);

    /**
     * 在单台机器上停止Logstash进程
     *
     * @param processId 进程ID
     * @param machineInfo 目标机器
     */
    void stopMachine(Long processId, MachineInfo machineInfo);

    /**
     * 在单台机器上强制停止Logstash进程 应急停止功能：执行原有的停止逻辑，但无论命令成功与否，都强制将状态更改为未启动
     *
     * @param processId 进程ID
     * @param machineInfo 目标机器
     */
    void forceStopMachine(Long processId, MachineInfo machineInfo);

    /**
     * 重启单台机器上的Logstash进程
     *
     * @param processId 进程ID
     * @param machineInfo 目标机器
     */
    void restartMachine(Long processId, MachineInfo machineInfo);

    /**
     * 删除进程目录（清理）
     *
     * @param processId 进程ID
     * @param machineInfos 目标机器列表
     * @return 删除结果
     */
    CompletableFuture<Boolean> deleteProcessDirectory(
            Long processId, List<MachineInfo> machineInfos);

    /**
     * 获取Logstash进程部署的基础目录
     *
     * @return 基础目录路径
     */
    String getDeployBaseDir();

    /**
     * 获取指定进程在指定机器上的实际部署路径 优先从数据库获取，如果没有则根据用户定义路径或默认路径生成
     *
     * @param processId 进程ID
     * @param machineInfo 机器信息
     * @return 实际部署路径
     */
    String getProcessDeployPath(Long processId, MachineInfo machineInfo);

    /**
     * 生成默认的进程部署路径（基础目录 + 进程ID） 会根据机器用户名规范化基础目录路径
     *
     * @param processId 进程ID
     * @param machineInfo 机器信息
     * @return 默认部署路径
     */
    String generateDefaultProcessPath(Long processId, MachineInfo machineInfo);

    /**
     * 获取配置服务实例
     *
     * @return LogstashProcessConfigService 实例
     */
    LogstashProcessConfigService getConfigService();
}
