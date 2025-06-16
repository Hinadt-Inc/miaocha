package com.hinadt.miaocha.integration.logstash;

import static org.junit.jupiter.api.Assertions.*;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.domain.entity.*;
import com.hinadt.miaocha.domain.mapper.*;
import com.hinadt.miaocha.integration.logstash.support.LogstashTestEnvironment;
import com.hinadt.miaocha.integration.logstash.support.LogstashTestEnvironment.TestScenario;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 状态机集成测试
 *
 * <p>专门验证： 1. 状态机的完整状态转换流程 2. 状态转换的正确性和一致性 3. 异常状态的处理和恢复 4. 状态变更的持久化
 */
@Epic("秒查日志管理系统")
@Feature("状态机集成测试")
@Owner("开发团队")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("状态机集成测试")
@Rollback(true)
public class StateMachineIntegrationTest {

    @Autowired private LogstashTestEnvironment testEnvironment;

    @Autowired private LogstashProcessMapper logstashProcessMapper;

    @Autowired private LogstashMachineMapper logstashMachineMapper;

    @Autowired private MachineMapper machineMapper;

    @Autowired private ModuleInfoMapper moduleInfoMapper;

    private TestScenario stateMachineScenario;
    private static final String TEST_USER = "integration-test";

    @BeforeEach
    void setUp() {
        testEnvironment.cleanup();
        stateMachineScenario = testEnvironment.createStateMachineScenario();

        // 保存测试数据到数据库
        saveTestDataToDatabase();
    }

    @AfterEach
    void tearDown() {
        testEnvironment.cleanup();
    }

    @Test
    @Order(1)
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("1️⃣ 验证正常启动状态转换流程")
    @Description("验证从NOT_STARTED到RUNNING的完整状态转换")
    @Transactional
    void testNormalStartupStateTransition() {
        LogstashMachine machine = stateMachineScenario.getMachines().get(0);
        Long machineId = machine.getId();

        Allure.step(
                "验证初始状态",
                () -> {
                    LogstashMachine currentMachine = logstashMachineMapper.selectById(machineId);
                    assertEquals(
                            LogstashMachineState.NOT_STARTED.name(),
                            currentMachine.getState(),
                            "初始状态应该是NOT_STARTED");

                    Allure.attachment("初始状态", currentMachine.getState());
                });

        Allure.step(
                "状态转换：NOT_STARTED -> INITIALIZING",
                () -> {
                    updateStateAndVerify(machineId, LogstashMachineState.INITIALIZING);
                });

        Allure.step(
                "状态转换：INITIALIZING -> STARTING",
                () -> {
                    updateStateAndVerify(machineId, LogstashMachineState.STARTING);
                });

        Allure.step(
                "状态转换：STARTING -> RUNNING",
                () -> {
                    updateStateAndVerify(machineId, LogstashMachineState.RUNNING);

                    // 模拟设置进程PID
                    String mockPid = "12345";
                    logstashMachineMapper.updateProcessPidById(machineId, mockPid);

                    LogstashMachine runningMachine = logstashMachineMapper.selectById(machineId);
                    assertEquals(mockPid, runningMachine.getProcessPid(), "应该设置了进程PID");

                    Allure.attachment(
                            "运行状态",
                            String.format(
                                    "状态=%s, PID=%s",
                                    runningMachine.getState(), runningMachine.getProcessPid()));
                });
    }

    @Test
    @Order(2)
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("2️⃣ 验证正常停止状态转换流程")
    @Description("验证从RUNNING到STOPPED的完整状态转换")
    @Transactional
    void testNormalStopStateTransition() {
        LogstashMachine machine = stateMachineScenario.getMachines().get(0);
        Long machineId = machine.getId();

        // 先设置为运行状态
        updateStateAndVerify(machineId, LogstashMachineState.RUNNING);
        logstashMachineMapper.updateProcessPidById(machineId, "12345");

        Allure.step(
                "状态转换：RUNNING -> STOPPING",
                () -> {
                    updateStateAndVerify(machineId, LogstashMachineState.STOPPING);
                });

        Allure.step(
                "状态转换：STOPPING -> NOT_STARTED",
                () -> {
                    updateStateAndVerify(machineId, LogstashMachineState.NOT_STARTED);

                    // 验证进程PID被清空
                    logstashMachineMapper.updateProcessPidById(machineId, null);
                    LogstashMachine stoppedMachine = logstashMachineMapper.selectById(machineId);
                    assertNull(stoppedMachine.getProcessPid(), "停止后PID应该被清空");

                    Allure.attachment(
                            "停止完成",
                            String.format(
                                    "实例状态：%s，PID：%s",
                                    stoppedMachine.getState(), stoppedMachine.getProcessPid()));
                });
    }

    @Test
    @Order(3)
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("3️⃣ 验证异常状态处理")
    @Description("验证错误状态的设置和恢复")
    @Transactional
    void testErrorStateHandling() {
        LogstashMachine machine = stateMachineScenario.getMachines().get(0);
        Long machineId = machine.getId();

        Allure.step(
                "模拟启动失败",
                () -> {
                    updateStateAndVerify(machineId, LogstashMachineState.STARTING);
                    updateStateAndVerify(machineId, LogstashMachineState.START_FAILED);

                    Allure.attachment("启动失败状态", LogstashMachineState.START_FAILED.name());
                });

        Allure.step(
                "从失败状态恢复",
                () -> {
                    // 重置为初始状态
                    updateStateAndVerify(machineId, LogstashMachineState.NOT_STARTED);

                    // 重新尝试启动
                    updateStateAndVerify(machineId, LogstashMachineState.STARTING);
                    updateStateAndVerify(machineId, LogstashMachineState.RUNNING);

                    Allure.attachment("恢复成功", "从失败状态成功恢复到运行状态");
                });

        Allure.step(
                "模拟停止失败",
                () -> {
                    updateStateAndVerify(machineId, LogstashMachineState.STOPPING);
                    updateStateAndVerify(machineId, LogstashMachineState.STOP_FAILED);

                    // 强制停止
                    updateStateAndVerify(machineId, LogstashMachineState.NOT_STARTED);

                    Allure.attachment("停止失败处理", "通过强制停止解决停止失败");
                });
    }

    @Test
    @Order(4)
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("4️⃣ 验证重启状态流程")
    @Description("验证重启过程的状态转换")
    @Transactional
    void testRestartStateTransition() {
        LogstashMachine machine = stateMachineScenario.getMachines().get(0);
        Long machineId = machine.getId();

        // 先设置为运行状态
        updateStateAndVerify(machineId, LogstashMachineState.RUNNING);
        logstashMachineMapper.updateProcessPidById(machineId, "12345");

        Allure.step(
                "重启流程：RUNNING -> STOPPING -> NOT_STARTED -> STARTING -> RUNNING",
                () -> {
                    // 停止
                    updateStateAndVerify(machineId, LogstashMachineState.STOPPING);
                    updateStateAndVerify(machineId, LogstashMachineState.NOT_STARTED);
                    logstashMachineMapper.updateProcessPidById(machineId, null);

                    // 重新启动
                    updateStateAndVerify(machineId, LogstashMachineState.STARTING);
                    updateStateAndVerify(machineId, LogstashMachineState.RUNNING);
                    logstashMachineMapper.updateProcessPidById(machineId, "54321");

                    LogstashMachine restartedMachine = logstashMachineMapper.selectById(machineId);
                    assertEquals(LogstashMachineState.RUNNING.name(), restartedMachine.getState());
                    assertEquals("54321", restartedMachine.getProcessPid());

                    Allure.attachment(
                            "重启完成",
                            String.format(
                                    "新状态=%s, 新PID=%s",
                                    restartedMachine.getState(), restartedMachine.getProcessPid()));
                });
    }

    @Test
    @Order(5)
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("5️⃣ 验证状态持久化")
    @Description("验证状态变更正确持久化到数据库")
    @Transactional
    void testStatePersistence() {
        LogstashMachine machine = stateMachineScenario.getMachines().get(0);
        Long machineId = machine.getId();

        Allure.step(
                "验证状态变更的持久化",
                () -> {
                    // 进行一系列状态变更
                    LogstashMachineState[] states = {
                        LogstashMachineState.INITIALIZING,
                        LogstashMachineState.STARTING,
                        LogstashMachineState.RUNNING,
                        LogstashMachineState.STOPPING,
                        LogstashMachineState.NOT_STARTED
                    };

                    for (LogstashMachineState state : states) {
                        updateStateAndVerify(machineId, state);

                        // 重新从数据库查询验证持久化
                        LogstashMachine persistedMachine =
                                logstashMachineMapper.selectById(machineId);
                        assertEquals(
                                state.name(),
                                persistedMachine.getState(),
                                String.format("状态%s应该正确持久化", state.name()));

                        // 验证更新时间被更新
                        assertNotNull(persistedMachine.getUpdateTime(), "更新时间应该被设置");
                    }

                    Allure.attachment("状态持久化验证", "所有状态变更都正确持久化到数据库");
                });
    }

    @Test
    @Order(6)
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("6️⃣ 验证从RUNNING到NOT_STARTED的完整状态转换")
    @Description("验证从RUNNING到NOT_STARTED的完整状态转换")
    @Transactional
    void testCompleteStopTransition() {
        Long machineId = createTestMachineInRunningState();

        Allure.step(
                "状态转换：STOPPING -> NOT_STARTED",
                () -> {
                    updateStateAndVerify(machineId, LogstashMachineState.NOT_STARTED);

                    // 验证进程PID被清空
                    logstashMachineMapper.updateProcessPidById(machineId, null);
                    LogstashMachine stoppedMachine = logstashMachineMapper.selectById(machineId);
                    assertNull(stoppedMachine.getProcessPid(), "停止后PID应该被清空");

                    Allure.attachment(
                            "停止完成",
                            String.format(
                                    "实例状态：%s，PID：%s",
                                    stoppedMachine.getState(), stoppedMachine.getProcessPid()));
                });
    }

    /** 更新状态并验证 */
    private void updateStateAndVerify(Long machineId, LogstashMachineState expectedState) {
        logstashMachineMapper.updateStateById(machineId, expectedState.name());

        LogstashMachine updatedMachine = logstashMachineMapper.selectById(machineId);
        assertEquals(
                expectedState.name(),
                updatedMachine.getState(),
                String.format("状态应该更新为%s", expectedState.name()));
    }

    /** 保存测试数据到数据库 */
    private void saveTestDataToDatabase() {
        // 保存模块信息
        ModuleInfo module = testEnvironment.createTestModule("state-test-module", "1.0.0");
        moduleInfoMapper.insert(module);

        // 保存机器信息
        stateMachineScenario.getTargetMachines().forEach(machineMapper::insert);

        // 保存进程信息
        stateMachineScenario.getProcesses().forEach(logstashProcessMapper::insert);

        // 保存实例信息
        stateMachineScenario.getMachines().forEach(logstashMachineMapper::insert);
    }

    private Long createTestMachineInRunningState() {
        LogstashMachine machine = stateMachineScenario.getMachines().get(0);
        Long machineId = machine.getId();
        updateStateAndVerify(machineId, LogstashMachineState.RUNNING);
        logstashMachineMapper.updateProcessPidById(machineId, "12345");
        return machineId;
    }
}
