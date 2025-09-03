package com.hinadt.miaocha.application.logstash;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.application.logstash.path.LogstashPathUtils;
import com.hinadt.miaocha.application.logstash.tail.SshStreamExecutor;
import com.hinadt.miaocha.application.logstash.tail.StreamCommandTask;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.logstash.LogTailResponseDTO;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.enums.LogTailResponseStatus;
import com.hinadt.miaocha.infrastructure.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.infrastructure.mapper.MachineMapper;
import com.hinadt.miaocha.infrastructure.ssh.SshConfig;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Log tail service implementation - self-contained SSE streams without memory caching */
@Slf4j
@Service
public class LogTailServiceImpl implements LogTailService {

    private final LogstashMachineMapper logstashMachineMapper;
    private final MachineMapper machineMapper;
    private final LogstashDeployPathManager deployPathManager;
    private final SshStreamExecutor sshStreamExecutor;

    /** Scheduled executor for batch sending and heartbeat */
    private final ScheduledExecutorService scheduler;

    /** Current active SSE connections counter */
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    /** Maximum allowed concurrent SSE connections */
    @Value("${miaocha.log-tail.max-connections:10}")
    private int maxConnections;

    /** SSE timeout in milliseconds */
    @Value("${miaocha.log-tail.timeout:1800000}")
    private long sseTimeoutMs;

    /** Batch send interval in seconds */
    private static final int BATCH_SEND_INTERVAL = 1;

    /** Maximum batch size for log lines */
    private static final int MAX_BATCH_SIZE = 300;

    public LogTailServiceImpl(
            LogstashMachineMapper logstashMachineMapper,
            MachineMapper machineMapper,
            LogstashDeployPathManager deployPathManager,
            SshStreamExecutor sshStreamExecutor) {
        this.logstashMachineMapper = logstashMachineMapper;
        this.machineMapper = machineMapper;
        this.deployPathManager = deployPathManager;
        this.sshStreamExecutor = sshStreamExecutor;

        // Create scheduler for batch operations
        this.scheduler =
                Executors.newScheduledThreadPool(
                        5,
                        r -> {
                            Thread thread = new Thread(r);
                            thread.setName("log-tail-scheduler-" + thread.getId());
                            thread.setDaemon(true);
                            return thread;
                        });

        log.info(
                "LogTailService initialized with maxConnections={}, timeoutMs={}",
                maxConnections,
                sseTimeoutMs);
    }

    @Override
    public SseEmitter getLogStream(Long logstashMachineId, Integer tailLines) {
        log.info(
                "Creating log stream: logstashMachineId={}, tailLines={}",
                logstashMachineId,
                tailLines);

        // Check connection limit
        int currentConnections = activeConnections.get();
        if (currentConnections >= maxConnections) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "Too many active log streams. Current: "
                            + currentConnections
                            + ", Max: "
                            + maxConnections);
        }

        // Validate Logstash instance
        LogstashMachine logstashMachine = logstashMachineMapper.selectById(logstashMachineId);
        if (logstashMachine == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Logstash instance [" + logstashMachineId + "] not found");
        }

        // Validate machine info
        MachineInfo machineInfo = machineMapper.selectById(logstashMachine.getMachineId());
        if (machineInfo == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Machine for Logstash instance [" + logstashMachineId + "] not found");
        }

        // Create self-contained SSE stream
        return createSelfContainedStream(logstashMachineId, tailLines, machineInfo);
    }

    /** Create a self-contained SSE stream that manages its own lifecycle */
    private SseEmitter createSelfContainedStream(
            Long logstashMachineId, Integer tailLines, MachineInfo machineInfo) {
        // Increment connection counter
        int connectionCount = activeConnections.incrementAndGet();
        log.debug(
                "Creating SSE stream, active connections: {}/{}", connectionCount, maxConnections);

        // Create SSE emitter with timeout
        SseEmitter emitter = new SseEmitter(sseTimeoutMs);

        // Build log file path
        String deployPath = deployPathManager.getInstanceDeployPath(logstashMachineId);
        String logFilePath = LogstashPathUtils.buildLogFilePath(deployPath);
        String tailCommand = String.format("tail -n %d -f %s", tailLines, logFilePath);

        log.debug("Built tail command: {}", tailCommand);

        // Create SSH config
        SshConfig sshConfig =
                SshConfig.builder()
                        .host(machineInfo.getIp())
                        .port(machineInfo.getPort())
                        .username(machineInfo.getUsername())
                        .password(machineInfo.getPassword())
                        .privateKey(machineInfo.getSshKey())
                        .build();

        // Create self-contained stream context
        SelfContainedStreamContext context =
                new SelfContainedStreamContext(
                        logstashMachineId, emitter, logFilePath, activeConnections);

        // Setup SSE event handlers for automatic cleanup
        emitter.onCompletion(
                () -> {
                    log.debug("SSE stream completed for instance {}", logstashMachineId);
                    context.cleanup();
                });

        emitter.onTimeout(
                () -> {
                    log.debug("SSE stream timeout for instance {}", logstashMachineId);
                    context.cleanup();
                });

        emitter.onError(
                throwable -> {
                    log.warn(
                            "SSE stream error for instance {}: {}",
                            logstashMachineId,
                            throwable.getMessage());
                    context.cleanup();
                });

        // Start SSH stream asynchronously
        scheduler.execute(() -> startSshStream(context, sshConfig, tailCommand));

        return emitter;
    }

    /** Start SSH stream for the context */
    private void startSshStream(
            SelfContainedStreamContext context, SshConfig sshConfig, String tailCommand) {
        try {
            log.debug("Starting SSH stream for instance {}", context.getLogstashMachineId());

            // Send initial connection message
            sendConnectionMessage(context);

            // Create SSH stream
            StreamCommandTask streamTask =
                    sshStreamExecutor.executeStreamCommand(
                            sshConfig,
                            tailCommand,
                            line -> context.addLogLine(line),
                            error -> context.addLogLine("[ERROR] " + error));

            context.setStreamTask(streamTask);

            // Start batch sender
            context.setBatchSendFuture(
                    scheduler.scheduleAtFixedRate(
                            () -> sendBatchData(context),
                            BATCH_SEND_INTERVAL,
                            BATCH_SEND_INTERVAL,
                            TimeUnit.SECONDS));

            // Start heartbeat
            context.setHeartbeatFuture(
                    scheduler.scheduleAtFixedRate(
                            () -> sendHeartbeat(context), 30, 10, TimeUnit.SECONDS));

            log.info(
                    "SSH stream started successfully for instance {}",
                    context.getLogstashMachineId());

        } catch (Exception e) {
            log.error(
                    "Failed to start SSH stream for instance {}: {}",
                    context.getLogstashMachineId(),
                    e.getMessage(),
                    e);
            try {
                context.getEmitter().completeWithError(e);
            } catch (Exception ex) {
                log.warn("Failed to complete SSE emitter with error", ex);
            }
            context.cleanup();
        }
    }

    /** Send initial connection message */
    private void sendConnectionMessage(SelfContainedStreamContext context) {
        try {
            LogTailResponseDTO connectMsg =
                    LogTailResponseDTO.builder()
                            .logstashMachineId(context.getLogstashMachineId())
                            .logLines(
                                    List.of(
                                            "=== Started tracking log file: "
                                                    + context.getLogFilePath()
                                                    + " ==="))
                            .timestamp(LocalDateTime.now())
                            .status(LogTailResponseStatus.CONNECTED)
                            .build();

            context.getEmitter().send(SseEmitter.event().name("log-data").data(connectMsg));
        } catch (IOException e) {
            log.warn(
                    "Failed to send connection message for instance {}: {}",
                    context.getLogstashMachineId(),
                    e.getMessage());
        }
    }

    /** Send batch log data */
    private void sendBatchData(SelfContainedStreamContext context) {
        if (context.isCompleted()) {
            return;
        }

        List<String> batch = context.getBatchLogLines();
        if (!batch.isEmpty()) {
            LogTailResponseDTO response =
                    LogTailResponseDTO.builder()
                            .logstashMachineId(context.getLogstashMachineId())
                            .logLines(batch)
                            .timestamp(LocalDateTime.now())
                            .status(LogTailResponseStatus.CONNECTED)
                            .build();

            context.sendSafely(response, "log-data");
        }
    }

    /** Send heartbeat */
    private void sendHeartbeat(SelfContainedStreamContext context) {
        if (context.isCompleted()) {
            return;
        }

        LogTailResponseDTO heartbeat =
                LogTailResponseDTO.builder()
                        .logstashMachineId(context.getLogstashMachineId())
                        .logLines(new ArrayList<>())
                        .timestamp(LocalDateTime.now())
                        .status(LogTailResponseStatus.HEARTBEAT)
                        .build();

        context.sendSafely(heartbeat, "heartbeat");
    }

    /** Self-contained stream context that manages its own lifecycle */
    private static class SelfContainedStreamContext {
        private final Long logstashMachineId;
        private final SseEmitter emitter;
        private final String logFilePath;
        private final AtomicInteger globalConnectionCounter;
        private final List<String> logBuffer = new ArrayList<>();
        private final AtomicInteger completed = new AtomicInteger(0);

        private volatile StreamCommandTask streamTask;
        private volatile java.util.concurrent.ScheduledFuture<?> batchSendFuture;
        private volatile java.util.concurrent.ScheduledFuture<?> heartbeatFuture;

        public SelfContainedStreamContext(
                Long logstashMachineId,
                SseEmitter emitter,
                String logFilePath,
                AtomicInteger globalConnectionCounter) {
            this.logstashMachineId = logstashMachineId;
            this.emitter = emitter;
            this.logFilePath = logFilePath;
            this.globalConnectionCounter = globalConnectionCounter;
        }

        public synchronized void addLogLine(String line) {
            if (isCompleted()) {
                return;
            }

            logBuffer.add(line);

            // Auto-flush if buffer is full
            if (logBuffer.size() >= MAX_BATCH_SIZE) {
                sendImmediately();
            }
        }

        private void sendImmediately() {
            List<String> batch = getBatchLogLines();
            if (!batch.isEmpty()) {
                LogTailResponseDTO response =
                        LogTailResponseDTO.builder()
                                .logstashMachineId(logstashMachineId)
                                .logLines(batch)
                                .timestamp(LocalDateTime.now())
                                .status(LogTailResponseStatus.CONNECTED)
                                .build();

                sendSafely(response, "log-data");
            }
        }

        public synchronized List<String> getBatchLogLines() {
            if (logBuffer.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> batch = new ArrayList<>(logBuffer);
            logBuffer.clear();
            return batch;
        }

        public void sendSafely(LogTailResponseDTO data, String eventName) {
            if (isCompleted()) {
                return;
            }

            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                log.warn(
                        "Failed to send {} event for instance {}: {}",
                        eventName,
                        logstashMachineId,
                        e.getMessage());
                markCompleted();
            }
        }

        public void cleanup() {
            if (!markCompleted()) {
                return; // Already cleaned up
            }

            log.debug("Cleaning up stream context for instance {}", logstashMachineId);

            // Cancel scheduled tasks
            if (batchSendFuture != null) {
                batchSendFuture.cancel(true);
            }
            if (heartbeatFuture != null) {
                heartbeatFuture.cancel(true);
            }

            // Stop SSH stream
            if (streamTask != null) {
                try {
                    streamTask.stop();
                } catch (Exception e) {
                    log.warn(
                            "Error stopping SSH stream for instance {}: {}",
                            logstashMachineId,
                            e.getMessage());
                }
            }

            // Clear log buffer
            synchronized (this) {
                logBuffer.clear();
            }

            // Decrement global counter
            int remainingConnections = globalConnectionCounter.decrementAndGet();
            log.debug(
                    "Stream cleaned up for instance {}, remaining connections: {}",
                    logstashMachineId,
                    remainingConnections);
        }

        private boolean markCompleted() {
            return completed.compareAndSet(0, 1);
        }

        public boolean isCompleted() {
            return completed.get() == 1;
        }

        // Getters and setters
        public Long getLogstashMachineId() {
            return logstashMachineId;
        }

        public SseEmitter getEmitter() {
            return emitter;
        }

        public String getLogFilePath() {
            return logFilePath;
        }

        public void setStreamTask(StreamCommandTask streamTask) {
            this.streamTask = streamTask;
        }

        public void setBatchSendFuture(java.util.concurrent.ScheduledFuture<?> future) {
            this.batchSendFuture = future;
        }

        public void setHeartbeatFuture(java.util.concurrent.ScheduledFuture<?> future) {
            this.heartbeatFuture = future;
        }
    }
}
