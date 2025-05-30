package com.hina.log.application.logstash.command;

import com.hina.log.common.ssh.SshClient;
import com.hina.log.domain.entity.LogstashMachine;
import com.hina.log.domain.entity.MachineInfo;
import com.hina.log.domain.mapper.LogstashMachineMapper;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/** Logstash命令抽象基类 */
public abstract class AbstractLogstashCommand implements LogstashCommand {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final SshClient sshClient;
    protected final String deployDir;
    protected final Long processId;
    protected final LogstashMachineMapper logstashMachineMapper;

    protected AbstractLogstashCommand(
            SshClient sshClient,
            String deployDir,
            Long processId,
            LogstashMachineMapper logstashMachineMapper) {
        this.sshClient = sshClient;
        this.deployDir = deployDir;
        this.processId = processId;
        this.logstashMachineMapper = logstashMachineMapper;
    }

    /**
     * 规范化部署目录路径 如果deployDir不是绝对路径（不以/开头），则将其转换为用户家目录下的路径
     *
     * @param machineInfo 机器信息，用于获取用户名
     * @return 规范化后的绝对路径
     */
    protected String normalizeDeployDir(MachineInfo machineInfo) {
        if (deployDir.startsWith("/")) {
            // 已经是绝对路径，直接返回
            return deployDir;
        } else {
            // 相对路径，转换为用户家目录下的路径
            String username = machineInfo.getUsername();
            return String.format("/home/%s/%s", username, deployDir);
        }
    }

    /** 执行命令 */
    @Override
    public CompletableFuture<Boolean> execute(MachineInfo machineInfo) {
        try {
            logger.info("在机器 [{}] 上执行 [{}]", machineInfo.getIp(), getDescription());

            // 检查命令是否已执行成功（幂等性检查）
            CompletableFuture<Boolean> alreadyExecutedFuture = checkAlreadyExecuted(machineInfo);

            return alreadyExecutedFuture.thenCompose(
                    alreadyExecuted -> {
                        if (alreadyExecuted) {
                            logger.info(
                                    "命令 [{}] 已在机器 [{}] 上成功执行过，跳过执行",
                                    getDescription(),
                                    machineInfo.getIp());
                            return CompletableFuture.completedFuture(true);
                        }

                        // 如果未执行成功，则执行命令
                        return doExecute(machineInfo)
                                .whenComplete(
                                        (success, e) -> {
                                            if (e != null) {
                                                // 只记录debug级别日志，详细错误由外层处理
                                                logger.debug(
                                                        "在机器 [{}] 上执行 [{}] 失败: {}",
                                                        machineInfo.getIp(),
                                                        getDescription(),
                                                        e.getMessage());
                                            } else if (!success) {
                                                // 执行成功但返回false的情况
                                                logger.debug(
                                                        "在机器 [{}] 上执行 [{}] 返回失败状态",
                                                        machineInfo.getIp(),
                                                        getDescription());
                                            }
                                        });
                    });
        } catch (Exception e) {
            // 捕获初始化或检查阶段的异常，只记录debug级别日志
            logger.debug(
                    "在机器 [{}] 上执行 [{}] 过程中发生异常: {}",
                    machineInfo.getIp(),
                    getDescription(),
                    e.getMessage());
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /** 检查命令是否已成功执行过（幂等性检查） 子类可以覆盖此方法以提供特定的幂等性检查 */
    protected CompletableFuture<Boolean> checkAlreadyExecuted(MachineInfo machineInfo) {
        // 默认情况下，假设命令未执行过
        return CompletableFuture.completedFuture(false);
    }

    /** 实际执行命令 */
    protected abstract CompletableFuture<Boolean> doExecute(MachineInfo machineInfo);

    /** 获取Logstash进程目录 优先使用数据库中的部署路径，否则使用默认路径 */
    protected String getProcessDirectory(MachineInfo machineInfo) {
        String actualDeployDir = getActualDeployDir(machineInfo);
        return String.format("%s/logstash-%d", actualDeployDir, processId);
    }

    /** 获取实际的部署目录 优先使用数据库中保存的机器特定部署路径，如果没有则使用规范化的默认路径 */
    protected String getActualDeployDir(MachineInfo machineInfo) {
        try {
            // 尝试从数据库获取机器特定的部署路径
            LogstashMachine logstashMachine =
                    logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                            processId, machineInfo.getId());
            if (logstashMachine != null && StringUtils.hasText(logstashMachine.getDeployPath())) {
                return logstashMachine.getDeployPath();
            }
        } catch (Exception e) {
            logger.warn("无法从数据库获取部署路径，使用默认路径: {}", e.getMessage());
        }

        // 使用规范化的默认路径
        return normalizeDeployDir(machineInfo);
    }
}
