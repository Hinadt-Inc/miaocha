package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import java.util.concurrent.CompletableFuture;

/** 验证Logstash进程命令 */
public class VerifyProcessCommand extends AbstractLogstashCommand {

    public VerifyProcessCommand(
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
            String logDir = processDir + "/logs";
            String pidFile = processDir + "/logs/logstash-" + processId + ".pid";
            String logFile = processDir + "/logs/logstash-" + processId + ".log";

            // 尝试多次验证，最多尝试5次，每次间隔3秒
            verifyProcessWithRetry(machineInfo, processDir, pidFile, logFile, 5, future);
        } catch (Exception e) {
            logger.error("验证Logstash进程时发生错误: {}", e.getMessage(), e);
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
            int remainingAttempts,
            CompletableFuture<Boolean> future) {
        if (remainingAttempts <= 0) {
            logger.error("已达到最大重试次数，验证Logstash进程失败");
            future.complete(false);
            return;
        }

        try {
            // 检查PID文件
            String checkPidCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                            pidFile);
            String checkPidResult = sshClient.executeCommand(machineInfo, checkPidCommand);

            boolean pidFileExists = "exists".equals(checkPidResult.trim());
            if (!pidFileExists) {
                int currentAttempt = 6 - remainingAttempts;
                logger.warn("PID文件不存在，验证失败 (尝试 {}/5)", currentAttempt);

                if (remainingAttempts > 1) {
                    logger.info("3秒后将重新尝试验证");
                    // 调度一个延迟任务
                    CompletableFuture.runAsync(
                            () -> {
                                try {
                                    Thread.sleep(3000);
                                    verifyProcessWithRetry(
                                            machineInfo,
                                            processDir,
                                            pidFile,
                                            logFile,
                                            remainingAttempts - 1,
                                            future);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    logger.error("线程被中断", e);
                                    future.complete(false);
                                }
                            });
                } else {
                    future.complete(false);
                }
                return;
            }

            // 读取PID
            String readPidCommand = String.format("cat %s", pidFile);
            String pid = sshClient.executeCommand(machineInfo, readPidCommand).trim();

            if (pid.isEmpty()) {
                logger.warn("PID文件为空，验证失败");

                if (remainingAttempts > 1) {
                    logger.info("3秒后将重新尝试验证");
                    // 调度一个延迟任务
                    CompletableFuture.runAsync(
                            () -> {
                                try {
                                    Thread.sleep(3000);
                                    verifyProcessWithRetry(
                                            machineInfo,
                                            processDir,
                                            pidFile,
                                            logFile,
                                            remainingAttempts - 1,
                                            future);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    logger.error("线程被中断", e);
                                    future.complete(false);
                                }
                            });
                } else {
                    future.complete(false);
                }
                return;
            }

            // 验证进程存在且是Java进程(Logstash基于Java)
            // 使用一个不会失败的命令来检查进程
            String checkProcessCommand =
                    String.format("ps -p %s -o comm= | grep -c java || echo 0", pid);
            String checkProcessResult =
                    sshClient.executeCommand(machineInfo, checkProcessCommand).trim();

            boolean isJavaProcess = !"0".equals(checkProcessResult.trim());
            if (!isJavaProcess) {
                // 尝试简单的进程验证，使用不会失败的命令
                String simpleCheckCommand =
                        String.format(
                                "if ps -p %s > /dev/null; then echo \"running\"; else echo"
                                        + " \"not_running\"; fi",
                                pid);
                String simpleCheckResult =
                        sshClient.executeCommand(machineInfo, simpleCheckCommand);

                boolean isRunning = "running".equals(simpleCheckResult.trim());
                if (!isRunning) {
                    logger.error("Logstash进程不存在，验证失败");

                    if (remainingAttempts > 1) {
                        logger.info("3秒后将重新尝试验证");
                        // 调度一个延迟任务
                        CompletableFuture.runAsync(
                                () -> {
                                    try {
                                        Thread.sleep(3000);
                                        verifyProcessWithRetry(
                                                machineInfo,
                                                processDir,
                                                pidFile,
                                                logFile,
                                                remainingAttempts - 1,
                                                future);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        logger.error("线程被中断", e);
                                        future.complete(false);
                                    }
                                });
                    } else {
                        future.complete(false);
                    }
                    return;
                }

                logger.warn("Logstash进程PID {} 存在但不是标准的Java进程，可能存在问题", pid);
            }

            // 检查是否在日志中有成功启动的标志
            // 使用一个不会失败的命令
            String logCheckCommand =
                    String.format(
                            "if grep -q \"Successfully started Logstash\" %s; then echo"
                                    + " \"success\"; else echo \"\"; fi",
                            logFile);
            String logCheckResult = sshClient.executeCommand(machineInfo, logCheckCommand);

            boolean logSuccess = "success".equals(logCheckResult.trim());
            if (logSuccess) {
                logger.info("日志文件显示Logstash已成功启动");

                // 保存PID到数据库中
                try {
                    // 更新LogstashMachine表中的process_pid
                    int rows =
                            logstashMachineMapper.updateProcessPid(
                                    processId, machineInfo.getId(), pid);
                    if (rows > 0) {
                        logger.info(
                                "成功更新Logstash进程PID: {}，关联进程ID: {}，机器ID: {}",
                                pid,
                                processId,
                                machineInfo.getId());
                    } else {
                        logger.warn("未能更新Logstash进程PID，可能找不到对应的LogstashMachine记录");
                    }
                } catch (Exception e) {
                    logger.error("更新Logstash进程PID到数据库时发生错误: {}", e.getMessage(), e);
                    // 继续完成验证，不因为数据库操作失败而影响进程验证结果
                }

                future.complete(true);
            } else {
                logger.warn("未在日志中找到成功启动的标记，但进程正在运行");

                if (remainingAttempts > 1) {
                    logger.info("3秒后将重新尝试检查日志");
                    // 调度一个延迟任务
                    CompletableFuture.runAsync(
                            () -> {
                                try {
                                    Thread.sleep(3000);
                                    verifyProcessWithRetry(
                                            machineInfo,
                                            processDir,
                                            pidFile,
                                            logFile,
                                            remainingAttempts - 1,
                                            future);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    logger.error("线程被中断", e);
                                    future.complete(false);
                                }
                            });
                } else {
                    // 如果最后一次检查仍未找到成功标记，但进程在运行，仍然视为成功
                    logger.info("虽然未找到成功标记，但进程正在运行，验证通过");

                    // 保存PID到数据库中
                    try {
                        // 更新LogstashMachine表中的process_pid
                        int rows =
                                logstashMachineMapper.updateProcessPid(
                                        processId, machineInfo.getId(), pid);
                        if (rows > 0) {
                            logger.info(
                                    "成功更新Logstash进程PID: {}，关联进程ID: {}，机器ID: {}",
                                    pid,
                                    processId,
                                    machineInfo.getId());
                        } else {
                            logger.warn("未能更新Logstash进程PID，可能找不到对应的LogstashMachine记录");
                        }
                    } catch (Exception e) {
                        logger.error("更新Logstash进程PID到数据库时发生错误: {}", e.getMessage(), e);
                        // 继续完成验证，不因为数据库操作失败而影响进程验证结果
                    }

                    future.complete(true);
                }
            }
        } catch (Exception e) {
            if (remainingAttempts > 1) {
                logger.warn("验证过程中发生错误: {}，3秒后将重新尝试", e.getMessage());
                // 调度一个延迟任务
                CompletableFuture.runAsync(
                        () -> {
                            try {
                                Thread.sleep(3000);
                                verifyProcessWithRetry(
                                        machineInfo,
                                        processDir,
                                        pidFile,
                                        logFile,
                                        remainingAttempts - 1,
                                        future);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                logger.error("线程被中断", ie);
                                future.complete(false);
                            }
                        });
            } else {
                logger.error("验证Logstash进程时发生错误: {}", e.getMessage(), e);
                future.complete(false);
            }
        }
    }

    @Override
    public String getDescription() {
        return "验证Logstash进程";
    }
}
