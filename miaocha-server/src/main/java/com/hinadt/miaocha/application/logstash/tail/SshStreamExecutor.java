package com.hinadt.miaocha.application.logstash.tail;

import com.hinadt.miaocha.infrastructure.ssh.SshClientUtil;
import com.hinadt.miaocha.infrastructure.ssh.SshConfig;
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

/** SSH stream command executor - optimized for self-contained SSE streams */
@Slf4j
@Component
public class SshStreamExecutor {

    /** Thread pool for SSH stream processing - optimized for concurrent streams */
    private final ExecutorService streamProcessorPool;

    /** Connection manager for SSH sessions */
    private final ScheduledExecutorService connectionManagerPool;

    /** SSH client instance (Apache MINA SSHD) */
    private final SshClient sshClient;

    public SshStreamExecutor() {
        // Create optimized thread pool for concurrent streams
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

        // Initialize SSH client with optimized settings
        this.sshClient = SshClient.setUpDefaultClient();

        // Accept all server keys for internal infrastructure
        this.sshClient.setServerKeyVerifier(
                (clientSession, remoteAddress, serverKey) -> {
                    log.debug(
                            "Accepting server key: {}@{}", serverKey.getAlgorithm(), remoteAddress);
                    return true;
                });

        this.sshClient.start();

        log.info("SSH stream executor initialized - optimized for self-contained streams");
    }

    /**
     * Execute streaming command (e.g., tail -f) - optimized for self-contained streams Creates
     * independent SSH connection for each stream
     *
     * @param sshConfig SSH connection configuration
     * @param command Command to execute
     * @param outputConsumer Output line handler
     * @param errorConsumer Error line handler
     * @return Command execution task for lifecycle management
     */
    public StreamCommandTask executeStreamCommand(
            SshConfig sshConfig,
            String command,
            Consumer<String> outputConsumer,
            Consumer<String> errorConsumer) {
        log.debug("Executing stream command: {}", command);

        CompletableFuture<StreamCommandTask> taskFuture =
                CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return createStreamTask(
                                        sshConfig, command, outputConsumer, errorConsumer);
                            } catch (Exception e) {
                                log.error(
                                        "Failed to create stream task for command: {}", command, e);
                                errorConsumer.accept("Task creation failed: " + e.getMessage());
                                throw new RuntimeException(
                                        "Stream command task creation failed", e);
                            }
                        },
                        connectionManagerPool);

        try {
            // Wait for task creation with reasonable timeout
            return taskFuture.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Stream command task creation timeout: {}", command);
            errorConsumer.accept("Task creation timeout");
            throw new RuntimeException("Stream command task creation timeout", e);
        } catch (Exception e) {
            log.error("Failed to execute stream command: {}", command, e);
            errorConsumer.accept("Command execution failed: " + e.getMessage());
            throw new RuntimeException("Stream command execution failed", e);
        }
    }

    /** Create optimized stream task for independent SSH connection */
    private StreamCommandTask createStreamTask(
            SshConfig sshConfig,
            String command,
            Consumer<String> outputConsumer,
            Consumer<String> errorConsumer)
            throws Exception {
        // Create dedicated SSH session for this stream
        ClientSession session = createSession(sshConfig);

        // Create command channel
        ClientChannel channel = session.createChannel(Channel.CHANNEL_EXEC, command);

        // Open channel with timeout
        channel.open().verify(sshConfig.getConnectTimeout(), TimeUnit.SECONDS);

        // Get streams after channel is opened
        InputStream outputStream = channel.getInvertedOut();
        InputStream errorStream = channel.getInvertedErr();

        // Create task object for lifecycle management
        StreamCommandTask task = new StreamCommandTask(session, channel);

        // Start stream processing asynchronously
        Future<?> outputFuture =
                streamProcessorPool.submit(
                        () -> processOutputStream(outputStream, outputConsumer, task, "OUTPUT"));

        Future<?> errorFuture =
                streamProcessorPool.submit(
                        () -> processOutputStream(errorStream, errorConsumer, task, "ERROR"));

        // Set futures for cleanup
        task.setOutputFuture(outputFuture);
        task.setErrorFuture(errorFuture);

        log.debug("Stream command task created: {}", command);
        return task;
    }

    /** Process output stream with optimized buffering */
    private void processOutputStream(
            InputStream inputStream,
            Consumer<String> consumer,
            StreamCommandTask task,
            String streamType) {
        log.debug("Starting {} stream processing", streamType);
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            int lineCount = 0;
            while (!task.isStopped()
                    && !Thread.currentThread().isInterrupted()
                    && (line = reader.readLine()) != null) {
                lineCount++;
                consumer.accept(line);
            }

            log.debug("{} stream processing completed, {} lines processed", streamType, lineCount);

        } catch (IOException e) {
            if (!task.isStopped() && !Thread.currentThread().isInterrupted()) {
                log.error("Error reading {} stream", streamType, e);
                consumer.accept(
                        String.format("[%s] Stream read error: %s", streamType, e.getMessage()));
            } else {
                log.debug("{} stream processing stopped or interrupted", streamType);
            }
        }
    }

    /** Create optimized SSH session */
    private ClientSession createSession(SshConfig config) throws Exception {
        try {
            // Connect to server with timeout
            ClientSession session =
                    sshClient
                            .connect(config.getUsername(), config.getHost(), config.getPort())
                            .verify(config.getConnectTimeout(), TimeUnit.SECONDS)
                            .getSession();

            // Configure authentication
            if (StringUtils.hasText(config.getPrivateKey())) {
                // Private key authentication
                KeyPair keyPair = SshClientUtil.loadPrivateKey(config.getPrivateKey());
                session.addPublicKeyIdentity(keyPair);
            } else if (StringUtils.hasText(config.getPassword())) {
                // Password authentication
                session.addPasswordIdentity(config.getPassword());
            } else {
                throw new RuntimeException("No authentication provided for SSH connection");
            }

            // Authenticate with timeout
            session.auth().verify(config.getConnectTimeout(), TimeUnit.SECONDS);

            return session;
        } catch (Exception e) {
            throw new RuntimeException(
                    "SSH connection or authentication failed: " + e.getMessage(), e);
        }
    }

    /** Application shutdown cleanup */
    @PreDestroy
    public void destroy() {
        log.info("Shutting down SSH stream executor...");

        // Graceful shutdown of thread pools
        shutdownExecutor(streamProcessorPool, "StreamProcessor");
        shutdownExecutor(connectionManagerPool, "ConnectionManager");

        // Close SSH client
        if (sshClient != null) {
            try {
                sshClient.stop();
                log.info("SSH client closed");
            } catch (Exception e) {
                log.warn("Error closing SSH client", e);
            }
        }

        log.info("SSH stream executor shutdown complete");
    }

    /** Graceful thread pool shutdown */
    private void shutdownExecutor(ExecutorService executor, String name) {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn(
                        "{} thread pool did not shutdown gracefully in 5 seconds, forcing shutdown",
                        name);
                executor.shutdownNow();
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.error("{} thread pool forced shutdown failed", name);
                }
            } else {
                log.info("{} thread pool shutdown gracefully", name);
            }
        } catch (InterruptedException e) {
            log.warn("{} thread pool shutdown interrupted", name);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
