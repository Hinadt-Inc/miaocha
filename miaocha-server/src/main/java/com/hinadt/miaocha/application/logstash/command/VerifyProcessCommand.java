package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.application.logstash.path.LogstashPathUtils;
import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;

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
    protected boolean checkAlreadyExecuted(MachineInfo machineInfo) {
        return false;
    }

    @Override
    protected boolean doExecute(MachineInfo machineInfo) {
        try {
            String processDir = getProcessDirectory();
            String logDir = LogstashPathUtils.buildLogDirPath(processDir);
            String pidFile = LogstashPathUtils.buildPidFilePath(processDir, logstashMachineId);
            String logFile = LogstashPathUtils.buildLogFilePath(processDir, logstashMachineId);

            // 尝试多次验证，最多尝试5次，每次间隔3秒
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    logger.info("验证Logstash进程，实例ID: {}, 第{}次尝试", logstashMachineId, attempt);

                    // 1. 检查PID文件是否存在
                    String checkPidFileCommand =
                            String.format(
                                    "if [ -f \"%s\" ]; then echo \"exists\"; else echo"
                                            + " \"not_exists\"; fi",
                                    pidFile);
                    String pidFileResult =
                            sshClient.executeCommand(machineInfo, checkPidFileCommand);

                    if (!"exists".equals(pidFileResult.trim())) {
                        logger.warn("PID文件不存在，实例ID: {}, 路径: {}", logstashMachineId, pidFile);
                        if (attempt == 5) {
                            return false;
                        }
                        Thread.sleep(3000);
                        continue;
                    }

                    // 2. 读取PID
                    String readPidCommand = String.format("cat %s", pidFile);
                    String pid = sshClient.executeCommand(machineInfo, readPidCommand).trim();

                    if (pid.isEmpty()) {
                        logger.warn("PID文件为空，实例ID: {}", logstashMachineId);
                        if (attempt == 5) {
                            return false;
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
                        if (attempt == 5) {
                            return false;
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
                    String logResult = sshClient.executeCommand(machineInfo, checkLogCommand);

                    // 5. 更新数据库中的PID（确保数据一致性）
                    logstashMachineMapper.updateProcessPidById(logstashMachineId, pid);

                    logger.info(
                            "成功验证Logstash进程，实例ID: {}, PID: {}, 日志文件: {}",
                            logstashMachineId,
                            pid,
                            "exists".equals(logResult.trim()) ? "存在" : "不存在");
                    return true;

                } catch (Exception e) {
                    logger.warn(
                            "验证进程时出错，实例ID: {}, 第{}次尝试, 错误: {}",
                            logstashMachineId,
                            attempt,
                            e.getMessage());
                    if (attempt == 5) {
                        throw e;
                    }
                    Thread.sleep(3000);
                }
            }

            // 这里不应该被执行到，因为在循环中已经处理了所有情况
            return false;
        } catch (Exception e) {
            throw new SshOperationException("验证Logstash进程失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getDescription() {
        return "验证Logstash进程";
    }
}
