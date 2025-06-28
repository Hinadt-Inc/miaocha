package com.hinadt.miaocha.integration.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
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
import java.util.Base64;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;

/**
 * LogSearch集成测试数据初始化器 (Stream Load高性能导入版本)
 *
 * <p>职责： 1. 在Doris容器中创建测试数据库和表结构 2. 多线程生成JSON数据文件，使用Stream Load高速导入10000条测试日志数据 3.
 * 在MySQL中创建对应的数据源和模块配置 4. 确保集成测试环境的完整性
 *
 * <p>核心特色： - 使用Apache Doris Stream Load进行高性能批量导入 - 多线程生成JSON数据文件，然后合并为单个数组文件 -
 * HTTP协议直接向Doris发送导入请求，速度比逐条INSERT快10-100倍
 */
@Slf4j
@Component
public class LogSearchTestDataInitializer {

    @Autowired private DatasourceMapper datasourceMapper;

    @Autowired private ModuleInfoMapper moduleInfoMapper;

    @Autowired private ObjectMapper objectMapper;

    // ==================== 常量定义 ====================

    /** 测试表名 */
    public static final String TEST_TABLE_NAME = "test_doris_table";

    /** 测试模块名 */
    public static final String TEST_MODULE_NAME = "test-doris-logs";

    /** 测试数据源名 */
    public static final String TEST_DATASOURCE_NAME = "test-doris-datasource";

    /** 测试数据总数 */
    public static final int TOTAL_LOG_RECORDS = 10000;

    /** 批次大小 */
    public static final int BATCH_SIZE = 500;

    /** 线程池大小 */
    public static final int THREAD_POOL_SIZE = 16;

    /** 测试用户名 - 与IntegrationTestDataInitializer保持一致 */
    public static final String TEST_USER = "test_admin";

    // ==================== 性能优化常量 ====================

    /** 预生成的UUID池大小 */
    private static final int UUID_POOL_SIZE = 1000;

    /** 预生成的UUID池 */
    private static final String[] UUID_POOL = new String[UUID_POOL_SIZE];

    /** 预生成的token池 */
    private static final String[] TOKEN_POOL = new String[UUID_POOL_SIZE];

    /** 预生成的traceId池 */
    private static final String[] TRACE_ID_POOL = new String[UUID_POOL_SIZE];

    static {
        // 预生成UUID、token等，避免运行时生成
        for (int i = 0; i < UUID_POOL_SIZE; i++) {
            UUID_POOL[i] = UUID.randomUUID().toString();
            TOKEN_POOL[i] =
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyXyIgKyAoaW5kZXggJSAxMDAwMCkgKyAiIiwiaWF0IjoxNjI2MzQ"
                            + String.format("%08d", i);
            TRACE_ID_POOL[i] = String.format("%032x", i * 0x123456789ABCDEFL);
        }
    }

    // ==================== Doris建表SQL ====================

    /** Doris建表SQL */
    private static final String CREATE_TABLE_SQL =
            """
        CREATE TABLE `test_doris_table` (
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

    // ==================== 测试数据模板 ====================

    /** 主机列表 */
    private static final String[] HOSTS = {
        "172.20.61.22", "172.20.61.18", "172.20.61.35", "172.20.61.42", "192.168.1.10",
        "10.0.1.15", "10.0.2.20", "172.16.1.100", "192.168.2.50", "10.10.10.88"
    };

    /** 日志源路径 */
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

    /** 日志级别 */
    private static final String[] LEVELS = {"INFO", "ERROR", "WARN", "DEBUG"};

    /** 服务名称 */
    private static final String[] SERVICES = {
        "hina-cloud-engine", "order-service", "user-service", "payment-service",
        "notification-service", "report-service", "api-gateway", "auth-service",
        "inventory-service", "shipping-service"
    };

    // ==================== 主要方法 ====================

    /**
     * 初始化完整的测试环境
     *
     * @param dorisContainer 启动的Doris容器
     */
    @Transactional
    public void initializeTestEnvironment(GenericContainer<?> dorisContainer) {
        log.info("开始初始化LogSearch集成测试环境...");

        long startTime = System.currentTimeMillis();

        try {
            // 1. 先创建数据库
            createTestDatabase(dorisContainer);
            log.info("在Doris中创建测试数据库: log_db");

            // 2. 创建数据源配置（指向新数据库）
            DatasourceInfo datasource = createTestDatasource(dorisContainer);
            log.info("创建测试数据源: {}", datasource.getName());

            // 3. 在Doris中创建表
            createTestTableInDoris(datasource);
            log.info("在Doris中创建测试表: {}", TEST_TABLE_NAME);

            // 4. 多线程高性能生成测试数据
            generateTestLogDataConcurrently(datasource, dorisContainer);
            log.info("多线程生成{}条测试日志数据", TOTAL_LOG_RECORDS);

            // 5. 创建模块配置
            ModuleInfo module = createTestModule(datasource);
            log.info("创建测试模块: {}", module.getName());

            long endTime = System.currentTimeMillis();
            log.info("LogSearch集成测试环境初始化完成，耗时: {}ms", endTime - startTime);

        } catch (Exception e) {
            log.error("初始化LogSearch测试环境失败", e);
            throw new RuntimeException("初始化LogSearch测试环境失败", e);
        }
    }

    /** 创建测试数据源 */
    private DatasourceInfo createTestDatasource(GenericContainer<?> dorisContainer) {
        // 检查是否已存在
        DatasourceInfo existing = datasourceMapper.selectByName(TEST_DATASOURCE_NAME);
        if (existing != null) {
            // 更新JDBC URL为新的容器地址
            updateDatasourceConnection(existing, dorisContainer);
            return existing;
        }

        DatasourceInfo datasource = new DatasourceInfo();
        datasource.setName(TEST_DATASOURCE_NAME);
        datasource.setType("DORIS");
        datasource.setDescription("LogSearch集成测试用Doris数据源");

        // 构建JDBC URL，连接到容器映射的端口
        String jdbcUrl = buildDorisJdbcUrl(dorisContainer);
        datasource.setJdbcUrl(jdbcUrl);
        datasource.setUsername("root");
        datasource.setPassword(""); // Doris默认root用户密码为空

        datasource.setCreateUser(TEST_USER);
        datasource.setUpdateUser(TEST_USER);

        datasourceMapper.insert(datasource);
        return datasource;
    }

    /** 更新数据源连接信息 */
    private void updateDatasourceConnection(
            DatasourceInfo datasource, GenericContainer<?> dorisContainer) {
        String jdbcUrl = buildDorisJdbcUrl(dorisContainer);
        datasource.setJdbcUrl(jdbcUrl);
        datasourceMapper.update(datasource);
        log.debug("更新数据源连接: {}", jdbcUrl);
    }

    /**
     * 创建测试数据库
     *
     * @param dorisContainer Doris容器
     * @throws Exception 创建失败时抛出异常
     */
    private void createTestDatabase(GenericContainer<?> dorisContainer) throws Exception {
        String defaultJdbcUrl = buildDefaultDorisJdbcUrl(dorisContainer);

        try (Connection connection = DriverManager.getConnection(defaultJdbcUrl, "root", "");
                Statement statement = connection.createStatement()) {

            String createDbSql = "CREATE DATABASE log_db";
            statement.execute(createDbSql);
            log.info("成功创建数据库 log_db");

        } catch (Exception e) {
            log.error("创建数据库失败", e);
            throw e;
        }
    }

    /** 构建默认Doris JDBC URL（连接到information_schema） */
    private String buildDefaultDorisJdbcUrl(GenericContainer<?> dorisContainer) {
        return String.format(
                "jdbc:mysql://%s:%d/information_schema?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                dorisContainer.getHost(), dorisContainer.getMappedPort(9030));
    }

    /** 构建指向log_db数据库的Doris JDBC URL */
    private String buildDorisJdbcUrl(GenericContainer<?> dorisContainer) {
        return String.format(
                "jdbc:mysql://%s:%d/log_db?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                dorisContainer.getHost(), dorisContainer.getMappedPort(9030));
    }

    /** 在Doris中创建测试表（带重试机制） */
    private void createTestTableInDoris(DatasourceInfo datasource) throws Exception {
        int maxRetries = 8;
        int retryInterval = 2000; // 4秒

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Connection conn =
                    DriverManager.getConnection(
                            datasource.getJdbcUrl(),
                            datasource.getUsername(),
                            datasource.getPassword())) {

                // 先删除可能存在的表
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP TABLE IF EXISTS " + TEST_TABLE_NAME);
                    log.debug("删除已存在的测试表");
                } catch (Exception e) {
                    log.debug("删除表时出错（可能表不存在）: {}", e.getMessage());
                }

                // 创建新表
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(CREATE_TABLE_SQL);
                    log.info("成功创建Doris测试表: {} (第{}次尝试)", TEST_TABLE_NAME, attempt);
                    return; // 成功则直接返回
                }

            } catch (Exception e) {
                log.warn("第{}次创建测试表失败: {}", attempt, e.getMessage());

                if (attempt == maxRetries) {
                    log.error("创建测试表失败，已达到最大重试次数({}次)", maxRetries, e);
                    throw new RuntimeException("创建测试表失败，重试" + maxRetries + "次后仍然失败", e);
                }

                try {
                    Thread.sleep(retryInterval);
                    log.info("等待{}ms后进行第{}次重试...", retryInterval, attempt + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("创建测试表过程中被中断", ie);
                }
            }
        }
    }

    /** 多线程高性能生成测试数据（Stream Load方式） */
    private void generateTestLogDataConcurrently(
            DatasourceInfo datasource, GenericContainer<?> dorisContainer) throws Exception {
        log.info("开始多线程生成{}条测试日志数据(Stream Load方式)...", TOTAL_LOG_RECORDS);

        long startTime = System.currentTimeMillis();

        try {
            // 第一阶段：多线程生成JSON数据文件
            List<String> tempFiles = generateJsonDataFiles();

            // 第二阶段：合并JSON文件为单个数组文件
            String mergedJsonFile = mergeJsonFiles(tempFiles);

            // 第三阶段：通过Stream Load导入数据
            streamLoadImportData(datasource, mergedJsonFile, dorisContainer);

            // 第四阶段：清理临时文件
            cleanupTempFiles(tempFiles, mergedJsonFile);

            long endTime = System.currentTimeMillis();
            log.info(
                    "Stream Load数据导入完成！总计{}条记录，耗时: {}ms，平均: {}条/秒",
                    TOTAL_LOG_RECORDS,
                    endTime - startTime,
                    String.format("%.2f", TOTAL_LOG_RECORDS * 1000.0 / (endTime - startTime)));

        } catch (Exception e) {
            log.error("Stream Load数据导入失败", e);
            throw e;
        }
    }

    // ==================== Stream Load 新方法 ====================

    /** 第一阶段：多线程生成JSON数据文件 */
    private List<String> generateJsonDataFiles() throws Exception {
        log.info("🏭 ============ 第一阶段：多线程生成JSON数据文件 ============");
        log.info("📋 目标记录数: {}", TOTAL_LOG_RECORDS);
        log.info("📦 批次大小: {}", BATCH_SIZE);
        log.info("🧵 线程池大小: {}", THREAD_POOL_SIZE);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CompletionService<String> completionService =
                new ExecutorCompletionService<>(executorService);
        List<String> tempFiles = new ArrayList<>();

        try {
            // 计算任务分配
            int totalBatches = (TOTAL_LOG_RECORDS + BATCH_SIZE - 1) / BATCH_SIZE;
            log.info("📊 计算结果：分配{}个批次，每批{}条记录，使用{}个线程", totalBatches, BATCH_SIZE, THREAD_POOL_SIZE);
            log.info("📁 临时文件目录: {}", System.getProperty("java.io.tmpdir"));

            // 提交所有批次任务
            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                final int currentBatch = batchIndex;
                final int startIndex = currentBatch * BATCH_SIZE;
                final int endIndex = Math.min(startIndex + BATCH_SIZE, TOTAL_LOG_RECORDS);

                completionService.submit(
                        () -> generateBatchJsonFile(currentBatch, startIndex, endIndex));
            }

            // 等待所有任务完成并收集临时文件名
            for (int i = 0; i < totalBatches; i++) {
                Future<String> future = completionService.take();
                String tempFile = future.get();
                tempFiles.add(tempFile);

                if ((i + 1) % 5 == 0 || (i + 1) == totalBatches) {
                    log.info("📈 进度更新：已完成 {}/{} 个JSON文件生成", i + 1, totalBatches);
                }
            }

            log.info("✅ JSON数据文件生成完成，共生成{}个文件", tempFiles.size());

            // 调试：验证生成的文件
            log.debug("🔍 验证生成的临时文件:");
            for (int i = 0; i < tempFiles.size(); i++) {
                String filePath = tempFiles.get(i);
                File file = new File(filePath);
                log.debug(
                        "   文件[{}]: {} (存在={}, 大小={} bytes)",
                        i,
                        filePath,
                        file.exists(),
                        file.length());
            }

            return tempFiles;

        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /** 生成单个批次的JSON数据文件 */
    private String generateBatchJsonFile(int batchIndex, int startIndex, int endIndex) {
        String tempFileName = String.format("temp_log_data_%d.jsonl", batchIndex);
        Path tempFilePath = Paths.get(System.getProperty("java.io.tmpdir"), tempFileName);

        try (BufferedWriter writer =
                Files.newBufferedWriter(tempFilePath, StandardCharsets.UTF_8)) {
            // 生成JSON Lines格式：每行一个完整的JSON对象
            for (int i = startIndex; i < endIndex; i++) {
                Map<String, Object> logRecord = generateSingleLogRecord(i);
                String jsonLine = objectMapper.writeValueAsString(logRecord);
                writer.write(jsonLine);
                writer.write("\n"); // 每个JSON对象占一行
            }

            log.debug("批次[{}] JSON文件生成完成：{} 条记录", batchIndex, endIndex - startIndex);
            return tempFilePath.toString();

        } catch (Exception e) {
            log.error("批次[{}] JSON文件生成失败：索引{}-{}", batchIndex, startIndex, endIndex - 1, e);
            throw new RuntimeException("JSON文件生成失败", e);
        }
    }

    /** 生成单条日志记录的JSON对象 */
    private Map<String, Object> generateSingleLogRecord(int index) throws Exception {
        // 时间分布策略：创建具有测试特性的时间分布
        LocalDateTime logTime = generateTestTime(index);

        // 选择当前记录的属性
        String host = HOSTS[index % HOSTS.length];
        String source = SOURCES[index % SOURCES.length];
        String level = LEVELS[index % LEVELS.length];
        String service = SERVICES[index % SERVICES.length];
        String logId = UUID_POOL[index % UUID_POOL_SIZE];

        // 构建完整的JSON记录（直接用于Stream Load）
        Map<String, Object> logRecord = new HashMap<>();
        logRecord.put(
                "log_time", logTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        logRecord.put("host", host);
        logRecord.put("source", source);
        logRecord.put("log_offset", String.valueOf(60000000L + index));

        // 构建复杂的message JSON对象
        Map<String, Object> messageObj = createMessageObject(index, service, level, logId, logTime);
        logRecord.put("message", messageObj);
        logRecord.put("message_text", objectMapper.writeValueAsString(messageObj));

        return logRecord;
    }

    /** 第二阶段：合并JSON文件为单个数组文件 */
    private String mergeJsonFiles(List<String> tempFiles) throws Exception {
        log.info("📝 ============ 第二阶段：合并JSON文件 ============");
        log.info("📂 输入文件数量: {}", tempFiles.size());

        String mergedFileName = "merged_log_data.json";
        Path mergedFilePath = Paths.get(System.getProperty("java.io.tmpdir"), mergedFileName);
        log.info("📄 合并文件路径: {}", mergedFilePath);

        try (BufferedWriter writer =
                Files.newBufferedWriter(mergedFilePath, StandardCharsets.UTF_8)) {
            writer.write("[\n");

            boolean isFirstRecord = true;
            int totalRecords = 0;
            int processedFiles = 0;

            for (String tempFile : tempFiles) {
                Path tempFilePath = Paths.get(tempFile);
                processedFiles++;

                log.debug("🔄 处理文件 [{}/{}]: {}", processedFiles, tempFiles.size(), tempFile);

                try (BufferedReader reader =
                        Files.newBufferedReader(tempFilePath, StandardCharsets.UTF_8)) {
                    String line;
                    int fileRecords = 0;

                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) {
                            continue; // 跳过空行
                        }

                        // 如果不是第一个记录，先写逗号和换行
                        if (!isFirstRecord) {
                            writer.write(",\n");
                        }

                        writer.write(line);
                        isFirstRecord = false;
                        totalRecords++;
                        fileRecords++;
                    }

                    log.debug("   文件记录数: {}", fileRecords);
                } catch (Exception e) {
                    log.error("❌ 处理文件失败: {}", tempFile, e);
                    throw e;
                }

                // 每处理几个文件打印一次进度
                if (processedFiles % 5 == 0 || processedFiles == tempFiles.size()) {
                    log.info(
                            "📈 合并进度: {}/{} 文件, 累计记录: {}",
                            processedFiles,
                            tempFiles.size(),
                            totalRecords);
                }
            }

            writer.write("\n]");

            log.info("✅ JSON数组文件合并完成：{} 条记录", totalRecords);
            log.info("📄 合并文件路径：{}", mergedFilePath);

            // 验证生成的JSON文件格式正确性
            validateMergedJsonFile(mergedFilePath.toString(), totalRecords);

            return mergedFilePath.toString();
        }
    }

    /** 验证合并后的JSON文件格式正确性 */
    private void validateMergedJsonFile(String jsonFilePath, int expectedRecords) {
        try {
            log.info("验证JSON文件格式正确性...");

            // 读取文件开头和结尾，检查JSON数组格式
            try (BufferedReader reader =
                    Files.newBufferedReader(Paths.get(jsonFilePath), StandardCharsets.UTF_8)) {
                String firstLine = reader.readLine();
                if (!"[".equals(firstLine.trim())) {
                    throw new RuntimeException("JSON文件格式错误：缺少开始的'['");
                }

                // 检查文件大小是否合理
                long fileSize = Files.size(Paths.get(jsonFilePath));
                log.info("合并文件大小: {} MB", fileSize / (1024 * 1024));

                if (fileSize < 1000) { // 如果文件太小，可能有问题
                    throw new RuntimeException("JSON文件太小，可能生成有误");
                }
            }

            // 尝试解析JSON数组来验证格式
            try (FileInputStream fis = new FileInputStream(jsonFilePath)) {
                // 只读取前1KB来检查格式，避免内存占用过大
                byte[] buffer = new byte[1024];
                int bytesRead = fis.read(buffer);
                String sample = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);

                if (!sample.trim().startsWith("[")) {
                    throw new RuntimeException("JSON文件格式错误：不是有效的JSON数组开始");
                }

                log.info("✅ JSON文件格式验证通过");
                log.info(
                        "📄 文件样例 (前100字符): {}",
                        sample.length() > 100 ? sample.substring(0, 100) + "..." : sample);
            }

        } catch (Exception e) {
            log.error("JSON文件验证失败", e);
            throw new RuntimeException("JSON文件验证失败", e);
        }
    }

    /** 第三阶段：通过Stream Load导入数据 */
    private void streamLoadImportData(
            DatasourceInfo datasource, String jsonFilePath, GenericContainer<?> dorisContainer)
            throws Exception {
        log.info("开始Stream Load导入数据...");

        String streamLoadUrl = buildStreamLoadUrl(datasource, dorisContainer);
        File jsonFile = new File(jsonFilePath);
        if (!jsonFile.exists()) {
            throw new RuntimeException("JSON文件不存在: " + jsonFilePath);
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(streamLoadUrl);
            connection = (HttpURLConnection) url.openConnection();

            // 基础HTTP设置
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(300000);
            connection.setInstanceFollowRedirects(true);

            // 认证
            String credentials = datasource.getUsername() + ":" + datasource.getPassword();
            String encodedCredentials =
                    Base64.getEncoder()
                            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);

            // Stream Load参数
            connection.setRequestProperty("Expect", "100-continue");
            connection.setRequestProperty("format", "json");
            connection.setRequestProperty("strip_outer_array", "true");
            connection.setRequestProperty("fuzzy_parse", "true");
            connection.setRequestProperty("strict_mode", "false");
            connection.setRequestProperty("max_filter_ratio", "0.05");
            connection.setRequestProperty("timeout", "300");
            connection.setRequestProperty("max_batch_interval", "20");
            connection.setRequestProperty("max_batch_size", "200MB");
            connection.setRequestProperty(
                    "columns", "log_time,host,source,log_offset,message,message_text");
            connection.setRequestProperty("label", "test_log_data_" + System.currentTimeMillis());

            // 发送文件
            long startTime = System.currentTimeMillis();
            try (OutputStream outputStream = connection.getOutputStream();
                    FileInputStream fileInputStream = new FileInputStream(jsonFilePath);
                    BufferedInputStream bufferedInput =
                            new BufferedInputStream(fileInputStream, 64 * 1024)) {

                byte[] buffer = new byte[64 * 1024];
                int bytesRead;
                long totalBytes = 0;

                while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                outputStream.flush();

                long transferTime = System.currentTimeMillis() - startTime;
                log.info("文件传输完成，大小: {} MB, 耗时: {}ms", totalBytes / (1024 * 1024), transferTime);
            }

            // 处理响应
            int responseCode = connection.getResponseCode();
            String responseMessage = getResponseMessage(connection);

            if (responseCode == 200 || responseCode == 202) {
                log.info("Stream Load导入成功！");
                parseAndLogImportResult(responseMessage);
            } else {
                log.error("Stream Load导入失败，响应码：{}", responseCode);
                log.error("错误消息：{}", responseMessage);
                throw new RuntimeException(
                        "Stream Load导入失败，响应码：" + responseCode + "，消息：" + responseMessage);
            }

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** 构建Stream Load URL（使用容器映射的8040端口，避免重定向） */
    private String buildStreamLoadUrl(
            DatasourceInfo datasource, GenericContainer<?> dorisContainer) {
        String host = dorisContainer.getHost();
        Integer httpPort = dorisContainer.getMappedPort(8040);
        return String.format(
                "http://%s:%d/api/log_db/%s/_stream_load", host, httpPort, TEST_TABLE_NAME);
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

    /** 解析并记录导入结果的关键指标 */
    private void parseAndLogImportResult(String responseMessage) {
        try {
            // 尝试解析JSON响应
            Map<String, Object> result = objectMapper.readValue(responseMessage, Map.class);

            // 提取基础信息
            Object txnId = result.get("TxnId");
            String label = (String) result.get("Label");
            String comment = (String) result.get("Comment");
            String twoPhaseCommit = (String) result.get("TwoPhaseCommit");
            String status = (String) result.get("Status");
            String message = (String) result.get("Message");

            // 提取数据统计
            Object totalRows = result.get("NumberTotalRows");
            Object loadedRows = result.get("NumberLoadedRows");
            Object filteredRows = result.get("NumberFilteredRows");
            Object unselectedRows = result.get("NumberUnselectedRows");
            Object loadBytes = result.get("LoadBytes");

            // 提取时间统计
            Object loadTimeMs = result.get("LoadTimeMs");
            Object beginTxnTimeMs = result.get("BeginTxnTimeMs");
            Object streamLoadPutTimeMs = result.get("StreamLoadPutTimeMs");
            Object readDataTimeMs = result.get("ReadDataTimeMs");
            Object writeDataTimeMs = result.get("WriteDataTimeMs");
            Object receiveDataTimeMs = result.get("ReceiveDataTimeMs");
            Object commitAndPublishTimeMs = result.get("CommitAndPublishTimeMs");

            log.info("📊 Stream Load导入结果:");
            log.info("   事务ID: {}", txnId);
            log.info("   标签: {}", label);
            log.info("   状态: {}", status);
            log.info("   消息: {}", message);
            log.info("   两阶段提交: {}", twoPhaseCommit);
            if (comment != null && !comment.trim().isEmpty()) {
                log.info("   备注: {}", comment);
            }

            log.info("📈 数据统计:");
            log.info("   总行数: {}", totalRows);
            log.info("   成功导入: {}", loadedRows);
            log.info("   过滤行数: {}", filteredRows);
            log.info("   未选择行数: {}", unselectedRows);
            log.info(
                    "   数据大小: {} bytes ({} MB)",
                    loadBytes,
                    loadBytes instanceof Number
                            ? String.format(
                                    "%.2f", ((Number) loadBytes).doubleValue() / (1024 * 1024))
                            : "0");

            log.info("⏱️ 时间分析:");
            log.info("   总导入时间: {} ms", loadTimeMs);
            log.info("   开始事务: {} ms", beginTxnTimeMs);
            log.info("   Stream Load上传: {} ms", streamLoadPutTimeMs);
            log.info("   读取数据: {} ms", readDataTimeMs);
            log.info("   写入数据: {} ms", writeDataTimeMs);
            log.info("   接收数据: {} ms", receiveDataTimeMs);
            log.info("   提交发布: {} ms", commitAndPublishTimeMs);

            // 计算导入速度
            if (loadTimeMs instanceof Number && loadedRows instanceof Number) {
                double timeSeconds = ((Number) loadTimeMs).doubleValue() / 1000.0;
                double rowsPerSecond = ((Number) loadedRows).doubleValue() / timeSeconds;
                log.info("🚀 导入速度: {} 行/秒", String.format("%.2f", rowsPerSecond));
            }

        } catch (Exception e) {
            log.warn("解析导入结果失败，原始响应: {}", responseMessage, e);
        }
    }

    /** 第四阶段：清理临时文件 */
    private void cleanupTempFiles(List<String> tempFiles, String mergedFile) {
        log.info("开始清理临时文件...");

        int deletedCount = 0;

        // 删除批次临时文件
        for (String tempFile : tempFiles) {
            try {
                Path tempFilePath = Paths.get(tempFile);
                if (Files.deleteIfExists(tempFilePath)) {
                    deletedCount++;
                }
            } catch (Exception e) {
                log.warn("删除临时文件失败：{}", tempFile, e);
            }
        }

        // 删除合并后的文件
        try {
            Path mergedFilePath = Paths.get(mergedFile);
            if (Files.deleteIfExists(mergedFilePath)) {
                deletedCount++;
            }
        } catch (Exception e) {
            log.warn("删除合并文件失败：{}", mergedFile, e);
        }

        log.info("临时文件清理完成，删除{}个文件", deletedCount);
    }

    /** 生成具有测试特性的时间分布 */
    private LocalDateTime generateTestTime(int index) {
        LocalDateTime baseTime = LocalDateTime.now().minusHours(24);

        if (index < 2000) {
            // 前2000条：最近1小时，高密度 - 用于测试实时日志查询
            return baseTime.plusHours(23)
                    .plusMinutes((index / 40) % 60)
                    .plusSeconds(index % 60)
                    .plusNanos((index % 1000) * 1_000_000);
        } else if (index < 5000) {
            // 中间3000条：最近6小时，中密度 - 用于测试中期日志查询
            return baseTime.plusHours(18 + (index % 6))
                    .plusMinutes((index / 10) % 60)
                    .plusSeconds(index % 60);
        } else if (index < 8000) {
            // 3000条：12小时内均匀分布 - 用于测试长期日志查询
            return baseTime.plusHours(12 + (index % 12))
                    .plusMinutes(index % 60)
                    .plusSeconds((index / 2) % 60);
        } else {
            // 最后2000条：24小时内分散 - 用于测试历史日志查询
            return baseTime.plusHours(index % 24)
                    .plusMinutes((index * 7) % 60)
                    .plusSeconds((index * 3) % 60);
        }
    }

    /** 创建消息对象 - 高性能生成复杂JSON结构用于variant字段测试 */
    private Map<String, Object> createMessageObject(
            int index, String service, String level, String logId, LocalDateTime logTime)
            throws Exception {
        // 使用预估大小的HashMap减少扩容
        Map<String, Object> messageObj = new HashMap<>(16);

        // ============= 基础字段（直接赋值，避免字符串拼接）=============
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

        // ============= 消息内容 =============
        String msg = generateLogMessage(level, index);
        messageObj.put("message", msg);
        messageObj.put("messageId", UUID_POOL[index % UUID_POOL_SIZE]); // 使用预生成的UUID

        // ============= 简化的请求上下文（减少嵌套深度）=============
        Map<String, Object> request = new HashMap<>(8);
        request.put("method", index % 5 == 0 ? "POST" : (index % 3 == 0 ? "PUT" : "GET"));
        request.put("uri", "/api/v" + (index % 3 + 1) + "/" + service.toLowerCase() + "/" + index);
        request.put("userAgent", generateUserAgent(index));
        request.put(
                "contentType",
                index % 4 == 0 ? "application/json" : "application/x-www-form-urlencoded");
        request.put("contentLength", index % 10000 + 100);
        request.put("remoteAddr", generateIpAddress(index));
        request.put(
                "sessionId", "SESSION-" + Long.toHexString(index * 1234567L)); // 避免String.format

        // 简化请求头部信息（只保留核心字段）
        if (index % 3 == 0) {
            request.put("authorization", "Bearer " + TOKEN_POOL[index % UUID_POOL_SIZE]);
        }
        request.put("x-request-id", UUID_POOL[(index + 1) % UUID_POOL_SIZE]);
        request.put(
                "accept-language",
                index % 3 == 0 ? "zh-CN,zh" : (index % 2 == 0 ? "en-US,en" : "ja,en"));

        messageObj.put("request", request);

        // ============= 响应信息（合并性能指标）=============
        Map<String, Object> response = new HashMap<>(8);
        response.put("status", generateHttpStatus(level, index));
        response.put("responseTime", index % 5000 + 10);
        response.put("contentLength", index % 50000 + 500);
        response.put("mimeType", index % 6 == 0 ? "application/json" : "text/html");
        response.put("dbQueryTime", index % 1000 + 5);
        response.put("cacheHitRate", (index % 100) / 100.0);
        response.put("memoryUsage", index % 1024 + 256);
        response.put("threadCount", index % 200 + 50);
        messageObj.put("response", response);

        // ============= 用户信息（条件简化）=============
        if (index % 3 != 0) {
            Map<String, Object> user = new HashMap<>(8);
            user.put("userId", index % 100000 + 1);
            user.put("username", "user_" + (index % 10000));
            user.put("email", "user" + (index % 10000) + "@test.com");
            user.put("role", index % 5 == 0 ? "admin" : (index % 3 == 0 ? "user" : "guest"));
            user.put("isActive", index % 7 != 0);
            user.put("language", index % 3 == 0 ? "zh-CN" : "en-US");
            user.put("theme", index % 2 == 0 ? "dark" : "light");

            // 简化权限为字符串而非数组
            String[] availablePerms = {"read", "write", "delete", "admin", "audit", "export"};
            user.put("permissions", availablePerms[index % availablePerms.length]);

            messageObj.put("user", user);
        }

        // ============= 数据库操作信息（条件简化）=============
        if (index % 4 == 0) {
            Map<String, Object> database = new HashMap<>(6);
            database.put(
                    "operation",
                    index % 6 == 0 ? "SELECT" : (index % 5 == 0 ? "INSERT" : "UPDATE"));
            database.put("table", "tb_" + service.toLowerCase() + "_" + (index % 20));
            database.put("rows", index % 10000 + 1);
            database.put("executionTime", index % 5000 + 10);
            database.put("poolActive", index % 50);
            database.put("poolIdle", index % 30);

            messageObj.put("database", database);
        }

        // ============= 精简的业务数据 =============
        Map<String, Object> business = generateOptimizedBusinessData(service, index);
        messageObj.put("business", business);

        // ============= 错误信息（仅ERROR级别）=============
        if ("ERROR".equals(level)) {
            Map<String, Object> error = new HashMap<>(6);
            error.put("type", generateErrorType(index));
            error.put("code", "ERR_" + String.format("%04d", index % 9999));
            error.put("message", generateErrorMessage(index));
            error.put("recoverable", index % 3 == 0);
            error.put("retryCount", index % 5);
            error.put(
                    "stackFrame",
                    "Service"
                            + (index % 10)
                            + ".method"
                            + (index % 20)
                            + ":Line"
                            + (100 + index % 500));

            messageObj.put("error", error);
        }

        // ============= 标签和元数据（扁平化）=============
        messageObj.put("tags", generateOptimizedTags(service, level, index));
        messageObj.put("source", "miaocha-server");
        messageObj.put("hostname", HOSTS[index % HOSTS.length]);
        messageObj.put("processId", index % 65536 + 1000);
        messageObj.put("traceId", TRACE_ID_POOL[index % UUID_POOL_SIZE]);
        messageObj.put("spanId", Long.toHexString(index * 0x987654321L));

        // ============= 特性标志（扁平化）=============
        messageObj.put("newUI", index % 2 == 0);
        messageObj.put("cacheEnabled", index % 3 == 0);
        messageObj.put("debugMode", index % 10 == 0);
        messageObj.put("region", index % 3 == 0 ? "us-east-1" : "ap-southeast-1");
        messageObj.put("clusterId", "cluster-" + (index % 10 + 1));

        return messageObj;
    }

    // ==================== JSON辅助生成方法 ====================

    /** 生成用户代理字符串 */
    private String generateUserAgent(int index) {
        String[] browsers = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/91.0.4472.124",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/91.0.4472.124",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15"
                    + " Mobile/15E148",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0"
        };
        return browsers[index % browsers.length];
    }

    /** 生成IP地址 */
    private String generateIpAddress(int index) {
        return "192.168." + (index % 255 + 1) + "." + ((index * 17) % 255 + 1);
    }

    /** 生成HTTP状态码 */
    private Integer generateHttpStatus(String level, int index) {
        if ("ERROR".equals(level)) {
            int[] errorCodes = {500, 503, 502, 504, 400, 401, 403, 404, 422};
            return errorCodes[index % errorCodes.length];
        } else if ("WARN".equals(level)) {
            int[] warnCodes = {200, 201, 202, 429, 304};
            return warnCodes[index % warnCodes.length];
        } else {
            int[] successCodes = {200, 201, 202, 204};
            return successCodes[index % successCodes.length];
        }
    }

    /** 生成错误类型 */
    private String generateErrorType(int index) {
        String[] errorTypes = {
            "NullPointerException", "IllegalArgumentException", "SQLException",
            "TimeoutException", "IOException", "SecurityException",
            "ValidationException", "BusinessException", "SystemException"
        };
        return errorTypes[index % errorTypes.length];
    }

    /** 生成优化的标签字符串（避免创建List） */
    private String generateOptimizedTags(String service, String level, int index) {
        StringBuilder tags = new StringBuilder(64);
        tags.append(service.toLowerCase()).append(",").append(level.toLowerCase());

        // 根据索引添加动态标签
        if (index % 5 == 0) tags.append(",high-priority");
        if (index % 7 == 0) tags.append(",user-facing");
        if (index % 3 == 0) tags.append(",database");
        if (index % 11 == 0) tags.append(",external-api");
        if (index % 13 == 0) tags.append(",cache");
        if (index % 17 == 0) tags.append(",security");

        // 业务标签
        String[] businessTags = {
            "order", "payment", "user", "product", "inventory", "notification"
        };
        tags.append(",").append(businessTags[index % businessTags.length]);

        return tags.toString();
    }

    /** 生成跟踪ID（已优化为预生成） */
    private String generateTraceId(int index) {
        return TRACE_ID_POOL[index % UUID_POOL_SIZE];
    }

    /** 生成Span ID（优化字符串格式化） */
    private String generateSpanId(int index) {
        return Long.toHexString(index * 0x987654321L);
    }

    /** 生成优化的业务数据（减少对象创建和嵌套） */
    private Map<String, Object> generateOptimizedBusinessData(String service, int index) {
        Map<String, Object> business = new HashMap<>(6);

        switch (service) {
            case "OrderService" -> {
                business.put("orderId", "ORD" + String.format("%010d", index));
                business.put("customerId", index % 50000 + 1);
                business.put("amount", (index % 10000 + 100) / 100.0);
                business.put("currency", index % 3 == 0 ? "USD" : "CNY");
                business.put(
                        "status",
                        index % 6 == 0 ? "PENDING" : (index % 5 == 0 ? "COMPLETED" : "PROCESSING"));
                business.put("itemCount", index % 5 + 1);
            }
            case "UserService" -> {
                business.put(
                        "action",
                        index % 4 == 0 ? "LOGIN" : (index % 3 == 0 ? "REGISTER" : "UPDATE"));
                business.put("userId", index % 100000 + 1);
                business.put("profileComplete", index % 7 != 0);
                business.put("accountType", index % 5 == 0 ? "PREMIUM" : "BASIC");
                business.put("loginCount", index % 1000);
                business.put("sessionDuration", index % 7200 + 60);
            }
            case "PaymentService" -> {
                business.put("transactionId", "TXN" + String.format("%012d", index));
                business.put(
                        "paymentMethod",
                        index % 4 == 0 ? "CREDIT_CARD" : (index % 3 == 0 ? "ALIPAY" : "WECHAT"));
                business.put("amount", (index % 100000 + 1000) / 100.0);
                business.put("fee", (index % 1000 + 10) / 100.0);
                business.put("riskScore", (index % 100) / 100.0);
                business.put("fraudDetected", index % 50 == 0);
            }
            default -> {
                business.put("action", "GENERIC_OPERATION");
                business.put("resourceId", index % 10000 + 1);
                business.put("resourceType", service.replaceAll("Service", "").toUpperCase());
                business.put("operationTime", index % 5000 + 10);
                business.put("priority", index % 5);
                business.put("category", index % 3 == 0 ? "CRITICAL" : "NORMAL");
            }
        }

        return business;
    }

    /** 根据日志级别生成相应的消息内容 */
    private String generateLogMessage(String level, int index) {
        return switch (level) {
            case "ERROR" -> generateErrorMessage(index);
            case "WARN" -> generateWarnMessage(index);
            case "INFO" -> generateInfoMessage(index);
            case "DEBUG" -> generateDebugMessage(index);
            default -> "Test log message " + index;
        };
    }

    private String generateErrorMessage(int index) {
        String[] errorMessages = {
            "NullPointerException in processing request: user data is null",
            "Database connection timeout after 30 seconds",
            "Failed to parse JSON request body: invalid format at line " + (index % 100),
            "OutOfMemoryError: Java heap space exceeded, current usage: "
                    + (index % 100 + 50)
                    + "%",
            "StackOverflowError in recursive method call, depth: " + (index % 1000),
            "IOException: Unable to read configuration file /opt/config/app.properties",
            "IllegalArgumentException: Invalid parameter value: " + index,
            "ClassNotFoundException: Driver class com.mysql.cj.jdbc.Driver not found",
            "SQLException: Duplicate entry '" + index + "' for key 'PRIMARY'",
            "TimeoutException: Operation timed out after " + (index % 120 + 10) + " seconds",
            "SecurityException: Access denied for operation: DELETE_USER_" + index,
            "ValidationException: Required field 'email' is missing in request"
        };
        return errorMessages[index % errorMessages.length];
    }

    private String generateWarnMessage(int index) {
        String[] warnMessages = {
            "Deprecated API usage detected in request handler /api/v1/users/" + index,
            "Large result set returned: " + (index * 10 + 100) + " records, consider pagination",
            "Slow query detected: execution time "
                    + (index % 5000 + 1000)
                    + "ms for SELECT * FROM users",
            "Memory usage high: " + (60 + index % 40) + "% of heap used, triggering GC",
            "Retry attempt " + (index % 3 + 1) + " for failed operation: SEND_EMAIL",
            "Configuration value 'max.connections' missing, using default: 100",
            "Thread pool queue size approaching limit: " + (index % 100) + "% full",
            "Cache hit ratio low: " + (index % 50 + 20) + "% for key pattern user_*",
            "Rate limit warning: "
                    + (index % 1000)
                    + " requests/minute from IP 192.168.1."
                    + (index % 255),
            "Disk space warning: " + (index % 20 + 70) + "% used on /var/log partition"
        };
        return warnMessages[index % warnMessages.length];
    }

    private String generateInfoMessage(int index) {
        String[] infoMessages = {
            "Processing request for user ID: " + (index % 100000 + 1),
            "SHOW RETURN 处理原始请求事件("
                    + (index % 100 + 1)
                    + "ms) TPS:"
                    + (index % 10 + 1)
                    + " TOPIC: ORIGINAL_DATA_test_"
                    + (index % 100),
            "ServletPath: /api/v1/orders/"
                    + index
                    + " token: QGfwA69s Headers: user-agent Mozilla/5.0",
            "Successfully processed order: ORD" + String.format("%08d", index),
            "User login successful: session created for user_"
                    + (index % 10000)
                    + " from IP 192.168.1."
                    + (index % 255),
            "Cache refresh completed for key: config_"
                    + (index % 1000)
                    + " in "
                    + (index % 500 + 10)
                    + "ms",
            "Scheduled task executed successfully: backup_task_"
                    + index
                    + " at "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            "Health check passed for service: "
                    + (index % 5 == 0 ? "database" : "redis")
                    + " response time: "
                    + (index % 50 + 1)
                    + "ms",
            "File uploaded successfully: document_"
                    + index
                    + ".pdf size: "
                    + (index % 10000 + 1000)
                    + " bytes",
            "Backup completed: " + (index % 1000 + 100) + " records backed up to storage location"
        };
        return infoMessages[index % infoMessages.length];
    }

    private String generateDebugMessage(int index) {
        String[] debugMessages = {
            "Method entry: processUserRequest(userId=" + (index % 10000) + ", action=UPDATE)",
            "SQL query: SELECT * FROM users WHERE id = "
                    + (index % 10000)
                    + " AND status = 'ACTIVE'",
            "Cache lookup: key=user_"
                    + index
                    + ", result="
                    + (index % 2 == 0 ? "HIT" : "MISS")
                    + ", ttl="
                    + (index % 3600)
                    + "s",
            "Validation passed for request parameter: userId="
                    + index
                    + ", email=user"
                    + index
                    + "@test.com",
            "Thread pool status: active="
                    + (index % 20)
                    + ", queue="
                    + (index % 50)
                    + ", completed="
                    + (index % 10000),
            "Memory allocation: " + (index % 1024 + 100) + "KB for operation_" + index,
            "Network call latency: "
                    + (index % 200 + 10)
                    + "ms to external service API_"
                    + (index % 10),
            "Database connection pool: active=" + (index % 20) + "/100, idle=" + (index % 80),
            "Serialization completed: object_" + index + " to JSON in " + (index % 50 + 1) + "ms",
            "Lock acquired: MUTEX_"
                    + (index % 100)
                    + " by thread-"
                    + (index % 50)
                    + " wait_time="
                    + (index % 100)
                    + "ms"
        };
        return debugMessages[index % debugMessages.length];
    }

    /** 创建测试模块 */
    private ModuleInfo createTestModule(DatasourceInfo datasource) {
        // 检查是否已存在
        ModuleInfo existing = moduleInfoMapper.selectByName(TEST_MODULE_NAME);
        if (existing != null) {
            return existing;
        }

        ModuleInfo module = new ModuleInfo();
        module.setName(TEST_MODULE_NAME);
        module.setDatasourceId(datasource.getId());
        module.setDorisSql(CREATE_TABLE_SQL);
        module.setTableName(TEST_TABLE_NAME);
        module.setCreateUser(TEST_USER);
        module.setUpdateUser(TEST_USER);

        // 设置查询配置，基于实际表结构和message variant字段
        String queryConfig =
                """
            {
              "timeField": "log_time",
              "keywordFields": [
                {
                  "fieldName": "message_text",
                  "searchMethod": "MATCH_PHRASE"
                },
                {
                  "fieldName": "host",
                  "searchMethod": "LIKE"
                },
                {
                  "fieldName": "source",
                  "searchMethod": "LIKE"
                },
                {
                  "fieldName": "message.level",
                  "searchMethod": "MATCH_PHRASE"
                },
                {
                  "fieldName": "message.service",
                  "searchMethod": "MATCH_ALL"
                },
                {
                  "fieldName": "message.logger",
                  "searchMethod": "LIKE"
                },
                {
                  "fieldName": "message.thread",
                  "searchMethod": "LIKE"
                },
                {
                  "fieldName": "message.request.method",
                  "searchMethod": "LIKE"
                }
              ]
            }
            """;
        module.setQueryConfig(queryConfig);

        moduleInfoMapper.insert(module);
        return module;
    }

    // ==================== 清理方法 ====================

    /** 清理测试数据 */
    @Transactional
    public void cleanupTestData() {
        log.info("开始清理LogSearch测试数据...");

        try {
            // 按照外键依赖关系的反序删除

            // 1. 删除模块
            cleanupModules();

            // 2. 删除数据源
            cleanupDatasources();

            log.info("LogSearch测试数据清理完成");

        } catch (Exception e) {
            log.error("清理LogSearch测试数据失败", e);
            // 清理失败不抛异常，避免影响测试
        }
    }

    private void cleanupModules() {
        try {
            ModuleInfo module = moduleInfoMapper.selectByName(TEST_MODULE_NAME);
            if (module != null) {
                moduleInfoMapper.deleteById(module.getId());
                log.debug("删除测试模块: {}", TEST_MODULE_NAME);
            }
        } catch (Exception e) {
            log.warn("清理测试模块数据时出错: {}", e.getMessage());
        }
    }

    private void cleanupDatasources() {
        try {
            DatasourceInfo datasource = datasourceMapper.selectByName(TEST_DATASOURCE_NAME);
            if (datasource != null) {
                datasourceMapper.deleteById(datasource.getId());
                log.debug("删除测试数据源: {}", TEST_DATASOURCE_NAME);
            }
        } catch (Exception e) {
            log.warn("清理测试数据源数据时出错: {}", e.getMessage());
        }
    }

    // ==================== 查询方法 ====================

    /** 获取测试模块信息 */
    public ModuleInfo getTestModule() {
        return moduleInfoMapper.selectByName(TEST_MODULE_NAME);
    }

    /** 获取测试数据源信息 */
    public DatasourceInfo getTestDatasource() {
        return datasourceMapper.selectByName(TEST_DATASOURCE_NAME);
    }
}
