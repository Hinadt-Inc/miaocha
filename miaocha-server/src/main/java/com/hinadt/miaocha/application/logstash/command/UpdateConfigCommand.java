package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import java.util.concurrent.CompletableFuture;
import org.springframework.util.StringUtils;

/** 更新Logstash配置文件命令 - 重构支持多实例，基于logstashMachineId 支持更新主配置文件、JVM配置和系统配置 */
public class UpdateConfigCommand extends AbstractLogstashCommand {

    private final String configContent; // 主配置文件内容
    private final String jvmOptions; // JVM配置文件内容
    private final String logstashYml; // 系统配置文件内容

    /** 创建仅更新主配置文件的命令（向后兼容构造函数） */
    public UpdateConfigCommand(
            SshClient sshClient,
            String deployBaseDir,
            Long logstashMachineId,
            String configContent,
            LogstashMachineMapper logstashMachineMapper,
            LogstashDeployPathManager deployPathManager) {
        this(
                sshClient,
                deployBaseDir,
                logstashMachineId,
                configContent,
                null,
                null,
                logstashMachineMapper,
                deployPathManager);
    }

    /**
     * 创建可同时更新多种配置文件的命令
     *
     * @param sshClient SSH客户端
     * @param deployBaseDir 部署基础目录
     * @param logstashMachineId LogstashMachine实例ID
     * @param configContent 主配置文件内容（可为null）
     * @param jvmOptions JVM配置文件内容（可为null）
     * @param logstashYml 系统配置文件内容（可为null）
     * @param logstashMachineMapper Logstash机器映射器
     * @param deployPathManager 部署路径管理器
     */
    public UpdateConfigCommand(
            SshClient sshClient,
            String deployBaseDir,
            Long logstashMachineId,
            String configContent,
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
        this.configContent = configContent;
        this.jvmOptions = jvmOptions;
        this.logstashYml = logstashYml;
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(MachineInfo machineInfo) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        boolean success = true;
                        String processDir = getProcessDirectory(machineInfo);
                        String configDir = processDir + "/config";

                        // 确保配置目录存在
                        String createDirCommand = String.format("mkdir -p %s", configDir);
                        sshClient.executeCommand(machineInfo, createDirCommand);

                        // 1. 更新主配置文件
                        if (StringUtils.hasText(configContent)) {
                            success = updateMainConfig(machineInfo, configDir) && success;
                        }

                        // 2. 更新JVM配置（如果需要）
                        if (StringUtils.hasText(jvmOptions)) {
                            success = updateJvmOptions(machineInfo, configDir) && success;
                        }

                        // 3. 更新系统配置（如果需要）
                        if (StringUtils.hasText(logstashYml)) {
                            success = updateLogstashYml(machineInfo, configDir) && success;
                        }

                        // 4. 更新数据库中的配置内容
                        if (success) {
                            updateDatabaseConfig();
                        }

                        return success;
                    } catch (Exception e) {
                        logger.error(
                                "更新Logstash配置文件时发生错误，实例ID: {}, 错误: {}",
                                logstashMachineId,
                                e.getMessage(),
                                e);
                        return false;
                    }
                });
    }

    /** 更新主配置文件 */
    private boolean updateMainConfig(MachineInfo machineInfo, String configDir) {
        try {
            String configPath = configDir + "/logstash-" + logstashMachineId + ".conf";
            String tempFile =
                    String.format(
                            "/tmp/logstash-config-%d-%d.conf",
                            logstashMachineId, System.currentTimeMillis());

            // 将配置写入临时文件
            String createConfigCommand =
                    String.format("cat > %s << 'EOF'\n%s\nEOF", tempFile, configContent);
            sshClient.executeCommand(machineInfo, createConfigCommand);

            // 移动到最终位置
            String moveCommand = String.format("mv %s %s", tempFile, configPath);
            sshClient.executeCommand(machineInfo, moveCommand);

            // 验证文件是否更新成功
            String verifyCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"success\"; else echo \"failed\"; fi",
                            configPath);
            String verifyResult = sshClient.executeCommand(machineInfo, verifyCommand);

            boolean success = "success".equals(verifyResult.trim());
            if (success) {
                logger.info("成功更新主配置文件，实例ID: {}, 路径: {}", logstashMachineId, configPath);
            } else {
                logger.error("更新主配置文件失败，实例ID: {}, 路径: {}", logstashMachineId, configPath);
            }

            return success;
        } catch (Exception e) {
            logger.error("更新主配置文件时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            return false;
        }
    }

    /** 更新JVM配置文件 */
    private boolean updateJvmOptions(MachineInfo machineInfo, String configDir) {
        try {
            String jvmOptionsPath = configDir + "/jvm.options";
            String tempFile =
                    String.format(
                            "/tmp/jvm-options-%d-%d.txt",
                            logstashMachineId, System.currentTimeMillis());

            // 将JVM配置写入临时文件
            String createJvmCommand =
                    String.format("cat > %s << 'EOF'\n%s\nEOF", tempFile, jvmOptions);
            sshClient.executeCommand(machineInfo, createJvmCommand);

            // 移动到最终位置
            String moveCommand = String.format("mv %s %s", tempFile, jvmOptionsPath);
            sshClient.executeCommand(machineInfo, moveCommand);

            // 验证文件是否更新成功
            String verifyCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"success\"; else echo \"failed\"; fi",
                            jvmOptionsPath);
            String verifyResult = sshClient.executeCommand(machineInfo, verifyCommand);

            boolean success = "success".equals(verifyResult.trim());
            if (success) {
                logger.info("成功更新JVM配置文件，实例ID: {}, 路径: {}", logstashMachineId, jvmOptionsPath);
            } else {
                logger.error("更新JVM配置文件失败，实例ID: {}, 路径: {}", logstashMachineId, jvmOptionsPath);
            }

            return success;
        } catch (Exception e) {
            logger.error("更新JVM配置文件时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            return false;
        }
    }

    /** 更新系统配置文件 */
    private boolean updateLogstashYml(MachineInfo machineInfo, String configDir) {
        try {
            String ymlPath = configDir + "/logstash.yml";
            String tempFile =
                    String.format(
                            "/tmp/logstash-yml-%d-%d.yml",
                            logstashMachineId, System.currentTimeMillis());

            // 将系统配置写入临时文件
            String createYmlCommand =
                    String.format("cat > %s << 'EOF'\n%s\nEOF", tempFile, logstashYml);
            sshClient.executeCommand(machineInfo, createYmlCommand);

            // 移动到最终位置
            String moveCommand = String.format("mv %s %s", tempFile, ymlPath);
            sshClient.executeCommand(machineInfo, moveCommand);

            // 验证文件是否更新成功
            String verifyCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"success\"; else echo \"failed\"; fi",
                            ymlPath);
            String verifyResult = sshClient.executeCommand(machineInfo, verifyCommand);

            boolean success = "success".equals(verifyResult.trim());
            if (success) {
                logger.info("成功更新系统配置文件，实例ID: {}, 路径: {}", logstashMachineId, ymlPath);
            } else {
                logger.error("更新系统配置文件失败，实例ID: {}, 路径: {}", logstashMachineId, ymlPath);
            }

            return success;
        } catch (Exception e) {
            logger.error("更新系统配置文件时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            return false;
        }
    }

    /** 更新数据库中的配置内容 */
    private void updateDatabaseConfig() {
        try {
            if (StringUtils.hasText(configContent)) {
                logstashMachineMapper.updateConfigContentById(logstashMachineId, configContent);
            }
            if (StringUtils.hasText(jvmOptions)) {
                logstashMachineMapper.updateJvmOptionsById(logstashMachineId, jvmOptions);
            }
            if (StringUtils.hasText(logstashYml)) {
                logstashMachineMapper.updateLogstashYmlById(logstashMachineId, logstashYml);
            }
            logger.info("成功更新数据库中的配置内容，实例ID: {}", logstashMachineId);
        } catch (Exception e) {
            logger.error("更新数据库配置时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
        }
    }

    @Override
    public String getDescription() {
        return "更新Logstash配置文件";
    }
}
