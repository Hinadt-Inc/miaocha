package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.config.LogstashProperties;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.domain.mapper.LogstashProcessMapper;
import org.springframework.stereotype.Component;

/** Logstash命令工厂 用于创建各种Logstash命令对象 */
@Component
public class LogstashCommandFactory {

    private final LogstashProperties logstashProperties;
    private final SshClient sshClient;
    private final LogstashMachineMapper logstashMachineMapper;
    private final LogstashProcessMapper logstashProcessMapper;

    public LogstashCommandFactory(
            LogstashProperties logstashProperties,
            SshClient sshClient,
            LogstashMachineMapper logstashMachineMapper,
            LogstashProcessMapper logstashProcessMapper) {
        this.logstashProperties = logstashProperties;
        this.sshClient = sshClient;
        this.logstashMachineMapper = logstashMachineMapper;
        this.logstashProcessMapper = logstashProcessMapper;
    }

    /** 创建创建目录命令 */
    public LogstashCommand createDirectoryCommand(Long processId) {
        return new CreateDirectoryCommand(
                sshClient, logstashProperties.getDeployDir(), processId, logstashMachineMapper);
    }

    /** 创建上传文件命令 */
    public LogstashCommand uploadPackageCommand(Long processId) {
        return new UploadPackageCommand(
                sshClient,
                logstashProperties.getPackagePath(),
                logstashProperties.getDeployDir(),
                processId,
                logstashMachineMapper);
    }

    /** 创建解压文件命令 */
    public LogstashCommand extractPackageCommand(Long processId) {
        return new ExtractPackageCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                logstashProperties.getPackagePath(),
                processId,
                logstashMachineMapper);
    }

    /** 创建创建配置文件命令 */
    public LogstashCommand createConfigCommand(LogstashProcess process) {
        return new CreateConfigCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                process.getId(),
                process.getConfigContent(),
                logstashMachineMapper);
    }

    /** 创建更新多种配置文件命令 可同时更新主配置文件、JVM配置和系统配置 */
    public LogstashCommand updateConfigCommand(
            Long processId, String configContent, String jvmOptions, String logstashYml) {
        return new UpdateConfigCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                processId,
                configContent,
                jvmOptions,
                logstashYml,
                logstashMachineMapper);
    }

    /** 创建刷新多种配置文件命令 可同时刷新主配置文件、JVM配置和系统配置 */
    public LogstashCommand refreshConfigCommand(
            Long processId, String configContent, String jvmOptions, String logstashYml) {
        return new RefreshConfigCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                processId,
                logstashProcessMapper,
                logstashMachineMapper,
                configContent,
                jvmOptions,
                logstashYml);
    }

    /** 创建修改系统配置命令（增强版：支持JVM选项和系统配置） */
    public LogstashCommand modifySystemConfigCommand(
            Long processId, String jvmOptions, String logstashYml) {
        return new ModifySystemConfigCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                processId,
                jvmOptions,
                logstashYml,
                logstashMachineMapper);
    }

    /** 创建启动进程命令 */
    public LogstashCommand startProcessCommand(Long processId) {
        return new StartProcessCommand(
                sshClient, logstashProperties.getDeployDir(), processId, logstashMachineMapper);
    }

    /** 创建验证进程命令 */
    public LogstashCommand verifyProcessCommand(Long processId) {
        return new VerifyProcessCommand(
                sshClient, logstashProperties.getDeployDir(), processId, logstashMachineMapper);
    }

    /** 创建停止进程命令 */
    public LogstashCommand stopProcessCommand(Long processId) {
        return new StopProcessCommand(
                sshClient, logstashProperties.getDeployDir(), processId, logstashMachineMapper);
    }

    /** 创建强制停止进程命令 */
    public LogstashCommand forceStopProcessCommand(Long processId) {
        return new ForceStopProcessCommand(
                sshClient, logstashProperties.getDeployDir(), processId, logstashMachineMapper);
    }

    /** 创建删除进程目录命令 */
    public LogstashCommand deleteProcessDirectoryCommand(Long processId) {
        return new DeleteProcessDirectoryCommand(
                sshClient, logstashProperties.getDeployDir(), processId, logstashMachineMapper);
    }
}
