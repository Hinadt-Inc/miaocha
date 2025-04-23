package com.hina.log.logstash.command;

import com.hina.log.entity.Machine;
import com.hina.log.exception.SshOperationException;
import com.hina.log.ssh.SshClient;

import java.util.concurrent.CompletableFuture;

/**
 * 修改Logstash系统配置命令
 */
public class ModifySystemConfigCommand extends AbstractLogstashCommand {

    public ModifySystemConfigCommand(SshClient sshClient, String deployDir, Long processId) {
        super(sshClient, deployDir, processId);
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(Machine machine) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            String processDir = getProcessDirectory();
            String configPath = processDir + "/config/logstash.yml";

            // 检查配置文件是否存在
            String checkCommand = String.format("if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi", configPath);
            String checkResult = sshClient.executeCommand(machine, checkCommand);

            if ("exists".equals(checkResult.trim())) {
                // 添加配置
                String appendCommand = String.format("echo '\nallow_superuser: true' >> %s", configPath);
                sshClient.executeCommand(machine, appendCommand);
            } else {
                // 文件不存在，创建新文件
                String createCommand = String.format("echo 'allow_superuser: true' > %s", configPath);
                sshClient.executeCommand(machine, createCommand);
            }

            // 验证配置是否添加成功
            String validateCommand = String.format("grep \"allow_superuser: true\" %s | wc -l", configPath);
            String validateResult = sshClient.executeCommand(machine, validateCommand);

            boolean success = !"0".equals(validateResult.trim());
            if (success) {
                logger.info("成功修改Logstash系统配置: {}", configPath);
            } else {
                logger.error("修改Logstash系统配置失败: {}", configPath);
            }

            future.complete(success);
        } catch (Exception e) {
            logger.error("修改Logstash系统配置时发生错误: {}", e.getMessage(), e);
            future.completeExceptionally(new SshOperationException("修改Logstash系统配置失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public String getDescription() {
        return "修改Logstash系统配置";
    }
}