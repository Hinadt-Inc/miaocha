package com.hinadt.miaocha.integration.logstash;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.TestContainersFactory;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.enums.TaskOperationType;
import com.hinadt.miaocha.application.logstash.parser.LogstashConfigParser;
import com.hinadt.miaocha.application.service.LogstashProcessService;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessCreateDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessResponseDTO;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.domain.mapper.LogstashProcessMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import com.hinadt.miaocha.integration.data.IntegrationTestDataInitializer;
import com.hinadt.miaocha.integration.logstash.support.LogstashDatabaseVerifier;
import com.hinadt.miaocha.integration.logstash.support.LogstashInstanceStateVerifier;
import com.hinadt.miaocha.integration.logstash.support.LogstashMachineTestEnvironment;
import com.hinadt.miaocha.integration.logstash.support.LogstashPackageManager;
import com.hinadt.miaocha.integration.logstash.support.LogstashProcessVerifier;
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
 * Story 2: Logstash实例基础生命周期测试
 *
 * <p>目标：验证单个实例的完整生命周期管理 - 创建进程和实例记录 - 异步初始化过程 - 启动和运行验证 - 停止和清理验证
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Logstash Story 2: Logstash实例基础生命周期测试")
public class LogstashBasicLifecycleIntegrationTest {

    /** 测试配置类 - 使用Mock替代真实的LogstashConfigParser 这样集成测试可以专注于进程生命周期管理，而不受配置验证逻辑影响 */
    @TestConfiguration
    static class MockConfigParserConfiguration {

        @Bean
        @Primary
        public LogstashConfigParser logstashConfigParser() {
            LogstashConfigParser mockParser = mock(LogstashConfigParser.class);

            // Mock配置验证始终返回成功，让测试专注于进程管理逻辑
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

    @Autowired private LogstashProcessService logstashProcessService;
    @Autowired private LogstashMachineMapper logstashMachineMapper;
    @Autowired private LogstashProcessMapper logstashProcessMapper;
    @Autowired private IntegrationTestDataInitializer dataInitializer;
    @Autowired private ModuleInfoMapper moduleInfoMapper;

    // 支持类
    @Autowired private LogstashMachineTestEnvironment testEnvironment;
    @Autowired private LogstashPackageManager packageManager;
    @Autowired private LogstashInstanceStateVerifier stateVerifier;
    @Autowired private LogstashProcessVerifier processVerifier;
    @Autowired private LogstashDatabaseVerifier databaseVerifier;

    // Mock Logstash配置解析器，绕过配置验证，专注于进程生命周期测试
    @Autowired private LogstashConfigParser logstashConfigParser;

    private List<MachineInfo> testMachines;
    private Long testProcessId;
    private Long testInstanceId;
    private Long testModuleId;

    @BeforeAll
    void setupTestEnvironment() {
        log.info("=== Logstash Story 2: 开始搭建测试环境 ===");

        // 初始化完整的测试数据（用户、机器、数据源、模块、进程等）
        dataInitializer.initializeTestData();

        // 通过模块名称获取创建的测试模块ID
        ModuleInfo testModule = moduleInfoMapper.selectByName("test-module");
        if (testModule == null) {
            throw new IllegalStateException("测试模块未找到，请检查数据初始化");
        }
        testModuleId = testModule.getId();

        // 确保Logstash包可用并动态设置配置
        packageManager.ensureLogstashPackageAvailable();

        // 验证配置是否正确设置
        boolean configValid = packageManager.verifyLogstashPropertiesConfiguration();
        if (!configValid) {
            throw new IllegalStateException("LogstashProperties配置设置失败");
        }

        testEnvironment.startSshContainers(1);
        testMachines = testEnvironment.machines();

        // 将容器对应的机器信息保存到数据库中，供业务代码查询使用
        testEnvironment.persistMachinesToDatabase(testMachines);

        log.info(
                "测试环境搭建完成 - SSH容器: {}, MySQL容器: {}, 模块ID: {}, 配置验证: Mock模式",
                testMachines.size(),
                mysqlContainer.isRunning(),
                testModuleId);
    }

    @AfterAll
    void cleanupTestEnvironment() {
        log.info("=== Logstash Story 2: 开始清理测试环境 ===");

        // 先清理容器机器记录
        testEnvironment.cleanupContainerMachines(testMachines);

        dataInitializer.cleanupTestData();
        testEnvironment.cleanup();
        log.info("测试环境清理完成");
    }

    @Test
    @Order(1)
    @DisplayName("创建进程和实例记录")
    void createProcessAndInstances() {
        log.info("📝 测试：创建进程和实例记录");

        LogstashProcessCreateDTO createDTO = buildCreateDTO("lifecycle-test-process");
        LogstashProcessResponseDTO createdProcess =
                logstashProcessService.createLogstashProcess(createDTO);

        // 验证进程创建成功
        assertThat(createdProcess).isNotNull();
        assertThat(createdProcess.getId()).isNotNull();
        testProcessId = createdProcess.getId();

        // 验证数据库记录
        var processRecord = logstashProcessMapper.selectById(testProcessId);
        assertThat(processRecord).isNotNull();
        assertThat(processRecord.getName()).isEqualTo("lifecycle-test-process");

        // 验证实例创建
        List<LogstashMachine> instances =
                logstashMachineMapper.selectByLogstashProcessId(testProcessId);
        assertThat(instances).hasSize(1);
        testInstanceId = instances.get(0).getId();

        log.info("✅ 进程和实例数据库记录创建成功 - 进程ID: {}, 实例ID: {}", testProcessId, testInstanceId);
    }

    @Test
    @Order(2)
    @DisplayName("等待异步初始化完成")
    void waitForInitializationCompletion() throws InterruptedException {
        log.info("📝 测试：等待异步初始化完成");

        // 等待初始化完成
        boolean initCompleted =
                stateVerifier.waitForInstanceState(
                        testInstanceId,
                        List.of(
                                LogstashMachineState.NOT_STARTED,
                                LogstashMachineState.INITIALIZE_FAILED),
                        180,
                        TimeUnit.SECONDS);

        assertThat(initCompleted).isTrue();

        // 验证最终状态
        LogstashMachineState finalState = stateVerifier.getCurrentState(testInstanceId);
        assertThat(finalState).isEqualTo(LogstashMachineState.NOT_STARTED);

        log.info("✅ 异步初始化完成 - 最终状态: {}", finalState);
    }

    @Test
    @Order(3)
    @DisplayName("验证初始化步骤执行结果")
    void verifyInitializationSteps() {
        log.info("📝 测试：验证初始化步骤执行结果");

        // 验证初始化任务完成（StateVerifier层面）
        boolean hasCompletedTask = stateVerifier.hasCompletedInitializationTask(testInstanceId);
        assertThat(hasCompletedTask).isTrue();

        // 验证关键步骤完成（StateVerifier层面）
        boolean stepsCompleted = stateVerifier.verifyInitializationStepsCompleted(testInstanceId);
        assertThat(stepsCompleted).isTrue();

        // 验证数据库层面的初始化任务和步骤完整性（基于真实代码流程）
        boolean initTaskAndStepsValid =
                databaseVerifier.verifyInitializationTaskAndSteps(testInstanceId);
        assertThat(initTaskAndStepsValid).isTrue();

        // 验证初始化任务的时间戳
        boolean initTaskTimestampsValid =
                databaseVerifier.verifyTaskTimestamps(testInstanceId, TaskOperationType.INITIALIZE);
        assertThat(initTaskTimestampsValid).isTrue();

        // 验证初始化步骤的时间戳
        boolean initStepTimestampsValid =
                databaseVerifier.verifyStepTimestamps(testInstanceId, TaskOperationType.INITIALIZE);
        assertThat(initStepTimestampsValid).isTrue();

        // 打印详细信息用于调试
        databaseVerifier.logInstanceTasksAndStepsDetails(testInstanceId);

        log.info("✅ 初始化步骤及数据库记录验证通过");
    }

    @Test
    @Order(4)
    @DisplayName("验证实例目录和文件结构")
    void verifyInstanceDirectoryStructure() {
        log.info("📝 测试：验证实例目录和文件结构");

        LogstashMachine instance = logstashMachineMapper.selectById(testInstanceId);
        boolean directoryValid =
                processVerifier.verifyInstanceDirectoryStructure(instance, testMachines);

        assertThat(directoryValid).isTrue();
        log.info("✅ 实例目录结构验证通过");
    }

    @Test
    @Order(5)
    @DisplayName("启动进程")
    void startProcess() throws InterruptedException {
        log.info("📝 测试：启动进程");

        LogstashProcessResponseDTO startedProcess =
                logstashProcessService.startLogstashProcess(testProcessId);
        assertThat(startedProcess).isNotNull();

        // 等待启动完成
        boolean startCompleted =
                stateVerifier.waitForInstanceState(
                        testInstanceId,
                        List.of(LogstashMachineState.RUNNING, LogstashMachineState.START_FAILED),
                        120,
                        TimeUnit.SECONDS);

        assertThat(startCompleted).isTrue();

        LogstashMachineState finalState = stateVerifier.getCurrentState(testInstanceId);
        assertThat(finalState).isEqualTo(LogstashMachineState.RUNNING);

        log.info("✅ 进程启动成功 - 状态: {}", finalState);
    }

    @Test
    @Order(6)
    @DisplayName("验证进程真实运行状态")
    void verifyProcessRunning() {
        log.info("📝 测试：验证进程真实运行状态");

        LogstashMachine instance = logstashMachineMapper.selectById(testInstanceId);

        // 验证启动步骤完成
        boolean startupStepsCompleted = stateVerifier.verifyStartupStepsCompleted(testInstanceId);
        assertThat(startupStepsCompleted).isTrue();

        // 验证进程实际运行
        boolean processRunning =
                processVerifier.verifyProcessActuallyRunning(instance, testMachines);
        assertThat(processRunning).isTrue();

        // 验证PID记录
        String currentPid = stateVerifier.getCurrentPid(testInstanceId);
        assertThat(currentPid).isNotBlank();

        // 验证启动任务和步骤的数据库记录（基于真实代码流程）
        boolean startTaskAndStepsValid = databaseVerifier.verifyStartTaskAndSteps(testInstanceId);
        assertThat(startTaskAndStepsValid).isTrue();

        // 验证启动任务的时间戳
        boolean startTaskTimestampsValid =
                databaseVerifier.verifyTaskTimestamps(testInstanceId, TaskOperationType.START);
        assertThat(startTaskTimestampsValid).isTrue();

        // 验证启动步骤的时间戳
        boolean startStepTimestampsValid =
                databaseVerifier.verifyStepTimestamps(testInstanceId, TaskOperationType.START);
        assertThat(startStepTimestampsValid).isTrue();

        log.info("✅ 进程真实运行状态及数据库记录验证通过 - PID: {}", currentPid);
    }

    @Test
    @Order(7)
    @DisplayName("停止进程")
    void stopProcess() throws InterruptedException {
        log.info("📝 测试：停止进程");

        LogstashProcessResponseDTO stoppedProcess =
                logstashProcessService.stopLogstashProcess(testProcessId);
        assertThat(stoppedProcess).isNotNull();

        // 等待停止完成
        boolean stopCompleted =
                stateVerifier.waitForInstanceState(
                        testInstanceId,
                        List.of(LogstashMachineState.NOT_STARTED, LogstashMachineState.STOP_FAILED),
                        90,
                        TimeUnit.SECONDS);

        assertThat(stopCompleted).isTrue();

        LogstashMachineState finalState = stateVerifier.getCurrentState(testInstanceId);
        assertThat(finalState).isEqualTo(LogstashMachineState.NOT_STARTED);

        log.info("✅ 进程停止成功 - 状态: {}", finalState);
    }

    @Test
    @Order(8)
    @DisplayName("验证进程停止状态")
    void verifyProcessStopped() {
        log.info("📝 测试：验证进程停止状态");

        LogstashMachine instance = logstashMachineMapper.selectById(testInstanceId);

        // 验证进程确实停止
        boolean processStopped = processVerifier.verifyProcessStopped(instance, testMachines);
        assertThat(processStopped).isTrue();

        // 验证PID已清理
        boolean pidCleared = stateVerifier.isPidCleared(testInstanceId);
        assertThat(pidCleared).isTrue();

        // 验证停止任务和步骤的数据库记录（基于真实代码流程）
        boolean stopTaskAndStepsValid = databaseVerifier.verifyStopTaskAndSteps(testInstanceId);
        assertThat(stopTaskAndStepsValid).isTrue();

        // 验证停止任务的时间戳
        boolean stopTaskTimestampsValid =
                databaseVerifier.verifyTaskTimestamps(testInstanceId, TaskOperationType.STOP);
        assertThat(stopTaskTimestampsValid).isTrue();

        // 验证停止步骤的时间戳
        boolean stopStepTimestampsValid =
                databaseVerifier.verifyStepTimestamps(testInstanceId, TaskOperationType.STOP);
        assertThat(stopStepTimestampsValid).isTrue();

        log.info("✅ 进程停止状态及数据库记录验证通过");
    }

    @Test
    @Order(9)
    @DisplayName("删除进程")
    void deleteProcess() {
        log.info("📝 测试：删除进程");

        logstashProcessService.deleteLogstashProcess(testProcessId);

        // 验证数据库记录删除
        var processRecord = logstashProcessMapper.selectById(testProcessId);
        assertThat(processRecord).isNull();

        // 验证实例记录删除
        List<LogstashMachine> instances =
                logstashMachineMapper.selectByLogstashProcessId(testProcessId);
        assertThat(instances).isEmpty();

        log.info("✅ 进程删除成功");
    }

    @Test
    @Order(10)
    @DisplayName("验证实例目录清理和数据库记录清理")
    void verifyCleanup() {
        log.info("📝 测试：验证实例目录清理和数据库记录清理");

        // 重新获取实例信息（在删除前的状态）
        LogstashMachine instanceForCleanup = new LogstashMachine();
        instanceForCleanup.setId(testInstanceId);
        instanceForCleanup.setMachineId(testMachines.get(0).getId());
        instanceForCleanup.setDeployPath("/opt/logstash/lifecycle-test-process_" + testInstanceId);

        // 验证实例目录清理
        boolean directoryCleanedUp =
                processVerifier.verifyInstanceDirectoryCleanup(instanceForCleanup, testMachines);
        assertThat(directoryCleanedUp).isTrue();

        // 验证任务和步骤记录被正确清理
        boolean recordsCleanedUp =
                databaseVerifier.verifyTaskAndStepRecordsCleanedUp(testInstanceId);
        assertThat(recordsCleanedUp).isTrue();

        log.info("✅ 实例目录清理和数据库记录清理验证通过");
    }

    // ==================== 辅助方法 ====================

    private LogstashProcessCreateDTO buildCreateDTO(String processName) {
        LogstashProcessCreateDTO dto = new LogstashProcessCreateDTO();
        dto.setName(processName);
        dto.setMachineIds(List.of(testMachines.get(0).getId()));
        dto.setModuleId(testModuleId); // 设置模块ID
        dto.setJvmOptions("-Xms1g -Xmx1g");
        // 使用简化的测试配置 - 通过Mock LogstashConfigParser绕过Kafka+Doris验证
        // 集成测试重点关注进程生命周期管理，而非配置语法正确性
        dto.setConfigContent(
                """
            input {
              stdin {}
            }

            filter {
              # 测试标记
              mutate {
                add_field => { "test_mode" => "integration_lifecycle" }
              }
            }

            output {
              stdout { codec => rubydebug }
            }
            """);
        return dto;
    }
}
