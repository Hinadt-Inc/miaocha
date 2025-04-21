package com.hina.log.ssh.impl;

import com.hina.log.ssh.SshCommandValidator;
import com.hina.log.ssh.SshConfig;
import com.hina.log.exception.SshException;
import com.hina.log.exception.SshDependencyException;
import com.hina.log.ssh.SshFileTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 使用本地scp命令实现SSH文件传输
 */
@Component
public class NativeSshFileTransfer implements SshFileTransfer {

    private static final Logger logger = LoggerFactory.getLogger(NativeSshFileTransfer.class);

    private final SshCommandValidator commandValidator;

    public NativeSshFileTransfer(SshCommandValidator commandValidator) {
        this.commandValidator = commandValidator;
    }

    @Override
    public void uploadFile(SshConfig config, String localPath, String remotePath)
            throws IOException, TimeoutException, SshException {

        // 验证SCP命令是否可用
        commandValidator.validateScpCommand();

        // 如果使用密码认证，验证sshpass是否可用
        if (StringUtils.hasText(config.getPassword()) && !StringUtils.hasText(config.getPrivateKey())) {
            commandValidator.validateSshpassCommand();
        }

        Path srcPath = Paths.get(localPath);
        if (!Files.exists(srcPath)) {
            throw new IOException("文件不存在: " + localPath);
        }

        ProcessBuilder pb = buildScpProcessWithAuth(config, localPath, remotePath, true);
        transferFile(pb, config.getTransferTimeout());
    }

    @Override
    public void downloadFile(SshConfig config, String remotePath, String localPath)
            throws IOException, TimeoutException, SshException {

        // 验证SCP命令是否可用
        commandValidator.validateScpCommand();

        // 如果使用密码认证，验证sshpass是否可用
        if (StringUtils.hasText(config.getPassword()) && !StringUtils.hasText(config.getPrivateKey())) {
            commandValidator.validateSshpassCommand();
        }

        ProcessBuilder pb = buildScpProcessWithAuth(config, remotePath, localPath, false);
        transferFile(pb, config.getTransferTimeout());
    }

    /**
     * 执行文件传输
     */
    private void transferFile(ProcessBuilder pb, int timeout)
            throws IOException, TimeoutException, SshException {

        Process process = pb.start();

        try {
            if (!process.waitFor(timeout, TimeUnit.MINUTES)) {
                process.destroy();
                throw new TimeoutException("文件传输超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SshException("文件传输被中断", e);
        }

        if (process.exitValue() != 0) {
            String error = readProcessError(process);
            throw new SshException("文件传输失败，退出码: " + process.exitValue() + "\n" + error);
        }
    }

    /**
     * 读取进程错误输出
     */
    private String readProcessError(Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            return reader.lines().reduce("", (a, b) -> a + "\n" + b);
        } catch (IOException e) {
            return "Failed to read error stream: " + e.getMessage();
        }
    }

    /**
     * 构建带有认证信息的SCP命令ProcessBuilder
     */
    private ProcessBuilder buildScpProcessWithAuth(SshConfig config, String src, String dest, boolean isUpload)
            throws IOException {

        List<String> cmdArgs = new ArrayList<>();

        // 基本SCP命令
        cmdArgs.add("scp");
        cmdArgs.add("-P");
        cmdArgs.add(String.valueOf(config.getPort()));
        cmdArgs.add("-o");
        cmdArgs.add("StrictHostKeyChecking=no");
        cmdArgs.add("-o");
        cmdArgs.add("ConnectTimeout=" + config.getConnectTimeout());

        // 如果提供了SSH密钥，优先使用密钥认证
        if (StringUtils.hasText(config.getPrivateKey())) {
            // 将SSH密钥写入临时文件
            Path keyFile = Files.createTempFile("ssh-key-", "");
            Files.write(keyFile, config.getPrivateKey().getBytes(StandardCharsets.UTF_8));
            Files.setPosixFilePermissions(keyFile,
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));

            cmdArgs.add("-i");
            cmdArgs.add(keyFile.toString());

            // 设置文件删除钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.deleteIfExists(keyFile);
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary SSH key file: {}", e.getMessage());
                }
            }));
        }
        // 否则使用密码认证 (需要使用sshpass)
        else if (StringUtils.hasText(config.getPassword())) {
            cmdArgs.add(0, "sshpass");
            cmdArgs.add(1, "-p");
            cmdArgs.add(2, config.getPassword());
        }

        // 源和目标
        if (isUpload) {
            cmdArgs.add(src);
            cmdArgs.add(config.getUsername() + "@" + config.getHost() + ":" + dest);
        } else {
            cmdArgs.add(config.getUsername() + "@" + config.getHost() + ":" + src);
            cmdArgs.add(dest);
        }

        return new ProcessBuilder(cmdArgs);
    }
}