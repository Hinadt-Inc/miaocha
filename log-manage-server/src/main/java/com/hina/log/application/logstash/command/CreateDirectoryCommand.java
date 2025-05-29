package com.hina.log.application.logstash.command;

import com.hina.log.common.exception.SshOperationException;
import com.hina.log.common.ssh.SshClient;
import com.hina.log.domain.entity.MachineInfo;
import java.util.concurrent.CompletableFuture;

/** 创建目录命令 */
public class CreateDirectoryCommand extends AbstractLogstashCommand {

    public CreateDirectoryCommand(SshClient sshClient, String deployDir, Long processId) {
        super(sshClient, deployDir, processId);
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(MachineInfo machineInfo) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // 创建进程目录
            String processDir = getProcessDirectory(machineInfo);
            String command = String.format("mkdir -p %s", processDir);
            sshClient.executeCommand(machineInfo, command);

            // 检查目录是否创建成功
            String checkCommand =
                    String.format(
                            "if [ -d \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                            processDir);
            String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

            boolean success = "exists".equals(checkResult.trim());
            if (success) {
                logger.info("成功创建目录: {}", processDir);
            } else {
                logger.error("创建目录失败: {}", processDir);
            }

            future.complete(success);
        } catch (Exception e) {
            logger.error("创建目录时发生错误: {}", e.getMessage(), e);
            future.completeExceptionally(new SshOperationException("创建目录失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    protected CompletableFuture<Boolean> checkAlreadyExecuted(MachineInfo machineInfo) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // 检查目录是否已经存在
            String processDir = getProcessDirectory(machineInfo);

            // 使用不会因目录不存在而失败的命令
            // 使用 || 实现短路逻辑：如果第一个命令失败（目录不存在），则执行第二个命令返回"not_exists"
            String checkCommand =
                    String.format(
                            "if [ -d \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                            processDir);
            String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

            boolean exists = "exists".equals(checkResult.trim());
            logger.info("检查目录 [{}] 存在性: {}", processDir, exists ? "已存在" : "不存在");
            future.complete(exists);
        } catch (Exception e) {
            logger.error("检查目录存在性时发生错误: {}", e.getMessage(), e);
            // 发生异常时，假设目录不存在
            future.complete(false);
        }

        return future;
    }

    @Override
    public String getDescription() {
        return "创建Logstash进程目录";
    }
}
