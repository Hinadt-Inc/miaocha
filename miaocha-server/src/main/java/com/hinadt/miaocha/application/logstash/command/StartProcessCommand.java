package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import java.util.concurrent.CompletableFuture;

/** 启动Logstash进程命令 - 重构支持多实例，基于logstashMachineId */
public class StartProcessCommand extends AbstractLogstashCommand {

    public StartProcessCommand(
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

                        // 检查PID文件是否存在且进程正在运行
                        String checkCommand =
                                String.format(
                                        "if [ -f \"%s\" ]; then "
                                                + "  pid=$(cat %s); "
                                                + "  if ps -p $pid > /dev/null 2>&1; then "
                                                + "    echo \"running\"; "
                                                + "  else "
                                                + "    echo \"not_running\"; "
                                                + "  fi; "
                                                + "else "
                                                + "  echo \"no_pid_file\"; "
                                                + "fi",
                                        pidFile, pidFile);
                        String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

                        boolean running = "running".equals(checkResult.trim());
                        if (running) {
                            logger.info("Logstash进程已在运行，跳过启动，实例ID: {}", logstashMachineId);
                        }
                        return running;
                    } catch (Exception e) {
                        logger.warn(
                                "检查Logstash进程是否运行时出错，实例ID: {}, 错误: {}",
                                logstashMachineId,
                                e.getMessage());
                        return false;
                    }
                });
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(MachineInfo machineInfo) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            String processDir = getProcessDirectory(machineInfo);
            String configDir = processDir + "/config";
            String logDir = processDir + "/logs";
            String configPath = configDir + "/logstash-" + logstashMachineId + ".conf";
            String pidFile = logDir + "/logstash-" + logstashMachineId + ".pid";
            String logFile = logDir + "/logstash-" + logstashMachineId + ".log";

            // 确保日志目录存在
            String createLogDirCommand = String.format("mkdir -p %s", logDir);
            sshClient.executeCommand(machineInfo, createLogDirCommand);

            // 构建启动命令
            String startCommand =
                    String.format(
                            "cd %s && nohup ./bin/logstash -f %s --path.logs %s --path.data %s/data"
                                + " --log.level info --config.reload.automatic > %s 2>&1 & echo $!"
                                + " > %s",
                            processDir, configPath, logDir, processDir, logFile, pidFile);

            logger.info("启动Logstash进程，实例ID: {}, 命令: {}", logstashMachineId, startCommand);
            sshClient.executeCommand(machineInfo, startCommand);

            // 等待一段时间让进程启动
            Thread.sleep(3000);

            // 检查进程是否启动成功
            String checkCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then "
                                    + "  pid=$(cat %s); "
                                    + "  if ps -p $pid > /dev/null 2>&1; then "
                                    + "    echo \"success\"; "
                                    + "  else "
                                    + "    echo \"failed\"; "
                                    + "  fi; "
                                    + "else "
                                    + "  echo \"no_pid_file\"; "
                                    + "fi",
                            pidFile, pidFile);
            String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

            boolean success = "success".equals(checkResult.trim());
            if (success) {
                // 读取PID并更新数据库
                String readPidCommand = String.format("cat %s", pidFile);
                String pid = sshClient.executeCommand(machineInfo, readPidCommand).trim();

                // 更新数据库中的PID
                logstashMachineMapper.updateProcessPidById(logstashMachineId, pid);

                logger.info("成功启动Logstash进程，实例ID: {}, PID: {}", logstashMachineId, pid);
            } else {
                logger.error("启动Logstash进程失败，实例ID: {}, 检查结果: {}", logstashMachineId, checkResult);
            }

            future.complete(success);
        } catch (Exception e) {
            logger.error(
                    "启动Logstash进程时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            future.completeExceptionally(new SshOperationException("启动进程失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public String getDescription() {
        return "启动Logstash进程";
    }
}
