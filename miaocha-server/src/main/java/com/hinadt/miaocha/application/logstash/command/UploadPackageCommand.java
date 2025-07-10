package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import java.io.File;

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
    protected boolean checkAlreadyExecuted(MachineInfo machineInfo) {
        try {
            String processDir = getProcessDirectory();
            String packageName = new File(localPackagePath).getName();
            String remotePackagePath = processDir + "/" + packageName;

            // 检查文件是否存在
            String checkCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                            remotePackagePath);
            String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

            boolean exists = "exists".equals(checkResult.trim());
            if (exists) {
                logger.info("安装包已存在，跳过上传，实例ID: {}, 路径: {}", logstashMachineId, remotePackagePath);
            }
            return exists;
        } catch (Exception e) {
            logger.warn("检查安装包是否存在时出错，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage());
            return false;
        }
    }

    @Override
    protected boolean doExecute(MachineInfo machineInfo) {
        try {
            String processDir = getProcessDirectory();
            String packageName = new File(localPackagePath).getName();
            String remotePackagePath = processDir + "/" + packageName;

            // 上传文件
            sshClient.uploadFile(machineInfo, localPackagePath, remotePackagePath);

            // 验证文件是否上传成功
            String verifyCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                            remotePackagePath);
            String verifyResult = sshClient.executeCommand(machineInfo, verifyCommand);

            boolean success = "exists".equals(verifyResult.trim());
            if (success) {
                logger.info("成功上传安装包，实例ID: {}, 路径: {}", logstashMachineId, remotePackagePath);
            } else {
                logger.error("上传安装包失败，实例ID: {}, 路径: {}", logstashMachineId, remotePackagePath);
            }

            return success;
        } catch (Exception e) {
            logger.error("上传安装包时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            throw new SshOperationException("上传安装包失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getDescription() {
        return "上传Logstash安装包";
    }
}
