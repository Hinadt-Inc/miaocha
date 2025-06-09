package com.hinadt.miaocha.common.util;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CompletableFuture工具类 提供处理异步操作的通用方法 */
public class FutureUtils {
    private static final Logger logger = LoggerFactory.getLogger(FutureUtils.class);

    /**
     * 将异步操作包装为同步Runnable 适用于需要在TaskService.executeAsync中执行返回CompletableFuture的方法
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
                boolean success = futureSupplier.get().join(); // 等待Future完成，异常会自动传播

                logger.info(
                        "{} [{}] {}{}", entityName, entityId, operationName, success ? "成功" : "失败");

                if (!success) {
                    throw new RuntimeException(
                            String.format("%s [%s] %s失败", entityName, entityId, operationName));
                }
            } catch (Exception e) {
                // 只在这里记录一次详细的错误日志，包含完整的异常链信息
                logger.error(
                        "{} [{}] {}失败: {}", entityName, entityId, operationName, e.getMessage(), e);
                throw e; // 重新抛出异常，让外层executeAsync捕获
            }
        };
    }
}
