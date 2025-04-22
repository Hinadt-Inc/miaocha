package com.hina.log.logstash.command;

import com.hina.log.config.LogstashProperties;
import com.hina.log.entity.LogstashProcess;
import com.hina.log.mapper.LogstashMachineMapper;
import com.hina.log.ssh.SshClient;
import org.springframework.stereotype.Component;

/**
 * Logstash命令工厂
 * 用于创建各种Logstash命令对象
 */
@Component
public class LogstashCommandFactory {

    private final LogstashProperties logstashProperties;
    private final SshClient sshClient;
    private final LogstashMachineMapper logstashMachineMapper;

    public LogstashCommandFactory(LogstashProperties logstashProperties, SshClient sshClient,
            LogstashMachineMapper logstashMachineMapper) {
        this.logstashProperties = logstashProperties;
        this.sshClient = sshClient;
        this.logstashMachineMapper = logstashMachineMapper;
    }

    /**
     * 创建创建目录命令
     */
    public LogstashCommand createDirectoryCommand(Long processId) {
        return new CreateDirectoryCommand(sshClient, logstashProperties.getDeployDir(), processId);
    }

    /**
     * 创建上传文件命令
     */
    public LogstashCommand uploadPackageCommand(Long processId) {
        return new UploadPackageCommand(sshClient, logstashProperties.getPackagePath(),
                logstashProperties.getDeployDir(), processId);
    }

    /**
     * 创建解压文件命令
     */
    public LogstashCommand extractPackageCommand(Long processId) {
        return new ExtractPackageCommand(sshClient, logstashProperties.getDeployDir(),
                logstashProperties.getPackagePath(), processId);
    }

    /**
     * 创建创建配置文件命令
     */
    public LogstashCommand createConfigCommand(LogstashProcess process) {
        return new CreateConfigCommand(sshClient, logstashProperties.getDeployDir(),
                process.getId(), process.getConfigJson());
    }

    /**
     * 创建修改系统配置命令
     */
    public LogstashCommand modifySystemConfigCommand(Long processId) {
        return new ModifySystemConfigCommand(sshClient, logstashProperties.getDeployDir(), processId);
    }

    /**
     * 创建启动进程命令
     */
    public LogstashCommand startProcessCommand(Long processId) {
        return new StartProcessCommand(sshClient, logstashProperties.getDeployDir(), processId);
    }

    /**
     * 创建验证进程命令
     */
    public LogstashCommand verifyProcessCommand(Long processId) {
        return new VerifyProcessCommand(sshClient, logstashProperties.getDeployDir(), processId, logstashMachineMapper);
    }

    /**
     * 创建停止进程命令
     */
    public LogstashCommand stopProcessCommand(Long processId) {
        return new StopProcessCommand(sshClient, logstashProperties.getDeployDir(), processId);
    }

    /**
     * 创建删除进程目录命令
     */
    public LogstashCommand deleteProcessDirectoryCommand(Long processId) {
        return new DeleteProcessDirectoryCommand(sshClient, logstashProperties.getDeployDir(), processId);
    }
}