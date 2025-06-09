package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import java.util.concurrent.CompletableFuture;

/** 停止Logstash进程命令 */
public class StopProcessCommand extends AbstractLogstashCommand {

    public StopProcessCommand(
            SshClient sshClient,
            String deployDir,
            Long processId,
            LogstashMachineMapper logstashMachineMapper) {
        super(sshClient, deployDir, processId, logstashMachineMapper);
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(MachineInfo machineInfo) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            String processDir = getProcessDirectory(machineInfo);
            String pidFile = processDir + "/logs/logstash-" + processId + ".pid";

            // 检查PID文件是否存在 - 使用不会出错的命令
            String checkPidCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                            pidFile);
            String checkPidResult = sshClient.executeCommand(machineInfo, checkPidCommand);

            if ("exists".equals(checkPidResult.trim())) {
                // 读取PID
                String readPidCommand = String.format("cat %s", pidFile);
                String pid = sshClient.executeCommand(machineInfo, readPidCommand).trim();

                if (!pid.isEmpty()) {
                    // 检查进程是否存在
                    String checkProcessCommand =
                            String.format("ps -p %s > /dev/null && echo \"running\"", pid);
                    String checkProcessResult =
                            sshClient.executeCommand(machineInfo, checkProcessCommand);

                    if ("running".equals(checkProcessResult.trim())) {
                        // 杀死进程
                        logger.info("停止Logstash进程，PID: {}", pid);
                        String killCommand = String.format("kill -15 %s", pid);
                        sshClient.executeCommand(machineInfo, killCommand);

                        // 给进程一些时间来优雅地终止
                        Thread.sleep(3000);

                        // 检查进程是否仍然存在
                        String recheckCommand =
                                String.format("ps -p %s > /dev/null || echo \"stopped\"", pid);
                        String recheckResult =
                                sshClient.executeCommand(machineInfo, recheckCommand);

                        if (!"stopped".equals(recheckResult.trim())) {
                            // 如果进程仍在运行，强制终止
                            logger.warn("Logstash进程没有响应SIGTERM信号，使用SIGKILL强制终止");
                            String forceKillCommand = String.format("kill -9 %s", pid);
                            sshClient.executeCommand(machineInfo, forceKillCommand);
                        }
                    } else {
                        logger.info("Logstash进程已不在运行状态，PID: {}", pid);
                    }
                } else {
                    logger.warn("PID文件为空");
                }

                // 删除PID文件
                String removePidCommand = String.format("rm -f %s", pidFile);
                sshClient.executeCommand(machineInfo, removePidCommand);
            } else {
                logger.info("找不到PID文件，尝试通过进程名查找并终止Logstash进程");

                // 尝试通过进程名称查找
                String findPidCommand =
                        String.format(
                                "ps -ef | grep logstash | grep \"logstash-%s.conf\" | grep -v grep"
                                        + " | awk '{print $2}'",
                                processId);
                String foundPids = sshClient.executeCommand(machineInfo, findPidCommand);

                if (foundPids != null && !foundPids.trim().isEmpty()) {
                    // 终止找到的所有进程
                    for (String pid : foundPids.trim().split("\\s+")) {
                        logger.info("通过进程名找到Logstash进程，PID: {}", pid);
                        String killCommand = String.format("kill -9 %s", pid);
                        sshClient.executeCommand(machineInfo, killCommand);
                    }
                } else {
                    logger.info("未找到相关的Logstash进程");
                }
            }

            // 不论进程是否存在，我们都认为停止操作成功完成
            logger.info("Logstash进程停止操作完成");
            future.complete(true);
        } catch (Exception e) {
            logger.error("停止Logstash进程时发生错误: {}", e.getMessage(), e);
            future.completeExceptionally(
                    new SshOperationException("停止Logstash进程失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public String getDescription() {
        return "停止Logstash进程";
    }
}
