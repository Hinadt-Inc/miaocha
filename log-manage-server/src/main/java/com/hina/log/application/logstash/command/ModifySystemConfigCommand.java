package com.hina.log.application.logstash.command;

import com.hina.log.common.exception.SshOperationException;
import com.hina.log.common.ssh.SshClient;
import com.hina.log.domain.entity.Machine;
import java.util.concurrent.CompletableFuture;
import org.springframework.util.StringUtils;

/** 修改Logstash系统配置命令 支持设置默认系统配置以及应用JVM选项 */
public class ModifySystemConfigCommand extends AbstractLogstashCommand {

    private final String jvmOptions; // JVM配置文件内容
    private final String logstashYml; // 系统配置文件内容

    /** 创建基本系统配置命令（仅添加allow_superuser设置） */
    public ModifySystemConfigCommand(SshClient sshClient, String deployDir, Long processId) {
        this(sshClient, deployDir, processId, null, null);
    }

    /**
     * 创建增强型系统配置命令（支持JVM选项和完整的系统配置）
     *
     * @param sshClient SSH客户端
     * @param deployDir 部署基础目录
     * @param processId 进程ID
     * @param jvmOptions JVM配置文件内容（可为null）
     * @param logstashYml 系统配置文件内容（可为null）
     */
    public ModifySystemConfigCommand(
            SshClient sshClient,
            String deployDir,
            Long processId,
            String jvmOptions,
            String logstashYml) {
        super(sshClient, deployDir, processId);
        this.jvmOptions = jvmOptions;
        this.logstashYml = logstashYml;
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(Machine machine) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        boolean success = true;
                        String processDir = getProcessDirectory();
                        String configDir = processDir + "/config";

                        // 1. 修改系统配置文件 (logstash.yml)
                        success = modifyLogstashYml(machine, configDir) && success;

                        // 2. 修改JVM选项 (jvm.options)，如果提供了内容
                        if (StringUtils.hasText(jvmOptions)) {
                            success = modifyJvmOptions(machine, configDir) && success;
                        }

                        return success;
                    } catch (Exception e) {
                        logger.error("修改Logstash系统配置时发生错误: {}", e.getMessage(), e);
                        throw new SshOperationException("修改Logstash系统配置失败: " + e.getMessage(), e);
                    }
                });
    }

    /** 修改Logstash系统配置文件 */
    private boolean modifyLogstashYml(Machine machine, String configDir) throws Exception {
        String configPath = configDir + "/logstash.yml";

        // 如果提供了完整的系统配置文件内容，则使用该内容
        if (StringUtils.hasText(logstashYml)) {
            // 检查配置文件是否存在
            String checkCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                            configPath);
            String checkResult = sshClient.executeCommand(machine, checkCommand);

            if ("exists".equals(checkResult.trim())) {
                // 备份现有配置
                String backupFile =
                        String.format("%s.bak.%d", configPath, System.currentTimeMillis());
                String backupCommand = String.format("cp %s %s", configPath, backupFile);
                sshClient.executeCommand(machine, backupCommand);
                logger.info("已备份现有系统配置文件: {}", backupFile);
            }

            // 创建临时文件
            String tempFile =
                    String.format(
                            "/tmp/logstash-yml-%d-%d.yml", processId, System.currentTimeMillis());

            // 将新配置写入临时文件，使用heredoc避免特殊字符问题
            String createConfigCommand =
                    String.format("cat > %s << 'EOF'\n%s\nEOF", tempFile, logstashYml);
            sshClient.executeCommand(machine, createConfigCommand);

            // 移动到最终位置
            String moveCommand = String.format("mv %s %s", tempFile, configPath);
            sshClient.executeCommand(machine, moveCommand);

            // 验证配置是否写入成功
            String verifyCommand =
                    String.format(
                            "if [ -f \"%s\" ] && [ -s \"%s\" ]; then echo \"success\"; else echo"
                                    + " \"failed\"; fi",
                            configPath, configPath);
            String verifyResult = sshClient.executeCommand(machine, verifyCommand);

            boolean success = "success".equals(verifyResult.trim());
            if (success) {
                logger.info("成功写入Logstash系统配置: {}", configPath);
            } else {
                logger.error("写入Logstash系统配置失败: {}", configPath);
            }

            return success;
        } else {
            // 默认行为：添加allow_superuser设置
            // 检查配置文件是否存在
            String checkCommand =
                    String.format(
                            "if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                            configPath);
            String checkResult = sshClient.executeCommand(machine, checkCommand);

            if ("exists".equals(checkResult.trim())) {
                // 添加配置
                String appendCommand =
                        String.format("echo '\nallow_superuser: true' >> %s", configPath);
                sshClient.executeCommand(machine, appendCommand);
            } else {
                // 文件不存在，创建新文件
                String createCommand =
                        String.format("echo 'allow_superuser: true' > %s", configPath);
                sshClient.executeCommand(machine, createCommand);
            }

            // 验证配置是否添加成功
            String validateCommand =
                    String.format("grep \"allow_superuser: true\" %s | wc -l", configPath);
            String validateResult = sshClient.executeCommand(machine, validateCommand);

            boolean success = !"0".equals(validateResult.trim());
            if (success) {
                logger.info("成功修改Logstash系统配置: {}", configPath);
            } else {
                logger.error("修改Logstash系统配置失败: {}", configPath);
            }

            return success;
        }
    }

    /** 修改Logstash JVM选项 */
    private boolean modifyJvmOptions(Machine machine, String configDir) throws Exception {
        String jvmFile = configDir + "/jvm.options";

        // 检查JVM配置文件是否存在
        String checkCommand =
                String.format(
                        "if [ -f \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                        jvmFile);
        String checkResult = sshClient.executeCommand(machine, checkCommand);

        if ("exists".equals(checkResult.trim())) {
            // 备份现有配置
            String backupFile = String.format("%s.bak.%d", jvmFile, System.currentTimeMillis());
            String backupCommand = String.format("cp %s %s", jvmFile, backupFile);
            sshClient.executeCommand(machine, backupCommand);
            logger.info("已备份现有JVM配置文件: {}", backupFile);
        }

        // 创建临时文件
        String tempFile =
                String.format(
                        "/tmp/logstash-jvm-%d-%d.options", processId, System.currentTimeMillis());

        // 将JVM选项写入临时文件，使用heredoc避免特殊字符问题
        String createConfigCommand =
                String.format("cat > %s << 'EOF'\n%s\nEOF", tempFile, jvmOptions);
        sshClient.executeCommand(machine, createConfigCommand);

        // 移动到最终位置
        String moveCommand = String.format("mv %s %s", tempFile, jvmFile);
        sshClient.executeCommand(machine, moveCommand);

        // 验证配置是否写入成功
        String verifyCommand =
                String.format(
                        "if [ -f \"%s\" ] && [ -s \"%s\" ]; then echo \"success\"; else echo"
                                + " \"failed\"; fi",
                        jvmFile, jvmFile);
        String verifyResult = sshClient.executeCommand(machine, verifyCommand);

        boolean success = "success".equals(verifyResult.trim());
        if (success) {
            logger.info("成功写入Logstash JVM配置: {}", jvmFile);
        } else {
            logger.error("写入Logstash JVM配置失败: {}", jvmFile);
        }

        return success;
    }

    @Override
    public String getDescription() {
        return "修改Logstash系统配置";
    }
}
