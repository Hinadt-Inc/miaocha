package com.hina.log.application.logstash.command;

import com.hina.log.common.ssh.SshClient;
import com.hina.log.domain.entity.Machine;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logstash命令抽象基类 */
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

    /** 执行命令 */
    @Override
    public CompletableFuture<Boolean> execute(Machine machine) {
        try {
            logger.info("在机器 [{}] 上执行 [{}]", machine.getIp(), getDescription());

            // 检查命令是否已执行成功（幂等性检查）
            CompletableFuture<Boolean> alreadyExecutedFuture = checkAlreadyExecuted(machine);

            return alreadyExecutedFuture.thenCompose(
                    alreadyExecuted -> {
                        if (alreadyExecuted) {
                            logger.info(
                                    "命令 [{}] 已在机器 [{}] 上成功执行过，跳过执行",
                                    getDescription(),
                                    machine.getIp());
                            return CompletableFuture.completedFuture(true);
                        }

                        // 如果未执行成功，则执行命令
                        return doExecute(machine)
                                .whenComplete(
                                        (success, e) -> {
                                            if (e != null) {
                                                // 记录错误但不吞异常，让它传播给调用者
                                                logger.error(
                                                        "在机器 [{}] 上执行 [{}] 失败: {}",
                                                        machine.getIp(),
                                                        getDescription(),
                                                        e.getMessage(),
                                                        e);
                                            } else if (!success) {
                                                // 执行成功但返回false的情况也需要记录
                                                logger.error(
                                                        "在机器 [{}] 上执行 [{}] 返回失败状态",
                                                        machine.getIp(),
                                                        getDescription());
                                            }
                                        });
                    });
        } catch (Exception e) {
            // 捕获初始化或检查阶段的异常，将其包装为CompletableFuture异常并传播
            logger.error(
                    "在机器 [{}] 上执行 [{}] 过程中发生异常: {}",
                    machine.getIp(),
                    getDescription(),
                    e.getMessage(),
                    e);
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /** 检查命令是否已成功执行过（幂等性检查） 子类可以覆盖此方法以提供特定的幂等性检查 */
    protected CompletableFuture<Boolean> checkAlreadyExecuted(Machine machine) {
        // 默认情况下，假设命令未执行过
        return CompletableFuture.completedFuture(false);
    }

    /** 实际执行命令 */
    protected abstract CompletableFuture<Boolean> doExecute(Machine machine);

    /** 获取Logstash进程目录 */
    protected String getProcessDirectory() {
        return String.format("%s/logstash-%d", deployDir, processId);
    }
}
