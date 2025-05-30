package com.hina.log.application.logstash.command;

import com.hina.log.common.exception.BusinessException;
import com.hina.log.common.exception.ErrorCode;
import com.hina.log.common.exception.SshOperationException;
import com.hina.log.common.ssh.SshClient;
import com.hina.log.domain.entity.LogstashMachine;
import com.hina.log.domain.entity.LogstashProcess;
import com.hina.log.domain.entity.MachineInfo;
import com.hina.log.domain.mapper.LogstashMachineMapper;
import com.hina.log.domain.mapper.LogstashProcessMapper;
import java.util.concurrent.CompletableFuture;
import org.springframework.util.StringUtils;

/** 刷新Logstash配置文件命令 从数据库获取最新配置并刷新到目标机器 支持刷新主配置文件、JVM配置和系统配置 */
public class RefreshConfigCommand extends AbstractLogstashCommand {

    private final LogstashProcessMapper logstashProcessMapper;
    private final String configContent; // 可选的主配置内容，如果为null则从数据库获取
    private final String jvmOptions; // 可选的JVM选项内容，如果为null则从数据库获取
    private final String logstashYml; // 可选的系统配置内容，如果为null则从数据库获取

    /** 创建刷新单个配置文件的命令（向后兼容构造函数） */
    public RefreshConfigCommand(
            SshClient sshClient,
            String deployDir,
            Long processId,
            LogstashProcessMapper logstashProcessMapper,
            String configContent) {
        this(
                sshClient,
                deployDir,
                processId,
                logstashProcessMapper,
                null,
                configContent,
                null,
                null);
    }

    /**
     * 创建可同时刷新多种配置文件的命令
     *
     * @param sshClient SSH客户端
     * @param deployDir 部署基础目录
     * @param processId 进程ID
     * @param logstashProcessMapper 进程Mapper，用于获取主配置
     * @param logstashMachineMapper 机器Mapper，用于获取机器特定配置
     * @param configContent 主配置内容（可为null，从数据库获取）
     * @param jvmOptions JVM配置内容（可为null，从数据库获取）
     * @param logstashYml 系统配置内容（可为null，从数据库获取）
     */
    public RefreshConfigCommand(
            SshClient sshClient,
            String deployDir,
            Long processId,
            LogstashProcessMapper logstashProcessMapper,
            LogstashMachineMapper logstashMachineMapper,
            String configContent,
            String jvmOptions,
            String logstashYml) {
        super(sshClient, deployDir, processId, logstashMachineMapper);
        this.logstashProcessMapper = logstashProcessMapper;
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

                        // 1. 刷新主配置文件
                        success = refreshMainConfig(machineInfo, configDir) && success;

                        // 2. 刷新JVM配置（如果需要）
                        success = refreshJvmOptions(machineInfo, configDir) && success;

                        // 3. 刷新系统配置（如果需要）
                        success = refreshLogstashYml(machineInfo, configDir) && success;

                        return success;
                    } catch (Exception e) {
                        logger.error("刷新Logstash配置文件时发生错误: {}", e.getMessage(), e);
                        throw new SshOperationException("刷新Logstash配置文件失败: " + e.getMessage(), e);
                    }
                });
    }

    /** 刷新主配置文件 */
    private boolean refreshMainConfig(MachineInfo machineInfo, String configDir) throws Exception {
        // 获取配置内容，优先使用传入内容，否则从数据库获取
        String actualConfigContent = configContent;
        if (!StringUtils.hasText(actualConfigContent)) {
            LogstashProcess process = logstashProcessMapper.selectById(processId);
            if (process == null) {
                throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
            }

            actualConfigContent = process.getConfigContent();
            if (!StringUtils.hasText(actualConfigContent)) {
                logger.warn("数据库中的主配置为空，跳过主配置文件刷新");
                return true; // 没有配置可以刷新，视为成功
            }
        }

        String configPath = configDir + "/logstash-" + processId + ".conf";

        // 首先检查旧配置文件是否存在
        String checkCommand =
                String.format(
                        "if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                        configPath);
        String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

        if ("exists".equals(checkResult.trim())) {
            // 备份旧配置
            String backupFile = String.format("%s.bak.%d", configPath, System.currentTimeMillis());
            String backupCommand = String.format("cp %s %s", configPath, backupFile);
            sshClient.executeCommand(machineInfo, backupCommand);
            logger.info("已备份旧主配置文件: {}", backupFile);
        }

        // 创建临时文件
        String tempFile =
                String.format(
                        "/tmp/logstash-config-%d-%d.conf", processId, System.currentTimeMillis());

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
            logger.info("成功刷新Logstash主配置文件: {}", configPath);
        } else {
            logger.error("刷新Logstash主配置文件失败: {}", configPath);
        }

        return success;
    }

    /** 刷新JVM配置文件 */
    private boolean refreshJvmOptions(MachineInfo machineInfo, String configDir) throws Exception {
        // 获取JVM选项，优先使用传入内容，否则从数据库获取
        String actualJvmOptions = jvmOptions;
        if (!StringUtils.hasText(actualJvmOptions) && logstashMachineMapper != null) {
            LogstashMachine logstashMachine =
                    logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                            processId, machineInfo.getId());
            if (logstashMachine != null) {
                actualJvmOptions = logstashMachine.getJvmOptions();
            }
        }

        if (!StringUtils.hasText(actualJvmOptions)) {
            logger.debug("没有JVM选项需要刷新");
            return true; // 没有配置可以刷新，视为成功
        }

        String jvmFile = configDir + "/jvm.options";

        // 首先检查旧配置文件是否存在
        String checkCommand =
                String.format(
                        "if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                        jvmFile);
        String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

        if ("exists".equals(checkResult.trim())) {
            // 备份旧配置
            String backupFile = String.format("%s.bak.%d", jvmFile, System.currentTimeMillis());
            String backupCommand = String.format("cp %s %s", jvmFile, backupFile);
            sshClient.executeCommand(machineInfo, backupCommand);
            logger.info("已备份旧JVM配置文件: {}", backupFile);
        }

        // 创建临时文件
        String tempFile =
                String.format(
                        "/tmp/logstash-jvm-%d-%d.options", processId, System.currentTimeMillis());

        // 将配置写入临时文件，使用heredoc避免特殊字符问题
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
            logger.info("成功刷新Logstash JVM配置文件: {}", jvmFile);
        } else {
            logger.error("刷新Logstash JVM配置文件失败: {}", jvmFile);
        }

        return success;
    }

    /** 刷新系统配置文件 */
    private boolean refreshLogstashYml(MachineInfo machineInfo, String configDir) throws Exception {
        // 获取系统配置，优先使用传入内容，否则从数据库获取
        String actualLogstashYml = logstashYml;
        if (!StringUtils.hasText(actualLogstashYml) && logstashMachineMapper != null) {
            LogstashMachine logstashMachine =
                    logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                            processId, machineInfo.getId());
            if (logstashMachine != null) {
                actualLogstashYml = logstashMachine.getLogstashYml();
            }
        }

        if (!StringUtils.hasText(actualLogstashYml)) {
            logger.debug("没有系统配置需要刷新");
            return true; // 没有配置可以刷新，视为成功
        }

        String ymlFile = configDir + "/logstash.yml";

        // 首先检查旧配置文件是否存在
        String checkCommand =
                String.format(
                        "if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                        ymlFile);
        String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

        if ("exists".equals(checkResult.trim())) {
            // 备份旧配置
            String backupFile = String.format("%s.bak.%d", ymlFile, System.currentTimeMillis());
            String backupCommand = String.format("cp %s %s", ymlFile, backupFile);
            sshClient.executeCommand(machineInfo, backupCommand);
            logger.info("已备份旧系统配置文件: {}", backupFile);
        }

        // 创建临时文件
        String tempFile =
                String.format("/tmp/logstash-yml-%d-%d.yml", processId, System.currentTimeMillis());

        // 将配置写入临时文件，使用heredoc避免特殊字符问题
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
            logger.info("成功刷新Logstash系统配置文件: {}", ymlFile);
        } else {
            logger.error("刷新Logstash系统配置文件失败: {}", ymlFile);
        }

        return success;
    }

    @Override
    public String getDescription() {
        return "刷新Logstash配置文件";
    }
}
