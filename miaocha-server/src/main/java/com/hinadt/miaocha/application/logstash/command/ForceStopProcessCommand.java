package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;

/**
 * 强制停止Logstash进程命令 - 重构支持多实例，基于logstashMachineId 应急停止功能：执行原有的停止逻辑，但无论命令成功与否，都强制返回成功
 * 用于应急情况下确保进程状态的一致性
 */
public class ForceStopProcessCommand extends AbstractLogstashCommand {

    private final StopProcessCommand normalStopCommand;

    public ForceStopProcessCommand(
            SshClient sshClient,
            String deployBaseDir,
            Long logstashMachineId,
            LogstashMachineMapper logstashMachineMapper,
            LogstashDeployPathManager deployPathManager) {
        super(
                sshClient,
                deployBaseDir,
                logstashMachineId,
                logstashMachineMapper,
                deployPathManager);
        // 创建普通的停止命令
        this.normalStopCommand =
                new StopProcessCommand(
                        sshClient,
                        deployBaseDir,
                        logstashMachineId,
                        logstashMachineMapper,
                        deployPathManager);
    }

    @Override
    protected boolean doExecute(MachineInfo machineInfo) {
        logger.info("执行强制停止命令，实例ID: {}", logstashMachineId);

        try {
            // 执行普通停止命令，但无论结果如何都返回成功
            boolean result = normalStopCommand.doExecute(machineInfo);
            if (!result) {
                logger.warn("强制停止命令执行失败，但仍返回成功，实例ID: {}", logstashMachineId);
            } else {
                logger.info("强制停止命令执行成功，实例ID: {}", logstashMachineId);
            }
        } catch (Exception e) {
            logger.warn("强制停止过程中发生异常，但仍返回成功，实例ID: {}, 异常: {}", logstashMachineId, e.getMessage());
        }

        // 无论如何都返回成功，确保状态机能够继续
        return true;
    }

    @Override
    public String getDescription() {
        return "强制停止Logstash进程";
    }
}
