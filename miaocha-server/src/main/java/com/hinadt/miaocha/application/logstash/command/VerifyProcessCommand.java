package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.application.logstash.path.LogstashPathUtils;
import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import java.util.concurrent.CompletableFuture;

/** 验证Logstash进程命令 - 重构支持多实例，基于logstashMachineId */
public class VerifyProcessCommand extends AbstractLogstashCommand {

    public VerifyProcessCommand(
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
    protected CompletableFuture<Boolean> doExecute(MachineInfo machineInfo) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            String processDir = getProcessDirectory(machineInfo);
            String logDir = LogstashPathUtils.buildLogDirPath(processDir);
            String pidFile = LogstashPathUtils.buildPidFilePath(processDir, logstashMachineId);
            String logFile = LogstashPathUtils.buildLogFilePath(processDir, logstashMachineId);

            // 尝试多次验证，最多尝试5次，每次间隔3秒
            verifyProcessWithRetry(machineInfo, processDir, pidFile, logFile, 5, future);
        } catch (Exception e) {
            logger.error(
                    "验证Logstash进程时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            future.completeExceptionally(
                    new SshOperationException("验证Logstash进程失败: " + e.getMessage(), e));
        }

        return future;
    }

    /** 多次尝试验证进程 */
    private void verifyProcessWithRetry(
            MachineInfo machineInfo,
            String processDir,
            String pidFile,
            String logFile,
            int maxRetries,
            CompletableFuture<Boolean> future) {

        CompletableFuture.runAsync(
                () -> {
                    for (int attempt = 1; attempt <= maxRetries; attempt++) {
                        try {
                            logger.info(
                                    "验证Logstash进程，实例ID: {}, 第{}次尝试", logstashMachineId, attempt);

                            // 1. 检查PID文件是否存在
                            String checkPidFileCommand =
                                    String.format(
                                            "if [ -f \"%s\" ]; then echo \"exists\"; else echo"
                                                    + " \"not_exists\"; fi",
                                            pidFile);
                            String pidFileResult =
                                    sshClient.executeCommand(machineInfo, checkPidFileCommand);

                            if (!"exists".equals(pidFileResult.trim())) {
                                logger.warn(
                                        "PID文件不存在，实例ID: {}, 路径: {}", logstashMachineId, pidFile);
                                if (attempt == maxRetries) {
                                    future.complete(false);
                                    return;
                                }
                                Thread.sleep(3000);
                                continue;
                            }

                            // 2. 读取PID
                            String readPidCommand = String.format("cat %s", pidFile);
                            String pid =
                                    sshClient.executeCommand(machineInfo, readPidCommand).trim();

                            if (pid.isEmpty()) {
                                logger.warn("PID文件为空，实例ID: {}", logstashMachineId);
                                if (attempt == maxRetries) {
                                    future.complete(false);
                                    return;
                                }
                                Thread.sleep(3000);
                                continue;
                            }

                            // 3. 检查进程是否存在
                            String checkProcessCommand =
                                    String.format(
                                            "if ps -p %s > /dev/null 2>&1; then echo \"running\";"
                                                    + " else echo \"not_running\"; fi",
                                            pid);
                            String processResult =
                                    sshClient.executeCommand(machineInfo, checkProcessCommand);

                            if (!"running".equals(processResult.trim())) {
                                logger.warn("进程未运行，实例ID: {}, PID: {}", logstashMachineId, pid);
                                if (attempt == maxRetries) {
                                    future.complete(false);
                                    return;
                                }
                                Thread.sleep(3000);
                                continue;
                            }

                            // 4. 检查日志文件是否存在（可选）
                            String checkLogCommand =
                                    String.format(
                                            "if [ -f \"%s\" ]; then echo \"exists\"; else echo"
                                                    + " \"not_exists\"; fi",
                                            logFile);
                            String logResult =
                                    sshClient.executeCommand(machineInfo, checkLogCommand);

                            // 5. 更新数据库中的PID（确保数据一致性）
                            logstashMachineMapper.updateProcessPidById(logstashMachineId, pid);

                            logger.info(
                                    "成功验证Logstash进程，实例ID: {}, PID: {}, 日志文件: {}",
                                    logstashMachineId,
                                    pid,
                                    "exists".equals(logResult.trim()) ? "存在" : "不存在");
                            future.complete(true);
                            return;

                        } catch (Exception e) {
                            logger.warn(
                                    "验证进程时出错，实例ID: {}, 第{}次尝试, 错误: {}",
                                    logstashMachineId,
                                    attempt,
                                    e.getMessage());
                            if (attempt == maxRetries) {
                                future.completeExceptionally(e);
                                return;
                            }
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                future.completeExceptionally(ie);
                                return;
                            }
                        }
                    }
                });
    }

    @Override
    public String getDescription() {
        return "验证Logstash进程";
    }
}
