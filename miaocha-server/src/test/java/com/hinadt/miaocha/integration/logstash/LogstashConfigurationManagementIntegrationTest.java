package com.hinadt.miaocha.integration.logstash;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.hinadt.miaocha.TestContainersFactory;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.parser.LogstashConfigParser;
import com.hinadt.miaocha.application.service.LogstashProcessService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessConfigUpdateRequestDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessCreateDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessResponseDTO;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.domain.mapper.LogstashProcessMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import com.hinadt.miaocha.integration.data.IntegrationMySQLTestDataInitializer;
import com.hinadt.miaocha.integration.logstash.support.LogstashConfigurationVerifier;
import com.hinadt.miaocha.integration.logstash.support.LogstashDatabaseVerifier;
import com.hinadt.miaocha.integration.logstash.support.LogstashInstanceStateVerifier;
import com.hinadt.miaocha.integration.logstash.support.LogstashMachineTestEnvironment;
import com.hinadt.miaocha.integration.logstash.support.LogstashPackageManager;
import com.hinadt.miaocha.integration.logstash.support.LogstashProcessVerifier;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Logstash配置管理集成测试
 *
 * <p>测试范围： 1. 配置回填任务验证和回调机制 2. 配置更新操作和远程文件验证 3. 配置刷新操作和不一致场景处理 4. 状态边界测试（运行/非运行状态下的操作限制）
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Logstash Story 4: 配置管理集成测试")
public class LogstashConfigurationManagementIntegrationTest {

    // Mock配置解析器以避免依赖真实解析逻辑
    @TestConfiguration
    static class MockConfigParserConfiguration {
        @Bean
        @Primary
        public LogstashConfigParser logstashConfigParser() {
            LogstashConfigParser mockParser = Mockito.mock(LogstashConfigParser.class);
            // mock 解析器, 跳过解析器校验方法
            when(mockParser.validateConfig(anyString()))
                    .thenReturn(LogstashConfigParser.ValidationResult.valid());
            when(mockParser.validateKafkaConfig(anyString()))
                    .thenReturn(LogstashConfigParser.ValidationResult.valid());
            when(mockParser.validateDorisOutput(anyString()))
                    .thenReturn(LogstashConfigParser.ValidationResult.valid());

            return mockParser;
        }
    }

    @Container @ServiceConnection
    static MySQLContainer<?> mysqlContainer = TestContainersFactory.mysqlContainer();

    // 核心服务
    @Autowired private LogstashProcessService logstashProcessService;
    @Autowired private LogstashMachineMapper logstashMachineMapper;
    @Autowired private LogstashProcessMapper logstashProcessMapper;
    @Autowired private IntegrationMySQLTestDataInitializer dataInitializer;
    @Autowired private ModuleInfoMapper moduleInfoMapper;

    // 支持类
    @Autowired private LogstashMachineTestEnvironment testEnvironment;
    @Autowired private LogstashPackageManager packageManager;
    @Autowired private LogstashInstanceStateVerifier stateVerifier;
    @Autowired private LogstashProcessVerifier processVerifier;
    @Autowired private LogstashDatabaseVerifier databaseVerifier;
    @Autowired private LogstashConfigurationVerifier configVerifier;

    private List<MachineInfo> testMachines; // 2台测试机器
    private Long testProcessId;
    private Long testModuleId;
    private List<LogstashMachine> testInstances;

    // 测试配置常量
    private static final String INITIAL_CONFIG_CONTENT =
            """
        input {
          beats {
            port => 5044
          }
        }
        output {
          elasticsearch {
            hosts => ["localhost:9200"]
            index => "test-logs-%{+YYYY.MM.dd}"
          }
        }
        """;

    private static final String UPDATED_CONFIG_CONTENT =
            """
        input {
          beats {
            port => 5044
          }
          file {
            path => "/var/log/test.log"
            start_position => "beginning"
          }
        }
        filter {
          grok {
            match => { "message" => "%{COMBINEDAPACHELOG}" }
          }
        }
        output {
          elasticsearch {
            hosts => ["localhost:9200"]
            index => "updated-logs-%{+YYYY.MM.dd}"
          }
        }
        """;

    private static final String UPDATED_JVM_OPTIONS =
            """
        -Xms1g
        -Xmx2g
        -XX:+UseG1GC
        -XX:MaxGCPauseMillis=200
        -Dfile.encoding=UTF-8
        """;

    private static final String UPDATED_LOGSTASH_YML =
            """
        http.host: 0.0.0.0
        http.port: 9600
        log.level: info
        path.logs: /var/log/logstash
        pipeline.workers: 4
        pipeline.batch.size: 250
        queue.type: memory
        """;

    @BeforeAll
    void setupTestEnvironment() {
        log.info("=== Logstash Story 4: 开始搭建测试环境 ===");

        // 初始化完整的测试数据
        dataInitializer.initializeTestData();

        // 获取测试模块ID
        ModuleInfo testModule = moduleInfoMapper.selectByName("test-module");
        if (testModule == null) {
            throw new IllegalStateException("测试模块未找到，请检查数据初始化");
        }
        testModuleId = testModule.getId();

        // 确保Logstash包可用并配置正确
        packageManager.ensureLogstashPackageAvailable();
        boolean configValid = packageManager.verifyLogstashPropertiesConfiguration();
        if (!configValid) {
            throw new IllegalStateException("LogstashProperties配置设置失败");
        }

        // 启动2个SSH容器用于配置管理测试
        testEnvironment.startSshContainers(2);
        testMachines = testEnvironment.machines();

        // 持久化机器信息到数据库
        testEnvironment.persistMachinesToDatabase(testMachines);

        log.info(
                "测试环境搭建完成 - SSH容器: {}, MySQL容器: {}, 模块ID: {}",
                testMachines.size(),
                mysqlContainer.isRunning(),
                testModuleId);
    }

    @AfterAll
    void cleanupTestEnvironment() {
        log.info("=== Logstash Story 4: 开始清理测试环境 ===");
        testEnvironment.cleanupContainerMachines(testMachines);
        dataInitializer.cleanupTestData();
        testEnvironment.cleanup();
        log.info("测试环境清理完成");
    }

    /** 创建仅包含主配置的进程，触发配置回填任务 */
    @Test
    @Order(1)
    @DisplayName("创建进程触发配置回填任务验证")
    void createProcessWithConfigSyncCallback() throws InterruptedException {
        log.info("🧪 测试：创建仅包含主配置的进程，验证配置回填任务");

        // 1. 创建仅包含主配置的进程（缺少JVM和系统配置，应触发回填）
        LogstashProcessCreateDTO createDTO = buildCreateDTO("config-sync-test");
        createDTO.setConfigContent(INITIAL_CONFIG_CONTENT);
        // 故意不设置jvmOptions和logstashYml，触发回填机制
        createDTO.setJvmOptions(null);
        createDTO.setLogstashYml(null);

        LogstashProcessResponseDTO createdProcess =
                logstashProcessService.createLogstashProcess(createDTO);
        assertThat(createdProcess).isNotNull();
        testProcessId = createdProcess.getId();

        // 2. 验证实例创建并等待初始化完成
        List<LogstashMachine> instances =
                logstashMachineMapper.selectByLogstashProcessId(testProcessId);
        assertThat(instances).hasSize(2);
        testInstances = instances;

        for (LogstashMachine instance : instances) {
            boolean initCompleted =
                    stateVerifier.waitForInstanceState(
                            instance.getId(),
                            List.of(
                                    LogstashMachineState.NOT_STARTED,
                                    LogstashMachineState.INITIALIZE_FAILED),
                            180,
                            TimeUnit.SECONDS);
            assertThat(initCompleted).isTrue();
            assertThat(stateVerifier.getCurrentState(instance.getId()))
                    .isEqualTo(LogstashMachineState.NOT_STARTED);
        }

        log.info("✅ 进程创建成功，实例初始化完成");
    }

    /** 验证配置回填任务执行结果和回调机制 */
    @Test
    @Order(2)
    @DisplayName("验证配置回填任务执行结果")
    void verifyConfigurationSyncCallbackResult() {
        log.info("🧪 测试：验证配置回填任务执行结果");

        // 等待配置回填任务完成
        assertThat(configVerifier.verifyConfigSyncTask(testProcessId, 40, 3))
                .withFailMessage("配置回填任务验证失败")
                .isTrue();

        // 验证进程级和实例级配置内容一致
        assertThat(configVerifier.verifyInstancesSync(testProcessId))
                .withFailMessage("实例配置同步验证失败")
                .isTrue();

        log.info("✅ 配置回填任务验证通过");
    }

    /** 测试在NOT_STARTED状态下的配置更新（应该成功） */
    @Test
    @Order(3)
    @DisplayName("NOT_STARTED状态下配置更新测试")
    void testConfigurationUpdateInNotStartedState() {
        log.info("🧪 测试：NOT_STARTED状态下配置更新");

        // 1. 确认所有实例都在NOT_STARTED状态
        for (LogstashMachine instance : testInstances) {
            assertThat(stateVerifier.getCurrentState(instance.getId()))
                    .isEqualTo(LogstashMachineState.NOT_STARTED);
        }

        // 2. 更新主配置
        LogstashProcessConfigUpdateRequestDTO updateRequest =
                LogstashProcessConfigUpdateRequestDTO.builder()
                        .configContent(UPDATED_CONFIG_CONTENT)
                        .logstashMachineIds(null) // 更新所有实例
                        .build();

        LogstashProcessResponseDTO response =
                logstashProcessService.updateLogstashConfig(testProcessId, updateRequest);
        assertThat(response).isNotNull();

        // 3. 验证数据库和远程文件都已更新
        assertThat(
                        configVerifier.verifyCompleteUpdate(
                                testProcessId, UPDATED_CONFIG_CONTENT, null, null))
                .withFailMessage("主配置更新验证失败")
                .isTrue();

        log.info("✅ NOT_STARTED状态下配置更新验证通过");
    }

    /** 测试JVM配置更新并验证远程文件 */
    @Test
    @Order(4)
    @DisplayName("JVM配置更新并验证远程文件")
    void testJvmConfigurationUpdateWithRemoteVerification() {
        log.info("🧪 测试：JVM配置更新并验证远程文件");

        // 更新JVM配置
        LogstashProcessConfigUpdateRequestDTO updateRequest =
                LogstashProcessConfigUpdateRequestDTO.builder()
                        .jvmOptions(UPDATED_JVM_OPTIONS)
                        .logstashMachineIds(null) // 更新所有实例
                        .build();

        LogstashProcessResponseDTO response =
                logstashProcessService.updateLogstashConfig(testProcessId, updateRequest);
        assertThat(response).isNotNull();

        // 验证完整的配置更新（数据库 + 远程文件）
        assertThat(
                        configVerifier.verifyCompleteUpdate(
                                testProcessId, null, UPDATED_JVM_OPTIONS, null))
                .withFailMessage("JVM配置更新验证失败")
                .isTrue();

        log.info("✅ JVM配置更新验证通过");
    }

    /** 测试系统配置更新并验证远程文件 */
    @Test
    @Order(5)
    @DisplayName("系统配置更新并验证远程文件")
    void testSystemConfigurationUpdateWithRemoteVerification() {
        log.info("🧪 测试：系统配置更新并验证远程文件");

        // 更新系统配置
        LogstashProcessConfigUpdateRequestDTO updateRequest =
                LogstashProcessConfigUpdateRequestDTO.builder()
                        .logstashYml(UPDATED_LOGSTASH_YML)
                        .logstashMachineIds(null) // 更新所有实例
                        .build();

        LogstashProcessResponseDTO response =
                logstashProcessService.updateLogstashConfig(testProcessId, updateRequest);
        assertThat(response).isNotNull();

        // 验证完整的配置更新（数据库 + 远程文件）
        assertThat(
                        configVerifier.verifyCompleteUpdate(
                                testProcessId, null, null, UPDATED_LOGSTASH_YML))
                .withFailMessage("系统配置更新验证失败")
                .isTrue();

        log.info("✅ 系统配置更新验证通过");
    }

    /** 启动实例并测试RUNNING状态下配置操作的边界条件 */
    @Test
    @Order(6)
    @DisplayName("RUNNING状态下配置操作边界测试")
    void testConfigurationOperationsInRunningState() throws InterruptedException {
        log.info("🧪 测试：RUNNING状态下配置操作边界条件");

        // 1. 启动实例
        List<Long> instanceIds = testInstances.stream().map(LogstashMachine::getId).toList();
        logstashProcessService.startLogstashProcess(testProcessId);

        // 等待启动完成
        for (Long instanceId : instanceIds) {
            boolean startCompleted =
                    stateVerifier.waitForInstanceState(
                            instanceId,
                            List.of(
                                    LogstashMachineState.RUNNING,
                                    LogstashMachineState.START_FAILED),
                            120,
                            TimeUnit.SECONDS);
            assertThat(startCompleted).isTrue();
            assertThat(stateVerifier.getCurrentState(instanceId))
                    .isEqualTo(LogstashMachineState.RUNNING);
        }

        // 验证实例确实在运行状态
        for (Long instanceId : instanceIds) {
            assertThat(stateVerifier.getCurrentState(instanceId))
                    .withFailMessage("实例 {} 未能启动到运行状态", instanceId)
                    .isEqualTo(LogstashMachineState.RUNNING);
        }

        // 2. 尝试在运行状态下更新配置（应该失败）
        LogstashProcessConfigUpdateRequestDTO updateRequest =
                LogstashProcessConfigUpdateRequestDTO.builder()
                        .configContent("input { tcp { port => 9999 } } output { stdout { } }")
                        .build();

        log.info("📝 在运行状态下尝试更新配置（预期失败）");
        assertThatThrownBy(
                        () ->
                                logstashProcessService.updateLogstashConfig(
                                        testProcessId, updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请先停止实例");

        // 3. 尝试在运行状态下刷新配置（应该失败）
        LogstashProcessConfigUpdateRequestDTO refreshRequest =
                LogstashProcessConfigUpdateRequestDTO.builder()
                        .logstashMachineIds(instanceIds)
                        .build();

        log.info("📝 在运行状态下尝试刷新配置（预期失败）");
        assertThatThrownBy(
                        () ->
                                logstashProcessService.refreshLogstashConfig(
                                        testProcessId, refreshRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请先停止实例");

        // 4. 停止实例为后续测试做准备
        logstashProcessService.stopLogstashProcess(testProcessId);

        // 等待停止完成
        for (Long instanceId : instanceIds) {
            boolean stopCompleted =
                    stateVerifier.waitForInstanceState(
                            instanceId,
                            List.of(
                                    LogstashMachineState.NOT_STARTED,
                                    LogstashMachineState.STOP_FAILED),
                            90,
                            TimeUnit.SECONDS);
            assertThat(stopCompleted).isTrue();
            assertThat(stateVerifier.getCurrentState(instanceId))
                    .isEqualTo(LogstashMachineState.NOT_STARTED);
        }

        log.info("✅ RUNNING状态下配置操作边界测试通过");
    }

    /** 测试配置刷新操作 - 模拟数据库和远程文件不一致后刷新 */
    @Test
    @Order(7)
    @DisplayName("配置刷新操作 - 数据库配置同步到远程文件")
    void testConfigurationRefreshAfterMismatch() throws InterruptedException {
        log.info("🧪 测试：配置刷新操作");

        // 1. 获取第一个实例进行测试
        LogstashMachine firstInstance = testInstances.get(0);

        // 2. 模拟远程文件被手动修改，造成与数据库不一致
        String mismatchConfig =
                """
            input {
              tcp {
                port => 9999
              }
            }
            output {
              stdout { }
            }
            """;

        assertThat(configVerifier.simulateConfigMismatch(firstInstance, mismatchConfig))
                .withFailMessage("模拟配置不一致失败")
                .isTrue();

        // 3. 执行配置刷新操作
        LogstashProcessConfigUpdateRequestDTO refreshRequest =
                LogstashProcessConfigUpdateRequestDTO.builder()
                        .logstashMachineIds(List.of(firstInstance.getId()))
                        .build();

        log.info("📝 执行配置刷新");
        logstashProcessService.refreshLogstashConfig(testProcessId, refreshRequest);

        // 等待刷新完成
        Thread.sleep(5000);

        // 4. 验证远程文件被恢复为数据库中的配置
        LogstashProcess process = logstashProcessMapper.selectById(testProcessId);
        assertThat(configVerifier.verifyConfigRefresh(firstInstance, process.getConfigContent()))
                .withFailMessage("配置刷新验证失败")
                .isTrue();

        log.info("✅ 配置刷新验证通过");
    }

    /** 部分实例配置更新测试 */
    @Test
    @Order(8)
    @DisplayName("部分实例配置更新测试")
    void testPartialInstanceConfigurationUpdate() {
        log.info("🧪 测试：部分实例配置更新");

        // 只更新第一个实例的JVM配置
        String partialJvmOptions =
                """
            -Xms512m
            -Xmx1g
            -XX:+UseParallelGC
            """;

        LogstashProcessConfigUpdateRequestDTO updateRequest =
                LogstashProcessConfigUpdateRequestDTO.builder()
                        .jvmOptions(partialJvmOptions)
                        .logstashMachineIds(Arrays.asList(testInstances.get(0).getId()))
                        .build();

        LogstashProcessResponseDTO response =
                logstashProcessService.updateLogstashConfig(testProcessId, updateRequest);
        assertThat(response).isNotNull();

        // 验证只有指定实例的配置被更新
        LogstashMachine firstInstance =
                logstashMachineMapper.selectById(testInstances.get(0).getId());
        LogstashMachine secondInstance =
                logstashMachineMapper.selectById(testInstances.get(1).getId());

        assertThat(firstInstance.getJvmOptions()).isEqualTo(partialJvmOptions);
        assertThat(secondInstance.getJvmOptions()).isNotEqualTo(partialJvmOptions); // 应该保持原来的配置

        // 验证远程文件也已更新
        assertThat(
                        configVerifier.verifyInstanceRemoteFiles(
                                firstInstance, null, partialJvmOptions, null))
                .withFailMessage("第一个实例远程JVM配置文件验证失败")
                .isTrue();

        log.info("✅ 部分实例配置更新验证通过");
    }

    /** 最终清理验证 */
    @Test
    @Order(9)
    @DisplayName("最终清理验证")
    void finalCleanupAndValidation() {
        log.info("🧪 测试：最终清理验证");

        // 删除测试进程
        if (testProcessId != null) {
            logstashProcessService.deleteLogstashProcess(testProcessId);

            // 验证进程和实例记录被清理
            LogstashProcess deletedProcess = logstashProcessMapper.selectById(testProcessId);
            assertThat(deletedProcess).isNull();

            List<LogstashMachine> deletedInstances =
                    logstashMachineMapper.selectByLogstashProcessId(testProcessId);
            assertThat(deletedInstances).isEmpty();
        }

        log.info("✅ 最终清理验证通过");
    }

    // ==================== 私有方法 ====================

    private LogstashProcessCreateDTO buildCreateDTO(String processName) {
        return LogstashProcessCreateDTO.builder()
                .name(processName)
                .moduleId(testModuleId)
                .machineIds(testMachines.stream().map(MachineInfo::getId).toList())
                .build();
    }
}
