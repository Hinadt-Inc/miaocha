package com.hinadt.miaocha.common.ssh;

import com.hinadt.miaocha.common.exception.SshException;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.security.Security;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** SSH客户端，提供基础SSH操作 使用Apache MINA SSHD库实现 */
@Component
public class SshClient {

    private static final Logger logger = LoggerFactory.getLogger(SshClient.class);

    static {
        // 注册Bouncy Castle安全提供者，以支持更多加密算法
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            logger.info("已注册BouncyCastle安全提供者");
        }
    }

    /**
     * 从Machine实体创建SSH配置
     *
     * @param machineInfo 机器信息
     * @return SSH配置
     */
    public static SshConfig createConfig(MachineInfo machineInfo) {
        return SshConfig.builder()
                .host(machineInfo.getIp())
                .port(machineInfo.getPort())
                .username(machineInfo.getUsername())
                .password(machineInfo.getPassword())
                .privateKey(machineInfo.getSshKey())
                .build();
    }

    /**
     * 测试SSH连接
     *
     * @param machineInfo 机器信息
     * @return 是否连接成功
     */
    public boolean testConnection(MachineInfo machineInfo) {
        try {
            SshConfig config = createConfig(machineInfo);
            executeCommand(config, "echo 'Connection test successful'");
            return true;
        } catch (Exception e) {
            logger.error("测试SSH连接到 {} 失败: {}", machineInfo.getIp(), e.getMessage());
            return false;
        }
    }

    /**
     * 执行SSH命令
     *
     * @param machineInfo 机器信息
     * @param command 要执行的命令
     * @return 命令执行结果
     * @throws SshException 如果执行失败
     */
    public String executeCommand(MachineInfo machineInfo, String command) throws SshException {
        try {
            SshConfig config = createConfig(machineInfo);
            return executeCommand(config, command);
        } catch (Exception e) {
            logger.error("在 {} 上执行命令失败: {}", machineInfo.getIp(), e.getMessage());
            throw new SshException("命令执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行SSH命令
     *
     * @param config SSH配置
     * @param command 要执行的命令
     * @return 命令执行结果
     * @throws SshException 如果执行失败
     */
    public String executeCommand(SshConfig config, String command) throws SshException {
        logger.debug("执行SSH命令: {}", command);

        // 创建SSHD客户端
        org.apache.sshd.client.SshClient sshd =
                org.apache.sshd.client.SshClient.setUpDefaultClient();
        try {
            sshd.start();

            // 连接到服务器并创建会话
            try (ClientSession session = createSession(sshd, config)) {
                // 创建命令通道
                ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

                try (ClientChannel channel = session.createChannel(Channel.CHANNEL_EXEC, command)) {
                    channel.setOut(responseStream);
                    channel.setErr(errorStream);

                    // 打开通道并等待完成
                    channel.open().verify(config.getConnectTimeout(), TimeUnit.SECONDS);

                    // 等待命令执行完成
                    channel.waitFor(
                            EnumSet.of(ClientChannelEvent.CLOSED),
                            TimeUnit.MINUTES.toMillis(config.getCommandTimeout()));

                    // 检查退出状态
                    Integer exitStatus = channel.getExitStatus();
                    if (exitStatus != null && exitStatus != 0) {
                        String errorMessage = errorStream.toString(StandardCharsets.UTF_8);
                        throw new SshException("命令执行失败，退出码: " + exitStatus + "\n" + errorMessage);
                    }

                    // 返回命令输出
                    return responseStream.toString(StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            throw new SshException("SSH命令执行失败: " + e.getMessage(), e);
        } finally {
            sshd.stop();
        }
    }

    /**
     * 上传文件
     *
     * @param machineInfo 机器信息
     * @param localPath 本地文件路径
     * @param remotePath 远程文件路径
     * @throws SshException 如果上传失败
     */
    public void uploadFile(MachineInfo machineInfo, String localPath, String remotePath)
            throws SshException {
        try {
            SshConfig config = createConfig(machineInfo);
            uploadFile(config, localPath, remotePath);
        } catch (Exception e) {
            logger.error("上传文件到 {} 失败: {}", machineInfo.getIp(), e.getMessage());
            throw new SshException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传文件
     *
     * @param config SSH配置
     * @param localPath 本地文件路径
     * @param remotePath 远程文件路径
     * @throws SshException 如果上传失败
     */
    public void uploadFile(SshConfig config, String localPath, String remotePath)
            throws SshException {
        logger.debug("上传文件: {} -> {}", localPath, remotePath);
        Path localFile = Paths.get(localPath);

        if (!Files.exists(localFile)) {
            throw new SshException("本地文件不存在: " + localPath);
        }

        // 创建SSHD客户端
        org.apache.sshd.client.SshClient sshd =
                org.apache.sshd.client.SshClient.setUpDefaultClient();
        try {
            sshd.start();

            // 连接到服务器并创建会话
            try (ClientSession session = createSession(sshd, config)) {
                // 使用SFTP传输文件
                if (Files.isDirectory(localFile)) {
                    uploadDirectory(session, localFile, remotePath);
                } else {
                    uploadSingleFile(session, localFile, remotePath);
                }
            }
        } catch (IOException e) {
            throw new SshException("文件上传失败: " + e.getMessage(), e);
        } finally {
            sshd.stop();
        }
    }

    /**
     * 下载文件
     *
     * @param machineInfo 机器信息
     * @param remotePath 远程文件路径
     * @param localPath 本地文件路径
     * @throws SshException 如果下载失败
     */
    public void downloadFile(MachineInfo machineInfo, String remotePath, String localPath)
            throws SshException {
        try {
            SshConfig config = createConfig(machineInfo);
            downloadFile(config, remotePath, localPath);
        } catch (Exception e) {
            logger.error("从 {} 下载文件失败: {}", machineInfo.getIp(), e.getMessage());
            throw new SshException("文件下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载文件
     *
     * @param config SSH配置
     * @param remotePath 远程文件路径
     * @param localPath 本地文件路径
     * @throws SshException 如果下载失败
     */
    public void downloadFile(SshConfig config, String remotePath, String localPath)
            throws SshException {
        logger.debug("下载文件: {} -> {}", remotePath, localPath);
        Path localFile = Paths.get(localPath);

        // 创建SSHD客户端
        org.apache.sshd.client.SshClient sshd =
                org.apache.sshd.client.SshClient.setUpDefaultClient();
        try {
            sshd.start();

            // 连接到服务器并创建会话
            try (ClientSession session = createSession(sshd, config)) {
                // 创建本地目录(如果需要)
                Path parentDir = localFile.getParent();
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }

                // 使用SFTP下载文件
                try (SftpClient sftpClient =
                        SftpClientFactory.instance().createSftpClient(session)) {
                    try (InputStream inputStream = sftpClient.read(remotePath)) {
                        Files.copy(inputStream, localFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (IOException e) {
            throw new SshException("文件下载失败: " + e.getMessage(), e);
        } finally {
            sshd.stop();
        }
    }

    /** 创建SSH会话并进行认证 */
    private ClientSession createSession(org.apache.sshd.client.SshClient sshd, SshConfig config)
            throws SshException {
        try {
            // 连接到服务器
            ClientSession session =
                    sshd.connect(config.getUsername(), config.getHost(), config.getPort())
                            .verify(config.getConnectTimeout(), TimeUnit.SECONDS)
                            .getSession();

            // 设置认证方式
            if (StringUtils.hasText(config.getPrivateKey())) {
                // 私钥认证
                KeyPair keyPair = SshClientUtil.loadPrivateKey(config.getPrivateKey());
                session.addPublicKeyIdentity(keyPair);
            } else if (StringUtils.hasText(config.getPassword())) {
                // 密码认证
                session.addPasswordIdentity(config.getPassword());
            } else {
                throw new SshException("未提供认证信息，无法连接SSH服务器");
            }

            // 尝试认证
            session.auth().verify(config.getConnectTimeout(), TimeUnit.SECONDS);

            return session;
        } catch (Exception e) {
            throw new SshException("SSH连接或认证失败: " + e.getMessage(), e);
        }
    }

    /** 上传单个文件 */
    private void uploadSingleFile(ClientSession session, Path localFile, String remotePath)
            throws IOException {
        // 判断remotePath是文件路径还是目录路径
        String finalRemotePath;
        if (remotePath.endsWith("/")) {
            // 如果以/结尾，说明是目录路径，需要拼接文件名
            String fileName = localFile.getFileName().toString();
            finalRemotePath = remotePath + fileName;
        } else {
            // 否则直接使用完整的远程路径
            finalRemotePath = remotePath;
        }

        // 确保远程目录存在
        String remoteDir = finalRemotePath.substring(0, finalRemotePath.lastIndexOf('/'));
        try (SftpClient sftpClient = SftpClientFactory.instance().createSftpClient(session)) {
            try {
                sftpClient.mkdir(remoteDir);
            } catch (IOException e) {
                // 目录可能已经存在，忽略错误
                logger.debug("创建远程目录失败(可能已存在): {}", e.getMessage());
            }
        }

        // 使用SFTP上传文件
        try (SftpClient sftpClient = SftpClientFactory.instance().createSftpClient(session);
                InputStream inputStream = Files.newInputStream(localFile);
                OutputStream outputStream =
                        sftpClient.write(
                                finalRemotePath,
                                SftpClient.OpenMode.Create,
                                SftpClient.OpenMode.Write)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        }
    }

    /** 递归上传目录 */
    private void uploadDirectory(ClientSession session, Path localDir, String remoteDir)
            throws IOException {
        try (SftpClient sftpClient = SftpClientFactory.instance().createSftpClient(session)) {
            // 确保远程目录存在
            try {
                sftpClient.mkdir(remoteDir);
            } catch (IOException e) {
                // 目录可能已经存在，忽略错误
                logger.debug("创建远程目录失败(可能已存在): {}", e.getMessage());
            }

            // 递归上传目录内容
            Files.list(localDir)
                    .forEach(
                            path -> {
                                String filename = path.getFileName().toString();
                                String remoteFilePath = remoteDir + "/" + filename;

                                try {
                                    if (Files.isDirectory(path)) {
                                        uploadDirectory(session, path, remoteFilePath);
                                    } else {
                                        uploadSingleFile(session, path, remoteFilePath);
                                    }
                                } catch (IOException e) {
                                    logger.error("上传文件失败: {}", path, e);
                                }
                            });
        }
    }
}
