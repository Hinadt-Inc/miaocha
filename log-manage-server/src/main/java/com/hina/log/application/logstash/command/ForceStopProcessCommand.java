package com.hina.log.application.logstash.command;

import com.hina.log.common.ssh.SshClient;
import com.hina.log.domain.entity.MachineInfo;
import com.hina.log.domain.mapper.LogstashMachineMapper;
import java.util.concurrent.CompletableFuture;

/** 强制停止Logstash进程命令 应急停止功能：执行原有的停止逻辑，但无论命令成功与否，都强制返回成功 用于应急情况下确保进程状态的一致性 */
public class ForceStopProcessCommand extends AbstractLogstashCommand {

    private final StopProcessCommand normalStopCommand;

    public ForceStopProcessCommand(
            SshClient sshClient,
            String deployDir,
            Long processId,
            LogstashMachineMapper logstashMachineMapper) {
        super(sshClient, deployDir, processId, logstashMachineMapper);
        // 创建普通的停止命令
        this.normalStopCommand =
                new StopProcessCommand(sshClient, deployDir, processId, logstashMachineMapper);
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(MachineInfo machineInfo) {
        logger.warn("执行强制停止操作 - 机器: {}, 进程ID: {}", machineInfo.getIp(), processId);

        // 执行普通的停止逻辑
        return normalStopCommand
                .doExecute(machineInfo)
                .handle(
                        (success, throwable) -> {
                            if (throwable != null) {
                                logger.warn(
                                        "强制停止过程中SSH命令执行失败，但将强制返回成功: {}", throwable.getMessage());
                            } else if (!success) {
                                logger.warn("强制停止过程中停止命令返回失败，但将强制返回成功");
                            } else {
                                logger.info("强制停止执行成功");
                            }

                            // 无论如何都返回成功，确保状态能够更新为未启动
                            return true;
                        });
    }

    @Override
    public String getDescription() {
        return "强制停止Logstash进程";
    }
}
