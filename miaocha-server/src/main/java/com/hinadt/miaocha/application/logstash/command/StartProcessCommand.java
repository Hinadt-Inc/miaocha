package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.application.logstash.path.LogstashPathUtils;
import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;

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
    protected boolean checkAlreadyExecuted(MachineInfo machineInfo) {
        try {
            String processDir = getProcessDirectory();
            String pidFile = LogstashPathUtils.buildPidFilePath(processDir, logstashMachineId);

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
            logger.warn("检查Logstash进程是否运行时出错，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage());
            return false;
        }
    }

    @Override
    protected boolean doExecute(MachineInfo machineInfo) {
        try {
            String processDir = getProcessDirectory();
            String configDir = LogstashPathUtils.buildConfigDirPath(processDir);
            String logDir = LogstashPathUtils.buildLogDirPath(processDir);
            String configPath =
                    LogstashPathUtils.buildConfigFilePath(processDir, logstashMachineId);
            String pidFile = LogstashPathUtils.buildPidFilePath(processDir, logstashMachineId);
            String logFile = LogstashPathUtils.buildLogFilePath(processDir, logstashMachineId);

            // 确保日志目录存在
            String createLogDirCommand = String.format("mkdir -p %s", logDir);
            sshClient.executeCommand(machineInfo, createLogDirCommand);

            // 删除可能存在的旧PID文件
            String removePidCommand = String.format("rm -f %s", pidFile);
            sshClient.executeCommand(machineInfo, removePidCommand);

            // 创建启动脚本
            String scriptPath = processDir + "/start-logstash-" + logstashMachineId + ".sh";
            String scriptContent =
                    String.format(
                            "#!/bin/bash\n"
                                + "cd %s\n"
                                + "nohup ./bin/logstash -f %s --path.logs %s --path.data %s/data"
                                + " --log.level info --config.reload.automatic > %s 2>&1 </dev/null"
                                + " & \n"
                                + "echo $! > %s\n",
                            processDir, configPath, logDir, processDir, logFile, pidFile);

            // 将脚本写入临时文件
            String tempScript =
                    String.format(
                            "/tmp/start-logstash-%d-%d.sh",
                            logstashMachineId, System.currentTimeMillis());
            String createScriptCommand =
                    String.format("cat > %s << 'EOF'\n%s\nEOF", tempScript, scriptContent);
            sshClient.executeCommand(machineInfo, createScriptCommand);

            // 移动脚本到目标位置并设置可执行权限
            String moveScriptCommand =
                    String.format("mv %s %s && chmod +x %s", tempScript, scriptPath, scriptPath);
            sshClient.executeCommand(machineInfo, moveScriptCommand);

            // 执行启动脚本
            logger.info("执行启动脚本，实例ID: {}, 脚本路径: {}", logstashMachineId, scriptPath);
            sshClient.executeCommand(machineInfo, scriptPath);

            // 等待几秒钟，确保进程有时间启动
            Thread.sleep(3000);

            // 检查PID文件是否生成 - 这只是一个初步检查，确认启动命令执行后有输出PID
            // 注意：完整的进程验证将由VerifyProcessCommand完成
            String checkPidCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                            pidFile);
            String checkPidResult = sshClient.executeCommand(machineInfo, checkPidCommand);

            boolean pidFileExists = "exists".equals(checkPidResult.trim());
            if (!pidFileExists) {
                logger.warn("PID文件未生成，Logstash进程启动命令可能执行失败，实例ID: {}", logstashMachineId);
                return true;
            }

            // 读取PID - 仅确认PID文件非空，不验证进程是否真正运行
            String readPidCommand = String.format("cat %s", pidFile);
            String pid = sshClient.executeCommand(machineInfo, readPidCommand).trim();

            if (pid.isEmpty()) {
                logger.warn("PID文件为空，Logstash进程可能启动失败，实例ID: {}", logstashMachineId);
                return true;
            }

            // 更新数据库中的PID
            logstashMachineMapper.updateProcessPidById(logstashMachineId, pid);

            // 只检查PID是否有效，不检查进程是否实际运行
            // 进程的完整验证将由VerifyProcessCommand执行
            logger.info("已创建Logstash进程，实例ID: {}, PID: {}，进一步验证将由验证命令完成", logstashMachineId, pid);
            return true;
        } catch (Exception e) {
            logger.error(
                    "启动Logstash进程时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            throw new SshOperationException("启动进程失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getDescription() {
        return "启动Logstash进程";
    }
}
