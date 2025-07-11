package com.hinadt.miaocha.common.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * LogSearch Doris 日志数据 Mock 工具
 *
 * <p>独立的命令行工具，用于向指定的 Doris 实例生成测试日志数据
 *
 * <p>使用方法： java -cp ".:lib/*" com.hinadt.miaocha.common.tools.LogSearchDataMockTool \
 * --host=127.0.0.1 --port=9030 --user=root --password= --count=10000
 *
 * <p>功能特性： - 自动创建数据库和表结构 - 多线程生成JSON数据文件 - 使用Stream Load高性能导入 - 支持自定义数据条数
 */
public class LogSearchDataMockTool {

    // ==================== 常量定义 ====================

    /** 测试表名 */
    public static final String TEST_TABLE_NAME = "mock_log_table";

    /** 测试数据库名 */
    public static final String TEST_DATABASE_NAME = "mock_log_db";

    /** 批次大小 */
    public static final int BATCH_SIZE = 500;

    /** 线程池大小 */
    public static final int THREAD_POOL_SIZE = 16;

    /** Stream Load批次大小 */
    public static final int STREAM_LOAD_BATCH_SIZE = 10000;

    // ==================== 成员变量 ====================

    private String dorisHost;
    private int dorisPort;
    private int dorisHttpPort; // 用于Stream Load的HTTP端口，默认8040
    private String dorisUser;
    private String dorisPassword;
    private int totalRecords;
    private ObjectMapper objectMapper;

    // ==================== 预生成数据池 ====================

    private static final int UUID_POOL_SIZE = 1000;
    private static final String[] UUID_POOL = new String[UUID_POOL_SIZE];
    private static final String[] TOKEN_POOL = new String[UUID_POOL_SIZE];
    private static final String[] TRACE_ID_POOL = new String[UUID_POOL_SIZE];

    static {
        for (int i = 0; i < UUID_POOL_SIZE; i++) {
            UUID_POOL[i] = UUID.randomUUID().toString();
            TOKEN_POOL[i] =
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyXyIgKyAoaW5kZXggJSAxMDAwMCkgKyAiIiwiaWF0IjoxNjI2MzQ"
                            + String.format("%08d", i);
            TRACE_ID_POOL[i] = String.format("%032x", i * 0x123456789ABCDEFL);
        }
    }

    // ==================== 测试数据模板 ====================

    private static final String[] HOSTS = {
        "172.20.61.22", "172.20.61.18", "172.20.61.35", "172.20.61.42", "192.168.1.10",
        "10.0.1.15", "10.0.2.20", "172.16.1.100", "192.168.2.50", "10.10.10.88"
    };

    private static final String[] SOURCES = {
        "/data/log/hina-cloud/pro-hina-cloud-engine/pro-hina-cloud-engine-79b56c6d69-4j6h9/cloud-engine.log",
        "/data/log/hina-cloud/pro-hina-cloud-engine/pro-hina-cloud-engine-79b56c6d69-86b8j/cloud-engine.log",
        "/var/log/application/service.log",
        "/opt/logs/business/order-service.log",
        "/home/app/logs/user-service.log",
        "/var/log/microservice/payment-service.log",
        "/opt/apps/logs/notification-service.log",
        "/data/logs/analytics/report-service.log",
        "/var/log/gateway/api-gateway.log",
        "/opt/services/logs/auth-service.log"
    };

    private static final String[] LEVELS = {"INFO", "ERROR", "WARN", "DEBUG"};

    private static final String[] SERVICES = {
        "hina-cloud-engine", "order-service", "user-service", "payment-service",
        "notification-service", "report-service", "api-gateway", "auth-service",
        "inventory-service", "shipping-service"
    };

    // ==================== Doris建表SQL ====================

    private static final String CREATE_TABLE_SQL =
            """
        CREATE TABLE `mock_log_table` (
          `log_time` datetime(3) NOT NULL,
          `host` text NULL COMMENT "hostname or ip",
          `source` text NULL COMMENT "log path",
          `log_offset` text NULL COMMENT "日志所在kafka主题偏移量",
          `message` variant NULL,
          `message_text` text NULL,
          INDEX idx_message (`message_text`) USING INVERTED PROPERTIES("support_phrase" = "true", "parser" = "unicode", "lower_case" = "true")
        ) ENGINE=OLAP
        DUPLICATE KEY(`log_time`)
        AUTO PARTITION BY RANGE (date_trunc(`log_time`, 'hour'))
        ()
        DISTRIBUTED BY RANDOM BUCKETS 6
        PROPERTIES (
        "replication_allocation" = "tag.location.default: 1",
        "min_load_replica_num" = "-1",
        "is_being_synced" = "false",
        "dynamic_partition.enable" = "true",
        "dynamic_partition.time_unit" = "hour",
        "dynamic_partition.time_zone" = "Asia/Shanghai",
        "dynamic_partition.start" = "-1440",
        "dynamic_partition.end" = "0",
        "dynamic_partition.prefix" = "p",
        "dynamic_partition.replication_allocation" = "tag.location.default: 1",
        "dynamic_partition.buckets" = "6",
        "dynamic_partition.create_history_partition" = "false",
        "dynamic_partition.history_partition_num" = "-1",
        "dynamic_partition.hot_partition_num" = "0",
        "dynamic_partition.reserved_history_periods" = "NULL",
        "dynamic_partition.storage_policy" = "",
        "storage_medium" = "hdd",
        "storage_format" = "V2",
        "inverted_index_storage_format" = "V2",
        "compression" = "ZSTD",
        "light_schema_change" = "true",
        "disable_auto_compaction" = "false",
        "enable_single_replica_compaction" = "false",
        "group_commit_interval_ms" = "10000",
        "group_commit_data_bytes" = "134217728"
        );
        """;

    // ==================== 构造函数 ====================

    public LogSearchDataMockTool(
            String dorisHost,
            int dorisPort,
            String dorisUser,
            String dorisPassword,
            int totalRecords,
            int streamLoadPort) {
        // 参数验证
        if (dorisHost == null || dorisHost.trim().isEmpty()) {
            throw new IllegalArgumentException("Doris主机地址不能为空");
        }
        if (dorisPort <= 0 || dorisPort > 65535) {
            throw new IllegalArgumentException("Doris端口必须在1-65535之间");
        }
        if (streamLoadPort <= 0 || streamLoadPort > 65535) {
            throw new IllegalArgumentException("Stream Load端口必须在1-65535之间");
        }
        if (dorisUser == null) {
            throw new IllegalArgumentException("Doris用户名不能为null");
        }
        if (dorisPassword == null) {
            dorisPassword = ""; // 允许空密码，但不允许null
        }
        if (totalRecords <= 0) {
            throw new IllegalArgumentException("数据条数必须大于0");
        }
        if (totalRecords > 10_000_000) {
            throw new IllegalArgumentException("数据条数不能超过1000万，以防止资源耗尽");
        }

        this.dorisHost = dorisHost.trim();
        this.dorisPort = dorisPort;
        this.dorisHttpPort = streamLoadPort;
        this.dorisUser = dorisUser;
        this.dorisPassword = dorisPassword;
        this.totalRecords = totalRecords;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 主要执行方法 ====================

    /** 执行数据 Mock 流程 */
    public void execute() throws Exception {
        System.out.println("🚀 开始 LogSearch Doris 日志数据 Mock...");
        System.out.println("📋 目标: " + dorisHost + ":" + dorisPort);
        System.out.println("📊 数据条数: " + totalRecords);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 创建数据库
            createDatabase();

            // 2. 创建表
            createTable();

            // 3. 生成并导入数据
            generateAndImportData();

            long endTime = System.currentTimeMillis();
            System.out.println("✅ 数据 Mock 完成！耗时: " + (endTime - startTime) + "ms");

        } catch (Exception e) {
            System.err.println("❌ 数据 Mock 失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /** 创建数据库 */
    private void createDatabase() throws Exception {
        System.out.println("📦 创建数据库: " + TEST_DATABASE_NAME);

        String defaultJdbcUrl =
                String.format(
                        "jdbc:mysql://%s:%d/information_schema?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                        dorisHost, dorisPort);

        try (Connection connection =
                        DriverManager.getConnection(defaultJdbcUrl, dorisUser, dorisPassword);
                Statement statement = connection.createStatement()) {

            // 先尝试删除已存在的数据库
            try {
                statement.execute("DROP DATABASE IF EXISTS " + TEST_DATABASE_NAME);
                System.out.println("   删除已存在的数据库");
            } catch (Exception e) {
                // 忽略删除失败
            }

            // 创建新数据库
            String createDbSql = "CREATE DATABASE " + TEST_DATABASE_NAME;
            statement.execute(createDbSql);
            System.out.println("   成功创建数据库: " + TEST_DATABASE_NAME);
        }
    }

    /** 创建表 */
    private void createTable() throws Exception {
        System.out.println("🗄️ 创建表: " + TEST_TABLE_NAME);

        String jdbcUrl =
                String.format(
                        "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                        dorisHost, dorisPort, TEST_DATABASE_NAME);

        int maxRetries = 5;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Connection connection =
                            DriverManager.getConnection(jdbcUrl, dorisUser, dorisPassword);
                    Statement statement = connection.createStatement()) {

                // 先删除可能存在的表
                try {
                    statement.execute("DROP TABLE IF EXISTS " + TEST_TABLE_NAME);
                } catch (Exception e) {
                    // 忽略删除失败
                }

                // 创建新表
                statement.execute(CREATE_TABLE_SQL);
                System.out.println("   成功创建表: " + TEST_TABLE_NAME);
                return;

            } catch (Exception e) {
                System.err.println("   第" + attempt + "次创建表失败: " + e.getMessage());
                if (attempt == maxRetries) {
                    throw new RuntimeException("创建表失败，重试" + maxRetries + "次后仍然失败", e);
                }
                Thread.sleep(2000);
            }
        }
    }

    /** 生成并导入数据 */
    private void generateAndImportData() throws Exception {
        System.out.println("🏭 开始生成和导入 " + totalRecords + " 条测试数据...");

        // 分批导入
        int totalBatches = (int) Math.ceil((double) totalRecords / STREAM_LOAD_BATCH_SIZE);
        System.out.println("📦 将分 " + totalBatches + " 批导入，每批 " + STREAM_LOAD_BATCH_SIZE + " 条记录");

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int startIndex = batchIndex * STREAM_LOAD_BATCH_SIZE;
            int endIndex = Math.min(startIndex + STREAM_LOAD_BATCH_SIZE, totalRecords);
            int batchSize = endIndex - startIndex;

            System.out.println(
                    "📤 第 "
                            + (batchIndex + 1)
                            + "/"
                            + totalBatches
                            + " 批：导入 "
                            + batchSize
                            + " 条记录");

            // 生成当前批次的JSON数据文件
            List<String> tempFiles = generateJsonDataFilesForBatch(startIndex, endIndex);

            // 合并文件
            String mergedFile = mergeJsonFiles(tempFiles);

            // Stream Load导入
            streamLoadImportData(mergedFile, batchIndex + 1);

            // 清理临时文件
            cleanupTempFiles(tempFiles, mergedFile);

            // 批次间稍作停顿，避免对Doris造成过大压力
            if (batchIndex < totalBatches - 1) {
                System.out.println("⏳ 等待2秒后继续下一批...");
                Thread.sleep(2000);
            }
        }
    }

    /** 多线程生成JSON数据文件 */
    private List<String> generateJsonDataFiles() throws Exception {
        return generateJsonDataFilesForBatch(0, totalRecords);
    }

    /** 为指定批次生成JSON数据文件 */
    private List<String> generateJsonDataFilesForBatch(int startIndex, int endIndex)
            throws Exception {
        System.out.println("📝 生成批次数据文件：索引 " + startIndex + " 到 " + endIndex);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CompletionService<String> completionService =
                new ExecutorCompletionService<>(executorService);
        List<String> tempFiles = new ArrayList<>();

        try {
            int batchRecords = endIndex - startIndex;
            int totalBatches = (batchRecords + BATCH_SIZE - 1) / BATCH_SIZE;
            System.out.println("   分配 " + totalBatches + " 个批次，每批 " + BATCH_SIZE + " 条记录");

            // 提交所有批次任务
            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                final int currentBatch = batchIndex;
                final int batchStartIndex = startIndex + currentBatch * BATCH_SIZE;
                final int batchEndIndex = Math.min(batchStartIndex + BATCH_SIZE, endIndex);

                completionService.submit(
                        () -> generateBatchJsonFile(currentBatch, batchStartIndex, batchEndIndex));
            }

            // 等待所有任务完成
            for (int i = 0; i < totalBatches; i++) {
                Future<String> future = completionService.take();
                String tempFile = future.get();
                tempFiles.add(tempFile);

                if ((i + 1) % 5 == 0 || (i + 1) == totalBatches) {
                    System.out.println("   进度: " + (i + 1) + "/" + totalBatches + " 批次完成");
                }
            }

            return tempFiles;

        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("   线程池未能在60秒内正常关闭，强制关闭");
                    executorService.shutdownNow();
                    // 再等待一段时间确保强制关闭完成
                    if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                        System.err.println("   线程池强制关闭失败");
                    }
                }
            } catch (InterruptedException e) {
                System.err.println("   线程池关闭过程被中断");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /** 生成单个批次的JSON文件 */
    private String generateBatchJsonFile(int batchIndex, int startIndex, int endIndex) {
        String tempFileName = String.format("temp_log_data_%d.jsonl", batchIndex);
        Path tempFilePath = Paths.get(System.getProperty("java.io.tmpdir"), tempFileName);

        try (BufferedWriter writer =
                Files.newBufferedWriter(tempFilePath, StandardCharsets.UTF_8)) {
            for (int i = startIndex; i < endIndex; i++) {
                Map<String, Object> logRecord = generateSingleLogRecord(i);
                String jsonLine = objectMapper.writeValueAsString(logRecord);
                writer.write(jsonLine);
                writer.write("\n");
            }

            return tempFilePath.toString();

        } catch (Exception e) {
            throw new RuntimeException("JSON文件生成失败", e);
        }
    }

    /** 生成单条日志记录 */
    private Map<String, Object> generateSingleLogRecord(int index) throws Exception {
        LocalDateTime logTime = generateTestTime(index);

        String host = HOSTS[index % HOSTS.length];
        String source = SOURCES[index % SOURCES.length];
        String level = LEVELS[index % LEVELS.length];
        String service = SERVICES[index % SERVICES.length];
        String logId = UUID_POOL[index % UUID_POOL_SIZE];

        Map<String, Object> logRecord = new HashMap<>();
        logRecord.put(
                "log_time", logTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        logRecord.put("host", host);
        logRecord.put("source", source);
        logRecord.put("log_offset", String.valueOf(60000000L + index));

        Map<String, Object> messageObj = createMessageObject(index, service, level, logId, logTime);
        logRecord.put("message", messageObj);
        logRecord.put("message_text", objectMapper.writeValueAsString(messageObj));

        return logRecord;
    }

    /** 生成测试时间 */
    private LocalDateTime generateTestTime(int index) {
        LocalDateTime baseTime = LocalDateTime.now().minusHours(24);

        if (index < totalRecords / 5) {
            // 前20%：最近1小时
            return baseTime.plusHours(23)
                    .plusMinutes((index / 40) % 60)
                    .plusSeconds(index % 60)
                    .plusNanos((index % 1000) * 1_000_000);
        } else if (index < totalRecords / 2) {
            // 30%：最近6小时
            return baseTime.plusHours(18 + (index % 6))
                    .plusMinutes((index / 10) % 60)
                    .plusSeconds(index % 60);
        } else if (index < totalRecords * 4 / 5) {
            // 30%：12小时内
            return baseTime.plusHours(12 + (index % 12))
                    .plusMinutes(index % 60)
                    .plusSeconds((index / 2) % 60);
        } else {
            // 20%：24小时内
            return baseTime.plusHours(index % 24)
                    .plusMinutes((index * 7) % 60)
                    .plusSeconds((index * 3) % 60);
        }
    }

    /** 创建消息对象 */
    private Map<String, Object> createMessageObject(
            int index, String service, String level, String logId, LocalDateTime logTime)
            throws Exception {
        Map<String, Object> messageObj = new HashMap<>();

        // 基础字段
        messageObj.put("service", service);
        messageObj.put(
                "timestamp",
                logTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        messageObj.put("logId", logId);
        messageObj.put("level", level);
        messageObj.put("logger", "com.hinadt.miaocha.service." + service + "Service");
        messageObj.put("thread", "async-" + ((index % 100) + 1));
        messageObj.put("version", "1." + (index % 10) + "." + (index % 100));
        messageObj.put("environment", index % 4 == 0 ? "prod" : (index % 3 == 0 ? "test" : "dev"));

        // 消息内容
        messageObj.put("message", generateLogMessage(level, index));
        messageObj.put("messageId", UUID_POOL[index % UUID_POOL_SIZE]);

        // 简化的请求信息
        Map<String, Object> request = new HashMap<>();
        request.put("method", index % 5 == 0 ? "POST" : (index % 3 == 0 ? "PUT" : "GET"));
        request.put("uri", "/api/v" + (index % 3 + 1) + "/" + service.toLowerCase() + "/" + index);
        request.put("remoteAddr", "192.168." + (index % 255 + 1) + "." + ((index * 17) % 255 + 1));
        request.put("userAgent", "MockClient/1.0");
        messageObj.put("request", request);

        // 响应信息
        Map<String, Object> response = new HashMap<>();
        response.put("status", generateHttpStatus(level, index));
        response.put("responseTime", index % 5000 + 10);
        response.put("contentLength", index % 50000 + 500);
        messageObj.put("response", response);

        // 追踪信息
        messageObj.put("traceId", TRACE_ID_POOL[index % UUID_POOL_SIZE]);
        messageObj.put("spanId", Long.toHexString(index * 0x987654321L));

        return messageObj;
    }

    /** 生成HTTP状态码 */
    private Integer generateHttpStatus(String level, int index) {
        if ("ERROR".equals(level)) {
            int[] errorCodes = {500, 503, 502, 504, 400, 401, 403, 404};
            return errorCodes[index % errorCodes.length];
        } else if ("WARN".equals(level)) {
            int[] warnCodes = {200, 201, 429, 304};
            return warnCodes[index % warnCodes.length];
        } else {
            int[] successCodes = {200, 201, 202, 204};
            return successCodes[index % successCodes.length];
        }
    }

    /** 生成日志消息 */
    private String generateLogMessage(String level, int index) {
        return switch (level) {
            case "ERROR" -> "Error processing request " + index + ": " + generateErrorType(index);
            case "WARN" -> "Warning: Slow operation detected, took " + (index % 5000 + 1000) + "ms";
            case "INFO" -> "Successfully processed request " + index + " for service operation";
            case "DEBUG" -> "Debug: Method entry processRequest(id=" + index + ")";
            default -> "Test log message " + index;
        };
    }

    /** 生成错误类型 */
    private String generateErrorType(int index) {
        String[] errorTypes = {
            "NullPointerException", "IllegalArgumentException", "SQLException",
            "TimeoutException", "IOException", "ValidationException"
        };
        return errorTypes[index % errorTypes.length];
    }

    /** 合并JSON文件 */
    private String mergeJsonFiles(List<String> tempFiles) throws Exception {
        System.out.println("📝 第二阶段：合并JSON文件");

        String mergedFileName = "merged_log_data.json";
        Path mergedFilePath = Paths.get(System.getProperty("java.io.tmpdir"), mergedFileName);

        try (BufferedWriter writer =
                Files.newBufferedWriter(mergedFilePath, StandardCharsets.UTF_8)) {
            writer.write("[\n");

            boolean isFirstRecord = true;
            int totalRecords = 0;

            for (String tempFile : tempFiles) {
                try (BufferedReader reader =
                        Files.newBufferedReader(Paths.get(tempFile), StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        if (!isFirstRecord) {
                            writer.write(",\n");
                        }
                        writer.write(line);
                        isFirstRecord = false;
                        totalRecords++;
                    }
                }
            }

            writer.write("\n]");
            System.out.println("   合并完成：" + totalRecords + " 条记录");

            return mergedFilePath.toString();
        }
    }

    /** Stream Load导入数据 */
    private void streamLoadImportData(String jsonFilePath) throws Exception {
        streamLoadImportData(jsonFilePath, 1);
    }

    /** Stream Load导入数据 */
    private void streamLoadImportData(String jsonFilePath, int batchNumber) throws Exception {
        System.out.println("📤 第 " + batchNumber + " 批：Stream Load导入数据");

        String streamLoadUrl =
                String.format(
                        "http://%s:%d/api/%s/%s/_stream_load",
                        dorisHost, dorisHttpPort, TEST_DATABASE_NAME, TEST_TABLE_NAME);

        File jsonFile = new File(jsonFilePath);
        if (!jsonFile.exists()) {
            throw new RuntimeException("JSON文件不存在: " + jsonFilePath);
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(streamLoadUrl);
            connection = (HttpURLConnection) url.openConnection();

            // HTTP设置
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            connection.setConnectTimeout(60000); // 增加连接超时到60秒
            connection.setReadTimeout(600000); // 增加读取超时到10分钟

            // 认证
            String credentials = dorisUser + ":" + dorisPassword;
            String encodedCredentials =
                    Base64.getEncoder()
                            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);

            // Stream Load参数
            connection.setRequestProperty("Expect", "100-continue"); // 添加Expect头
            connection.setRequestProperty("format", "json");
            connection.setRequestProperty("strip_outer_array", "true");
            connection.setRequestProperty("fuzzy_parse", "true");
            connection.setRequestProperty("strict_mode", "false");
            connection.setRequestProperty("max_filter_ratio", "0.05");
            connection.setRequestProperty("timeout", "600"); // 增加超时到10分钟
            connection.setRequestProperty(
                    "columns", "log_time,host,source,log_offset,message,message_text");
            connection.setRequestProperty(
                    "label", "mock_data_batch_" + batchNumber + "_" + System.currentTimeMillis());

            // 发送文件
            long startTime = System.currentTimeMillis();
            try (OutputStream outputStream = connection.getOutputStream();
                    FileInputStream fileInputStream = new FileInputStream(jsonFilePath);
                    BufferedInputStream bufferedInput =
                            new BufferedInputStream(fileInputStream, 64 * 1024)) {

                byte[] buffer = new byte[128 * 1024]; // 增加缓冲区大小到128KB
                int bytesRead;
                long totalBytes = 0;

                while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                outputStream.flush();

                long transferTime = System.currentTimeMillis() - startTime;
                System.out.println(
                        "   文件传输完成，大小: "
                                + (totalBytes / (1024 * 1024))
                                + " MB, 耗时: "
                                + transferTime
                                + "ms");
            }

            // 处理响应
            int responseCode = connection.getResponseCode();
            String responseMessage = getResponseMessage(connection);

            if (responseCode == 200 || responseCode == 202) {
                System.out.println("   Stream Load导入成功！");
                parseAndLogImportResult(responseMessage);
            } else {
                System.err.println("   Stream Load导入失败，响应码：" + responseCode);
                System.err.println("   错误消息：" + responseMessage);
                throw new RuntimeException("Stream Load导入失败，响应码：" + responseCode);
            }

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** 获取HTTP响应消息 */
    private String getResponseMessage(HttpURLConnection connection) throws IOException {
        InputStream inputStream =
                connection.getResponseCode() >= 400
                        ? connection.getErrorStream()
                        : connection.getInputStream();

        if (inputStream == null) {
            return "No response message";
        }

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString();
        }
    }

    /** 解析并记录导入结果 */
    private void parseAndLogImportResult(String responseMessage) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(responseMessage, Map.class);

            Object status = result.get("Status");
            Object totalRows = result.get("NumberTotalRows");
            Object loadedRows = result.get("NumberLoadedRows");
            Object filteredRows = result.get("NumberFilteredRows");
            Object loadTimeMs = result.get("LoadTimeMs");

            System.out.println("   导入结果：");
            System.out.println("     状态: " + status);
            System.out.println("     总行数: " + totalRows);
            System.out.println("     成功导入: " + loadedRows);
            System.out.println("     过滤行数: " + filteredRows);
            System.out.println("     导入耗时: " + loadTimeMs + " ms");

            if (loadTimeMs instanceof Number && loadedRows instanceof Number) {
                double timeSeconds = ((Number) loadTimeMs).doubleValue() / 1000.0;
                double rowsPerSecond = ((Number) loadedRows).doubleValue() / timeSeconds;
                System.out.println("     导入速度: " + String.format("%.2f", rowsPerSecond) + " 行/秒");
            }

        } catch (Exception e) {
            System.out.println("   原始响应: " + responseMessage);
        }
    }

    /** 清理临时文件 */
    private void cleanupTempFiles(List<String> tempFiles, String mergedFile) {
        System.out.println("🧹 第四阶段：清理临时文件");

        int deletedCount = 0;

        for (String tempFile : tempFiles) {
            try {
                if (Files.deleteIfExists(Paths.get(tempFile))) {
                    deletedCount++;
                }
            } catch (Exception e) {
                System.err.println("   删除临时文件失败：" + tempFile);
            }
        }

        try {
            if (Files.deleteIfExists(Paths.get(mergedFile))) {
                deletedCount++;
            }
        } catch (Exception e) {
            System.err.println("   删除合并文件失败：" + mergedFile);
        }

        System.out.println("   清理完成，删除 " + deletedCount + " 个文件");
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        try {
            // 解析命令行参数
            CommandLineArgs cmdArgs = parseCommandLineArgs(args);

            // 创建工具实例并执行
            LogSearchDataMockTool tool =
                    new LogSearchDataMockTool(
                            cmdArgs.host,
                            cmdArgs.port,
                            cmdArgs.user,
                            cmdArgs.password,
                            cmdArgs.count,
                            cmdArgs.streamLoadPort);

            tool.execute();

        } catch (Exception e) {
            System.err.println("❌ 执行失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /** 解析命令行参数 */
    private static CommandLineArgs parseCommandLineArgs(String[] args) {
        CommandLineArgs cmdArgs = new CommandLineArgs();

        for (String arg : args) {
            try {
                if (arg.startsWith("--host=")) {
                    cmdArgs.host = arg.substring(7);
                } else if (arg.startsWith("--port=")) {
                    cmdArgs.port = Integer.parseInt(arg.substring(7));
                } else if (arg.startsWith("--user=")) {
                    cmdArgs.user = arg.substring(7);
                } else if (arg.startsWith("--password=")) {
                    cmdArgs.password = arg.substring(11);
                } else if (arg.startsWith("--count=")) {
                    cmdArgs.count = Integer.parseInt(arg.substring(8));
                } else if (arg.startsWith("--stream-load-port=")) {
                    cmdArgs.streamLoadPort = Integer.parseInt(arg.substring(19));
                } else if (arg.equals("--help") || arg.equals("-h")) {
                    printUsage();
                    System.exit(0);
                } else if (!arg.isEmpty()) {
                    System.err.println("未知参数: " + arg);
                    printUsage();
                    System.exit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println("数字参数解析失败: " + arg + " - " + e.getMessage());
                printUsage();
                System.exit(1);
            } catch (Exception e) {
                System.err.println("参数解析失败: " + arg + " - " + e.getMessage());
                printUsage();
                System.exit(1);
            }
        }

        // 验证必要参数
        if (cmdArgs.host == null || cmdArgs.host.isEmpty()) {
            System.err.println("错误: 必须指定 --host 参数");
            printUsage();
            System.exit(1);
        }

        return cmdArgs;
    }

    /** 打印使用说明 */
    private static void printUsage() {
        System.out.println("LogSearch Doris 日志数据 Mock 工具");
        System.out.println();
        System.out.println("用法:");
        System.out.println(
                "  java -cp \".:lib/*\" com.hinadt.miaocha.common.tools.LogSearchDataMockTool"
                        + " [选项]");
        System.out.println();
        System.out.println("选项:");
        System.out.println("  --host=HOST       Doris服务器地址 (必需)");
        System.out.println("  --port=PORT       Doris端口 (默认: 9030)");
        System.out.println("  --user=USER       Doris用户名 (默认: root)");
        System.out.println("  --password=PASS   Doris密码 (默认: 空)");
        System.out.println("  --count=COUNT     生成数据条数 (默认: 10000)");
        System.out.println("  --stream-load-port=PORT  Stream Load端口 (默认: 8040)");
        System.out.println("  --help, -h        显示此帮助信息");
        System.out.println();
        System.out.println("示例:");
        System.out.println(
                "  java -cp \".:lib/*\" com.hinadt.miaocha.common.tools.LogSearchDataMockTool \\");
        System.out.println(
                "    --host=127.0.0.1 --port=9030 --user=root --password= --count=10000"
                        + " --stream-load-port=8040");
    }

    /** 命令行参数类 */
    private static class CommandLineArgs {
        String host;
        int port = 9030;
        String user = "root";
        String password = "";
        int count = 10000;
        int streamLoadPort = 8040;
    }
}
