package com.hina.log.ssh.impl;

import com.hina.log.ssh.SshCommand;
import com.hina.log.ssh.SshCommandValidator;
import com.hina.log.ssh.SshConfig;
import com.hina.log.exception.SshException;
import com.hina.log.exception.SshDependencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 使用本地系统命令实现SSH命令执行
 */
@Component
public class NativeSshCommand implements SshCommand {

    private static final Logger logger = LoggerFactory.getLogger(NativeSshCommand.class);

    private final SshCommandValidator commandValidator;

    public NativeSshCommand(SshCommandValidator commandValidator) {
        this.commandValidator = commandValidator;
    }

    @Override
    public String execute(SshConfig config, String command)
            throws IOException, TimeoutException, SshException {

        // 验证SSH命令是否可用
        commandValidator.validateSshCommand();

        // 如果使用密码认证，验证sshpass是否可用
        if (StringUtils.hasText(config.getPassword()) && !StringUtils.hasText(config.getPrivateKey())) {
            commandValidator.validateSshpassCommand();
        }

        ProcessBuilder pb = buildSshProcessWithAuth(config, command);
        Process process = pb.start();

        // 读取标准输出
        StringWriter outputWriter = new StringWriter();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputWriter.append(line).append("\n");
            }
        }

        // 等待进程完成
        try {
            if (!process.waitFor(config.getCommandTimeout(), TimeUnit.MINUTES)) {
                process.destroy();
                throw new TimeoutException("命令执行超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SshException("命令执行被中断", e);
        }

        // 检查退出状态
        if (process.exitValue() != 0) {
            String error = readProcessError(process);
            throw new SshException("命令执行失败，退出码: " + process.exitValue() + "\n" + error);
        }

        return outputWriter.toString();
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
     * 构建带有认证信息的SSH命令ProcessBuilder
     */
    private ProcessBuilder buildSshProcessWithAuth(SshConfig config, String command) throws IOException {
        List<String> cmdArgs = new ArrayList<>();

        // 基本SSH命令
        cmdArgs.add("ssh");
        cmdArgs.add("-p");
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

        // 目标服务器和命令
        cmdArgs.add(config.getUsername() + "@" + config.getHost());
        cmdArgs.add(command);

        return new ProcessBuilder(cmdArgs);
    }
}