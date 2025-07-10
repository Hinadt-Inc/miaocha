package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import org.springframework.util.StringUtils;

/** 修改系统配置命令 - 重构支持多实例，基于logstashMachineId 支持修改JVM配置和系统配置 */
public class ModifySystemConfigCommand extends AbstractLogstashCommand {

    private final String jvmOptions; // JVM配置文件内容
    private final String logstashYml; // 系统配置文件内容

    /**
     * 创建可同时修改JVM配置和系统配置的命令
     *
     * @param sshClient SSH客户端
     * @param deployBaseDir 部署基础目录
     * @param logstashMachineId LogstashMachine实例ID
     * @param jvmOptions JVM配置文件内容（可为null）
     * @param logstashYml 系统配置文件内容（可为null）
     * @param logstashMachineMapper Logstash机器映射器
     * @param deployPathManager 部署路径管理器
     */
    public ModifySystemConfigCommand(
            SshClient sshClient,
            String deployBaseDir,
            Long logstashMachineId,
            String jvmOptions,
            String logstashYml,
            LogstashMachineMapper logstashMachineMapper,
            LogstashDeployPathManager deployPathManager) {
        super(
                sshClient,
                deployBaseDir,
                logstashMachineId,
                logstashMachineMapper,
                deployPathManager);
        this.jvmOptions = jvmOptions;
        this.logstashYml = logstashYml;
    }

    @Override
    protected boolean checkAlreadyExecuted(MachineInfo machineInfo) {
        return false;
    }

    @Override
    protected boolean doExecute(MachineInfo machineInfo) {
        try {
            boolean success = true;
            String processDir = getProcessDirectory();
            String configDir = processDir + "/config";

            // 确保配置目录存在
            String createDirCommand = String.format("mkdir -p %s", configDir);
            sshClient.executeCommand(machineInfo, createDirCommand);

            // 1. 修改JVM配置（如果提供）
            if (StringUtils.hasText(jvmOptions)) {
                success = modifyJvmOptions(machineInfo, configDir) && success;
            }

            // 2. 修改系统配置（如果提供，否则使用默认配置）
            success = modifyLogstashYml(machineInfo, configDir) && success;

            return success;
        } catch (Exception e) {
            logger.error(
                    "修改Logstash系统配置时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            throw new SshOperationException("修改Logstash系统配置失败: " + e.getMessage(), e);
        }
    }

    /** 修改JVM配置文件 */
    private boolean modifyJvmOptions(MachineInfo machineInfo, String configDir) {
        try {
            String jvmFile = configDir + "/jvm.options";

            // 创建临时文件
            String tempFile =
                    String.format(
                            "/tmp/logstash-jvm-%d-%d.options",
                            logstashMachineId, System.currentTimeMillis());

            // 将JVM配置写入临时文件
            String createConfigCommand =
                    String.format("cat > %s << 'EOF'\n%s\nEOF", tempFile, jvmOptions);
            sshClient.executeCommand(machineInfo, createConfigCommand);

            // 移动到最终位置
            String moveCommand = String.format("mv %s %s", tempFile, jvmFile);
            sshClient.executeCommand(machineInfo, moveCommand);

            // 检查配置文件是否修改成功
            String verifyCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"success\"; else echo \"failed\"; fi",
                            jvmFile);
            String verifyResult = sshClient.executeCommand(machineInfo, verifyCommand);

            boolean success = "success".equals(verifyResult.trim());
            if (success) {
                logger.info("成功修改JVM配置文件，实例ID: {}, 路径: {}", logstashMachineId, jvmFile);
            } else {
                logger.error("修改JVM配置文件失败，实例ID: {}, 路径: {}", logstashMachineId, jvmFile);
            }

            return success;
        } catch (Exception e) {
            logger.error("修改JVM配置文件时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            return false;
        }
    }

    /** 修改系统配置文件 */
    private boolean modifyLogstashYml(MachineInfo machineInfo, String configDir) {
        try {
            String ymlFile = configDir + "/logstash.yml";

            // 使用提供的配置或默认配置
            String actualLogstashYml = logstashYml;
            if (!StringUtils.hasText(actualLogstashYml)) {
                // 使用默认的基本配置
                actualLogstashYml =
                        "# Logstash系统配置\n"
                                + "# 允许以超级用户身份运行\n"
                                + "allow_superuser: true\n"
                                + "\n"
                                + "# 数据路径\n"
                                + "path.data: data\n"
                                + "\n"
                                + "# 日志配置\n"
                                + "log.level: info\n"
                                + "path.logs: logs\n";
            }

            // 创建临时文件
            String tempFile =
                    String.format(
                            "/tmp/logstash-yml-%d-%d.yml",
                            logstashMachineId, System.currentTimeMillis());

            // 将系统配置写入临时文件
            String createConfigCommand =
                    String.format("cat > %s << 'EOF'\n%s\nEOF", tempFile, actualLogstashYml);
            sshClient.executeCommand(machineInfo, createConfigCommand);

            // 移动到最终位置
            String moveCommand = String.format("mv %s %s", tempFile, ymlFile);
            sshClient.executeCommand(machineInfo, moveCommand);

            // 检查配置文件是否修改成功
            String verifyCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"success\"; else echo \"failed\"; fi",
                            ymlFile);
            String verifyResult = sshClient.executeCommand(machineInfo, verifyCommand);

            boolean success = "success".equals(verifyResult.trim());
            if (success) {
                logger.info("成功修改系统配置文件，实例ID: {}, 路径: {}", logstashMachineId, ymlFile);
            } else {
                logger.error("修改系统配置文件失败，实例ID: {}, 路径: {}", logstashMachineId, ymlFile);
            }

            return success;
        } catch (Exception e) {
            logger.error("修改系统配置文件时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getDescription() {
        return "修改Logstash系统配置";
    }
}
