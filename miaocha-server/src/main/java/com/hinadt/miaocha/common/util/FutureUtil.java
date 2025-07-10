package com.hinadt.miaocha.common.util;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.MDC;

/**
 * CompletableFuture 工具类
 *
 * <p>提供带 MDC 上下文传播的异步执行方法。
 */
public class FutureUtil {

    private static final Function<Throwable, ?> DEFAULT_ERROR_HANDLER =
            e -> {
                throw new RuntimeException(e);
            };

    private FutureUtil() {
        // 工具类不应该被实例化
    }

    /**
     * 创建带 MDC 上下文的异步任务，支持异常处理
     *
     * @param supplier 任务提供者
     * @param errorHandler 异常处理器，如果为 null 则使用默认处理器（抛出异常）
     * @param <T> 返回值类型
     * @return CompletableFuture 实例
     */
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> supplyAsync(
            Supplier<T> supplier, Function<Throwable, T> errorHandler) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        Function<Throwable, T> handler =
                errorHandler != null
                        ? errorHandler
                        : (Function<Throwable, T>) DEFAULT_ERROR_HANDLER;

        return CompletableFuture.supplyAsync(
                () -> {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    try {
                        return supplier.get();
                    } catch (Throwable e) {
                        return handler.apply(e);
                    } finally {
                        MDC.clear();
                    }
                });
    }

    /**
     * 创建带 MDC 上下文的异步任务
     *
     * @param supplier 任务提供者
     * @param <T> 返回值类型
     * @return CompletableFuture 实例
     */
    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return supplyAsync(supplier, null);
    }
}
