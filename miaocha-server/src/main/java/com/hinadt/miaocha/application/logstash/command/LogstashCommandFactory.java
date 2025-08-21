package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.config.LogstashProperties;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.infrastructure.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.infrastructure.ssh.SshClient;
import org.springframework.stereotype.Component;

/** Logstash命令工厂 - 重构支持多实例，基于logstashMachineId创建命令 */
@Component
public class LogstashCommandFactory {

    private final LogstashProperties logstashProperties;
    private final SshClient sshClient;
    private final LogstashMachineMapper logstashMachineMapper;
    private final LogstashDeployPathManager deployPathManager;

    public LogstashCommandFactory(
            LogstashProperties logstashProperties,
            SshClient sshClient,
            LogstashMachineMapper logstashMachineMapper,
            LogstashDeployPathManager deployPathManager) {
        this.logstashProperties = logstashProperties;
        this.sshClient = sshClient;
        this.logstashMachineMapper = logstashMachineMapper;
        this.deployPathManager = deployPathManager;
    }

    /** 创建创建目录命令 - 基于logstashMachineId */
    public LogstashCommand createDirectoryCommand(Long logstashMachineId) {
        return new CreateDirectoryCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                logstashMachineId,
                logstashMachineMapper,
                deployPathManager);
    }

    /** 创建上传文件命令 - 基于logstashMachineId */
    public LogstashCommand uploadPackageCommand(Long logstashMachineId) {
        return new UploadPackageCommand(
                sshClient,
                logstashProperties.getPackagePath(),
                logstashProperties.getDeployDir(),
                logstashMachineId,
                logstashMachineMapper,
                deployPathManager);
    }

    /** 创建解压文件命令 - 基于logstashMachineId */
    public LogstashCommand extractPackageCommand(Long logstashMachineId) {
        return new ExtractPackageCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                logstashProperties.getPackagePath(),
                logstashMachineId,
                logstashMachineMapper,
                deployPathManager);
    }

    /** 创建创建配置文件命令 - 基于logstashMachine实例 */
    public LogstashCommand createConfigCommand(LogstashMachine logstashMachine) {
        return new CreateConfigCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                logstashMachine.getId(),
                logstashMachine.getConfigContent(),
                logstashMachineMapper,
                deployPathManager);
    }

    /** 创建更新多种配置文件命令 - 基于logstashMachineId */
    public LogstashCommand updateConfigCommand(
            Long logstashMachineId, String configContent, String jvmOptions, String logstashYml) {
        return new UpdateConfigCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                logstashMachineId,
                configContent,
                jvmOptions,
                logstashYml,
                logstashMachineMapper,
                deployPathManager);
    }

    /** 创建刷新配置文件命令 - 从数据库获取配置并写入文件 */
    public LogstashCommand refreshConfigCommand(Long logstashMachineId) {
        return new RefreshConfigCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                logstashMachineId,
                logstashMachineMapper,
                deployPathManager);
    }

    /** 创建修改系统配置命令 - 基于logstashMachineId */
    public LogstashCommand modifySystemConfigCommand(
            Long logstashMachineId, String jvmOptions, String logstashYml) {
        return new ModifySystemConfigCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                logstashMachineId,
                jvmOptions,
                logstashYml,
                logstashMachineMapper,
                deployPathManager);
    }

    /** 创建启动进程命令 - 基于logstashMachineId */
    public LogstashCommand startProcessCommand(Long logstashMachineId) {
        return new StartProcessCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                logstashMachineId,
                logstashMachineMapper,
                deployPathManager);
    }

    /** 创建验证进程命令 - 基于logstashMachineId */
    public LogstashCommand verifyProcessCommand(Long logstashMachineId) {
        return new VerifyProcessCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                logstashMachineId,
                logstashMachineMapper,
                deployPathManager);
    }

    /** 创建停止进程命令 - 基于logstashMachineId */
    public LogstashCommand stopProcessCommand(Long logstashMachineId) {
        return new StopProcessCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                logstashMachineId,
                logstashMachineMapper,
                deployPathManager);
    }

    /** 创建强制停止进程命令 - 基于logstashMachineId */
    public LogstashCommand forceStopProcessCommand(Long logstashMachineId) {
        return new ForceStopProcessCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                logstashMachineId,
                logstashMachineMapper,
                deployPathManager);
    }

    /** 创建删除进程目录命令 - 基于logstashMachineId */
    public LogstashCommand deleteProcessDirectoryCommand(Long logstashMachineId) {
        return new DeleteProcessDirectoryCommand(
                sshClient,
                logstashProperties.getDeployDir(),
                logstashMachineId,
                logstashMachineMapper,
                deployPathManager);
    }
}
