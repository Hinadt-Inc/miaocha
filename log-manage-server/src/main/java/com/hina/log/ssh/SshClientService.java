package com.hina.log.ssh;

import com.hina.log.entity.Machine;

/**
 * SSH客户端服务接口
 * 负责在远程机器上执行命令
 */
public interface SshClientService {

    /**
     * 在远程机器上执行命令
     *
     * @param machine 目标机器
     * @param command 要执行的命令
     * @return 命令执行结果
     * @throws Exception 执行过程中的异常
     */
    String executeCommand(Machine machine, String command) throws Exception;
} 