package com.hina.log.logstash.command;

import com.hina.log.entity.Machine;
import com.hina.log.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Logstash命令抽象基类
 */
public abstract class AbstractLogstashCommand implements LogstashCommand {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final SshClient sshClient;
    protected final String deployDir;
    protected final Long processId;

    protected AbstractLogstashCommand(SshClient sshClient, String deployDir, Long processId) {
        this.sshClient = sshClient;
        this.deployDir = deployDir;
        this.processId = processId;
    }

    /**
     * 执行命令
     */
    @Override
    public CompletableFuture<Boolean> execute(Machine machine) {
        try {
            logger.info("在机器 [{}] 上执行 [{}]", machine.getIp(), getDescription());

            // 检查命令是否已执行成功（幂等性检查）
            CompletableFuture<Boolean> alreadyExecutedFuture = checkAlreadyExecuted(machine);

            return alreadyExecutedFuture.thenCompose(alreadyExecuted -> {
                if (alreadyExecuted) {
                    logger.info("命令 [{}] 已在机器 [{}] 上成功执行过，跳过执行", getDescription(), machine.getIp());
                    return CompletableFuture.completedFuture(true);
                }

                // 如果未执行成功，则执行命令
                return doExecute(machine)
                        .exceptionally(e -> {
                            logger.error("在机器 [{}] 上执行 [{}] 失败: {}",
                                    machine.getIp(), getDescription(), e.getMessage(), e);
                            return false;
                        });
            });
        } catch (Exception e) {
            logger.error("在机器 [{}] 上执行 [{}] 失败: {}",
                    machine.getIp(), getDescription(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * 检查命令是否已成功执行过（幂等性检查）
     * 子类可以覆盖此方法以提供特定的幂等性检查
     */
    protected CompletableFuture<Boolean> checkAlreadyExecuted(Machine machine) {
        // 默认情况下，假设命令未执行过
        return CompletableFuture.completedFuture(false);
    }

    /**
     * 实际执行命令
     */
    protected abstract CompletableFuture<Boolean> doExecute(Machine machine);

    /**
     * 获取Logstash进程目录
     */
    protected String getProcessDirectory() {
        return String.format("%s/logstash-%d", deployDir, processId);
    }
}