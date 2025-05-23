package com.hina.log.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * CompletableFuture工具类
 * 提供处理异步操作的通用方法
 */
public class FutureUtils {
    private static final Logger logger = LoggerFactory.getLogger(FutureUtils.class);

    /**
     * 将异步操作包装为同步Runnable
     * 适用于需要在TaskService.executeAsync中执行返回CompletableFuture的方法
     *
     * @param futureSupplier 返回CompletableFuture的操作
     * @param entityName 实体名称，用于日志
     * @param operationName 操作名称，用于日志
     * @param entityId 实体ID，用于日志
     * @return 包装后的Runnable
     */
    public static Runnable toSyncRunnable(
            Supplier<CompletableFuture<Boolean>> futureSupplier,
            String entityName,
            String operationName,
            Object entityId) {
        
        return () -> {
            try {
                boolean success = futureSupplier.get()
                        .exceptionally(ex -> {
                            logger.error("{} [{}] {} 异常: {}", 
                                    entityName, entityId, operationName, ex.getMessage(), ex);
                            throw new RuntimeException(operationName + "失败: " + ex.getMessage(), ex);
                        })
                        .join(); // 等待Future完成
                
                logger.info("{} [{}] {}{}", 
                        entityName, entityId, operationName, success ? "成功" : "失败");
                
                if (!success) {
                    throw new RuntimeException(String.format("%s [%s] %s失败", 
                            entityName, entityId, operationName));
                }
            } catch (Exception e) {
                logger.error("{} [{}] {}失败: {}", 
                        entityName, entityId, operationName, e.getMessage(), e);
                throw e; // 重新抛出异常，让外层executeAsync捕获
            }
        };
    }
} 