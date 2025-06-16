package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import java.util.concurrent.CompletableFuture;

/** 停止Logstash进程命令 - 重构支持多实例，基于logstashMachineId */
public class StopProcessCommand extends AbstractLogstashCommand {

    public StopProcessCommand(
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
    }

    @Override
    protected CompletableFuture<Boolean> checkAlreadyExecuted(MachineInfo machineInfo) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        String processDir = getProcessDirectory(machineInfo);
                        String pidFile =
                                processDir + "/logs/logstash-" + logstashMachineId + ".pid";

                        // 检查PID文件是否存在
                        String checkPidFileCommand =
                                String.format(
                                        "if [ -f \"%s\" ]; then echo \"exists\"; else echo"
                                                + " \"not_exists\"; fi",
                                        pidFile);
                        String pidFileResult =
                                sshClient.executeCommand(machineInfo, checkPidFileCommand);

                        if (!"exists".equals(pidFileResult.trim())) {
                            logger.info("PID文件不存在，进程可能已停止，实例ID: {}", logstashMachineId);
                            return true; // 已经停止
                        }

                        // 读取PID并检查进程是否运行
                        String readPidCommand = String.format("cat %s", pidFile);
                        String pid = sshClient.executeCommand(machineInfo, readPidCommand).trim();

                        if (pid.isEmpty()) {
                            logger.info("PID文件为空，进程可能已停止，实例ID: {}", logstashMachineId);
                            return true; // 已经停止
                        }

                        String checkProcessCommand =
                                String.format(
                                        "if ps -p %s > /dev/null 2>&1; then echo \"running\"; else"
                                                + " echo \"not_running\"; fi",
                                        pid);
                        String processResult =
                                sshClient.executeCommand(machineInfo, checkProcessCommand);

                        boolean running = "running".equals(processResult.trim());
                        if (!running) {
                            logger.info("进程已停止，实例ID: {}, PID: {}", logstashMachineId, pid);
                        }
                        return !running; // 如果不在运行，则已经停止
                    } catch (Exception e) {
                        logger.warn(
                                "检查进程是否已停止时出错，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage());
                        return false; // 出错时假设需要执行停止操作
                    }
                });
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(MachineInfo machineInfo) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            String processDir = getProcessDirectory(machineInfo);
            String pidFile = processDir + "/logs/logstash-" + logstashMachineId + ".pid";

            // 检查PID文件是否存在
            String checkPidCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                            pidFile);
            String checkPidResult = sshClient.executeCommand(machineInfo, checkPidCommand);

            if (!"exists".equals(checkPidResult.trim())) {
                logger.info("PID文件不存在，可能进程已停止，实例ID: {}", logstashMachineId);
                // 清理数据库中的PID
                logstashMachineMapper.updateProcessPidById(logstashMachineId, null);
                future.complete(true);
                return future;
            }

            // 读取PID
            String readPidCommand = String.format("cat %s", pidFile);
            String pid = sshClient.executeCommand(machineInfo, readPidCommand).trim();

            if (pid.isEmpty()) {
                logger.info("PID文件为空，可能进程已停止，实例ID: {}", logstashMachineId);
                // 删除空的PID文件并清理数据库
                String removePidCommand = String.format("rm -f %s", pidFile);
                sshClient.executeCommand(machineInfo, removePidCommand);
                logstashMachineMapper.updateProcessPidById(logstashMachineId, null);
                future.complete(true);
                return future;
            }

            logger.info("开始停止Logstash进程，实例ID: {}, PID: {}", logstashMachineId, pid);

            // 尝试优雅停止（发送TERM信号）
            String stopCommand = String.format("kill %s", pid);
            sshClient.executeCommand(machineInfo, stopCommand);

            // 等待进程停止
            Thread.sleep(5000);

            // 检查进程是否已停止
            String checkProcessCommand =
                    String.format(
                            "if ps -p %s > /dev/null 2>&1; then echo \"running\"; else echo"
                                    + " \"stopped\"; fi",
                            pid);
            String checkResult = sshClient.executeCommand(machineInfo, checkProcessCommand);

            boolean stopped = "stopped".equals(checkResult.trim());

            if (!stopped) {
                // 如果优雅停止失败，尝试强制停止
                logger.warn("优雅停止失败，尝试强制停止，实例ID: {}, PID: {}", logstashMachineId, pid);
                String forceStopCommand = String.format("kill -9 %s", pid);
                sshClient.executeCommand(machineInfo, forceStopCommand);

                // 再次等待
                Thread.sleep(2000);

                // 再次检查
                String finalCheckResult =
                        sshClient.executeCommand(machineInfo, checkProcessCommand);
                stopped = "stopped".equals(finalCheckResult.trim());
            }

            if (stopped) {
                // 删除PID文件并清理数据库
                String removePidCommand = String.format("rm -f %s", pidFile);
                sshClient.executeCommand(machineInfo, removePidCommand);
                logstashMachineMapper.updateProcessPidById(logstashMachineId, null);

                logger.info("成功停止Logstash进程，实例ID: {}, PID: {}", logstashMachineId, pid);
            } else {
                logger.error("停止Logstash进程失败，实例ID: {}, PID: {}", logstashMachineId, pid);
            }

            future.complete(stopped);
        } catch (Exception e) {
            logger.error(
                    "停止Logstash进程时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            future.completeExceptionally(new SshOperationException("停止进程失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public String getDescription() {
        return "停止Logstash进程";
    }
}
