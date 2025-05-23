package com.hina.log.application.logstash.command;

import com.hina.log.domain.entity.Machine;
import com.hina.log.common.exception.SshOperationException;
import com.hina.log.common.ssh.SshClient;

import java.util.concurrent.CompletableFuture;

/**
 * 删除Logstash进程目录命令
 * 
 * <p>
 * 此命令用于删除远程机器上指定Logstash进程的目录及其所有内容。它可以在以下两种场景中使用：
 * </p>
 * <ol>
 * <li>作为停止进程操作的一部分，在RunningStateHandler中由状态管理器调用</li>
 * <li>从LogstashProcessServiceImpl.deleteLogstashProcess方法直接调用，作为删除进程的清理步骤</li>
 * </ol>
 * 
 * <p>
 * 命令执行流程：
 * </p>
 * <ol>
 * <li>检查进程目录是否存在</li>
 * <li>如果存在，使用rm -rf命令删除目录</li>
 * <li>验证目录是否已成功删除</li>
 * </ol>
 * 
 * <p>
 * 如果目录不存在，则视为操作成功完成。
 * </p>
 */
public class DeleteProcessDirectoryCommand extends AbstractLogstashCommand {

    public DeleteProcessDirectoryCommand(SshClient sshClient, String deployDir, Long processId) {
        super(sshClient, deployDir, processId);
    }

    @Override
    protected CompletableFuture<Boolean> doExecute(Machine machine) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            String processDir = getProcessDirectory();

            // 检查目录是否存在
            String checkCommand = String.format("if [ -d \"%s\" ]; then echo \"exists\"; else echo \"not_exists\"; fi", processDir);
            String checkResult = sshClient.executeCommand(machine, checkCommand);

            if ("exists".equals(checkResult.trim())) {
                // 删除目录
                logger.info("删除Logstash进程目录: {}", processDir);
                String deleteCommand = String.format("rm -rf %s", processDir);
                sshClient.executeCommand(machine, deleteCommand);

                // 验证目录是否已删除
                String verifyCommand = String.format("if [ ! -d \"%s\" ]; then echo \"deleted\"; else echo \"still_exists\"; fi", processDir);
                String verifyResult = sshClient.executeCommand(machine, verifyCommand);

                boolean success = "deleted".equals(verifyResult.trim());
                if (success) {
                    logger.info("成功删除Logstash进程目录");
                } else {
                    logger.error("删除Logstash进程目录失败");
                }

                future.complete(success);
            } else {
                // 目录不存在，视为操作成功
                logger.info("Logstash进程目录不存在，无需删除");
                future.complete(true);
            }
        } catch (Exception e) {
            logger.error("删除Logstash进程目录时发生错误: {}", e.getMessage(), e);
            future.completeExceptionally(new SshOperationException("删除Logstash进程目录失败: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public String getDescription() {
        return "删除Logstash进程目录";
    }
}