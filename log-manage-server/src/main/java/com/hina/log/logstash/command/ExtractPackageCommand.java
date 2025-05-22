package com.hina.log.logstash.command;

import com.hina.log.entity.Machine;
import com.hina.log.exception.SshOperationException;
import com.hina.log.ssh.SshClient;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * 解压Logstash安装包命令
 */
public class ExtractPackageCommand extends AbstractLogstashCommand {

    private final String packagePath;

    public ExtractPackageCommand(SshClient sshClient, String deployDir, String packagePath, Long processId) {
        super(sshClient, deployDir, processId);
        this.packagePath = packagePath;
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(Machine machine) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // 获取进程目录和安装包文件名
            String processDir = getProcessDirectory();
            String fileName = new File(packagePath).getName();
            String remotePackagePath = processDir + "/" + fileName;

            // 解压安装包，使用--strip-components=1去除顶层目录
            logger.info("开始解压Logstash安装包: {}", remotePackagePath);
            String extractCommand = String.format("cd %s && tar -xzf %s --strip-components=1",
                    processDir, fileName);
            sshClient.executeCommand(machine, extractCommand);

            // 检查解压是否成功
            String checkCommand = String.format("if [ -d \"%s/bin\" ] && [ -f \"%s/bin/logstash\" ]; then echo \"success\"; else echo \"failed\"; fi",
                    processDir, processDir);
            String checkResult = sshClient.executeCommand(machine, checkCommand);

            boolean success = "success".equals(checkResult.trim());
            if (success) {
                logger.info("成功解压Logstash安装包");

                // 可选：删除安装包节省空间
                String removePackageCommand = String.format("rm -f %s", remotePackagePath);
                sshClient.executeCommand(machine, removePackageCommand);
                logger.info("已删除安装包: {}", remotePackagePath);
            } else {
                logger.error("解压Logstash安装包失败");
            }

            future.complete(success);
        } catch (Exception e) {
            logger.error("解压Logstash安装包时发生错误: {}", e.getMessage(), e);
            future.completeExceptionally(new SshOperationException("解压Logstash安装包失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public String getDescription() {
        return "解压Logstash安装包";
    }
}