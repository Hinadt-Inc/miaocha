package com.hinadt.miaocha.application.logstash;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.application.logstash.path.LogstashPathUtils;
import com.hinadt.miaocha.application.logstash.tail.SshStreamExecutor;
import com.hinadt.miaocha.application.logstash.tail.StreamCommandTask;
import com.hinadt.miaocha.common.ssh.SshConfig;
import com.hinadt.miaocha.domain.dto.logstash.LogTailResponseDTO;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.domain.mapper.MachineMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 日志尾部跟踪服务实现 使用SSE技术实现实时日志推送，支持批量发送以提高性能 */
@Slf4j
@Service
public class LogTailServiceImpl implements LogTailService {

    private final LogstashMachineMapper logstashMachineMapper;
    private final MachineMapper machineMapper;
    private final LogstashDeployPathManager deployPathManager;
    private final SshStreamExecutor sshStreamExecutor;

    /** 活跃的日志跟踪任务 Key: logstashMachineId, Value: 任务信息 */
    private final ConcurrentHashMap<Long, LogTailTask> activeTasks = new ConcurrentHashMap<>();

    /** 定时任务执行器，用于批量发送日志数据 */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    /** 批量发送间隔（秒） */
    private static final int BATCH_SEND_INTERVAL = 2;

    /** 最大批量大小 */
    private static final int MAX_BATCH_SIZE = 100;

    public LogTailServiceImpl(
            LogstashMachineMapper logstashMachineMapper,
            MachineMapper machineMapper,
            LogstashDeployPathManager deployPathManager,
            SshStreamExecutor sshStreamExecutor) {
        this.logstashMachineMapper = logstashMachineMapper;
        this.machineMapper = machineMapper;
        this.deployPathManager = deployPathManager;
        this.sshStreamExecutor = sshStreamExecutor;
    }

    @Override
    public SseEmitter startTailing(Long logstashMachineId, Integer tailLines) {
        log.info("开始跟踪Logstash实例[{}]的日志，读取最后{}行", logstashMachineId, tailLines);

        // 检查是否已存在任务
        if (activeTasks.containsKey(logstashMachineId)) {
            throw new IllegalStateException("Logstash实例[" + logstashMachineId + "]的日志跟踪任务已存在");
        }

        // 获取Logstash机器关联信息
        LogstashMachine logstashMachine = logstashMachineMapper.selectById(logstashMachineId);
        if (logstashMachine == null) {
            throw new IllegalArgumentException("Logstash实例[" + logstashMachineId + "]不存在");
        }

        // 获取实际的机器信息
        MachineInfo machineInfo = machineMapper.selectById(logstashMachine.getMachineId());
        if (machineInfo == null) {
            throw new IllegalArgumentException("Logstash实例[" + logstashMachineId + "]关联的机器不存在");
        }

        // 创建SSE发射器
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 不设置超时

        // 构建日志文件路径
        String deployPath = deployPathManager.getInstanceDeployPath(logstashMachineId);
        String logFilePath = LogstashPathUtils.buildLogFilePath(deployPath, logstashMachineId);

        // 构建tail命令
        String tailCommand = String.format("tail -n %d -f %s", tailLines, logFilePath);

        // 创建SSH配置
        SshConfig sshConfig =
                SshConfig.builder()
                        .host(machineInfo.getIp())
                        .port(machineInfo.getPort())
                        .username(machineInfo.getUsername())
                        .password(machineInfo.getPassword())
                        .privateKey(machineInfo.getSshKey())
                        .build();

        // 创建任务对象
        LogTailTask task = new LogTailTask(logstashMachineId, emitter);
        activeTasks.put(logstashMachineId, task);

        // 设置SSE完成和超时回调
        emitter.onCompletion(
                () -> {
                    log.info("SSE连接完成，停止日志跟踪任务[{}]", logstashMachineId);
                    stopTailing(logstashMachineId);
                });

        emitter.onTimeout(
                () -> {
                    log.warn("SSE连接超时，停止日志跟踪任务[{}]", logstashMachineId);
                    stopTailing(logstashMachineId);
                });

        emitter.onError(
                (throwable) -> {
                    log.error("SSE连接发生错误，停止日志跟踪任务[{}]", logstashMachineId, throwable);
                    stopTailing(logstashMachineId);
                });

        try {
            // 启动SSH流式命令
            StreamCommandTask streamTask =
                    sshStreamExecutor.executeStreamCommand(
                            sshConfig,
                            tailCommand,
                            line -> task.addLogLine(line), // 输出处理器
                            error -> task.addLogLine("[ERROR] " + error) // 错误处理器
                            );

            task.setStreamTask(streamTask);

            // 启动批量发送任务
            task.setBatchSendFuture(
                    scheduler.scheduleAtFixedRate(
                            () -> sendBatchData(task),
                            BATCH_SEND_INTERVAL,
                            BATCH_SEND_INTERVAL,
                            TimeUnit.SECONDS));

            // 发送连接成功消息
            LogTailResponseDTO connectMsg =
                    LogTailResponseDTO.builder()
                            .logstashMachineId(logstashMachineId)
                            .logLines(List.of("=== 开始跟踪日志文件: " + logFilePath + " ==="))
                            .timestamp(LocalDateTime.now())
                            .status("CONNECTED")
                            .build();

            emitter.send(SseEmitter.event().name("log-data").data(connectMsg));

            log.info("日志跟踪任务[{}]启动成功", logstashMachineId);

        } catch (Exception e) {
            log.error("启动日志跟踪任务[{}]失败", logstashMachineId, e);
            activeTasks.remove(logstashMachineId);

            try {
                LogTailResponseDTO errorMsg =
                        LogTailResponseDTO.builder()
                                .logstashMachineId(logstashMachineId)
                                .logLines(List.of("[ERROR] 启动日志跟踪失败: " + e.getMessage()))
                                .timestamp(LocalDateTime.now())
                                .status("ERROR")
                                .build();

                emitter.send(SseEmitter.event().name("log-data").data(errorMsg));

                emitter.complete();
            } catch (IOException ioException) {
                log.error("发送错误消息失败", ioException);
            }

            throw new RuntimeException("启动日志跟踪失败", e);
        }

        return emitter;
    }

    @Override
    public void stopTailing(Long logstashMachineId) {
        LogTailTask task = activeTasks.remove(logstashMachineId);
        if (task != null) {
            log.info("停止日志跟踪任务[{}]", logstashMachineId);
            task.stop();
        }
    }

    @Override
    public void stopAllTailing() {
        log.info("停止所有日志跟踪任务，当前活跃任务数量: {}", activeTasks.size());

        List<Long> taskIds = new ArrayList<>(activeTasks.keySet());
        for (Long taskId : taskIds) {
            stopTailing(taskId);
        }
    }

    /** 批量发送日志数据 */
    private void sendBatchData(LogTailTask task) {
        List<String> batch = task.getBatchLogLines();
        if (!batch.isEmpty()) {
            try {
                LogTailResponseDTO response =
                        LogTailResponseDTO.builder()
                                .logstashMachineId(task.getLogstashMachineId())
                                .logLines(batch)
                                .timestamp(LocalDateTime.now())
                                .status("CONNECTED")
                                .build();

                task.getEmitter().send(SseEmitter.event().name("log-data").data(response));

            } catch (IOException e) {
                log.error("发送日志数据失败，停止任务[{}]", task.getLogstashMachineId(), e);
                stopTailing(task.getLogstashMachineId());
            }
        }
    }

    /** 日志跟踪任务内部类 */
    private static class LogTailTask {
        private final Long logstashMachineId;
        private final SseEmitter emitter;
        private final List<String> logBuffer = new ArrayList<>();

        private StreamCommandTask streamTask;
        private java.util.concurrent.ScheduledFuture<?> batchSendFuture;

        public LogTailTask(Long logstashMachineId, SseEmitter emitter) {
            this.logstashMachineId = logstashMachineId;
            this.emitter = emitter;
        }

        /** 添加日志行到缓冲区 */
        public synchronized void addLogLine(String line) {
            logBuffer.add(line);

            // 如果缓冲区满了，立即发送
            if (logBuffer.size() >= MAX_BATCH_SIZE) {
                // 这里不直接发送，而是由定时任务处理，避免频繁发送
            }
        }

        /** 获取批量日志数据并清空缓冲区 */
        public synchronized List<String> getBatchLogLines() {
            if (logBuffer.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> batch = new ArrayList<>(logBuffer);
            logBuffer.clear();
            return batch;
        }

        /** 停止任务并清理资源 */
        public void stop() {
            if (streamTask != null) {
                streamTask.stop();
            }

            if (batchSendFuture != null) {
                batchSendFuture.cancel(true);
            }

            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("完成SSE连接时发生错误", e);
            }
        }

        // Getters and Setters
        public Long getLogstashMachineId() {
            return logstashMachineId;
        }

        public SseEmitter getEmitter() {
            return emitter;
        }

        public void setStreamTask(StreamCommandTask streamTask) {
            this.streamTask = streamTask;
        }

        public void setBatchSendFuture(java.util.concurrent.ScheduledFuture<?> batchSendFuture) {
            this.batchSendFuture = batchSendFuture;
        }
    }
}
