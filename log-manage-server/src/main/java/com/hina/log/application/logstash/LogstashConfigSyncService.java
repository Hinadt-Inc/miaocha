package com.hina.log.application.logstash;

import com.hina.log.application.logstash.enums.LogstashMachineState;
import com.hina.log.common.ssh.SshClient;
import com.hina.log.domain.entity.LogstashMachine;
import com.hina.log.domain.entity.LogstashProcess;
import com.hina.log.domain.entity.MachineInfo;
import com.hina.log.domain.mapper.LogstashMachineMapper;
import com.hina.log.domain.mapper.LogstashProcessMapper;
import com.hina.log.domain.mapper.MachineMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Logstash配置同步服务 解耦配置同步逻辑，从进程初始化中分离出来 */
@Service
public class LogstashConfigSyncService {
    private static final Logger logger = LoggerFactory.getLogger(LogstashConfigSyncService.class);

    private final LogstashProcessMapper logstashProcessMapper;
    private final LogstashMachineMapper logstashMachineMapper;
    private final MachineMapper machineMapper;
    private final LogstashProcessDeployService deployService;
    private final SshClient sshClient;

    public LogstashConfigSyncService(
            LogstashProcessMapper logstashProcessMapper,
            LogstashMachineMapper logstashMachineMapper,
            MachineMapper machineMapper,
            LogstashProcessDeployService deployService,
            SshClient sshClient) {
        this.logstashProcessMapper = logstashProcessMapper;
        this.logstashMachineMapper = logstashMachineMapper;
        this.machineMapper = machineMapper;
        this.deployService = deployService;
        this.sshClient = sshClient;
    }

    /**
     * 异步同步配置文件 在进程初始化完成后从远程读取配置并更新到数据库
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
                        // 等待初始化完成
                        logger.info("等待进程 [{}] 初始化完成后同步配置文件", processId);

                        // 创建一个间隔查询任务，间隔5秒检查一次初始化状态
                        int maxTries = 60; // 最多等待5分钟 (60 * 5秒)
                        int tries = 0;
                        boolean initializationComplete = false;

                        while (!initializationComplete && tries < maxTries) {
                            // 获取所有机器的状态
                            List<LogstashMachine> machineStates =
                                    logstashMachineMapper.selectByLogstashProcessId(processId);

                            if (machineStates.isEmpty()) {
                                logger.warn("进程 [{}] 没有关联的机器，无法同步配置文件", processId);
                                Thread.sleep(5000);
                                tries++;
                                continue;
                            }

                            // 检查是否所有机器都已完成初始化
                            initializationComplete =
                                    machineStates.stream()
                                            .allMatch(
                                                    m ->
                                                            !LogstashMachineState.INITIALIZING
                                                                            .name()
                                                                            .equals(m.getState())
                                                                    && !LogstashMachineState
                                                                            .INITIALIZE_FAILED
                                                                            .name()
                                                                            .equals(m.getState()));
                            if (!initializationComplete) {
                                tries++;
                                logger.debug(
                                        "进程 [{}] 等待初始化完成 ({}/{}): 还有机器在初始化中",
                                        processId,
                                        tries,
                                        maxTries);
                                Thread.sleep(5000); // 等待5秒
                            }
                        }

                        if (!initializationComplete) {
                            logger.warn("进程 [{}] 初始化超时，部分机器仍在初始化中，无法同步配置文件", processId);
                            return;
                        }

                        // 初始化完成后，选择一台成功的机器来读取配置
                        List<LogstashMachine> successfulMachines =
                                logstashMachineMapper.selectByLogstashProcessId(processId).stream()
                                        .filter(
                                                m ->
                                                        LogstashMachineState.NOT_STARTED
                                                                .name()
                                                                .equals(m.getState()))
                                        .toList();

                        if (successfulMachines.isEmpty()) {
                            logger.warn("进程 [{}] 没有初始化成功的机器，无法同步配置文件", processId);
                            return;
                        }

                        // 选择第一台成功的机器
                        LogstashMachine targetMachine = successfulMachines.get(0);
                        MachineInfo machineInfo =
                                machineMapper.selectById(targetMachine.getMachineId());

                        if (machineInfo == null) {
                            logger.warn("找不到机器 [{}]，无法同步配置文件", targetMachine.getMachineId());
                            return;
                        }

                        // 获取当前进程信息
                        LogstashProcess currentProcess =
                                logstashProcessMapper.selectById(processId);
                        if (currentProcess == null) {
                            logger.warn("找不到进程 [{}]，无法同步配置文件", processId);
                            return;
                        }

                        // 构建远程配置文件路径 - 优先从数据库获取完整路径
                        String processDir = getProcessDirectory(processId, machineInfo);
                        String configDir = processDir + "/config";

                        // 同步JVM配置
                        if (needJvmOptions) {
                            try {
                                String jvmOptionsFile = configDir + "/jvm.options";
                                String jvmOptions =
                                        syncRemoteFileContent(machineInfo, jvmOptionsFile);

                                if (StringUtils.hasText(jvmOptions)) {
                                    currentProcess.setJvmOptions(jvmOptions);

                                    // 同步到所有LogstashMachine
                                    updateConfigForAllMachines(processId, null, jvmOptions, null);

                                    logger.info("成功同步进程 [{}] 的JVM配置", processId);
                                }
                            } catch (Exception e) {
                                logger.error(
                                        "同步进程 [{}] 的JVM配置失败: {}", processId, e.getMessage(), e);
                            }
                        }

                        // 同步Logstash系统配置
                        if (needLogstashYml) {
                            try {
                                String logstashYmlFile = configDir + "/logstash.yml";
                                String logstashYml =
                                        syncRemoteFileContent(machineInfo, logstashYmlFile);

                                if (StringUtils.hasText(logstashYml)) {
                                    currentProcess.setLogstashYml(logstashYml);

                                    // 同步到所有LogstashMachine
                                    updateConfigForAllMachines(processId, null, null, logstashYml);

                                    logger.info("成功同步进程 [{}] 的系统配置", processId);
                                }
                            } catch (Exception e) {
                                logger.error("同步进程 [{}] 的系统配置失败: {}", processId, e.getMessage(), e);
                            }
                        }

                        // 更新进程记录
                        currentProcess.setUpdateTime(LocalDateTime.now());
                        logstashProcessMapper.update(currentProcess);
                        logger.info("已完成进程 [{}] 的配置同步操作", processId);

                    } catch (Exception e) {
                        logger.error("同步进程 [{}] 配置文件时发生错误: {}", processId, e.getMessage(), e);
                    }
                });
    }

    /**
     * 获取进程目录路径
     *
     * @param processId 进程ID
     * @param machineInfo 机器信息
     * @return 进程目录路径
     */
    private String getProcessDirectory(Long processId, MachineInfo machineInfo) {
        try {
            // 尝试从数据库获取机器特定的部署路径
            LogstashMachine logstashMachine =
                    logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                            processId, machineInfo.getId());
            if (logstashMachine != null && StringUtils.hasText(logstashMachine.getDeployPath())) {
                // 数据库中存储的是完整的部署路径，直接使用
                return logstashMachine.getDeployPath();
            }
        } catch (Exception e) {
            logger.warn("无法从数据库获取部署路径，使用默认路径: {}", e.getMessage());
        }

        // 使用默认路径并拼接进程ID
        return String.format("%s/logstash-%d", deployService.getDeployBaseDir(), processId);
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
     * 更新单台机器的配置
     *
     * @param processId 进程ID
     * @param machineId 机器ID
     * @param configContent 主配置内容，为null表示不更新
     * @param jvmOptions JVM配置内容，为null表示不更新
     * @param logstashYml Logstash系统配置内容，为null表示不更新
     */
    public void updateConfigForSingleMachine(
            Long processId,
            Long machineId,
            String configContent,
            String jvmOptions,
            String logstashYml) {
        try {
            // 获取指定机器的LogstashMachine记录
            LogstashMachine machine =
                    logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                            processId, machineId);

            if (machine == null) {
                logger.warn("找不到进程 [{}] 在机器 [{}] 上的记录，跳过数据库同步", processId, machineId);
                return;
            }

            boolean updated = false;

            // 更新主配置
            if (configContent != null) {
                machine.setConfigContent(configContent);
                updated = true;
            }

            // 更新JVM配置
            if (jvmOptions != null) {
                machine.setJvmOptions(jvmOptions);
                updated = true;
            }

            // 更新系统配置
            if (logstashYml != null) {
                machine.setLogstashYml(logstashYml);
                updated = true;
            }

            // 如果有更新，保存到数据库
            if (updated) {
                machine.setUpdateTime(LocalDateTime.now());
                logstashMachineMapper.update(machine);
                logger.info("已更新机器 [{}] 上的进程 [{}] 配置", machineId, processId);
            } else {
                logger.debug("机器 [{}] 上的进程 [{}] 配置无需更新", machineId, processId);
            }
        } catch (Exception e) {
            logger.error("更新机器 [{}] 配置时出错: {}", machineId, e.getMessage(), e);
        }
    }

    /**
     * 更新所有LogstashMachine的配置
     *
     * @param processId 进程ID
     * @param configContent 主配置内容，为null表示不更新
     * @param jvmOptions JVM配置内容，为null表示不更新
     * @param logstashYml Logstash系统配置内容，为null表示不更新
     */
    public void updateConfigForAllMachines(
            Long processId, String configContent, String jvmOptions, String logstashYml) {
        try {
            // 获取进程关联的所有LogstashMachine
            List<LogstashMachine> machines =
                    logstashMachineMapper.selectByLogstashProcessId(processId);

            for (LogstashMachine machine : machines) {
                boolean updated = false;

                // 更新主配置
                if (configContent != null) {
                    machine.setConfigContent(configContent);
                    updated = true;
                }

                // 更新JVM配置
                if (jvmOptions != null) {
                    machine.setJvmOptions(jvmOptions);
                    updated = true;
                }

                // 更新系统配置
                if (logstashYml != null) {
                    machine.setLogstashYml(logstashYml);
                    updated = true;
                }

                // 如果有更新，保存到数据库
                if (updated) {
                    machine.setUpdateTime(LocalDateTime.now());
                    logstashMachineMapper.update(machine);
                    logger.debug("已更新机器 [{}] 上的进程 [{}] 配置", machine.getMachineId(), processId);
                }
            }

            logger.info("已完成同步配置到所有 [{}] 台机器", machines.size());
        } catch (Exception e) {
            logger.error("更新所有机器配置时出错: {}", e.getMessage(), e);
        }
    }
}
