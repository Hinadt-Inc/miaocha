package com.hina.log.ssh;

import com.hina.log.entity.Machine;
import com.hina.log.exception.SshOperationException;
import com.hina.log.exception.SshException;
import com.hina.log.exception.SshDependencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeoutException;

/**
 * SSH客户端，提供基础SSH操作
 * 不包含任何业务逻辑，仅作为SSH操作的门面
 */
@Component
public class SshClient {

    private static final Logger logger = LoggerFactory.getLogger(SshClient.class);

    private final SshCommand sshCommand;
    private final SshFileTransfer sshFileTransfer;

    public SshClient(SshCommand sshCommand, SshFileTransfer sshFileTransfer) {
        this.sshCommand = sshCommand;
        this.sshFileTransfer = sshFileTransfer;
    }

    /**
     * 从Machine实体创建SSH配置
     *
     * @param machine 机器信息
     * @return SSH配置
     */
    public static SshConfig createConfig(Machine machine) {
        return SshConfig.builder()
                .host(machine.getIp())
                .port(machine.getPort())
                .username(machine.getUsername())
                .password(machine.getPassword())
                .privateKey(machine.getSshKey())
                .build();
    }

    /**
     * 测试SSH连接
     *
     * @param machine 机器信息
     * @return 是否连接成功
     */
    public boolean testConnection(Machine machine) {
        try {
            SshConfig config = createConfig(machine);
            sshCommand.execute(config, "echo 'Connection test successful'");
            return true;
        } catch (SshDependencyException e) {
            logger.error("测试连接到 {} 时SSH依赖缺失: {}", machine.getIp(), e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("测试SSH连接到 {} 失败: {}", machine.getIp(), e.getMessage());
            return false;
        }
    }

    /**
     * 执行SSH命令
     *
     * @param machine 机器信息
     * @param command 要执行的命令
     * @return 命令执行结果
     * @throws SshException 如果执行失败
     */
    public String executeCommand(Machine machine, String command) throws SshException {
        try {
            SshConfig config = createConfig(machine);
            return sshCommand.execute(config, command);
        } catch (SshDependencyException e) {
            logger.error("在 {} 上执行命令时SSH依赖缺失: {}", machine.getIp(), e.getMessage());
            throw e; // 直接抛出依赖异常，让上层处理
        } catch (TimeoutException e) {
            logger.error("在 {} 上执行命令超时: {}", machine.getIp(), e.getMessage());
            throw new SshException("命令执行超时: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("在 {} 上执行命令失败: {}", machine.getIp(), e.getMessage());
            throw new SshException("命令执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传文件
     *
     * @param machine    机器信息
     * @param localPath  本地文件路径
     * @param remotePath 远程文件路径
     * @throws SshException 如果上传失败
     */
    public void uploadFile(Machine machine, String localPath, String remotePath) throws SshException {
        try {
            SshConfig config = createConfig(machine);
            sshFileTransfer.uploadFile(config, localPath, remotePath);
        } catch (SshDependencyException e) {
            logger.error("上传文件到 {} 时SSH依赖缺失: {}", machine.getIp(), e.getMessage());
            throw e; // 直接抛出依赖异常，让上层处理
        } catch (TimeoutException e) {
            logger.error("上传文件到 {} 超时: {}", machine.getIp(), e.getMessage());
            throw new SshException("文件上传超时: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("上传文件到 {} 失败: {}", machine.getIp(), e.getMessage());
            throw new SshException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载文件
     *
     * @param machine    机器信息
     * @param remotePath 远程文件路径
     * @param localPath  本地文件路径
     * @throws SshException 如果下载失败
     */
    public void downloadFile(Machine machine, String remotePath, String localPath) throws SshException {
        try {
            SshConfig config = createConfig(machine);
            sshFileTransfer.downloadFile(config, remotePath, localPath);
        } catch (SshDependencyException e) {
            logger.error("从 {} 下载文件时SSH依赖缺失: {}", machine.getIp(), e.getMessage());
            throw e;
        } catch (TimeoutException e) {
            logger.error("从 {} 下载文件超时: {}", machine.getIp(), e.getMessage());
            throw new SshException("文件下载超时: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("从 {} 下载文件失败: {}", machine.getIp(), e.getMessage());
            throw new SshException("文件下载失败: " + e.getMessage(), e);
        }
    }
}