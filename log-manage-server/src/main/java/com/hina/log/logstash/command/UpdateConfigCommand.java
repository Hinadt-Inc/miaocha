package com.hina.log.logstash.command;

import com.hina.log.entity.Machine;
import com.hina.log.exception.SshOperationException;
import com.hina.log.ssh.SshClient;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;

/**
 * 更新Logstash配置文件命令
 * 支持更新主配置文件、JVM配置和系统配置
 */
public class UpdateConfigCommand extends AbstractLogstashCommand {

    private final String configContent; // 主配置文件内容
    private final String jvmOptions;    // JVM配置文件内容
    private final String logstashYml;   // 系统配置文件内容

    /**
     * 创建仅更新主配置文件的命令（向后兼容构造函数）
     */
    public UpdateConfigCommand(SshClient sshClient, String deployDir, Long processId, String configContent) {
        this(sshClient, deployDir, processId, configContent, null, null);
    }

    /**
     * 创建可同时更新多种配置文件的命令
     *
     * @param sshClient     SSH客户端
     * @param deployDir     部署基础目录
     * @param processId     进程ID
     * @param configContent 主配置文件内容（可为null）
     * @param jvmOptions    JVM配置文件内容（可为null）
     * @param logstashYml   系统配置文件内容（可为null）
     */
    public UpdateConfigCommand(SshClient sshClient, String deployDir, Long processId,
                              String configContent, String jvmOptions, String logstashYml) {
        super(sshClient, deployDir, processId);
        this.configContent = configContent;
        this.jvmOptions = jvmOptions;
        this.logstashYml = logstashYml;
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(Machine machine) {
        return CompletableFuture.supplyAsync(() -> {
            boolean success = true;
            String processDir = getProcessDirectory();
            
            // 确保配置目录存在
            String configDir = processDir + "/config";
            try {
                // 创建配置目录
                String createDirCommand = String.format("mkdir -p %s/pipeline", configDir);
                sshClient.executeCommand(machine, createDirCommand);
            } catch (Exception e) {
                logger.error("创建配置目录时发生错误: {}", e.getMessage(), e);
                return false;
            }

            // 1. 更新主配置文件（如果提供了内容）
            if (StringUtils.hasText(configContent)) {
                try {
                    String pipelineDir = configDir + "/pipeline";
                    String configPath = pipelineDir + "/logstash.conf";
                    
                    // 首先检查旧配置文件是否存在
                    String checkCommand = String.format("if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi", configPath);
                    String checkResult = sshClient.executeCommand(machine, checkCommand);
                    
                    if ("exists".equals(checkResult.trim())) {
                        // 备份旧配置
                        String backupFile = String.format("%s.bak.%d", configPath, System.currentTimeMillis());
                        String backupCommand = String.format("cp %s %s", configPath, backupFile);
                        sshClient.executeCommand(machine, backupCommand);
                        logger.info("已备份旧配置文件: {}", backupFile);
                    }
                    
                    // 创建临时文件
                    String tempFile = String.format("/tmp/logstash-config-%d-%d.conf",
                            processId, System.currentTimeMillis());
                    
                    // 将新配置写入临时文件，使用heredoc避免特殊字符问题
                    String createConfigCommand = String.format("cat > %s << 'EOF'\n%s\nEOF",
                            tempFile, configContent);
                    sshClient.executeCommand(machine, createConfigCommand);
                    
                    // 移动到最终位置
                    String moveCommand = String.format("mv %s %s", tempFile, configPath);
                    sshClient.executeCommand(machine, moveCommand);
                    
                    // 检查配置文件是否更新成功
                    String verifyCommand = String.format("if [ -f \"%s\" ]; then echo \"success\"; else echo \"failed\"; fi", configPath);
                    String verifyResult = sshClient.executeCommand(machine, verifyCommand);
                    
                    if ("success".equals(verifyResult.trim())) {
                        logger.info("成功更新Logstash主配置文件: {}", configPath);
                    } else {
                        logger.error("更新Logstash主配置文件失败: {}", configPath);
                        success = false;
                    }
                } catch (Exception e) {
                    logger.error("更新Logstash主配置文件时发生错误: {}", e.getMessage(), e);
                    success = false;
                }
            }
            
            // 2. 更新JVM配置文件（如果提供了内容）
            if (StringUtils.hasText(jvmOptions)) {
                try {
                    String jvmFile = configDir + "/jvm.options";
                    
                    // 首先检查旧配置文件是否存在
                    String checkCommand = String.format("if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi", jvmFile);
                    String checkResult = sshClient.executeCommand(machine, checkCommand);
                    
                    if ("exists".equals(checkResult.trim())) {
                        // 备份旧配置
                        String backupFile = String.format("%s.bak.%d", jvmFile, System.currentTimeMillis());
                        String backupCommand = String.format("cp %s %s", jvmFile, backupFile);
                        sshClient.executeCommand(machine, backupCommand);
                        logger.info("已备份旧JVM配置文件: {}", backupFile);
                    }
                    
                    // 创建临时文件
                    String tempFile = String.format("/tmp/logstash-jvm-%d-%d.options",
                            processId, System.currentTimeMillis());
                    
                    // 将新配置写入临时文件，使用heredoc避免特殊字符问题
                    String createConfigCommand = String.format("cat > %s << 'EOF'\n%s\nEOF",
                            tempFile, jvmOptions);
                    sshClient.executeCommand(machine, createConfigCommand);
                    
                    // 移动到最终位置
                    String moveCommand = String.format("mv %s %s", tempFile, jvmFile);
                    sshClient.executeCommand(machine, moveCommand);
                    
                    // 检查配置文件是否更新成功
                    String verifyCommand = String.format("if [ -f \"%s\" ]; then echo \"success\"; else echo \"failed\"; fi", jvmFile);
                    String verifyResult = sshClient.executeCommand(machine, verifyCommand);
                    
                    if ("success".equals(verifyResult.trim())) {
                        logger.info("成功更新Logstash JVM配置文件: {}", jvmFile);
                    } else {
                        logger.error("更新Logstash JVM配置文件失败: {}", jvmFile);
                        success = false;
                    }
                } catch (Exception e) {
                    logger.error("更新Logstash JVM配置文件时发生错误: {}", e.getMessage(), e);
                    success = false;
                }
            }
            
            // 3. 更新系统配置文件（如果提供了内容）
            if (StringUtils.hasText(logstashYml)) {
                try {
                    String ymlFile = configDir + "/logstash.yml";
                    
                    // 首先检查旧配置文件是否存在
                    String checkCommand = String.format("if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi", ymlFile);
                    String checkResult = sshClient.executeCommand(machine, checkCommand);
                    
                    if ("exists".equals(checkResult.trim())) {
                        // 备份旧配置
                        String backupFile = String.format("%s.bak.%d", ymlFile, System.currentTimeMillis());
                        String backupCommand = String.format("cp %s %s", ymlFile, backupFile);
                        sshClient.executeCommand(machine, backupCommand);
                        logger.info("已备份旧系统配置文件: {}", backupFile);
                    }
                    
                    // 创建临时文件
                    String tempFile = String.format("/tmp/logstash-yml-%d-%d.yml",
                            processId, System.currentTimeMillis());
                    
                    // 将新配置写入临时文件，使用heredoc避免特殊字符问题
                    String createConfigCommand = String.format("cat > %s << 'EOF'\n%s\nEOF",
                            tempFile, logstashYml);
                    sshClient.executeCommand(machine, createConfigCommand);
                    
                    // 移动到最终位置
                    String moveCommand = String.format("mv %s %s", tempFile, ymlFile);
                    sshClient.executeCommand(machine, moveCommand);
                    
                    // 检查配置文件是否更新成功
                    String verifyCommand = String.format("if [ -f \"%s\" ]; then echo \"success\"; else echo \"failed\"; fi", ymlFile);
                    String verifyResult = sshClient.executeCommand(machine, verifyCommand);
                    
                    if ("success".equals(verifyResult.trim())) {
                        logger.info("成功更新Logstash系统配置文件: {}", ymlFile);
                    } else {
                        logger.error("更新Logstash系统配置文件失败: {}", ymlFile);
                        success = false;
                    }
                } catch (Exception e) {
                    logger.error("更新Logstash系统配置文件时发生错误: {}", e.getMessage(), e);
                    success = false;
                }
            }
            
            return success;
        });
    }

    @Override
    public String getDescription() {
        return "更新Logstash配置文件";
    }
}