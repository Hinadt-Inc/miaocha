package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import java.util.concurrent.CompletableFuture;

/** 创建目录命令 - 重构支持多实例，基于logstashMachineId */
public class CreateDirectoryCommand extends AbstractLogstashCommand {

    public CreateDirectoryCommand(
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
    protected CompletableFuture<Boolean> doExecute(MachineInfo machineInfo) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // 创建实例目录
            String processDir = getProcessDirectory();
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
                logger.info("成功创建实例目录: {}, 实例ID: {}", processDir, logstashMachineId);
            } else {
                logger.error("创建实例目录失败: {}, 实例ID: {}", processDir, logstashMachineId);
            }

            future.complete(success);
        } catch (Exception e) {
            logger.error("创建实例目录时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            future.completeExceptionally(new SshOperationException("创建目录失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    protected CompletableFuture<Boolean> checkAlreadyExecuted(MachineInfo machineInfo) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // 检查目录是否已经存在
            String processDir = getProcessDirectory();

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
        return "创建Logstash实例目录";
    }
}
