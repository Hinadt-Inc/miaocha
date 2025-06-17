package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logstash命令抽象基类 - 重构支持多实例，基于logstashMachineId */
public abstract class AbstractLogstashCommand implements LogstashCommand {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final SshClient sshClient;
    protected final String deployBaseDir;
    protected final Long logstashMachineId;
    protected final LogstashMachineMapper logstashMachineMapper;
    protected final LogstashDeployPathManager deployPathManager;

    protected AbstractLogstashCommand(
            SshClient sshClient,
            String deployBaseDir,
            Long logstashMachineId,
            LogstashMachineMapper logstashMachineMapper,
            LogstashDeployPathManager deployPathManager) {
        this.sshClient = sshClient;
        this.deployBaseDir = deployBaseDir;
        this.logstashMachineId = logstashMachineId;
        this.logstashMachineMapper = logstashMachineMapper;
        this.deployPathManager = deployPathManager;
    }

    /** 执行命令 */
    @Override
    public final CompletableFuture<Boolean> execute(MachineInfo machineInfo) {
        logger.info(
                "开始执行命令: {} 在机器: {} 上，实例ID: {}",
                getDescription(),
                machineInfo.getName(),
                logstashMachineId);

        // 检查命令是否已经执行过
        return checkAlreadyExecuted(machineInfo)
                .thenCompose(
                        alreadyExecuted -> {
                            if (alreadyExecuted) {
                                logger.info(
                                        "命令 {} 在机器 {} 上已执行，跳过执行，实例ID: {}",
                                        getDescription(),
                                        machineInfo.getName(),
                                        logstashMachineId);
                                return CompletableFuture.completedFuture(true);
                            } else {
                                logger.info(
                                        "开始执行命令 {} 在机器 {} 上，实例ID: {}",
                                        getDescription(),
                                        machineInfo.getName(),
                                        logstashMachineId);
                                return doExecute(machineInfo);
                            }
                        })
                .whenComplete(
                        (result, throwable) -> {
                            if (throwable != null) {
                                logger.error(
                                        "命令 {} 在机器 {} 上执行失败，实例ID: {}, 错误: {}",
                                        getDescription(),
                                        machineInfo.getName(),
                                        logstashMachineId,
                                        throwable.getMessage(),
                                        throwable);
                            } else {
                                logger.info(
                                        "命令 {} 在机器 {} 上执行完成，实例ID: {}, 结果: {}",
                                        getDescription(),
                                        machineInfo.getName(),
                                        logstashMachineId,
                                        result ? "成功" : "失败");
                            }
                        });
    }

    /**
     * 检查命令是否已经执行过 子类可以重写此方法来实现幂等性检查
     *
     * @param machineInfo 机器信息
     * @return 如果已执行返回true，否则返回false
     */
    protected CompletableFuture<Boolean> checkAlreadyExecuted(MachineInfo machineInfo) {
        // 默认情况下，假设命令未执行过
        return CompletableFuture.completedFuture(false);
    }

    /** 实际执行命令 */
    protected abstract CompletableFuture<Boolean> doExecute(MachineInfo machineInfo);

    /** 获取Logstash实例目录 - 基于logstashMachineId 优先使用数据库中的部署路径，否则使用基于logstashMachineId的默认路径 */
    protected String getProcessDirectory() {
        return deployPathManager.getInstanceDeployPath(logstashMachineId);
    }

    /** 获取当前操作的LogstashMachine实例 */
    protected LogstashMachine getLogstashMachine() {
        return logstashMachineMapper.selectById(logstashMachineId);
    }

    /** 获取实例ID */
    protected Long getLogstashMachineId() {
        return logstashMachineId;
    }
}
