package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.infrastructure.mapper.LogstashMachineMapper;
import java.io.File;

/** 解压Logstash安装包命令 - 重构支持多实例，基于logstashMachineId */
public class ExtractPackageCommand extends AbstractLogstashCommand {

    private final String localPackagePath;

    public ExtractPackageCommand(
            SshClient sshClient,
            String deployBaseDir,
            String localPackagePath,
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

            // 检查是否已经解压（检查bin目录和logstash可执行文件）
            String checkCommand =
                    String.format(
                            "if [ -d \"%s/bin\" ] && [ -f \"%s/bin/logstash\" ]; then"
                                    + " echo \"extracted\"; else echo \"not_extracted\";"
                                    + " fi",
                            processDir, processDir);
            String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

            boolean extracted = "extracted".equals(checkResult.trim());
            if (extracted) {
                logger.info("安装包已解压，跳过解压步骤，实例ID: {}", logstashMachineId);
            }
            return extracted;
        } catch (Exception e) {
            logger.warn("检查安装包是否已解压时出错，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage());
            return false;
        }
    }

    @Override
    protected boolean doExecute(MachineInfo machineInfo) {
        try {
            String processDir = getProcessDirectory();
            String fileName = new File(localPackagePath).getName();
            String remotePackagePath = processDir + "/" + fileName;

            // 解压安装包，使用--strip-components=1去除顶层目录
            logger.info("开始解压Logstash安装包，实例ID: {}, 包路径: {}", logstashMachineId, remotePackagePath);
            String extractCommand =
                    String.format(
                            "cd %s && tar -xzf %s --strip-components=1", processDir, fileName);
            sshClient.executeCommand(machineInfo, extractCommand);

            // 检查解压是否成功
            String checkCommand =
                    String.format(
                            "if [ -d \"%s/bin\" ] && [ -f \"%s/bin/logstash\" ]; then echo"
                                    + " \"success\"; else echo \"failed\"; fi",
                            processDir, processDir);
            String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

            boolean success = "success".equals(checkResult.trim());
            if (success) {
                logger.info("成功解压Logstash安装包，实例ID: {}", logstashMachineId);

                // 可选：删除安装包节省空间
                String removePackageCommand = String.format("rm -f %s", remotePackagePath);
                sshClient.executeCommand(machineInfo, removePackageCommand);
                logger.info("已删除安装包，实例ID: {}, 路径: {}", logstashMachineId, remotePackagePath);
            } else {
                logger.error("解压Logstash安装包失败，实例ID: {}", logstashMachineId);
            }

            return success;
        } catch (Exception e) {
            throw new SshOperationException("解压安装包失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getDescription() {
        return "解压Logstash安装包";
    }
}
