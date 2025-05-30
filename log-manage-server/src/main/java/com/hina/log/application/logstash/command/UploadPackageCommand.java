package com.hina.log.application.logstash.command;

import com.hina.log.common.exception.SshOperationException;
import com.hina.log.common.ssh.SshClient;
import com.hina.log.domain.entity.MachineInfo;
import com.hina.log.domain.mapper.LogstashMachineMapper;
import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/** 上传Logstash安装包命令 */
public class UploadPackageCommand extends AbstractLogstashCommand {

    private final String packagePath;

    public UploadPackageCommand(
            SshClient sshClient,
            String packagePath,
            String deployDir,
            Long processId,
            LogstashMachineMapper logstashMachineMapper) {
        super(sshClient, deployDir, processId, logstashMachineMapper);
        this.packagePath = packagePath;
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(MachineInfo machineInfo) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            String processDir = getProcessDirectory(machineInfo);
            String fileName = new File(packagePath).getName();
            String targetPath = Paths.get(processDir, fileName).toString();

            // 上传文件
            logger.info("开始上传Logstash安装包到: {}", targetPath);
            sshClient.uploadFile(machineInfo, packagePath, processDir);

            // 检查文件是否上传成功
            String checkCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                            targetPath);
            String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

            boolean success = "exists".equals(checkResult.trim());
            if (success) {
                logger.info("成功上传Logstash安装包: {}", targetPath);
            } else {
                logger.error("上传Logstash安装包失败: {}", targetPath);
            }

            future.complete(success);
        } catch (Exception e) {
            future.completeExceptionally(
                    new SshOperationException("上传Logstash安装包失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public String getDescription() {
        return "上传Logstash安装包";
    }
}
