package com.hina.log.ssh;

import com.hina.log.exception.SshException;
import com.hina.log.exception.SshDependencyException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * SSH命令执行接口
 */
public interface SshCommand {

    /**
     * 执行SSH命令
     *
     * @param config  SSH配置
     * @param command 要执行的命令
     * @return 命令执行结果
     * @throws IOException      如果IO操作失败
     * @throws TimeoutException 如果命令执行超时
     * @throws SshException     如果SSH操作失败
     */
    String execute(SshConfig config, String command) throws IOException, TimeoutException, SshException;

    /**
     * 检查命令是否执行成功
     *
     * @param config  SSH配置
     * @param command 要执行的命令
     * @return 命令是否执行成功
     */
    default boolean executeCheck(SshConfig config, String command) {
        try {
            execute(config, command);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}