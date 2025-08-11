package com.hinadt.miaocha.application.logstash;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.infrastructure.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.infrastructure.mapper.LogstashProcessMapper;
import com.hinadt.miaocha.infrastructure.mapper.MachineMapper;
import com.hinadt.miaocha.infrastructure.ssh.SshClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Logstash配置同步服务 - 基于实例级架构 */
@Service
public class LogstashConfigSyncService {
    private static final Logger logger = LoggerFactory.getLogger(LogstashConfigSyncService.class);

    private final LogstashProcessMapper logstashProcessMapper;
    private final LogstashMachineMapper logstashMachineMapper;
    private final MachineMapper machineMapper;
    private final LogstashProcessDeployService deployService;
    private final SshClient sshClient;
    private final Executor logstashTaskExecutor;

    public LogstashConfigSyncService(
            LogstashProcessMapper logstashProcessMapper,
            LogstashMachineMapper logstashMachineMapper,
            MachineMapper machineMapper,
            LogstashProcessDeployService deployService,
            SshClient sshClient,
            @Qualifier("logstashTaskExecutor") Executor logstashTaskExecutor) {
        this.logstashProcessMapper = logstashProcessMapper;
        this.logstashMachineMapper = logstashMachineMapper;
        this.machineMapper = machineMapper;
        this.deployService = deployService;
        this.sshClient = sshClient;
        this.logstashTaskExecutor = logstashTaskExecutor;
    }

    /**
     * 异步同步配置文件 - 在进程初始化完成后从远程读取配置并更新到数据库
     *
     * @param processId 进程ID
     * @param needJvmOptions 是否需要同步JVM配置
     * @param needLogstashYml 是否需要同步系统配置
     */
    public void syncConfigurationAsync(
            Long processId, boolean needJvmOptions, boolean needLogstashYml) {
        CompletableFuture.runAsync(
                () -> {
                    try {
                        Thread.sleep(5000);
                        logger.info("等待进程 [{}] 所有实例初始化完成后同步配置文件", processId);

                        // 等待所有实例初始化完成
                        if (!waitForAllInstancesInitialized(processId)) {
                            logger.warn("进程 [{}] 初始化超时，部分实例仍在初始化中，无法同步配置文件", processId);
                            return;
                        }

                        // 选择一个成功初始化的实例来读取配置
                        LogstashMachine sourceInstance = selectSourceInstanceForSync(processId);
                        if (sourceInstance == null) {
                            logger.warn("进程 [{}] 没有可用的实例来同步配置文件", processId);
                            return;
                        }

                        // 获取机器信息
                        MachineInfo machineInfo =
                                machineMapper.selectById(sourceInstance.getMachineId());
                        if (machineInfo == null) {
                            logger.warn("找不到实例 [{}] 对应的机器信息，无法同步配置文件", sourceInstance.getId());
                            return;
                        }

                        // 获取当前进程信息
                        LogstashProcess currentProcess =
                                logstashProcessMapper.selectById(processId);
                        if (currentProcess == null) {
                            logger.warn("找不到进程 [{}]，无法同步配置文件", processId);
                            return;
                        }

                        // 构建远程配置文件路径
                        String instanceDeployPath =
                                deployService.getInstanceDeployPath(sourceInstance);
                        String configDir = instanceDeployPath + "/config";

                        // 同步JVM配置
                        if (needJvmOptions) {
                            syncJvmConfiguration(processId, currentProcess, machineInfo, configDir);
                        }

                        // 同步Logstash系统配置
                        if (needLogstashYml) {
                            syncLogstashYmlConfiguration(
                                    processId, currentProcess, machineInfo, configDir);
                        }

                        // 更新进程记录
                        currentProcess.setUpdateTime(LocalDateTime.now());
                        logstashProcessMapper.update(currentProcess);
                        logger.info("已完成进程 [{}] 的配置同步操作", processId);

                    } catch (Exception e) {
                        logger.error("同步进程 [{}] 配置文件时发生错误: {}", processId, e.getMessage(), e);
                    }
                },
                logstashTaskExecutor);
    }

    /** 等待所有实例初始化完成 */
    private boolean waitForAllInstancesInitialized(Long processId) throws InterruptedException {
        int maxTries = 60; // 最多等待5分钟 (60 * 5秒)
        int tries = 0;

        while (tries < maxTries) {
            List<LogstashMachine> allInstances =
                    logstashMachineMapper.selectByLogstashProcessId(processId);

            if (allInstances.isEmpty()) {
                logger.warn("进程 [{}] 没有关联的实例，无法同步配置文件", processId);
                Thread.sleep(5000);
                tries++;
                continue;
            }

            // 检查是否所有实例都已完成初始化
            boolean allInitialized =
                    allInstances.stream()
                            .allMatch(
                                    instance ->
                                            LogstashMachineState.NOT_STARTED
                                                            .name()
                                                            .equals(instance.getState())
                                                    || LogstashMachineState.INITIALIZE_FAILED
                                                            .name()
                                                            .equals(instance.getState()));

            if (allInitialized) {
                logger.info("进程 [{}] 所有 {} 个实例初始化完成", processId, allInstances.size());
                return true;
            }

            tries++;
            logger.debug("进程 [{}] 等待实例初始化完成 ({}/{}): 还有实例在初始化中", processId, tries, maxTries);
            Thread.sleep(5000);
        }

        return false;
    }

    /** 选择一个成功初始化的实例作为配置同步的源 */
    private LogstashMachine selectSourceInstanceForSync(Long processId) {
        List<LogstashMachine> successfulInstances =
                logstashMachineMapper.selectByLogstashProcessId(processId).stream()
                        .filter(
                                instance ->
                                        LogstashMachineState.NOT_STARTED
                                                .name()
                                                .equals(instance.getState()))
                        .toList();

        if (successfulInstances.isEmpty()) {
            logger.warn("进程 [{}] 没有初始化成功的实例", processId);
            return null;
        }

        // 选择第一个成功的实例
        LogstashMachine sourceInstance = successfulInstances.get(0);
        logger.info("选择实例 [{}] 作为配置同步源", sourceInstance.getId());
        return sourceInstance;
    }

    /** 同步JVM配置 */
    private void syncJvmConfiguration(
            Long processId,
            LogstashProcess currentProcess,
            MachineInfo machineInfo,
            String configDir) {
        try {
            String jvmOptionsFile = configDir + "/jvm.options";
            String jvmOptions = syncRemoteFileContent(machineInfo, jvmOptionsFile);

            if (StringUtils.hasText(jvmOptions)) {
                currentProcess.setJvmOptions(jvmOptions);
                updateConfigForAllInstances(processId, null, jvmOptions, null);
                logger.info("成功同步进程 [{}] 的JVM配置", processId);
            }
        } catch (Exception e) {
            logger.error("同步进程 [{}] 的JVM配置失败: {}", processId, e.getMessage(), e);
        }
    }

    /** 同步Logstash系统配置 */
    private void syncLogstashYmlConfiguration(
            Long processId,
            LogstashProcess currentProcess,
            MachineInfo machineInfo,
            String configDir) {
        try {
            String logstashYmlFile = configDir + "/logstash.yml";
            String logstashYml = syncRemoteFileContent(machineInfo, logstashYmlFile);

            if (StringUtils.hasText(logstashYml)) {
                currentProcess.setLogstashYml(logstashYml);
                updateConfigForAllInstances(processId, null, null, logstashYml);
                logger.info("成功同步进程 [{}] 的系统配置", processId);
            }
        } catch (Exception e) {
            logger.error("同步进程 [{}] 的系统配置失败: {}", processId, e.getMessage(), e);
        }
    }

    /**
     * 同步远程文件内容
     *
     * @param machineInfo 目标机器
     * @param remoteFilePath 远程文件路径
     * @return 文件内容
     */
    private String syncRemoteFileContent(MachineInfo machineInfo, String remoteFilePath)
            throws Exception {
        // 首先检查文件是否存在
        String checkCmd =
                String.format(
                        "[ -f %s ] && echo \"exists\" || echo \"not_exists\"", remoteFilePath);
        String checkResult = sshClient.executeCommand(machineInfo, checkCmd);

        if (!"exists".equals(checkResult.trim())) {
            logger.warn("远程文件 [{}] 不存在，无法同步", remoteFilePath);
            return null;
        }

        // 读取文件内容
        String catCmd = String.format("cat %s", remoteFilePath);
        return sshClient.executeCommand(machineInfo, catCmd);
    }

    /**
     * 更新单个实例的配置
     *
     * @param instanceId 实例ID
     * @param configContent 主配置内容，为null表示不更新
     * @param jvmOptions JVM配置内容，为null表示不更新
     * @param logstashYml Logstash系统配置内容，为null表示不更新
     */
    public void updateConfigForSingleInstance(
            Long instanceId, String configContent, String jvmOptions, String logstashYml) {
        try {
            LogstashMachine instance = logstashMachineMapper.selectById(instanceId);
            if (instance == null) {
                logger.warn("找不到实例 [{}]，跳过配置更新", instanceId);
                return;
            }

            boolean updated = false;

            // 更新主配置
            if (configContent != null) {
                instance.setConfigContent(configContent);
                updated = true;
            }

            // 更新JVM配置
            if (jvmOptions != null) {
                instance.setJvmOptions(jvmOptions);
                updated = true;
            }

            // 更新系统配置
            if (logstashYml != null) {
                instance.setLogstashYml(logstashYml);
                updated = true;
            }

            // 如果有更新，保存到数据库
            if (updated) {
                instance.setUpdateTime(LocalDateTime.now());
                logstashMachineMapper.update(instance);
                logger.info("已更新实例 [{}] 配置", instanceId);
            } else {
                logger.debug("实例 [{}] 配置无需更新", instanceId);
            }
        } catch (Exception e) {
            logger.error("更新实例 [{}] 配置时出错: {}", instanceId, e.getMessage(), e);
        }
    }

    /**
     * 更新所有实例的配置
     *
     * @param processId 进程ID
     * @param configContent 主配置内容，为null表示不更新
     * @param jvmOptions JVM配置内容，为null表示不更新
     * @param logstashYml Logstash系统配置内容，为null表示不更新
     */
    public void updateConfigForAllInstances(
            Long processId, String configContent, String jvmOptions, String logstashYml) {
        try {
            // 获取进程关联的所有实例
            List<LogstashMachine> allInstances =
                    logstashMachineMapper.selectByLogstashProcessId(processId);

            for (LogstashMachine instance : allInstances) {
                updateConfigForSingleInstance(
                        instance.getId(), configContent, jvmOptions, logstashYml);
            }

            logger.info("已完成同步配置到所有 [{}] 个实例", allInstances.size());
        } catch (Exception e) {
            logger.error("更新所有实例配置时出错: {}", e.getMessage(), e);
        }
    }
}
