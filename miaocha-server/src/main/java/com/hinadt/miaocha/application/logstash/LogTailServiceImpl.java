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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 日志跟踪服务实现 */
@Slf4j
@Service
public class LogTailServiceImpl implements LogTailService {

    private final LogstashMachineMapper logstashMachineMapper;
    private final MachineMapper machineMapper;
    private final LogstashDeployPathManager deployPathManager;
    private final SshStreamExecutor sshStreamExecutor;

    /** 活跃的跟踪任务 */
    private final ConcurrentHashMap<Long, LogTailTask> activeTasks = new ConcurrentHashMap<>();

    /** 定时任务调度器 */
    private final ScheduledExecutorService scheduler;

    /** 批量发送间隔(秒) */
    private static final int BATCH_SEND_INTERVAL = 1;

    /** 单次发送的最大行数 */
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

        // 创建简单的调度器
        this.scheduler =
                Executors.newScheduledThreadPool(
                        5,
                        r -> {
                            Thread thread = new Thread(r);
                            thread.setName("log-tail-scheduler-" + thread.getId());
                            thread.setDaemon(true);
                            return thread;
                        });

        log.info("LogTailService已初始化");
    }

    @Override
    public void createTailing(Long logstashMachineId, Integer tailLines) {
        log.info("创建日志跟踪任务: logstashMachineId={}, tailLines={}", logstashMachineId, tailLines);

        // 检查Logstash实例是否存在
        LogstashMachine logstashMachine = logstashMachineMapper.selectById(logstashMachineId);
        if (logstashMachine == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND, "Logstash实例[" + logstashMachineId + "]不存在");
        }

        // 检查关联的机器是否存在
        MachineInfo machineInfo = machineMapper.selectById(logstashMachine.getMachineId());
        if (machineInfo == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND, "Logstash实例[" + logstashMachineId + "]关联的机器不存在");
        }

        // 如果已存在任务，先停止旧任务
        LogTailTask existingTask = activeTasks.get(logstashMachineId);
        if (existingTask != null) {
            log.info("Logstash实例[{}]的日志跟踪任务已存在，先停止旧任务", logstashMachineId);
            stopTailing(logstashMachineId);
            // 稍等片刻让旧任务完全清理
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 构建日志文件路径
        String deployPath = deployPathManager.getInstanceDeployPath(logstashMachineId);
        String logFilePath = LogstashPathUtils.buildLogFilePath(deployPath);

        // 简化命令，测试基本的tail功能
        String tailCommand = String.format("tail -n %d -f %s", tailLines, logFilePath);

        log.info("构建的tail命令: {}", tailCommand);

        // 创建SSH配置
        SshConfig sshConfig =
                SshConfig.builder()
                        .host(machineInfo.getIp())
                        .port(machineInfo.getPort())
                        .username(machineInfo.getUsername())
                        .password(machineInfo.getPassword())
                        .privateKey(machineInfo.getSshKey())
                        .build();

        // 创建任务对象（不创建SSE发射器）
        LogTailTask task = new LogTailTask(logstashMachineId, null);
        task.setSshConfig(sshConfig);
        task.setTailCommand(tailCommand);
        task.setLogFilePath(logFilePath);

        activeTasks.put(logstashMachineId, task);
        log.info("日志跟踪任务[{}]创建成功", logstashMachineId);
    }

    @Override
    public SseEmitter getLogStream(Long logstashMachineId) {
        log.info("获取日志流: logstashMachineId={}", logstashMachineId);

        // 检查是否存在跟踪任务，如果不存在则自动创建
        LogTailTask task = activeTasks.get(logstashMachineId);
        if (task == null) {
            log.info("Logstash实例[{}]的日志跟踪任务不存在，自动创建任务", logstashMachineId);
            // 使用默认参数自动创建任务
            createTailing(logstashMachineId, 500); // 默认读取最后500行
            task = activeTasks.get(logstashMachineId);
            if (task == null) {
                throw new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "自动创建Logstash实例[" + logstashMachineId + "]的日志跟踪任务失败");
            }
        }

        // 创建新的SSE发射器
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30分钟超时

        // 使用AtomicReference的原子操作，如果已经有SSE流在运行，关闭旧的
        SseEmitter oldEmitter = task.swapEmitter(emitter);
        if (oldEmitter != null) {
            try {
                oldEmitter.complete();
            } catch (Exception e) {
                log.warn("关闭旧的SSE流时发生错误", e);
            }
        }

        // 启动SSH流式命令（如果还没启动）
        if (task.getStreamTask() == null) {
            try {
                log.debug(
                        "准备启动SSH流式命令，实例ID: {}, 命令: {}, SSH配置: {}:{}@{}",
                        logstashMachineId,
                        task.getTailCommand(),
                        task.getSshConfig().getUsername(),
                        task.getSshConfig().getPort(),
                        task.getSshConfig().getHost());

                // Create final references for lambda expressions
                final Long finalLogstashMachineId = logstashMachineId;
                final LogTailTask finalTask = task;

                StreamCommandTask streamTask =
                        sshStreamExecutor.executeStreamCommand(
                                task.getSshConfig(),
                                task.getTailCommand(),
                                line -> {
                                    log.trace(
                                            "接收到日志行，实例ID: {}, 内容: {}",
                                            finalLogstashMachineId,
                                            line);
                                    finalTask.addLogLine(line);
                                },
                                error -> {
                                    log.warn(
                                            "SSH流错误，实例ID: {}, 错误: {}",
                                            finalLogstashMachineId,
                                            error);
                                    finalTask.addLogLine("[ERROR] " + error);
                                });

                task.setStreamTask(streamTask);
                log.info("SSH流式命令已启动，实例ID: {}", logstashMachineId);

                // 启动批量发送任务
                task.setBatchSendFuture(
                        scheduler.scheduleAtFixedRate(
                                () -> sendBatchData(finalTask),
                                BATCH_SEND_INTERVAL,
                                BATCH_SEND_INTERVAL,
                                TimeUnit.SECONDS));

                // 启动心跳任务
                task.setHeartbeatFuture(
                        scheduler.scheduleAtFixedRate(
                                () -> sendHeartbeat(finalTask), 30, 10, TimeUnit.SECONDS));

            } catch (Exception e) {
                log.error("启动SSH流式命令失败，实例ID: {}, 错误: {}", logstashMachineId, e.getMessage(), e);
                activeTasks.remove(logstashMachineId);
                throw new BusinessException(
                        ErrorCode.INTERNAL_ERROR, "启动日志跟踪失败: " + e.getMessage(), e);
            }
        }

        // 发送连接成功消息
        try {
            LogTailResponseDTO connectMsg =
                    LogTailResponseDTO.builder()
                            .logstashMachineId(logstashMachineId)
                            .logLines(List.of("=== 开始跟踪日志文件: " + task.getLogFilePath() + " ==="))
                            .timestamp(LocalDateTime.now())
                            .status(LogTailResponseStatus.CONNECTED)
                            .build();

            emitter.send(SseEmitter.event().name("log-data").data(connectMsg));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "发送连接消息失败: " + e.getMessage(), e);
        }

        return emitter;
    }

    @Override
    public void stopTailing(Long logstashMachineId) {
        LogTailTask task = activeTasks.get(logstashMachineId);
        if (task != null) {
            log.debug("开始停止日志跟踪任务[{}]", logstashMachineId);

            // 先从活跃任务中移除，防止新的异步操作
            activeTasks.remove(logstashMachineId);

            // 停止任务并等待资源完全清理
            task.stop();

            // 等待更长时间确保所有异步操作完成
            try {
                Thread.sleep(200); // 增加等待时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("停止任务[{}]时等待被中断", logstashMachineId);
            }

            log.info("日志跟踪任务[{}]已停止", logstashMachineId);
        } else {
            log.debug("日志跟踪任务[{}]不存在或已停止", logstashMachineId);
        }
        // 幂等操作，不抛异常
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
        log.trace("开始批量发送数据 | 任务[{}]", task.getLogstashMachineId());

        // 检查任务是否仍然活跃
        if (!activeTasks.containsKey(task.getLogstashMachineId())) {
            log.trace("任务[{}]已移除，跳过批量发送", task.getLogstashMachineId());
            return;
        }

        List<String> batch = task.getBatchLogLines();
        if (!batch.isEmpty()) {
            log.trace("准备发送批量日志数据 | 任务[{}] | 行数: {}", task.getLogstashMachineId(), batch.size());

            LogTailResponseDTO response =
                    LogTailResponseDTO.builder()
                            .logstashMachineId(task.getLogstashMachineId())
                            .logLines(batch)
                            .timestamp(LocalDateTime.now())
                            .status(LogTailResponseStatus.CONNECTED)
                            .build();

            // 使用AtomicReference原子操作安全发送
            task.sendSafely(response, "log-data", "批量日志数据");
        } else {
            log.trace("缓冲区为空，跳过发送 | 任务[{}]", task.getLogstashMachineId());
        }
    }

    /** 发送心跳包，用于保持连接活跃 */
    private void sendHeartbeat(LogTailTask task) {
        log.trace("开始发送心跳包 | 任务[{}]", task.getLogstashMachineId());

        // 检查任务是否仍然活跃
        if (!activeTasks.containsKey(task.getLogstashMachineId())) {
            log.trace("任务[{}]已移除，跳过心跳发送", task.getLogstashMachineId());
            return;
        }

        LogTailResponseDTO heartbeat =
                LogTailResponseDTO.builder()
                        .logstashMachineId(task.getLogstashMachineId())
                        .logLines(new ArrayList<>()) // 心跳包设置空的logLines列表
                        .timestamp(LocalDateTime.now())
                        .status(LogTailResponseStatus.HEARTBEAT)
                        .build();

        // 使用AtomicReference原子操作安全发送心跳包
        task.sendSafely(heartbeat, "heartbeat", "心跳包");
    }

    /** 日志跟踪任务内部类 */
    private static class LogTailTask {
        // Getters and Setters
        @Getter private final Long logstashMachineId;
        private final AtomicReference<SseEmitter> emitter = new AtomicReference<>();
        private final List<String> logBuffer = new ArrayList<>();

        @Getter @Setter private StreamCommandTask streamTask;
        @Setter private java.util.concurrent.ScheduledFuture<?> batchSendFuture;
        @Setter private java.util.concurrent.ScheduledFuture<?> heartbeatFuture;

        @Setter @Getter private SshConfig sshConfig;
        @Setter @Getter private String tailCommand;
        @Setter @Getter private String logFilePath;

        public LogTailTask(Long logstashMachineId, SseEmitter emitter) {
            this.logstashMachineId = logstashMachineId;
            this.emitter.set(emitter);
        }

        /** 添加日志行到缓冲区 */
        public synchronized void addLogLine(String line) {
            logBuffer.add(line);
            log.trace(
                    "添加日志行到缓冲区，实例ID: {}, 当前缓冲区大小: {}, 内容: {}",
                    logstashMachineId,
                    logBuffer.size(),
                    line);

            // 如果缓冲区满了，立即发送，避免数据丢失
            if (logBuffer.size() >= MAX_BATCH_SIZE) {
                log.debug("缓冲区已满，立即发送数据，实例ID: {}, 大小: {}", logstashMachineId, logBuffer.size());
                sendImmediately();
            }
        }

        /** 立即发送缓冲区数据 */
        private void sendImmediately() {
            List<String> batch = getBatchLogLines();
            if (batch.isEmpty()) {
                return;
            }

            LogTailResponseDTO response =
                    LogTailResponseDTO.builder()
                            .logstashMachineId(logstashMachineId)
                            .logLines(batch)
                            .timestamp(LocalDateTime.now())
                            .status(LogTailResponseStatus.CONNECTED)
                            .build();

            // 使用AtomicReference原子操作安全发送
            sendSafely(response, "log-data", "立即批量日志数据");
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
            // 获取并清空emitter引用，防止新的操作
            SseEmitter currentEmitter = emitter.getAndSet(null);

            // 停止定时任务
            if (batchSendFuture != null) {
                try {
                    batchSendFuture.cancel(true);
                    batchSendFuture = null;
                } catch (Exception e) {
                    log.warn("取消批量发送任务时发生错误", e);
                }
            }

            if (heartbeatFuture != null) {
                try {
                    heartbeatFuture.cancel(true);
                    heartbeatFuture = null;
                } catch (Exception e) {
                    log.warn("取消心跳任务时发生错误", e);
                }
            }

            // 停止SSH流任务
            if (streamTask != null) {
                try {
                    streamTask.stop();
                    streamTask = null;
                } catch (Exception e) {
                    log.warn("停止SSH流任务时发生错误", e);
                }
            }

            // 最后关闭SSE连接
            if (currentEmitter != null) {
                try {
                    currentEmitter.complete();
                } catch (Exception e) {
                    log.warn("完成SSE连接时发生错误", e);
                }
            }

            // 清空缓冲区
            synchronized (this) {
                logBuffer.clear();
            }
        }

        public SseEmitter getEmitter() {
            return emitter.get();
        }

        public void setEmitter(SseEmitter emitter) {
            this.emitter.set(emitter);
        }

        /** 原子地交换emitter，返回旧的emitter */
        public SseEmitter swapEmitter(SseEmitter newEmitter) {
            return this.emitter.getAndSet(newEmitter);
        }

        /** 使用AtomicReference原子操作安全发送数据 */
        public void sendSafely(LogTailResponseDTO data, String eventName, String description) {
            SseEmitter currentEmitter = emitter.get();
            if (currentEmitter == null) {
                log.trace("任务[{}]无活跃SSE连接，跳过{}发送", logstashMachineId, description);
                return;
            }

            try {
                currentEmitter.send(SseEmitter.event().name(eventName).data(data));
                log.trace("成功发送{} | 任务[{}]", description, logstashMachineId);
            } catch (Exception e) {
                log.warn(
                        "{}发送失败 | 任务[{}] | 错误: {}", description, logstashMachineId, e.getMessage());
                // 使用compareAndSet确保只有当前emitter才会被置空，避免覆盖其他线程设置的新emitter
                emitter.compareAndSet(currentEmitter, null);
            }
        }
    }
}
