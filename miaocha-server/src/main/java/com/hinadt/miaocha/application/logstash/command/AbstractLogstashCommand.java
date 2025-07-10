package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logstash命令抽象基类 - 重构支持多实例，基于logstashMachineId */
public abstract class AbstractLogstashCommand implements LogstashCommand {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final SshClient sshClient;
    protected final String deployBaseDir;
    protected final Long logstashMachineId;
    protected final LogstashMachineMapper logstashMachineMapper;
    protected final LogstashDeployPathManager deployPathManager;

    protected AbstractLogstashCommand(
            SshClient sshClient,
            String deployBaseDir,
            Long logstashMachineId,
            LogstashMachineMapper logstashMachineMapper,
            LogstashDeployPathManager deployPathManager) {
        this.sshClient = sshClient;
        this.deployBaseDir = deployBaseDir;
        this.logstashMachineId = logstashMachineId;
        this.logstashMachineMapper = logstashMachineMapper;
        this.deployPathManager = deployPathManager;
    }

    /** 执行命令 */
    @Override
    public boolean execute(MachineInfo machineInfo) {
        logger.info(
                "开始执行命令: {} 在机器: {} 上，实例ID: {}",
                getDescription(),
                machineInfo.getName(),
                logstashMachineId);

        try {
            // 检查命令是否已经执行过
            boolean alreadyExecuted = checkAlreadyExecuted(machineInfo);
            if (alreadyExecuted) {
                logger.info(
                        "命令 {} 在机器 {} 上已执行，跳过执行，实例ID: {}",
                        getDescription(),
                        machineInfo.getName(),
                        logstashMachineId);
                return true;
            }

            logger.info(
                    "开始执行命令 {} 在机器 {} 上，实例ID: {}",
                    getDescription(),
                    machineInfo.getName(),
                    logstashMachineId);

            boolean result = doExecute(machineInfo);

            logger.info(
                    "命令 {} 在机器 {} 上执行完成，实例ID: {}, 结果: {}",
                    getDescription(),
                    machineInfo.getName(),
                    logstashMachineId,
                    result ? "成功" : "失败");

            return result;
        } catch (Exception e) {
            logger.error(
                    "命令 {} 在机器 {} 上执行发生错误，实例ID: {}, 错误: {}",
                    getDescription(),
                    machineInfo.getName(),
                    logstashMachineId,
                    e.getMessage(),
                    e);
            return false;
        }
    }

    /** 检查命令是否已经执行过 */
    protected boolean checkAlreadyExecuted(MachineInfo machineInfo) {
        // 默认情况下，假设命令未执行过
        return false;
    }

    /** 实际执行命令 */
    protected abstract boolean doExecute(MachineInfo machineInfo);

    /** 获取Logstash实例目录 */
    protected String getProcessDirectory() {
        return deployPathManager.getInstanceDeployPath(logstashMachineId);
    }

    /** 获取当前操作的LogstashMachine实例 */
    protected LogstashMachine getLogstashMachine() {
        return logstashMachineMapper.selectById(logstashMachineId);
    }
}
