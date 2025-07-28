package com.hinadt.miaocha.config.task;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * MDC 任务装饰器
 *
 * <p>用于在异步任务执行时传播 MDC (Mapped Diagnostic Context) 上下文， 确保 logId 等上下文信息能在不同线程间正确传递。
 *
 * <p>这个装饰器会在任务执行前将父线程的 MDC 上下文复制到子线程， 任务执行完成后清理子线程的 MDC 上下文，避免内存泄漏。
 *
 * <p>适用场景：
 *
 * <ul>
 *   <li>异步方法调用 (@Async)
 *   <li>线程池任务执行
 *   <li>CompletableFuture 异步处理
 * </ul>
 */
@Slf4j
public class MdcTaskDecorator implements TaskDecorator {

    /**
     * 装饰任务，确保 MDC 上下文传播
     *
     * @param runnable 原始任务
     * @return 装饰后的任务
     */
    @Override
    public Runnable decorate(Runnable runnable) {
        // 获取当前线程的 MDC 上下文副本
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                // 如果父线程有 MDC 上下文，则设置到当前子线程
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                    log.trace("MDC 上下文已传播到异步线程: {}", contextMap);
                }

                // 执行原始任务
                runnable.run();

            } finally {
                // 清理当前线程的 MDC 上下文，避免线程复用时的数据污染
                MDC.clear();
                log.trace("异步线程 MDC 上下文已清理");
            }
        };
    }
}
