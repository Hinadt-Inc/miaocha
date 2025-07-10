package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;

/** 创建Logstash配置文件命令 - 重构支持多实例，基于logstashMachineId */
public class CreateConfigCommand extends AbstractLogstashCommand {

    private final String configContent;

    public CreateConfigCommand(
            SshClient sshClient,
            String deployBaseDir,
            Long logstashMachineId,
            String configContent,
            LogstashMachineMapper logstashMachineMapper,
            LogstashDeployPathManager deployPathManager) {
        super(
                sshClient,
                deployBaseDir,
                logstashMachineId,
                logstashMachineMapper,
                deployPathManager);
        this.configContent = configContent;
    }

    @Override
    protected boolean checkAlreadyExecuted(MachineInfo machineInfo) {
        try {
            String processDir = getProcessDirectory();
            String configDir = processDir + "/config";
            String configPath = configDir + "/logstash-" + logstashMachineId + ".conf";

            // 检查配置文件是否已存在
            String checkCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                            configPath);
            String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

            boolean exists = "exists".equals(checkResult.trim());
            if (exists) {
                logger.info("配置文件已存在，跳过创建，实例ID: {}, 路径: {}", logstashMachineId, configPath);
            }
            return exists;
        } catch (Exception e) {
            logger.warn("检查配置文件是否存在时出错，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage());
            return false;
        }
    }

    @Override
    protected boolean doExecute(MachineInfo machineInfo) {
        try {
            String processDir = getProcessDirectory();
            String configDir = processDir + "/config";
            String configPath = configDir + "/logstash-" + logstashMachineId + ".conf";

            // 确保配置目录存在
            String createDirCommand = String.format("mkdir -p %s", configDir);
            sshClient.executeCommand(machineInfo, createDirCommand);

            // 创建临时文件
            String tempFile =
                    String.format(
                            "/tmp/logstash-config-%d-%d.conf",
                            logstashMachineId, System.currentTimeMillis());

            // 将配置写入临时文件，使用heredoc避免特殊字符问题
            String createConfigCommand =
                    String.format("cat > %s << 'EOF'\n%s\nEOF", tempFile, configContent);
            sshClient.executeCommand(machineInfo, createConfigCommand);

            // 移动到最终位置
            String moveCommand = String.format("mv %s %s", tempFile, configPath);
            sshClient.executeCommand(machineInfo, moveCommand);

            // 检查配置文件是否创建成功
            String checkCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"success\"; else echo \"failed\"; fi",
                            configPath);
            String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

            boolean success = "success".equals(checkResult.trim());
            if (success) {
                logger.info("成功创建Logstash配置文件，实例ID: {}, 路径: {}", logstashMachineId, configPath);
            } else {
                logger.error("创建Logstash配置文件失败，实例ID: {}, 路径: {}", logstashMachineId, configPath);
            }

            return success;
        } catch (Exception e) {
            logger.error(
                    "创建Logstash配置文件时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            throw new SshOperationException("创建配置文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getDescription() {
        return "创建Logstash配置文件";
    }
}
