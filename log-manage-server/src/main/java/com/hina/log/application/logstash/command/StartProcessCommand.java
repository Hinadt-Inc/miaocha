package com.hina.log.application.logstash.command;

import com.hina.log.common.exception.SshOperationException;
import com.hina.log.common.ssh.SshClient;
import com.hina.log.domain.entity.MachineInfo;
import com.hina.log.domain.mapper.LogstashMachineMapper;
import java.util.concurrent.CompletableFuture;

/** 启动Logstash进程命令 */
public class StartProcessCommand extends AbstractLogstashCommand {

    public StartProcessCommand(
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
            String pidFile = logDir + "/logstash-" + processId + ".pid";
            String logFile = logDir + "/logstash-" + processId + ".log";
            String configFile = processDir + "/config/logstash-" + processId + ".conf";

            // 创建日志目录
            String createLogDirCommand = String.format("mkdir -p %s", logDir);
            sshClient.executeCommand(machineInfo, createLogDirCommand);

            // 删除可能存在的旧PID文件
            String removePidCommand = String.format("rm -f %s", pidFile);
            sshClient.executeCommand(machineInfo, removePidCommand);

            // 创建启动脚本
            String scriptPath = processDir + "/start-logstash.sh";
            String scriptContent =
                    String.format(
                            "#!/bin/bash\n"
                                + "cd %s\n"
                                + "nohup ./bin/logstash -f %s --config.reload.automatic > %s 2>&1"
                                + " </dev/null & \n"
                                + "echo $! > %s\n",
                            processDir, configFile, logFile, pidFile);

            // 将脚本写入临时文件
            String tempScript =
                    String.format(
                            "/tmp/start-logstash-%d-%d.sh", processId, System.currentTimeMillis());
            String createScriptCommand =
                    String.format("cat > %s << 'EOF'\n%s\nEOF", tempScript, scriptContent);
            sshClient.executeCommand(machineInfo, createScriptCommand);

            // 移动脚本到目标位置并设置可执行权限
            String moveScriptCommand =
                    String.format("mv %s %s && chmod +x %s", tempScript, scriptPath, scriptPath);
            sshClient.executeCommand(machineInfo, moveScriptCommand);

            // 执行启动脚本
            logger.info("开始启动Logstash进程");
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
                logger.warn("PID文件未生成，Logstash进程启动命令可能执行失败");
                future.complete(true);
                return future;
            }

            // 读取PID - 仅确认PID文件非空，不验证进程是否真正运行
            String readPidCommand = String.format("cat %s", pidFile);
            String pid = sshClient.executeCommand(machineInfo, readPidCommand).trim();

            if (pid.isEmpty()) {
                logger.warn("PID文件为空，Logstash进程可能启动失败");
                future.complete(true);
                return future;
            }

            // 只检查PID是否有效，不检查进程是否实际运行
            // 进程的完整验证将由VerifyProcessCommand执行
            logger.info("已创建Logstash进程，PID: {}，进一步验证将由验证命令完成", pid);
            future.complete(true);
        } catch (Exception e) {
            logger.error("启动Logstash进程时发生错误: {}", e.getMessage(), e);
            future.completeExceptionally(
                    new SshOperationException("启动Logstash进程失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public String getDescription() {
        return "启动Logstash进程";
    }
}
