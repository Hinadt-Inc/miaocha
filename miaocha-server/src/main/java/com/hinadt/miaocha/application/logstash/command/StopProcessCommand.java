package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.application.logstash.path.LogstashPathUtils;
import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.infrastructure.mapper.LogstashMachineMapper;

/** 停止Logstash进程命令 - 重构支持多实例，基于logstashMachineId */
public class StopProcessCommand extends AbstractLogstashCommand {

    /** 停止进程的重试超时时间（秒） */
    private static final int GRACEFUL_STOP_TIMEOUT_SECONDS = 360; // 6分钟

    private static final int FORCE_STOP_TIMEOUT_SECONDS = 180; // 3分钟

    /** 轮询间隔时间（秒） */
    private static final int POLL_INTERVAL_SECONDS = 3;

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
    protected boolean checkAlreadyExecuted(MachineInfo machineInfo) {
        try {
            String processDir = getProcessDirectory();
            String pidFile = LogstashPathUtils.buildPidFilePath(processDir, logstashMachineId);

            // 检查PID文件是否存在
            String checkPidFileCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"exists\"; else echo"
                                    + " \"not_exists\"; fi",
                            pidFile);
            String pidFileResult = sshClient.executeCommand(machineInfo, checkPidFileCommand);

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
            String processResult = sshClient.executeCommand(machineInfo, checkProcessCommand);

            boolean running = "running".equals(processResult.trim());
            if (!running) {
                logger.info("进程已停止，实例ID: {}, PID: {}", logstashMachineId, pid);
            }
            return !running; // 如果不在运行，则已经停止
        } catch (Exception e) {
            logger.warn("检查进程是否已停止时出错，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage());
            return false; // 出错时假设需要执行停止操作
        }
    }

    @Override
    protected boolean doExecute(MachineInfo machineInfo) {
        try {
            String processDir = getProcessDirectory();
            String pidFile = LogstashPathUtils.buildPidFilePath(processDir, logstashMachineId);

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
                return true;
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
                return true;
            }

            logger.info("停止Logstash进程，实例ID: {}, PID: {}", logstashMachineId, pid);

            // 首先尝试优雅停止进程
            String gracefulStopCommand = String.format("kill %s", pid);
            sshClient.executeCommand(machineInfo, gracefulStopCommand);
            logger.info("已发送优雅停止信号，等待进程停止，实例ID: {}, PID: {}", logstashMachineId, pid);

            // 等待进程优雅停止，最多等待6分钟
            boolean stopped = waitForProcessToStop(machineInfo, pid, GRACEFUL_STOP_TIMEOUT_SECONDS);

            if (!stopped) {
                // 优雅停止失败，打印警告日志并使用强制停止
                logger.warn("优雅停止超时，将使用强制停止，实例ID: {}, PID: {}", logstashMachineId, pid);

                String forceStopCommand = String.format("kill -9 %s", pid);
                sshClient.executeCommand(machineInfo, forceStopCommand);
                logger.info("已发送强制停止信号，等待进程停止，实例ID: {}, PID: {}", logstashMachineId, pid);

                // 等待强制停止，最多等待3分钟
                stopped = waitForProcessToStop(machineInfo, pid, FORCE_STOP_TIMEOUT_SECONDS);
            }

            if (stopped) {
                // 删除PID文件并清理数据库
                String removePidCommand = String.format("rm -f %s", pidFile);
                sshClient.executeCommand(machineInfo, removePidCommand);
                logstashMachineMapper.updateProcessPidById(logstashMachineId, null);

                logger.info("成功停止Logstash进程，实例ID: {}, PID: {}", logstashMachineId, pid);
            } else {
                logger.error("停止Logstash进程失败，超时等待未停止，实例ID: {}, PID: {}", logstashMachineId, pid);
            }

            return stopped;
        } catch (Exception e) {
            throw new SshOperationException("停止进程失败: " + e.getMessage(), e);
        }
    }

    /**
     * 等待进程停止，使用轮询方式检查进程是否已停止
     *
     * @param machineInfo 机器信息
     * @param pid 进程ID
     * @param timeoutSeconds 超时时间（秒）
     * @return true 如果进程已停止，false 如果超时仍未停止
     */
    private boolean waitForProcessToStop(MachineInfo machineInfo, String pid, int timeoutSeconds) {
        String checkProcessCommand =
                String.format(
                        "if ps -p %s > /dev/null 2>&1; then echo \"running\"; else echo"
                                + " \"stopped\"; fi",
                        pid);

        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000L;
        long pollIntervalMillis = POLL_INTERVAL_SECONDS * 1000L;

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                String checkResult = sshClient.executeCommand(machineInfo, checkProcessCommand);
                boolean stopped = "stopped".equals(checkResult.trim());

                if (stopped) {
                    logger.info(
                            "进程已停止，实例ID: {}, PID: {}, 耗时: {}ms",
                            logstashMachineId,
                            pid,
                            System.currentTimeMillis() - startTime);
                    return true;
                }

                // 等待下一次轮询
                Thread.sleep(pollIntervalMillis);

                logger.debug(
                        "等待进程停止中，实例ID: {}, PID: {}, 已等待: {}ms",
                        logstashMachineId,
                        pid,
                        System.currentTimeMillis() - startTime);

            } catch (InterruptedException e) {
                logger.warn("等待进程停止被中断，实例ID: {}, PID: {}", logstashMachineId, pid);
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                logger.warn(
                        "检查进程状态时出错，实例ID: {}, PID: {}, 错误: {}",
                        logstashMachineId,
                        pid,
                        e.getMessage());
                // 发生异常时继续等待，可能是临时网络问题
            }
        }

        logger.warn(
                "等待进程停止超时，实例ID: {}, PID: {}, 超时时间: {}秒", logstashMachineId, pid, timeoutSeconds);
        return false;
    }

    @Override
    public String getDescription() {
        return "停止Logstash进程";
    }
}
