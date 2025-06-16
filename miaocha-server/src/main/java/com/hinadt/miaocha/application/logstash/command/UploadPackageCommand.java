package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/** 上传Logstash安装包命令 - 重构支持多实例，基于logstashMachineId */
public class UploadPackageCommand extends AbstractLogstashCommand {

    private final String localPackagePath;

    public UploadPackageCommand(
            SshClient sshClient,
            String localPackagePath,
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
        this.localPackagePath = localPackagePath;
    }

    @Override
    protected CompletableFuture<Boolean> checkAlreadyExecuted(MachineInfo machineInfo) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        String processDir = getProcessDirectory(machineInfo);
                        String fileName = new File(localPackagePath).getName();
                        String remotePackagePath = processDir + "/" + fileName;

                        // 检查远程文件是否已存在
                        String checkCommand =
                                String.format(
                                        "if [ -f \"%s\" ]; then echo \"exists\"; else echo"
                                                + " \"not_exists\"; fi",
                                        remotePackagePath);
                        String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

                        boolean exists = "exists".equals(checkResult.trim());
                        if (exists) {
                            logger.info(
                                    "安装包已存在，跳过上传，实例ID: {}, 路径: {}",
                                    logstashMachineId,
                                    remotePackagePath);
                        }
                        return exists;
                    } catch (Exception e) {
                        logger.warn(
                                "检查安装包是否存在时出错，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage());
                        return false;
                    }
                });
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(MachineInfo machineInfo) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // 检查本地文件是否存在
            File localFile = new File(localPackagePath);
            if (!localFile.exists()) {
                logger.error("本地安装包文件不存在: {}", localPackagePath);
                future.complete(false);
                return future;
            }

            String processDir = getProcessDirectory(machineInfo);
            String fileName = localFile.getName();
            String remotePackagePath = processDir + "/" + fileName;

            logger.info(
                    "开始上传Logstash安装包，实例ID: {}, 本地路径: {}, 远程路径: {}",
                    logstashMachineId,
                    localPackagePath,
                    remotePackagePath);

            // 上传文件
            sshClient.uploadFile(machineInfo, localPackagePath, remotePackagePath);

            // 验证上传是否成功
            String checkCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"success\"; else echo \"failed\"; fi",
                            remotePackagePath);
            String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

            boolean success = "success".equals(checkResult.trim());
            if (success) {
                logger.info(
                        "成功上传Logstash安装包，实例ID: {}, 远程路径: {}", logstashMachineId, remotePackagePath);
            } else {
                logger.error(
                        "上传Logstash安装包失败，实例ID: {}, 远程路径: {}", logstashMachineId, remotePackagePath);
            }

            future.complete(success);
        } catch (Exception e) {
            logger.error(
                    "上传Logstash安装包时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            future.completeExceptionally(
                    new SshOperationException("上传安装包失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public String getDescription() {
        return "上传Logstash安装包";
    }
}
