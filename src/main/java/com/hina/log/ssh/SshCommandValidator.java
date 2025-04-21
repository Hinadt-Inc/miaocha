package com.hina.log.ssh;

import com.hina.log.exception.SshDependencyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * SSH命令验证类
 * 用于验证系统是否安装了所需的SSH相关命令
 */
@Component
public class SshCommandValidator {

    private static final Logger logger = LoggerFactory.getLogger(SshCommandValidator.class);

    private boolean sshChecked = false;
    private boolean sshAvailable = false;

    private boolean sshpassChecked = false;
    private boolean sshpassAvailable = false;

    private boolean scpChecked = false;
    private boolean scpAvailable = false;

    /**
     * 验证SSH命令是否可用
     * 
     * @throws SshDependencyException 如果SSH命令不可用
     */
    public void validateSshCommand() throws SshDependencyException {
        if (!sshChecked) {
            sshAvailable = isCommandAvailable("ssh", "-V");
            sshChecked = true;
            logger.info("SSH command availability checked: {}", sshAvailable);
        }

        if (!sshAvailable) {
            throw new SshDependencyException("SSH命令未安装，请安装SSH客户端（如OpenSSH）后重试");
        }
    }

    /**
     * 验证sshpass命令是否可用
     * 
     * @throws SshDependencyException 如果sshpass命令不可用
     */
    public void validateSshpassCommand() throws SshDependencyException {
        if (!sshpassChecked) {
            sshpassAvailable = isCommandAvailable("sshpass", "-V");
            sshpassChecked = true;
            logger.info("SSHPASS command availability checked: {}", sshpassAvailable);
        }

        if (!sshpassAvailable) {
            throw new SshDependencyException("SSHPASS命令未安装，请安装sshpass后重试。" +
                    "在大多数Linux系统中，可以通过以下命令安装：\n" +
                    "Ubuntu/Debian: sudo apt-get install sshpass\n" +
                    "CentOS/RHEL: sudo yum install sshpass\n" +
                    "或者使用SSH密钥认证方式代替密码认证");
        }
    }

    /**
     * 验证SCP命令是否可用
     * 
     * @throws SshDependencyException 如果SCP命令不可用
     */
    public void validateScpCommand() throws SshDependencyException {
        if (!scpChecked) {
            scpAvailable = isCommandAvailable("scp", "-V");
            scpChecked = true;
            logger.info("SCP command availability checked: {}", scpAvailable);
        }

        if (!scpAvailable) {
            throw new SshDependencyException("SCP命令未安装，请安装SCP（通常包含在OpenSSH包中）后重试");
        }
    }

    /**
     * 检查命令是否可用
     */
    private boolean isCommandAvailable(String command, String arg) {
        try {
            Process process = new ProcessBuilder(command, arg)
                    .redirectErrorStream(true)
                    .start();

            // 读取输出（有些命令如ssh -V输出到stderr，所以需要合并流）
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(line -> logger.debug("Command check output [{}]: {}", command, line));
            }

            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            // 有些命令的帮助输出会返回非零值，所以这里我们只检查命令是否能执行，不检查返回值
            return completed;
        } catch (IOException | InterruptedException e) {
            logger.debug("Command {} is not available: {}", command, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}