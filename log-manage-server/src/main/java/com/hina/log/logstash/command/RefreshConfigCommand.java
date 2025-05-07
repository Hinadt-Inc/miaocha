package com.hina.log.logstash.command;

import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.exception.SshOperationException;
import com.hina.log.mapper.LogstashProcessMapper;
import com.hina.log.ssh.SshClient;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;

/**
 * 刷新Logstash配置文件命令
 * 从数据库获取最新配置并刷新到目标机器
 */
public class RefreshConfigCommand extends AbstractLogstashCommand {

    private final LogstashProcessMapper logstashProcessMapper;
    private final String configContent;

    public RefreshConfigCommand(SshClient sshClient, String deployDir, Long processId,
            LogstashProcessMapper logstashProcessMapper, String configContent) {
        super(sshClient, deployDir, processId);
        this.logstashProcessMapper = logstashProcessMapper;
        this.configContent = configContent;
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(Machine machine) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // 如果没有提供配置内容，从数据库获取
            String actualConfigContent = configContent;
            if (!StringUtils.hasText(actualConfigContent)) {
                LogstashProcess process = logstashProcessMapper.selectById(processId);
                if (process == null) {
                    throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
                }

                actualConfigContent = process.getConfigContent();
                if (!StringUtils.hasText(actualConfigContent)) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "数据库中的配置为空");
                }
            }

            String processDir = getProcessDirectory();
            String configDir = processDir + "/config";
            String configPath = configDir + "/logstash-" + processId + ".conf";

            // 确保配置目录存在
            String createDirCommand = String.format("mkdir -p %s", configDir);
            sshClient.executeCommand(machine, createDirCommand);

            // 首先检查旧配置文件是否存在
            String checkCommand = String.format("[ -f %s ] && echo \"exists\" || echo \"not_exists\"", configPath);
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

            // 将配置写入临时文件，使用heredoc避免特殊字符问题
            String createConfigCommand = String.format("cat > %s << 'EOF'\n%s\nEOF",
                    tempFile, actualConfigContent);
            sshClient.executeCommand(machine, createConfigCommand);

            // 移动到最终位置
            String moveCommand = String.format("mv %s %s", tempFile, configPath);
            sshClient.executeCommand(machine, moveCommand);

            // 检查配置文件是否刷新成功
            String verifyCommand = String.format("[ -f %s ] && echo \"success\"", configPath);
            String verifyResult = sshClient.executeCommand(machine, verifyCommand);

            boolean success = "success".equals(verifyResult.trim());
            if (success) {
                logger.info("成功刷新Logstash配置文件: {}", configPath);
            } else {
                logger.error("刷新Logstash配置文件失败: {}", configPath);
            }

            future.complete(success);
        } catch (Exception e) {
            logger.error("刷新Logstash配置文件时发生错误: {}", e.getMessage(), e);
            future.completeExceptionally(new SshOperationException("刷新Logstash配置文件失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public String getDescription() {
        return "刷新Logstash配置文件";
    }
}