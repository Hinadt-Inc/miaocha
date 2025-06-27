package com.hinadt.miaocha.integration.logstash;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.TestContainersFactory;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.parser.LogstashConfigParser;
import com.hinadt.miaocha.application.service.LogstashProcessService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessCreateDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessResponseDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessScaleRequestDTO;
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
 * Story 3: Logstash多实例扩缩容管理集成测试
 *
 * <p>目标：验证从1个实例扩容到3个实例的完整流程： - 基于自定义部署路径的扩容操作 - 路径冲突检查和一机多实例支持 - 多实例并行初始化和状态管理 - 基于LogstashMachine
 * ID的精确缩容 - 完整的生命周期验证（创建→扩容→启动→停止→缩容→删除）
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Logstash Story 3: 多实例扩缩容管理集成测试")
public class LogstashInstanceScalingIntegrationTest {

    /** 测试配置类 - Mock配置解析器 */
    @TestConfiguration
    static class MockConfigParserConfiguration {
        @Bean
        @Primary
        public LogstashConfigParser logstashConfigParser() {
            LogstashConfigParser mockParser = mock(LogstashConfigParser.class);
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

    private List<MachineInfo> testMachines; // 3台测试机器
    private Long testProcessId;
    private Long testModuleId;
    private Long initialInstanceId; // 初始实例ID
    private Long scaleOutInstance1Id; // 扩容实例1 ID
    private Long scaleOutInstance2Id; // 扩容实例2 ID

    @BeforeAll
    void setupTestEnvironment() {
        log.info("=== Logstash Story 3: 开始搭建测试环境 ===");

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

        // 启动3个SSH容器用于扩缩容测试
        testEnvironment.startSshContainers(3);
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
        log.info("=== Logstash Story 3: 开始清理测试环境 ===");
        testEnvironment.cleanupContainerMachines(testMachines);
        dataInitializer.cleanupTestData();
        testEnvironment.cleanup();
        log.info("测试环境清理完成");
    }

    @Test
    @Order(1)
    @DisplayName("创建+启动初始进程（1个实例，自定义路径）")
    void createAndStartInitialProcess() throws InterruptedException {
        log.info("📝 测试：创建+启动初始进程（1个实例，自定义路径）");

        // 1. 创建进程
        LogstashProcessCreateDTO createDTO = buildCreateDTO("scaling-test-process");
        createDTO.setCustomDeployPath("/opt/logstash/scaling-base"); // 基础路径
        createDTO.setMachineIds(List.of(testMachines.get(0).getId()));

        LogstashProcessResponseDTO createdProcess =
                logstashProcessService.createLogstashProcess(createDTO);
        assertThat(createdProcess).isNotNull();
        testProcessId = createdProcess.getId();

        // 获取初始实例ID
        List<LogstashMachine> instances =
                logstashMachineMapper.selectByLogstashProcessId(testProcessId);
        assertThat(instances).hasSize(1);
        initialInstanceId = instances.get(0).getId();

        // 2. 等待初始化完成
        boolean initCompleted =
                stateVerifier.waitForInstanceState(
                        initialInstanceId,
                        List.of(
                                LogstashMachineState.NOT_STARTED,
                                LogstashMachineState.INITIALIZE_FAILED),
                        180,
                        TimeUnit.SECONDS);
        assertThat(initCompleted).isTrue();
        assertThat(stateVerifier.getCurrentState(initialInstanceId))
                .isEqualTo(LogstashMachineState.NOT_STARTED);

        // 3. 启动进程
        LogstashProcessResponseDTO startedProcess =
                logstashProcessService.startLogstashProcess(testProcessId);
        assertThat(startedProcess).isNotNull();

        // 4. 等待启动完成
        boolean startCompleted =
                stateVerifier.waitForInstanceState(
                        initialInstanceId,
                        List.of(LogstashMachineState.RUNNING, LogstashMachineState.START_FAILED),
                        120,
                        TimeUnit.SECONDS);
        assertThat(startCompleted).isTrue();
        assertThat(stateVerifier.getCurrentState(initialInstanceId))
                .isEqualTo(LogstashMachineState.RUNNING);

        log.info("✅ 初始进程创建+启动完成 - 进程ID: {}, 实例ID: {}", testProcessId, initialInstanceId);
    }

    @Test
    @Order(2)
    @DisplayName("深度验证初始实例状态（数据库+文件系统+进程）")
    void deepVerifyInitialInstanceState() {
        log.info("📝 测试：深度验证初始实例状态");

        LogstashMachine initialInstance = logstashMachineMapper.selectById(initialInstanceId);

        // 1. 数据库层面验证
        assertThat(initialInstance.getDeployPath()).isEqualTo("/opt/logstash/scaling-base");
        assertThat(initialInstance.getState()).isEqualTo(LogstashMachineState.RUNNING.name());

        // 验证路径占用检查
        LogstashMachine pathCheck =
                logstashMachineMapper.selectByMachineAndPath(
                        testMachines.get(0).getId(), "/opt/logstash/scaling-base");
        assertThat(pathCheck.getId()).isEqualTo(initialInstanceId);

        // 验证初始化和启动任务步骤完整性
        boolean initTaskValid =
                databaseVerifier.verifyInitializationTaskAndSteps(initialInstanceId);
        boolean startTaskValid = databaseVerifier.verifyStartTaskAndSteps(initialInstanceId);
        assertThat(initTaskValid).isTrue();
        assertThat(startTaskValid).isTrue();

        // 2. 文件系统层面验证
        boolean directoryStructureValid =
                processVerifier.verifyInstanceDirectoryStructure(initialInstance, testMachines);
        assertThat(directoryStructureValid).isTrue();

        // 3. 进程层面验证
        boolean processRunning =
                processVerifier.verifyProcessActuallyRunning(initialInstance, testMachines);
        assertThat(processRunning).isTrue();

        String currentPid = stateVerifier.getCurrentPid(initialInstanceId);
        assertThat(currentPid).isNotBlank();

        log.info("✅ 初始实例深度验证通过 - PID: {}, 路径: {}", currentPid, initialInstance.getDeployPath());
    }

    @Test
    @Order(3)
    @DisplayName("路径冲突测试 - 验证扩容路径冲突检查逻辑")
    void testScaleOutPathConflict() {
        log.info("📝 测试：路径冲突检查逻辑");

        // 尝试在相同机器相同路径扩容 - 应该失败
        LogstashProcessScaleRequestDTO conflictDTO = new LogstashProcessScaleRequestDTO();
        conflictDTO.setAddMachineIds(List.of(testMachines.get(0).getId())); // 同一台机器
        conflictDTO.setCustomDeployPath("/opt/logstash/scaling-base"); // 冲突路径

        // 验证抛出路径冲突异常 - 基于实际实现代码的精确验证
        assertThatThrownBy(
                        () ->
                                logstashProcessService.scaleLogstashProcess(
                                        testProcessId, conflictDTO))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> {
                            BusinessException be = (BusinessException) ex;
                            assertThat(be.getErrorCode())
                                    .isEqualTo(ErrorCode.LOGSTASH_MACHINE_ALREADY_ASSOCIATED);
                        });

        // 验证实例数量没有增加
        List<LogstashMachine> instances =
                logstashMachineMapper.selectByLogstashProcessId(testProcessId);
        assertThat(instances).hasSize(1);

        log.info("✅ 路径冲突检查逻辑验证通过");
    }

    @Test
    @Order(4)
    @DisplayName("扩容操作 - 添加2个实例到不同机器")
    void scaleOutAddTwoInstances() throws InterruptedException {
        log.info("📝 测试：扩容操作 - 添加2个实例到不同机器");

        // 1. 执行扩容操作
        LogstashProcessScaleRequestDTO scaleDTO = new LogstashProcessScaleRequestDTO();
        scaleDTO.setAddMachineIds(
                List.of(testMachines.get(1).getId(), testMachines.get(2).getId()));
        scaleDTO.setCustomDeployPath("/opt/logstash/scaling-expanded"); // 不同路径避免冲突

        LogstashProcessResponseDTO scaledProcess =
                logstashProcessService.scaleLogstashProcess(testProcessId, scaleDTO);
        assertThat(scaledProcess).isNotNull();

        // 2. 等待扩容实例初始化完成
        List<LogstashMachine> allInstances =
                logstashMachineMapper.selectByLogstashProcessId(testProcessId);
        assertThat(allInstances).hasSize(3);

        List<LogstashMachine> newInstances =
                allInstances.stream()
                        .filter(instance -> !instance.getId().equals(initialInstanceId))
                        .toList();
        scaleOutInstance1Id = newInstances.get(0).getId();
        scaleOutInstance2Id = newInstances.get(1).getId();

        // 等待新实例初始化完成
        for (Long newInstanceId : List.of(scaleOutInstance1Id, scaleOutInstance2Id)) {
            boolean initCompleted =
                    stateVerifier.waitForInstanceState(
                            newInstanceId,
                            List.of(
                                    LogstashMachineState.NOT_STARTED,
                                    LogstashMachineState.INITIALIZE_FAILED),
                            180,
                            TimeUnit.SECONDS);
            assertThat(initCompleted).isTrue();
            assertThat(stateVerifier.getCurrentState(newInstanceId))
                    .isEqualTo(LogstashMachineState.NOT_STARTED);
        }

        // 3. 启动所有实例
        logstashProcessService.startLogstashProcess(testProcessId);

        // 等待所有实例启动完成
        for (Long instanceId :
                List.of(initialInstanceId, scaleOutInstance1Id, scaleOutInstance2Id)) {
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

        log.info("✅ 扩容+启动完成 - 新增实例ID: {}, {}, 总运行实例: 3个", scaleOutInstance1Id, scaleOutInstance2Id);
    }

    @Test
    @Order(5)
    @DisplayName("深度验证扩容后的多实例状态（数据库+文件系统+进程）")
    void deepVerifyScaledInstancesState() {
        log.info("📝 测试：深度验证扩容后的多实例状态");

        List<Long> allInstanceIds =
                List.of(initialInstanceId, scaleOutInstance1Id, scaleOutInstance2Id);

        for (Long instanceId : allInstanceIds) {
            LogstashMachine instance = logstashMachineMapper.selectById(instanceId);

            // 1. 数据库层面验证
            assertThat(instance.getState()).isEqualTo(LogstashMachineState.RUNNING.name());

            // 验证路径设置正确
            if (instanceId.equals(initialInstanceId)) {
                assertThat(instance.getDeployPath()).isEqualTo("/opt/logstash/scaling-base");
            } else {
                assertThat(instance.getDeployPath()).isEqualTo("/opt/logstash/scaling-expanded");
            }

            // 验证路径占用记录
            LogstashMachine pathCheck =
                    logstashMachineMapper.selectByMachineAndPath(
                            instance.getMachineId(), instance.getDeployPath());
            assertThat(pathCheck.getId()).isEqualTo(instanceId);

            // 验证任务步骤完整性
            boolean initTaskValid = databaseVerifier.verifyInitializationTaskAndSteps(instanceId);
            boolean startTaskValid = databaseVerifier.verifyStartTaskAndSteps(instanceId);
            assertThat(initTaskValid).isTrue();
            assertThat(startTaskValid).isTrue();

            // 2. 文件系统层面验证
            boolean directoryStructureValid =
                    processVerifier.verifyInstanceDirectoryStructure(instance, testMachines);
            assertThat(directoryStructureValid).isTrue();

            // 3. 进程层面验证
            boolean processRunning =
                    processVerifier.verifyProcessActuallyRunning(instance, testMachines);
            assertThat(processRunning).isTrue();

            String currentPid = stateVerifier.getCurrentPid(instanceId);
            assertThat(currentPid).isNotBlank();

            log.info(
                    "实例 {} 深度验证通过 - PID: {}, 路径: {}",
                    instanceId,
                    currentPid,
                    instance.getDeployPath());
        }

        log.info("✅ 所有扩容实例深度验证通过");
    }

    @Test
    @Order(6)
    @DisplayName("缩容冲突测试 - 验证运行中实例保护逻辑")
    void testScaleInRunningInstanceProtection() {
        log.info("📝 测试：缩容冲突测试 - 验证运行中实例保护逻辑");

        // 尝试在非强制模式下删除运行中的实例 - 应该失败
        LogstashProcessScaleRequestDTO conflictDTO = new LogstashProcessScaleRequestDTO();
        conflictDTO.setRemoveLogstashMachineIds(List.of(scaleOutInstance1Id));
        conflictDTO.setForceScale(false); // 非强制模式

        // 验证抛出运行中实例保护异常 - 基于实际实现代码的精确验证
        assertThatThrownBy(
                        () ->
                                logstashProcessService.scaleLogstashProcess(
                                        testProcessId, conflictDTO))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> {
                            BusinessException be = (BusinessException) ex;
                            assertThat(be.getErrorCode())
                                    .isEqualTo(ErrorCode.LOGSTASH_MACHINE_RUNNING_CANNOT_REMOVE);
                        });

        // 验证实例数量没有减少
        List<LogstashMachine> instances =
                logstashMachineMapper.selectByLogstashProcessId(testProcessId);
        assertThat(instances).hasSize(3);

        // 验证实例仍在运行
        assertThat(stateVerifier.getCurrentState(scaleOutInstance1Id))
                .isEqualTo(LogstashMachineState.RUNNING);

        log.info("✅ 运行中实例保护逻辑验证通过");
    }

    @Test
    @Order(7)
    @DisplayName("停止所有实例为缩容做准备")
    void stopAllInstancesForScaling() throws InterruptedException {
        log.info("📝 测试：停止所有实例为缩容做准备");

        logstashProcessService.stopLogstashProcess(testProcessId);

        // 等待所有实例停止完成
        List<Long> allInstanceIds =
                List.of(initialInstanceId, scaleOutInstance1Id, scaleOutInstance2Id);
        for (Long instanceId : allInstanceIds) {
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

        log.info("✅ 所有实例停止完成");
    }

    @Test
    @Order(8)
    @DisplayName("缩容操作 - 精确移除2个扩容实例")
    void scaleInRemoveTwoInstances() {
        log.info("📝 测试：缩容操作 - 精确移除2个扩容实例");

        // 1. 执行缩容操作
        LogstashProcessScaleRequestDTO scaleInDTO = new LogstashProcessScaleRequestDTO();
        scaleInDTO.setRemoveLogstashMachineIds(List.of(scaleOutInstance1Id, scaleOutInstance2Id));
        scaleInDTO.setForceScale(false); // 非强制模式

        LogstashProcessResponseDTO scaledProcess =
                logstashProcessService.scaleLogstashProcess(testProcessId, scaleInDTO);
        assertThat(scaledProcess).isNotNull();

        // 2. 验证数据库层面的缩容结果
        List<LogstashMachine> remainingInstances =
                logstashMachineMapper.selectByLogstashProcessId(testProcessId);
        assertThat(remainingInstances).hasSize(1);
        assertThat(remainingInstances.get(0).getId()).isEqualTo(initialInstanceId);

        // 验证被删除的实例记录确实从数据库中删除
        assertThat(logstashMachineMapper.selectById(scaleOutInstance1Id)).isNull();
        assertThat(logstashMachineMapper.selectById(scaleOutInstance2Id)).isNull();

        log.info("✅ 缩容操作完成 - 剩余实例: 1个");
    }

    @Test
    @Order(9)
    @DisplayName("深度验证缩容后的状态（数据库+文件系统+路径释放）")
    void deepVerifyScaleInResult() {
        log.info("📝 测试：深度验证缩容后的状态");

        LogstashMachine remainingInstance = logstashMachineMapper.selectById(initialInstanceId);

        // 1. 数据库层面验证
        assertThat(remainingInstance).isNotNull();
        assertThat(remainingInstance.getDeployPath()).isEqualTo("/opt/logstash/scaling-base");
        assertThat(remainingInstance.getState()).isEqualTo(LogstashMachineState.NOT_STARTED.name());

        // 验证被删除实例的任务记录保持不变（新行为：缩容时保留任务记录）
        boolean instance1TasksPreserved =
                databaseVerifier.verifyTaskAndStepRecordsPreserved(scaleOutInstance1Id);
        boolean instance2TasksPreserved =
                databaseVerifier.verifyTaskAndStepRecordsPreserved(scaleOutInstance2Id);
        assertThat(instance1TasksPreserved).isTrue();
        assertThat(instance2TasksPreserved).isTrue();

        log.info("✅ 被删除实例的任务记录已保留（缩容新行为）");

        // 2. 文件系统层面验证 - 验证被删除实例的目录清理
        LogstashMachine deletedInstance1ForCheck = new LogstashMachine();
        deletedInstance1ForCheck.setId(scaleOutInstance1Id);
        deletedInstance1ForCheck.setMachineId(testMachines.get(1).getId());
        deletedInstance1ForCheck.setDeployPath("/opt/logstash/scaling-expanded");

        LogstashMachine deletedInstance2ForCheck = new LogstashMachine();
        deletedInstance2ForCheck.setId(scaleOutInstance2Id);
        deletedInstance2ForCheck.setMachineId(testMachines.get(2).getId());
        deletedInstance2ForCheck.setDeployPath("/opt/logstash/scaling-expanded");

        boolean directory1CleanedUp =
                processVerifier.verifyInstanceDirectoryCleanup(
                        deletedInstance1ForCheck, testMachines);
        boolean directory2CleanedUp =
                processVerifier.verifyInstanceDirectoryCleanup(
                        deletedInstance2ForCheck, testMachines);
        assertThat(directory1CleanedUp).isTrue();
        assertThat(directory2CleanedUp).isTrue();

        // 3. 路径占用记录释放验证
        LogstashMachine pathCheck1 =
                logstashMachineMapper.selectByMachineAndPath(
                        testMachines.get(1).getId(), "/opt/logstash/scaling-expanded");
        LogstashMachine pathCheck2 =
                logstashMachineMapper.selectByMachineAndPath(
                        testMachines.get(2).getId(), "/opt/logstash/scaling-expanded");
        assertThat(pathCheck1).isNull();
        assertThat(pathCheck2).isNull();

        // 验证剩余实例路径仍被正确占用
        LogstashMachine remainingPathCheck =
                logstashMachineMapper.selectByMachineAndPath(
                        testMachines.get(0).getId(), "/opt/logstash/scaling-base");
        assertThat(remainingPathCheck.getId()).isEqualTo(initialInstanceId);

        log.info("✅ 缩容后深度验证通过");
    }

    @Test
    @Order(10)
    @DisplayName("验证剩余实例启停功能完整性")
    void verifyRemainingInstanceLifecycle() throws InterruptedException {
        log.info("📝 测试：验证剩余实例启停功能完整性");

        // 1. 启动剩余实例
        logstashProcessService.startLogstashProcess(testProcessId);

        boolean startCompleted =
                stateVerifier.waitForInstanceState(
                        initialInstanceId,
                        List.of(LogstashMachineState.RUNNING, LogstashMachineState.START_FAILED),
                        120,
                        TimeUnit.SECONDS);
        assertThat(startCompleted).isTrue();
        assertThat(stateVerifier.getCurrentState(initialInstanceId))
                .isEqualTo(LogstashMachineState.RUNNING);

        // 2. 深度验证运行状态
        LogstashMachine remainingInstance = logstashMachineMapper.selectById(initialInstanceId);
        boolean processRunning =
                processVerifier.verifyProcessActuallyRunning(remainingInstance, testMachines);
        assertThat(processRunning).isTrue();

        String currentPid = stateVerifier.getCurrentPid(initialInstanceId);
        assertThat(currentPid).isNotBlank();

        // 3. 停止实例
        logstashProcessService.stopLogstashProcess(testProcessId);

        boolean stopCompleted =
                stateVerifier.waitForInstanceState(
                        initialInstanceId,
                        List.of(LogstashMachineState.NOT_STARTED, LogstashMachineState.STOP_FAILED),
                        90,
                        TimeUnit.SECONDS);
        assertThat(stopCompleted).isTrue();
        assertThat(stateVerifier.getCurrentState(initialInstanceId))
                .isEqualTo(LogstashMachineState.NOT_STARTED);

        log.info("✅ 剩余实例功能验证通过 - PID: {}", currentPid);
    }

    @Test
    @Order(11)
    @DisplayName("最终清理和完整性验证")
    void finalCleanupAndValidation() {
        log.info("📝 测试：最终清理和完整性验证");

        // 1. 删除进程
        logstashProcessService.deleteLogstashProcess(testProcessId);

        // 2. 验证数据库清理
        assertThat(logstashProcessMapper.selectById(testProcessId)).isNull();
        assertThat(logstashMachineMapper.selectByLogstashProcessId(testProcessId)).isEmpty();

        boolean finalRecordsCleanedUp =
                databaseVerifier.verifyTaskAndStepRecordsCleanedUp(initialInstanceId);
        assertThat(finalRecordsCleanedUp).isTrue();

        // 3. 验证文件系统清理
        LogstashMachine finalInstanceForCleanup = new LogstashMachine();
        finalInstanceForCleanup.setId(initialInstanceId);
        finalInstanceForCleanup.setMachineId(testMachines.get(0).getId());
        finalInstanceForCleanup.setDeployPath("/opt/logstash/scaling-base");

        boolean finalDirectoryCleanedUp =
                processVerifier.verifyInstanceDirectoryCleanup(
                        finalInstanceForCleanup, testMachines);
        assertThat(finalDirectoryCleanedUp).isTrue();

        // 4. 验证路径完全释放
        LogstashMachine pathCheck =
                logstashMachineMapper.selectByMachineAndPath(
                        testMachines.get(0).getId(), "/opt/logstash/scaling-base");
        assertThat(pathCheck).isNull();

        log.info("✅ 扩缩容完整生命周期测试通过");
    }

    // ==================== 辅助方法 ====================

    private LogstashProcessCreateDTO buildCreateDTO(String processName) {
        LogstashProcessCreateDTO dto = new LogstashProcessCreateDTO();
        dto.setName(processName);
        dto.setModuleId(testModuleId);
        dto.setJvmOptions("-Xms512m -Xmx1g");
        dto.setConfigContent(
                """
            input {
              stdin {}
            }

            filter {
              mutate {
                add_field => { "test_mode" => "integration_scaling" }
                add_field => { "test_story" => "story_3_scaling" }
              }
            }

            output {
              stdout { codec => rubydebug }
            }
            """);
        return dto;
    }
}
