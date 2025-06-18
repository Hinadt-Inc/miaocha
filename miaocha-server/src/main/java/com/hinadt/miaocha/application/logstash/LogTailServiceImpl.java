package com.hinadt.miaocha.application.logstash;

import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.application.logstash.path.LogstashPathUtils;
import com.hinadt.miaocha.application.logstash.tail.SshStreamExecutor;
import com.hinadt.miaocha.application.logstash.tail.StreamCommandTask;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.ssh.SshConfig;
import com.hinadt.miaocha.domain.dto.logstash.LogTailResponseDTO;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.entity.enums.LogTailResponseStatus;
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
    private static final int MAX_BATCH_SIZE = 50;

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
        String logFilePath = LogstashPathUtils.buildLogFilePath(deployPath, logstashMachineId);

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

        // 检查是否存在跟踪任务
        LogTailTask task = activeTasks.get(logstashMachineId);
        if (task == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Logstash实例[" + logstashMachineId + "]的日志跟踪任务不存在，请先创建任务");
        }

        // 如果已经有SSE流在运行，关闭旧的
        if (task.getEmitter() != null) {
            try {
                task.getEmitter().complete();
            } catch (Exception e) {
                log.warn("关闭旧的SSE流时发生错误", e);
            }
        }

        // 创建新的SSE发射器
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30分钟超时
        task.setEmitter(emitter);

        // 设置SSE回调
        emitter.onCompletion(
                () -> {
                    log.info("SSE连接完成: logstashMachineId={}", logstashMachineId);
                    task.setEmitter(null);
                });

        emitter.onTimeout(
                () -> {
                    log.warn("SSE连接超时: logstashMachineId={}", logstashMachineId);
                    task.setEmitter(null);
                });

        emitter.onError(
                (throwable) -> {
                    log.error("SSE连接发生错误: logstashMachineId={}", logstashMachineId, throwable);
                    task.setEmitter(null);
                });

        // 启动SSH流式命令（如果还没启动）
        if (task.getStreamTask() == null) {
            try {
                log.info(
                        "准备启动SSH流式命令，实例ID: {}, 命令: {}, SSH配置: {}:{}@{}",
                        logstashMachineId,
                        task.getTailCommand(),
                        task.getSshConfig().getUsername(),
                        task.getSshConfig().getPort(),
                        task.getSshConfig().getHost());

                StreamCommandTask streamTask =
                        sshStreamExecutor.executeStreamCommand(
                                task.getSshConfig(),
                                task.getTailCommand(),
                                line -> {
                                    log.debug("接收到日志行，实例ID: {}, 内容: {}", logstashMachineId, line);
                                    task.addLogLine(line);
                                },
                                error -> {
                                    log.error("SSH流错误，实例ID: {}, 错误: {}", logstashMachineId, error);
                                    task.addLogLine("[ERROR] " + error);
                                });

                task.setStreamTask(streamTask);
                log.info("SSH流式命令已启动，实例ID: {}", logstashMachineId);

                // 启动批量发送任务
                task.setBatchSendFuture(
                        scheduler.scheduleAtFixedRate(
                                () -> sendBatchData(task),
                                BATCH_SEND_INTERVAL,
                                BATCH_SEND_INTERVAL,
                                TimeUnit.SECONDS));

                // 启动心跳任务
                task.setHeartbeatFuture(
                        scheduler.scheduleAtFixedRate(
                                () -> sendHeartbeat(task), 30, 30, TimeUnit.SECONDS));

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
            log.info("停止日志跟踪任务[{}]", logstashMachineId);

            // 先停止任务，等待资源完全清理
            task.stop();

            // 等待一小段时间确保所有异步操作完成
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 最后移除任务
            activeTasks.remove(logstashMachineId);
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
        // 检查任务是否仍然活跃
        if (!activeTasks.containsKey(task.getLogstashMachineId())) {
            return; // 任务已被移除，跳过发送
        }

        SseEmitter emitter = task.getEmitter();
        if (emitter == null) {
            return; // 没有活跃的SSE连接，跳过发送
        }

        List<String> batch = task.getBatchLogLines();
        if (!batch.isEmpty()) {
            log.debug("准备发送批量日志数据，实例ID: {}, 行数: {}", task.getLogstashMachineId(), batch.size());
            try {
                LogTailResponseDTO response =
                        LogTailResponseDTO.builder()
                                .logstashMachineId(task.getLogstashMachineId())
                                .logLines(batch)
                                .timestamp(LocalDateTime.now())
                                .status(LogTailResponseStatus.CONNECTED)
                                .build();

                emitter.send(SseEmitter.event().name("log-data").data(response));
                log.debug("成功发送批量日志数据，实例ID: {}, 行数: {}", task.getLogstashMachineId(), batch.size());

            } catch (IOException e) {
                log.error("发送日志数据失败，停止任务[{}]", task.getLogstashMachineId(), e);
                task.setEmitter(null);
            } catch (Exception e) {
                log.error("发送日志数据时发生未知错误，任务[{}]", task.getLogstashMachineId(), e);
                task.setEmitter(null);
            }
        } else {
            log.trace("缓冲区为空，跳过发送，实例ID: {}", task.getLogstashMachineId());
        }
    }

    /** 发送心跳包，用于保持连接活跃 */
    private void sendHeartbeat(LogTailTask task) {
        // 检查任务是否仍然活跃
        if (!activeTasks.containsKey(task.getLogstashMachineId())) {
            return; // 任务已被移除，跳过心跳
        }

        SseEmitter emitter = task.getEmitter();
        if (emitter == null) {
            return; // 没有活跃的SSE连接，跳过心跳
        }

        try {
            LogTailResponseDTO heartbeat =
                    LogTailResponseDTO.builder()
                            .logstashMachineId(task.getLogstashMachineId())
                            .logLines(new ArrayList<>()) // 心跳包设置空的logLines列表
                            .timestamp(LocalDateTime.now())
                            .status(LogTailResponseStatus.HEARTBEAT)
                            .build();

            emitter.send(SseEmitter.event().name("heartbeat").data(heartbeat));
            log.debug("发送心跳包给任务[{}]", task.getLogstashMachineId());

        } catch (IOException e) {
            log.error("发送心跳包失败，停止任务[{}]", task.getLogstashMachineId(), e);
            task.setEmitter(null);
        } catch (Exception e) {
            log.error("发送心跳包时发生未知错误，任务[{}]", task.getLogstashMachineId(), e);
            task.setEmitter(null);
        }
    }

    /** 日志跟踪任务内部类 */
    private static class LogTailTask {
        private final Long logstashMachineId;
        private SseEmitter emitter;
        private final List<String> logBuffer = new ArrayList<>();

        private StreamCommandTask streamTask;
        private java.util.concurrent.ScheduledFuture<?> batchSendFuture;
        private java.util.concurrent.ScheduledFuture<?> heartbeatFuture;

        private SshConfig sshConfig;
        private String tailCommand;
        private String logFilePath;

        public LogTailTask(Long logstashMachineId, SseEmitter emitter) {
            this.logstashMachineId = logstashMachineId;
            this.emitter = emitter;
        }

        /** 添加日志行到缓冲区 */
        public synchronized void addLogLine(String line) {
            logBuffer.add(line);
            log.trace(
                    "添加日志行到缓冲区，实例ID: {}, 当前缓冲区大小: {}, 内容: {}",
                    logstashMachineId,
                    logBuffer.size(),
                    line);

            // 如果缓冲区满了，立即发送
            if (logBuffer.size() >= MAX_BATCH_SIZE) {
                // 这里不直接发送，而是由定时任务处理，避免频繁发送
                log.debug("缓冲区已满，实例ID: {}, 大小: {}", logstashMachineId, logBuffer.size());
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
            // 标记为停止状态，防止新的操作
            SseEmitter currentEmitter = emitter;
            emitter = null; // 立即置空，防止其他线程继续使用

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

        public void setHeartbeatFuture(java.util.concurrent.ScheduledFuture<?> heartbeatFuture) {
            this.heartbeatFuture = heartbeatFuture;
        }

        public SshConfig getSshConfig() {
            return sshConfig;
        }

        public void setSshConfig(SshConfig sshConfig) {
            this.sshConfig = sshConfig;
        }

        public String getTailCommand() {
            return tailCommand;
        }

        public void setTailCommand(String tailCommand) {
            this.tailCommand = tailCommand;
        }

        public String getLogFilePath() {
            return logFilePath;
        }

        public void setLogFilePath(String logFilePath) {
            this.logFilePath = logFilePath;
        }

        public StreamCommandTask getStreamTask() {
            return streamTask;
        }

        public void setEmitter(SseEmitter emitter) {
            this.emitter = emitter;
        }
    }
}
