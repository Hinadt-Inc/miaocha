package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import java.util.concurrent.CompletableFuture;
import org.springframework.util.StringUtils;

/** 刷新Logstash配置文件命令 - 重构支持多实例，基于logstashMachineId 支持刷新主配置文件、JVM配置和系统配置 */
public class RefreshConfigCommand extends AbstractLogstashCommand {

    private final String configContent; // 可选的主配置内容，如果为null则从数据库获取
    private final String jvmOptions; // 可选的JVM选项内容，如果为null则从数据库获取
    private final String logstashYml; // 可选的系统配置内容，如果为null则从数据库获取

    /** 创建刷新配置文件命令 - 从数据库获取所有配置并写入文件 */
    public RefreshConfigCommand(
            SshClient sshClient,
            String deployBaseDir,
            Long logstashMachineId,
            LogstashMachineMapper logstashMachineMapper,
            LogstashDeployPathManager deployPathManager) {
        this(
                sshClient,
                deployBaseDir,
                logstashMachineId,
                logstashMachineMapper,
                null, // 从数据库获取主配置
                null, // 从数据库获取JVM配置
                null, // 从数据库获取系统配置
                deployPathManager);
    }

    /**
     * 创建可同时刷新多种配置文件的命令
     *
     * @param sshClient SSH客户端
     * @param deployBaseDir 部署基础目录
     * @param logstashMachineId LogstashMachine实例ID
     * @param logstashMachineMapper 机器Mapper，用于获取机器特定配置
     * @param configContent 主配置内容（可为null，从数据库获取）
     * @param jvmOptions JVM配置内容（可为null，从数据库获取）
     * @param logstashYml 系统配置内容（可为null，从数据库获取）
     * @param deployPathManager 部署路径管理器
     */
    public RefreshConfigCommand(
            SshClient sshClient,
            String deployBaseDir,
            Long logstashMachineId,
            LogstashMachineMapper logstashMachineMapper,
            String configContent,
            String jvmOptions,
            String logstashYml,
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
                        String processDir = getProcessDirectory();
                        String configDir = processDir + "/config";

                        // 确保配置目录存在
                        String createDirCommand = String.format("mkdir -p %s", configDir);
                        sshClient.executeCommand(machineInfo, createDirCommand);

                        // 1. 刷新主配置文件
                        success = refreshMainConfig(machineInfo, configDir) && success;

                        // 2. 刷新JVM配置（如果需要）
                        success = refreshJvmOptions(machineInfo, configDir) && success;

                        // 3. 刷新系统配置（如果需要）
                        success = refreshLogstashYml(machineInfo, configDir) && success;

                        return success;
                    } catch (Exception e) {
                        logger.error(
                                "刷新Logstash配置文件时发生错误，实例ID: {}, 错误: {}",
                                logstashMachineId,
                                e.getMessage(),
                                e);
                        throw new SshOperationException("刷新Logstash配置文件失败: " + e.getMessage(), e);
                    }
                });
    }

    /** 刷新主配置文件 */
    private boolean refreshMainConfig(MachineInfo machineInfo, String configDir) {
        try {
            String configPath = configDir + "/logstash-" + logstashMachineId + ".conf";

            // 获取配置内容
            String actualConfigContent = configContent;
            if (!StringUtils.hasText(actualConfigContent)) {
                // 从数据库获取配置内容
                LogstashMachine logstashMachine = getLogstashMachine();
                if (logstashMachine != null
                        && StringUtils.hasText(logstashMachine.getConfigContent())) {
                    actualConfigContent = logstashMachine.getConfigContent();
                } else {
                    logger.warn("无法获取主配置内容，实例ID: {}", logstashMachineId);
                    return false;
                }
            }

            // 创建临时文件
            String tempFile =
                    String.format(
                            "/tmp/logstash-config-%d-%d.conf",
                            logstashMachineId, System.currentTimeMillis());

            // 将配置写入临时文件，使用heredoc避免特殊字符问题
            String createConfigCommand =
                    String.format("cat > %s << 'EOF'\n%s\nEOF", tempFile, actualConfigContent);
            sshClient.executeCommand(machineInfo, createConfigCommand);

            // 移动到最终位置
            String moveCommand = String.format("mv %s %s", tempFile, configPath);
            sshClient.executeCommand(machineInfo, moveCommand);

            // 检查配置文件是否刷新成功
            String verifyCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"success\"; else echo \"failed\"; fi",
                            configPath);
            String verifyResult = sshClient.executeCommand(machineInfo, verifyCommand);

            boolean success = "success".equals(verifyResult.trim());
            if (success) {
                logger.info("成功刷新主配置文件，实例ID: {}, 路径: {}", logstashMachineId, configPath);
            } else {
                logger.error("刷新主配置文件失败，实例ID: {}, 路径: {}", logstashMachineId, configPath);
            }

            return success;
        } catch (Exception e) {
            logger.error("刷新主配置文件时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            return false;
        }
    }

    /** 刷新JVM配置文件 */
    private boolean refreshJvmOptions(MachineInfo machineInfo, String configDir) {
        try {
            // 获取JVM配置内容
            String actualJvmOptions = jvmOptions;
            if (!StringUtils.hasText(actualJvmOptions)) {
                // 从数据库获取JVM配置
                LogstashMachine logstashMachine = getLogstashMachine();
                if (logstashMachine != null
                        && StringUtils.hasText(logstashMachine.getJvmOptions())) {
                    actualJvmOptions = logstashMachine.getJvmOptions();
                } else {
                    logger.info("未提供JVM配置内容，跳过JVM配置刷新，实例ID: {}", logstashMachineId);
                    return true; // 跳过但不算失败
                }
            }

            String jvmFile = configDir + "/jvm.options";

            // 创建临时文件
            String tempFile =
                    String.format(
                            "/tmp/logstash-jvm-%d-%d.options",
                            logstashMachineId, System.currentTimeMillis());

            // 将JVM配置写入临时文件
            String createConfigCommand =
                    String.format("cat > %s << 'EOF'\n%s\nEOF", tempFile, actualJvmOptions);
            sshClient.executeCommand(machineInfo, createConfigCommand);

            // 移动到最终位置
            String moveCommand = String.format("mv %s %s", tempFile, jvmFile);
            sshClient.executeCommand(machineInfo, moveCommand);

            // 检查配置文件是否刷新成功
            String verifyCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"success\"; else echo \"failed\"; fi",
                            jvmFile);
            String verifyResult = sshClient.executeCommand(machineInfo, verifyCommand);

            boolean success = "success".equals(verifyResult.trim());
            if (success) {
                logger.info("成功刷新JVM配置文件，实例ID: {}, 路径: {}", logstashMachineId, jvmFile);
            } else {
                logger.error("刷新JVM配置文件失败，实例ID: {}, 路径: {}", logstashMachineId, jvmFile);
            }

            return success;
        } catch (Exception e) {
            logger.error("刷新JVM配置文件时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            return false;
        }
    }

    /** 刷新系统配置文件 */
    private boolean refreshLogstashYml(MachineInfo machineInfo, String configDir) {
        try {
            // 获取系统配置内容
            String actualLogstashYml = logstashYml;
            if (!StringUtils.hasText(actualLogstashYml)) {
                // 从数据库获取系统配置
                LogstashMachine logstashMachine = getLogstashMachine();
                if (logstashMachine != null
                        && StringUtils.hasText(logstashMachine.getLogstashYml())) {
                    actualLogstashYml = logstashMachine.getLogstashYml();
                } else {
                    logger.info("未提供系统配置内容，跳过系统配置刷新，实例ID: {}", logstashMachineId);
                    return true; // 跳过但不算失败
                }
            }

            String ymlFile = configDir + "/logstash.yml";

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

            // 检查配置文件是否刷新成功
            String verifyCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"success\"; else echo \"failed\"; fi",
                            ymlFile);
            String verifyResult = sshClient.executeCommand(machineInfo, verifyCommand);

            boolean success = "success".equals(verifyResult.trim());
            if (success) {
                logger.info("成功刷新系统配置文件，实例ID: {}, 路径: {}", logstashMachineId, ymlFile);
            } else {
                logger.error("刷新系统配置文件失败，实例ID: {}, 路径: {}", logstashMachineId, ymlFile);
            }

            return success;
        } catch (Exception e) {
            logger.error("刷新系统配置文件时发生错误，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getDescription() {
        return "刷新Logstash配置文件";
    }
}
