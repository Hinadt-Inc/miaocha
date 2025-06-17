package com.hinadt.miaocha.application.logstash.tail;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.session.ClientSession;

/** 流式命令任务（现代化版本） 用于管理SSH流式命令的生命周期和资源清理，支持现代化的线程池管理 */
@Slf4j
public class StreamCommandTask {

    private final ClientSession session;
    private final ClientChannel channel;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private Future<?> outputFuture;
    private Future<?> errorFuture;

    public StreamCommandTask(ClientSession session, ClientChannel channel) {
        this.session = session;
        this.channel = channel;
    }

    /** 停止命令执行并清理资源 */
    public void stop() {
        if (stopped.compareAndSet(false, true)) {
            log.debug("正在停止流式命令任务...");

            try {
                // 取消Future任务
                if (outputFuture != null && !outputFuture.isDone()) {
                    outputFuture.cancel(true);
                }
                if (errorFuture != null && !errorFuture.isDone()) {
                    errorFuture.cancel(true);
                }

                // 关闭通道
                if (channel != null && channel.isOpen()) {
                    try {
                        channel.close();
                    } catch (Exception e) {
                        log.warn("关闭通道时发生错误", e);
                    }
                }

                // 关闭SSH会话
                if (session != null && !session.isClosed()) {
                    try {
                        session.close();
                    } catch (Exception e) {
                        log.warn("关闭SSH会话时发生错误", e);
                    }
                }

                log.debug("流式命令任务已停止");

            } catch (Exception e) {
                log.error("停止流式命令任务时发生错误", e);
            }
        }
    }

    /** 检查任务是否已停止 */
    public boolean isStopped() {
        return stopped.get();
    }

    /** 设置输出处理Future */
    public void setOutputFuture(Future<?> outputFuture) {
        this.outputFuture = outputFuture;
    }

    /** 设置错误处理Future */
    public void setErrorFuture(Future<?> errorFuture) {
        this.errorFuture = errorFuture;
    }

    /** 检查命令是否仍在运行 */
    public boolean isRunning() {
        return !stopped.get() && channel != null && channel.isOpen();
    }
}
