package com.hina.log.logstash.command;

import com.hina.log.entity.Machine;
import com.hina.log.exception.SshOperationException;
import com.hina.log.ssh.SshClient;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 * 上传Logstash安装包命令
 */
public class UploadPackageCommand extends AbstractLogstashCommand {

    private final String packagePath;

    public UploadPackageCommand(SshClient sshClient, String packagePath, String deployDir, Long processId) {
        super(sshClient, deployDir, processId);
        this.packagePath = packagePath;
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(Machine machine) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // 创建目标目录
            String processDir = getProcessDirectory();

            // 获取文件名
            String fileName = new File(packagePath).getName();
            String targetPath = Paths.get(processDir, fileName).toString();

            // 上传文件
            logger.info("开始上传Logstash安装包到: {}", targetPath);
            sshClient.uploadFile(machine, packagePath, processDir);

            // 检查文件是否上传成功
            String checkCommand = String.format("[ -f %s ] && echo \"exists\"", targetPath);
            String checkResult = sshClient.executeCommand(machine, checkCommand);

            boolean success = "exists".equals(checkResult.trim());
            if (success) {
                logger.info("成功上传Logstash安装包: {}", targetPath);
            } else {
                logger.error("上传Logstash安装包失败: {}", targetPath);
            }

            future.complete(success);
        } catch (Exception e) {
            logger.error("上传Logstash安装包时发生错误: {}", e.getMessage(), e);
            future.completeExceptionally(new SshOperationException("上传Logstash安装包失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public String getDescription() {
        return "上传Logstash安装包";
    }
}