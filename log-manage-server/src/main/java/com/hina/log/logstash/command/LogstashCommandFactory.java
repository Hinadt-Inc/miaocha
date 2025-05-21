package com.hina.log.logstash.command;

import com.hina.log.config.LogstashProperties;
import com.hina.log.entity.LogstashProcess;
import com.hina.log.mapper.LogstashMachineMapper;
import com.hina.log.mapper.LogstashProcessMapper;
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
    private final LogstashProcessMapper logstashProcessMapper;

    public LogstashCommandFactory(LogstashProperties logstashProperties, SshClient sshClient,
            LogstashMachineMapper logstashMachineMapper, LogstashProcessMapper logstashProcessMapper) {
        this.logstashProperties = logstashProperties;
        this.sshClient = sshClient;
        this.logstashMachineMapper = logstashMachineMapper;
        this.logstashProcessMapper = logstashProcessMapper;
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
                process.getId(), process.getConfigContent());
    }

    /**
     * 创建更新配置文件命令
     */
    public LogstashCommand updateConfigCommand(Long processId, String configContent) {
        return new UpdateConfigCommand(sshClient, logstashProperties.getDeployDir(),
                processId, configContent);
    }

    /**
     * 创建更新多种配置文件命令
     * 可同时更新主配置文件、JVM配置和系统配置
     */
    public LogstashCommand updateConfigCommand(Long processId, String configContent, String jvmOptions, String logstashYml) {
        return new UpdateConfigCommand(sshClient, logstashProperties.getDeployDir(),
                processId, configContent, jvmOptions, logstashYml);
    }

    /**
     * 创建刷新配置文件命令
     */
    public LogstashCommand refreshConfigCommand(Long processId) {
        return new RefreshConfigCommand(sshClient, logstashProperties.getDeployDir(),
                processId, logstashProcessMapper, null);
    }

    /**
     * 创建刷新配置文件命令（带配置内容）
     */
    public LogstashCommand refreshConfigCommand(Long processId, String configContent) {
        return new RefreshConfigCommand(sshClient, logstashProperties.getDeployDir(),
                processId, logstashProcessMapper, configContent);
    }

    /**
     * 创建刷新多种配置文件命令
     * 可同时刷新主配置文件、JVM配置和系统配置
     */
    public LogstashCommand refreshConfigCommand(Long processId, String configContent, 
                                               String jvmOptions, String logstashYml) {
        return new RefreshConfigCommand(sshClient, logstashProperties.getDeployDir(),
                processId, logstashProcessMapper, logstashMachineMapper, 
                configContent, jvmOptions, logstashYml);
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