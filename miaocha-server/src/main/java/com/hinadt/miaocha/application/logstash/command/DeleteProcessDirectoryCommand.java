package com.hinadt.miaocha.application.logstash.command;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.common.exception.SshOperationException;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.infrastructure.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.infrastructure.ssh.SshClient;

/**
 * 删除Logstash进程目录命令
 *
 * <p>此命令用于删除远程机器上指定Logstash进程的目录及其所有内容。它可以在以下两种场景中使用：
 *
 * <ol>
 *   <li>作为停止进程操作的一部分，在RunningStateHandler中由状态管理器调用
 *   <li>从LogstashProcessServiceImpl.deleteLogstashProcess方法直接调用，作为删除进程的清理步骤
 * </ol>
 *
 * <p>命令执行流程：
 *
 * <ol>
 *   <li>检查进程目录是否存在
 *   <li>如果存在，使用rm -rf命令删除目录
 *   <li>验证目录是否已成功删除
 * </ol>
 *
 * <p>如果目录不存在，则视为操作成功完成。
 */
public class DeleteProcessDirectoryCommand extends AbstractLogstashCommand {

    public DeleteProcessDirectoryCommand(
            SshClient sshClient,
            String deployBaseDir,
            Long logstashMachineId,
            LogstashMachineMapper logstashMachineMapper,
            LogstashDeployPathManager deployPathManager) {
        super(
                sshClient,
                deployBaseDir,
                logstashMachineId,
                logstashMachineMapper,
                deployPathManager);
    }

    @Override
    protected boolean checkAlreadyExecuted(MachineInfo machineInfo) {
        try {
            String processDir = getProcessDirectory();

            // 检查目录是否存在
            String checkCommand =
                    String.format(
                            "if [ -d \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                            processDir);
            String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

            boolean exists = "exists".equals(checkResult.trim());
            if (!exists) {
                logger.info("实例目录不存在，跳过删除，实例ID: {}, 路径: {}", logstashMachineId, processDir);
            }
            return !exists; // 如果不存在，则已经删除
        } catch (Exception e) {
            return false; // 出错时假设需要执行删除操作
        }
    }

    @Override
    protected boolean doExecute(MachineInfo machineInfo) {
        try {
            String processDir = getProcessDirectory();

            // 检查目录是否存在
            String checkCommand =
                    String.format(
                            "if [ -d \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi",
                            processDir);
            String checkResult = sshClient.executeCommand(machineInfo, checkCommand);

            if (!"exists".equals(checkResult.trim())) {
                logger.info("实例目录不存在，无需删除，实例ID: {}, 路径: {}", logstashMachineId, processDir);
                return true;
            }

            logger.info("开始删除实例目录，实例ID: {}, 路径: {}", logstashMachineId, processDir);

            // 删除目录及其所有内容
            String deleteCommand = String.format("rm -rf %s", processDir);
            sshClient.executeCommand(machineInfo, deleteCommand);

            // 验证删除是否成功
            String verifyCommand =
                    String.format(
                            "if [ -d \"%s\" ]; then echo \"still_exists\"; else echo \"deleted\";"
                                    + " fi",
                            processDir);
            String verifyResult = sshClient.executeCommand(machineInfo, verifyCommand);

            boolean success = "deleted".equals(verifyResult.trim());
            if (success) {
                logger.info("成功删除实例目录，实例ID: {}, 路径: {}", logstashMachineId, processDir);
            } else {
                logger.error("删除实例目录失败，实例ID: {}, 路径: {}", logstashMachineId, processDir);
            }

            return success;
        } catch (Exception e) {
            throw new SshOperationException("删除目录失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getDescription() {
        return "删除Logstash实例目录";
    }
}
