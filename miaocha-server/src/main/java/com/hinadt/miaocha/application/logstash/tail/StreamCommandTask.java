package com.hinadt.miaocha.application.logstash.tail;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.session.ClientSession;

/** Stream command task - optimized for self-contained SSH stream lifecycle management */
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

    /** Stop command execution and cleanup all resources */
    public void stop() {
        if (stopped.compareAndSet(false, true)) {
            log.debug("Stopping stream command task...");

            try {
                // Cancel processing futures
                if (outputFuture != null && !outputFuture.isDone()) {
                    outputFuture.cancel(true);
                }
                if (errorFuture != null && !errorFuture.isDone()) {
                    errorFuture.cancel(true);
                }

                // Close channel
                if (channel != null && channel.isOpen()) {
                    try {
                        channel.close();
                    } catch (Exception e) {
                        log.warn("Error closing channel", e);
                    }
                }

                // Close SSH session
                if (session != null && !session.isClosed()) {
                    try {
                        session.close();
                    } catch (Exception e) {
                        log.warn("Error closing SSH session", e);
                    }
                }

                log.debug("Stream command task stopped successfully");

            } catch (Exception e) {
                log.error("Error stopping stream command task", e);
            }
        }
    }

    /** Check if task is stopped */
    public boolean isStopped() {
        return stopped.get();
    }

    /** Set output processing future for lifecycle management */
    public void setOutputFuture(Future<?> outputFuture) {
        this.outputFuture = outputFuture;
    }

    /** Set error processing future for lifecycle management */
    public void setErrorFuture(Future<?> errorFuture) {
        this.errorFuture = errorFuture;
    }

    /** Check if command is still running */
    public boolean isRunning() {
        return !stopped.get() && channel != null && channel.isOpen();
    }
}
