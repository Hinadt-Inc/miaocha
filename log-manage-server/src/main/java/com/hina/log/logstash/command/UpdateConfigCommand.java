package com.hina.log.logstash.command;

import com.hina.log.entity.Machine;
import com.hina.log.exception.SshOperationException;
import com.hina.log.ssh.SshClient;

import java.util.concurrent.CompletableFuture;

/**
 * 更新Logstash配置文件命令
 */
public class UpdateConfigCommand extends AbstractLogstashCommand {

    private final String configContent;

    public UpdateConfigCommand(SshClient sshClient, String deployDir, Long processId, String configContent) {
        super(sshClient, deployDir, processId);
        this.configContent = configContent;
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(Machine machine) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            String processDir = getProcessDirectory();
            String configDir = processDir + "/config";
            String configPath = configDir + "/logstash-" + processId + ".conf";

            // 确保配置目录存在
            String createDirCommand = String.format("mkdir -p %s", configDir);
            sshClient.executeCommand(machine, createDirCommand);

            // 首先检查旧配置文件是否存在
            String checkCommand = String.format("[ -f %s ] && echo \"exists\" || echo \"not_exists\"", configPath);
            String checkResult = sshClient.executeCommand(machine, checkCommand);

            if ("exists".equals(checkResult.trim())) {
                // 备份旧配置
                String backupFile = String.format("%s.bak.%d", configPath, System.currentTimeMillis());
                String backupCommand = String.format("cp %s %s", configPath, backupFile);
                sshClient.executeCommand(machine, backupCommand);
                logger.info("已备份旧配置文件: {}", backupFile);
            }

            // 创建临时文件
            String tempFile = String.format("/tmp/logstash-config-%d-%d.conf",
                    processId, System.currentTimeMillis());

            // 将新配置写入临时文件，使用heredoc避免特殊字符问题
            String createConfigCommand = String.format("cat > %s << 'EOF'\n%s\nEOF",
                    tempFile, configContent);
            sshClient.executeCommand(machine, createConfigCommand);

            // 移动到最终位置
            String moveCommand = String.format("mv %s %s", tempFile, configPath);
            sshClient.executeCommand(machine, moveCommand);

            // 检查配置文件是否更新成功
            String verifyCommand = String.format("[ -f %s ] && echo \"success\"", configPath);
            String verifyResult = sshClient.executeCommand(machine, verifyCommand);

            boolean success = "success".equals(verifyResult.trim());
            if (success) {
                logger.info("成功更新Logstash配置文件: {}", configPath);
            } else {
                logger.error("更新Logstash配置文件失败: {}", configPath);
            }

            future.complete(success);
        } catch (Exception e) {
            logger.error("更新Logstash配置文件时发生错误: {}", e.getMessage(), e);
            future.completeExceptionally(new SshOperationException("更新Logstash配置文件失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public String getDescription() {
        return "更新Logstash配置文件";
    }
}