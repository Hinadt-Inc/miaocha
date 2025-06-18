package com.hinadt.miaocha.application.logstash.tail;

import com.hinadt.miaocha.common.ssh.SshClientUtil;
import com.hinadt.miaocha.common.ssh.SshConfig;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.concurrent.*;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.Channel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** SSH流式命令执行器（现代化版本） 使用线程池和现代化的资源管理，支持流式命令的安全执行 */
@Slf4j
@Component
public class SshStreamExecutor {

    /** 用于处理SSH流式输出的线程池 */
    private final ExecutorService streamProcessorPool;

    /** 用于管理SSH连接的线程池 */
    private final ScheduledExecutorService connectionManagerPool;

    /** SSH客户端实例（Apache MINA SSHD） */
    private final SshClient sshClient;

    public SshStreamExecutor() {
        // 创建专用的线程池
        this.streamProcessorPool =
                Executors.newCachedThreadPool(
                        r -> {
                            Thread thread = new Thread(r);
                            thread.setName("ssh-stream-processor-" + thread.getId());
                            thread.setDaemon(true);
                            return thread;
                        });

        this.connectionManagerPool =
                Executors.newScheduledThreadPool(
                        2,
                        r -> {
                            Thread thread = new Thread(r);
                            thread.setName("ssh-connection-manager-" + thread.getId());
                            thread.setDaemon(true);
                            return thread;
                        });

        // 初始化SSH客户端
        this.sshClient = SshClient.setUpDefaultClient();
        this.sshClient.start();

        log.info(
                "SSH流式命令执行器已初始化，线程池大小: stream={}, connection=2",
                Runtime.getRuntime().availableProcessors());
    }

    /**
     * 执行流式命令（如tail -f） 使用现代化的线程池和资源管理
     *
     * @param sshConfig SSH连接配置
     * @param command 要执行的命令
     * @param outputConsumer 输出行处理器
     * @param errorConsumer 错误行处理器
     * @return 命令执行任务，可用于停止命令
     */
    public StreamCommandTask executeStreamCommand(
            SshConfig sshConfig,
            String command,
            Consumer<String> outputConsumer,
            Consumer<String> errorConsumer) {
        log.debug("开始执行流式命令: {}", command);

        CompletableFuture<StreamCommandTask> taskFuture =
                CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return createStreamTask(
                                        sshConfig, command, outputConsumer, errorConsumer);
                            } catch (Exception e) {
                                log.error("创建流式命令任务失败: {}", command, e);
                                errorConsumer.accept("创建任务失败: " + e.getMessage());
                                throw new RuntimeException("创建流式命令任务失败", e);
                            }
                        },
                        connectionManagerPool);

        try {
            // 等待任务创建完成，最多等待30秒
            return taskFuture.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("创建流式命令任务超时: {}", command);
            errorConsumer.accept("创建任务超时");
            throw new RuntimeException("创建流式命令任务超时", e);
        } catch (Exception e) {
            log.error("执行流式命令失败: {}", command, e);
            errorConsumer.accept("执行命令失败: " + e.getMessage());
            throw new RuntimeException("执行流式命令失败", e);
        }
    }

    /** 创建流式命令任务 */
    private StreamCommandTask createStreamTask(
            SshConfig sshConfig,
            String command,
            Consumer<String> outputConsumer,
            Consumer<String> errorConsumer)
            throws Exception {
        // 创建SSH会话
        ClientSession session = createSession(sshConfig);

        // 创建命令通道
        ClientChannel channel = session.createChannel(Channel.CHANNEL_EXEC, command);

        // 打开通道
        channel.open().verify(sshConfig.getConnectTimeout(), TimeUnit.SECONDS);

        // 获取输入输出流（必须在通道打开后获取）
        InputStream outputStream = channel.getInvertedOut();
        InputStream errorStream = channel.getInvertedErr();

        // 创建任务对象
        StreamCommandTask task = new StreamCommandTask(session, channel);

        // 启动输出读取任务
        Future<?> outputFuture =
                streamProcessorPool.submit(
                        () -> {
                            processOutputStream(outputStream, outputConsumer, task, "OUTPUT");
                        });

        // 启动错误读取任务
        Future<?> errorFuture =
                streamProcessorPool.submit(
                        () -> {
                            processOutputStream(errorStream, errorConsumer, task, "ERROR");
                        });

        // 设置Future到任务中，用于停止时取消
        task.setOutputFuture(outputFuture);
        task.setErrorFuture(errorFuture);

        log.debug("流式命令任务已创建: {}", command);
        return task;
    }

    /** 处理输出流 */
    private void processOutputStream(
            InputStream inputStream,
            Consumer<String> consumer,
            StreamCommandTask task,
            String streamType) {
        log.debug("开始处理{}流", streamType);
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            int lineCount = 0;
            while (!task.isStopped()
                    && !Thread.currentThread().isInterrupted()
                    && (line = reader.readLine()) != null) {
                lineCount++;
                log.debug("{}流读取到第{}行: {}", streamType, lineCount, line);
                consumer.accept(line);
            }

            log.debug("{}流处理完成，共读取{}行", streamType, lineCount);

        } catch (IOException e) {
            if (!task.isStopped() && !Thread.currentThread().isInterrupted()) {
                log.error("读取{}流时发生错误", streamType, e);
                consumer.accept(String.format("[%s] 读取流时发生错误: %s", streamType, e.getMessage()));
            } else {
                log.debug("{}流处理被停止或中断", streamType);
            }
        }
    }

    /** 创建SSH会话 */
    private ClientSession createSession(SshConfig config) throws Exception {
        try {
            // 连接到服务器
            ClientSession session =
                    sshClient
                            .connect(config.getUsername(), config.getHost(), config.getPort())
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
                throw new RuntimeException("未提供认证信息，无法连接SSH服务器");
            }

            // 尝试认证
            session.auth().verify(config.getConnectTimeout(), TimeUnit.SECONDS);

            return session;
        } catch (Exception e) {
            throw new RuntimeException("SSH连接或认证失败: " + e.getMessage(), e);
        }
    }

    /** 获取线程池状态信息 */
    public String getPoolStatus() {
        if (streamProcessorPool instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) streamProcessorPool;
            return String.format(
                    "StreamProcessor - 活跃线程: %d, 队列任务: %d, 完成任务: %d",
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    executor.getCompletedTaskCount());
        }
        return "StreamProcessor - 状态未知";
    }

    /** 应用关闭时清理资源 */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭SSH流式命令执行器...");

        // 关闭线程池
        shutdownExecutor(streamProcessorPool, "StreamProcessor");
        shutdownExecutor(connectionManagerPool, "ConnectionManager");

        // 关闭SSH客户端
        if (sshClient != null) {
            try {
                sshClient.stop();
                log.info("SSH客户端已关闭");
            } catch (Exception e) {
                log.warn("关闭SSH客户端时发生错误", e);
            }
        }

        log.info("SSH流式命令执行器已关闭");
    }

    /** 优雅关闭线程池 */
    private void shutdownExecutor(ExecutorService executor, String name) {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("{}线程池未能在5秒内优雅关闭，强制关闭", name);
                executor.shutdownNow();
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.error("{}线程池强制关闭失败", name);
                }
            } else {
                log.info("{}线程池已优雅关闭", name);
            }
        } catch (InterruptedException e) {
            log.warn("关闭{}线程池时被中断", name);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
