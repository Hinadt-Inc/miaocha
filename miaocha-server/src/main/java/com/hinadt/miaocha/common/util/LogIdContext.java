package com.hinadt.miaocha.common.util;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * LogId 上下文管理器
 *
 * <p>负责管理整个请求生命周期中的 logId，确保在整个业务处理过程中 （包括异步和多线程环境）都能追踪到同一个 logId。
 *
 * <p>使用 SLF4J 的 MDC (Mapped Diagnostic Context) 实现， 利用 ThreadLocal 确保线程安全。
 */
public class LogIdContext {

    /** MDC 中存储 logId 的键名 */
    private static final String LOG_ID_KEY = "logId";

    /** logId 前缀分隔符 */
    private static final String LOG_ID_SEPARATOR = "-";

    /**
     * 生成新的 logId
     *
     * <p>格式：时间戳-UUID前8位，例如：1704067245123-a1b2c3d4
     *
     * @return 新生成的 logId
     */
    public static String generateLogId() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + LOG_ID_SEPARATOR + uuid;
    }

    /**
     * 设置当前线程的 logId
     *
     * @param logId 要设置的 logId
     */
    public static void setLogId(String logId) {
        if (logId != null && !logId.trim().isEmpty()) {
            MDC.put(LOG_ID_KEY, logId);
        }
    }

    /**
     * 获取当前线程的 logId
     *
     * @return 当前线程的 logId，如果不存在则返回 null
     */
    public static String getLogId() {
        return MDC.get(LOG_ID_KEY);
    }

    /**
     * 检查当前线程是否存在 logId
     *
     * @return true 如果存在 logId，否则返回 false
     */
    public static boolean hasLogId() {
        String logId = getLogId();
        return logId != null && !logId.trim().isEmpty();
    }

    /** 清除当前线程的 logId */
    public static void clear() {
        MDC.remove(LOG_ID_KEY);
    }

    /** 清除当前线程的所有 MDC 上下文 */
    public static void clearAll() {
        MDC.clear();
    }

    /**
     * 如果当前线程没有 logId，则生成并设置一个新的
     *
     * @return 当前线程的 logId（可能是新生成的，也可能是已存在的）
     */
    public static String ensureLogId() {
        String existingLogId = getLogId();
        if (existingLogId == null || existingLogId.trim().isEmpty()) {
            String newLogId = generateLogId();
            setLogId(newLogId);
            return newLogId;
        }
        return existingLogId;
    }
}
