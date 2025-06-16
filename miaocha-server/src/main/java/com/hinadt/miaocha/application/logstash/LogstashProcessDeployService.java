package com.hinadt.miaocha.application.logstash;

import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Logstash进程部署服务接口 - 基于LogstashMachine实例级操作 */
public interface LogstashProcessDeployService {

    /**
     * 批量初始化LogstashMachine实例环境（并行执行）
     *
     * @param logstashMachines LogstashMachine实例列表
     * @param process 关联的Logstash进程
     */
    void initializeInstances(List<LogstashMachine> logstashMachines, LogstashProcess process);

    /**
     * 批量启动LogstashMachine实例（并行执行）
     *
     * @param logstashMachines LogstashMachine实例列表
     * @param process 关联的Logstash进程
     */
    void startInstances(List<LogstashMachine> logstashMachines, LogstashProcess process);

    /**
     * 批量停止LogstashMachine实例（并行执行）
     *
     * @param logstashMachines LogstashMachine实例列表
     */
    void stopInstances(List<LogstashMachine> logstashMachines);

    /**
     * 批量强制停止LogstashMachine实例（并行执行）
     *
     * @param logstashMachines LogstashMachine实例列表
     */
    void forceStopInstances(List<LogstashMachine> logstashMachines);

    /**
     * 批量更新LogstashMachine实例配置（并行执行）
     *
     * @param logstashMachines LogstashMachine实例列表
     * @param configContent 主配置内容 (可为null)
     * @param jvmOptions JVM配置内容 (可为null)
     * @param logstashYml 系统配置内容 (可为null)
     */
    void updateInstancesConfig(
            List<LogstashMachine> logstashMachines,
            String configContent,
            String jvmOptions,
            String logstashYml);

    /**
     * 批量刷新LogstashMachine实例配置（并行执行）
     *
     * @param logstashMachines LogstashMachine实例列表
     * @param process 关联的Logstash进程
     */
    void refreshInstancesConfig(List<LogstashMachine> logstashMachines, LogstashProcess process);

    /**
     * 批量删除LogstashMachine实例目录（并行执行）
     *
     * @param logstashMachines LogstashMachine实例列表
     */
    CompletableFuture<Boolean> deleteInstancesDirectory(List<LogstashMachine> logstashMachines);

    // ==================== 辅助方法 ====================

    /**
     * 获取Logstash进程部署的基础目录
     *
     * @return 基础目录路径
     */
    String getDeployBaseDir();

    /**
     * 获取指定LogstashMachine实例的部署路径
     *
     * @param logstashMachine LogstashMachine实例
     * @return 实例部署路径
     */
    String getInstanceDeployPath(LogstashMachine logstashMachine);

    /**
     * 生成默认的实例部署路径 基于进程ID和机器ID生成路径，不依赖实例ID避免插入前的依赖问题
     *
     * @param machineInfo 机器信息
     * @return 默认部署路径
     */
    String generateDefaultInstancePath(MachineInfo machineInfo);

    /**
     * 获取配置服务实例
     *
     * @return LogstashProcessConfigService 实例
     */
    LogstashProcessConfigService getConfigService();
}
