package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import java.util.concurrent.CompletableFuture;

/** 创建Logstash配置文件命令 */
public class CreateConfigCommand extends AbstractLogstashCommand {

    private final String configContent;

    public CreateConfigCommand(
            SshClient sshClient,
            String deployDir,
            Long processId,
            String configContent,
            LogstashMachineMapper logstashMachineMapper) {
        super(sshClient, deployDir, processId, logstashMachineMapper);
        this.configContent = configContent;
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(MachineInfo machineInfo) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            String processDir = getProcessDirectory(machineInfo);
            String configDir = processDir + "/config";
            String configPath = configDir + "/logstash-" + processId + ".conf";

            // 确保配置目录存在
            String createDirCommand = String.format("mkdir -p %s", configDir);
            sshClient.executeCommand(machineInfo, createDirCommand);

            // 创建临时文件
            String tempFile =
                    String.format(
                            "/tmp/logstash-config-%d-%d.conf",
                            processId, System.currentTimeMillis());

            // 将配置写入临时文件，使用heredoc避免特殊字符问题
            String createConfigCommand =
                    String.format("cat > %s << 'EOF'\n%s\nEOF", tempFile, configContent);
            sshClient.executeCommand(machineInfo, createConfigCommand);

            // 移动到最终位置
            String moveCommand = String.format("mv %s %s", tempFile, configPath);
            sshClient.executeCommand(machineInfo, moveCommand);

            // 检查配置文件是否创建成功
            String checkCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"success\"; else echo \"failed\"; fi",
                            configPath);
            String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

            boolean success = "success".equals(checkResult.trim());
            if (success) {
                logger.info("成功创建Logstash配置文件: {}", configPath);
            } else {
                logger.error("创建Logstash配置文件失败: {}", configPath);
            }

            future.complete(success);
        } catch (Exception e) {
            logger.error("创建Logstash配置文件时发生错误: {}", e.getMessage(), e);
            future.completeExceptionally(
                    new SshOperationException("创建Logstash配置文件失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public String getDescription() {
        return "创建Logstash配置文件";
    }
}
