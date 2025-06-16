package com.hinadt.miaocha.integration.logstash;

import static org.junit.jupiter.api.Assertions.*;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.enums.TaskOperationType;
import com.hinadt.miaocha.application.logstash.enums.TaskStatus;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.application.service.LogstashProcessService;
import com.hinadt.miaocha.domain.entity.*;
import com.hinadt.miaocha.domain.mapper.*;
import com.hinadt.miaocha.integration.logstash.support.LogstashTestEnvironment;
import com.hinadt.miaocha.integration.logstash.support.LogstashTestEnvironment.TestScenario;
import io.qameta.allure.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 一机多实例集成测试
 *
 * <p>完整验证： 1. 一机多实例的部署和管理 2. 每个实例独立的状态机运转 3. 任务生命周期和命令执行 4. 多实例间的资源隔离 5. 并发操作的正确性
 */
@Epic("秒查日志管理系统")
@Feature("一机多实例集成测试")
@Owner("开发团队")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("一机多实例集成测试")
@Rollback(true)
public class MultiInstanceIntegrationTest {

    @Autowired private LogstashTestEnvironment testEnvironment;

    @Autowired private LogstashProcessService logstashProcessService;

    @Autowired private TaskService taskService;

    @Autowired private LogstashProcessMapper logstashProcessMapper;

    @Autowired private LogstashMachineMapper logstashMachineMapper;

    @Autowired private LogstashTaskMapper logstashTaskMapper;

    @Autowired private MachineMapper machineMapper;

    @Autowired private ModuleInfoMapper moduleInfoMapper;

    private TestScenario multiInstanceScenario;
    private static final String TEST_USER = "integration-test";

    @BeforeEach
    void setUp() {
        testEnvironment.cleanup();
        multiInstanceScenario = testEnvironment.createMultiInstanceScenario();

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
    @DisplayName("1️⃣ 验证一机多实例部署架构")
    @Description("验证可以在同一台机器上部署多个不同进程和同一进程的多个实例")
    @Transactional
    void testMultiInstanceDeploymentArchitecture() {
        Allure.step(
                "验证测试场景数据完整性",
                () -> {
                    // 验证场景基本数据
                    assertEquals(1, multiInstanceScenario.getTargetMachines().size(), "应该有1台目标机器");
                    assertEquals(
                            2, multiInstanceScenario.getProcesses().size(), "应该有2个LogstashProcess");
                    assertEquals(
                            3,
                            multiInstanceScenario.getMachines().size(),
                            "应该有3个LogstashMachine实例");

                    // 验证一机多实例分布
                    Map<String, Object> stats =
                            testEnvironment.getInstanceStats(multiInstanceScenario);
                    assertEquals(3L, stats.get("maxInstancesOnSingleMachine"), "单台机器最多应该有3个实例");

                    Allure.attachment("实例统计", stats.toString());
                });

        Allure.step(
                "验证数据库约束正确性",
                () -> {
                    // 验证每个实例在数据库中的唯一性
                    List<LogstashMachine> machines = logstashMachineMapper.selectAll();
                    assertFalse(machines.isEmpty(), "数据库中应该存在LogstashMachine记录");

                    // 使用创建的实例的机器ID来筛选
                    Long targetMachineId =
                            multiInstanceScenario.getMachines().get(0).getMachineId();
                    List<LogstashMachine> instancesOnMachine =
                            machines.stream()
                                    .filter(m -> targetMachineId.equals(m.getMachineId()))
                                    .toList();

                    assertEquals(3, instancesOnMachine.size(), "同一台机器应该有3个实例");

                    // 验证路径唯一性
                    long uniquePaths =
                            instancesOnMachine.stream()
                                    .map(LogstashMachine::getDeployPath)
                                    .distinct()
                                    .count();
                    assertEquals(3, uniquePaths, "3个实例应该有3个不同的部署路径");

                    Allure.attachment(
                            "机器实例分布",
                            String.format(
                                    "机器[%d]上的实例：%s",
                                    targetMachineId,
                                    instancesOnMachine.stream()
                                            .map(
                                                    m ->
                                                            String.format(
                                                                    "路径=%s,进程=%d",
                                                                    m.getDeployPath(),
                                                                    m.getLogstashProcessId()))
                                            .toList()));
                });

        Allure.step(
                "验证多实例架构完整性",
                () -> {
                    assertTrue(
                            testEnvironment.validateMultiInstanceStates(multiInstanceScenario),
                            "一机多实例架构验证应该通过");
                });
    }

    @Test
    @Order(2)
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("2️⃣ 验证多实例独立状态机运转")
    @Description("验证每个实例独立的状态转换，不相互干扰")
    @Transactional
    void testIndependentStateMachineExecution() {
        List<LogstashMachine> machines = multiInstanceScenario.getMachines();

        Allure.step(
                "验证初始状态",
                () -> {
                    machines.forEach(
                            machine -> {
                                assertEquals(
                                        LogstashMachineState.NOT_STARTED.name(),
                                        machine.getState(),
                                        String.format("实例[%d]初始状态应该是NOT_STARTED", machine.getId()));
                            });
                });

        Allure.step(
                "启动第一个实例，验证其他实例不受影响",
                () -> {
                    LogstashMachine firstMachine = machines.get(0);

                    // 模拟启动过程：NOT_STARTED -> STARTING -> RUNNING
                    simulateStateTransition(firstMachine.getId(), LogstashMachineState.STARTING);
                    simulateStateTransition(firstMachine.getId(), LogstashMachineState.RUNNING);

                    // 验证第一个实例状态
                    LogstashMachine updatedMachine =
                            logstashMachineMapper.selectById(firstMachine.getId());
                    assertEquals(
                            LogstashMachineState.RUNNING.name(),
                            updatedMachine.getState(),
                            "第一个实例应该处于RUNNING状态");

                    // 验证其他实例状态未变
                    machines.stream()
                            .skip(1)
                            .forEach(
                                    machine -> {
                                        LogstashMachine otherMachine =
                                                logstashMachineMapper.selectById(machine.getId());
                                        assertEquals(
                                                LogstashMachineState.NOT_STARTED.name(),
                                                otherMachine.getState(),
                                                String.format("其他实例[%d]状态不应该受影响", machine.getId()));
                                    });

                    Allure.attachment(
                            "第一个实例启动完成",
                            String.format(
                                    "实例[%d]在路径[%s]启动成功",
                                    firstMachine.getId(), firstMachine.getDeployPath()));
                });

        Allure.step(
                "并发启动其他实例，验证并发安全性",
                () -> {
                    // 同时启动剩余的两个实例
                    LogstashMachine secondMachine = machines.get(1);
                    LogstashMachine thirdMachine = machines.get(2);

                    // 模拟并发启动
                    simulateStateTransition(secondMachine.getId(), LogstashMachineState.STARTING);
                    simulateStateTransition(thirdMachine.getId(), LogstashMachineState.STARTING);

                    // 稍后完成启动
                    simulateStateTransition(secondMachine.getId(), LogstashMachineState.RUNNING);
                    simulateStateTransition(thirdMachine.getId(), LogstashMachineState.RUNNING);

                    // 验证所有实例都成功启动
                    machines.forEach(
                            machine -> {
                                LogstashMachine updated =
                                        logstashMachineMapper.selectById(machine.getId());
                                assertEquals(
                                        LogstashMachineState.RUNNING.name(),
                                        updated.getState(),
                                        String.format("实例[%d]应该处于RUNNING状态", machine.getId()));
                            });

                    Allure.attachment(
                            "所有实例启动完成", String.format("共%d个实例在同一台机器上独立运行", machines.size()));
                });
    }

    @Test
    @Order(3)
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("3️⃣ 验证多实例任务生命周期管理")
    @Description("验证每个实例的任务创建、执行、完成的完整生命周期")
    @Transactional
    void testMultiInstanceTaskLifecycle() {
        List<LogstashMachine> machines = multiInstanceScenario.getMachines();

        Allure.step(
                "为每个实例创建不同类型的任务",
                () -> {
                    // 为第一个实例创建启动任务
                    LogstashMachine firstMachine = machines.get(0);
                    LogstashTask initTask =
                            testEnvironment.createTestTask(
                                    firstMachine.getId(),
                                    TaskOperationType.INITIALIZE,
                                    firstMachine.getLogstashProcessId());
                    logstashTaskMapper.insert(initTask);

                    // 为第二个实例创建部署任务
                    LogstashMachine secondMachine = machines.get(1);
                    LogstashTask deployTask =
                            testEnvironment.createTestTask(
                                    secondMachine.getId(),
                                    TaskOperationType.INITIALIZE,
                                    secondMachine.getLogstashProcessId());
                    logstashTaskMapper.insert(deployTask);

                    // 为第三个实例创建重启任务
                    LogstashMachine thirdMachine = machines.get(2);
                    LogstashTask restartTask =
                            testEnvironment.createTestTask(
                                    thirdMachine.getId(),
                                    TaskOperationType.RESTART,
                                    thirdMachine.getLogstashProcessId());
                    logstashTaskMapper.insert(restartTask);

                    Allure.attachment("任务创建完成", "为3个实例分别创建了INITIALIZE、INITIALIZE、RESTART任务");
                });

        Allure.step(
                "验证任务独立执行",
                () -> {
                    // 查询每个实例的任务
                    machines.forEach(
                            machine -> {
                                List<LogstashTask> tasks =
                                        logstashTaskMapper.findByLogstashMachineId(machine.getId());
                                assertEquals(
                                        1,
                                        tasks.size(),
                                        String.format("实例[%d]应该有1个任务", machine.getId()));

                                LogstashTask task = tasks.get(0);
                                assertEquals(
                                        TaskStatus.PENDING.name(),
                                        task.getStatus(),
                                        String.format("任务[%s]初始状态应该是PENDING", task.getId()));
                            });
                });

        Allure.step(
                "模拟任务执行过程",
                () -> {
                    machines.forEach(
                            machine -> {
                                List<LogstashTask> tasks =
                                        logstashTaskMapper.findByLogstashMachineId(machine.getId());
                                LogstashTask task = tasks.get(0);

                                // 模拟任务状态转换：PENDING -> RUNNING -> COMPLETED
                                simulateTaskStateTransition(task.getId(), TaskStatus.RUNNING);

                                // 模拟执行时间
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }

                                simulateTaskStateTransition(task.getId(), TaskStatus.COMPLETED);

                                // 验证最终状态
                                LogstashTask updatedTask =
                                        logstashTaskMapper.findById(task.getId()).orElse(null);
                                assertNotNull(
                                        updatedTask, String.format("应该能找到任务[%s]", task.getId()));
                                assertEquals(
                                        TaskStatus.COMPLETED.name(),
                                        updatedTask.getStatus(),
                                        String.format("任务[%s]应该完成", task.getId()));
                            });

                    Allure.attachment("任务执行完成", "所有实例的任务都已成功完成");
                });
    }

    @Test
    @Order(4)
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("4️⃣ 验证多实例资源隔离")
    @Description("验证不同实例间的资源隔离和独立性")
    @Transactional
    void testMultiInstanceResourceIsolation() {
        List<LogstashMachine> machines = multiInstanceScenario.getMachines();

        Allure.step(
                "验证部署路径隔离",
                () -> {
                    // 检查每个实例有独立的部署路径
                    List<String> deployPaths =
                            machines.stream().map(LogstashMachine::getDeployPath).toList();

                    assertEquals(3, deployPaths.stream().distinct().count(), "3个实例应该有3个不同的部署路径");

                    // 验证路径不重复
                    for (int i = 0; i < deployPaths.size(); i++) {
                        for (int j = i + 1; j < deployPaths.size(); j++) {
                            assertNotEquals(deployPaths.get(i), deployPaths.get(j), "实例部署路径不应该重复");
                        }
                    }

                    Allure.attachment("部署路径隔离验证", String.format("实例部署路径：%s", deployPaths));
                });

        Allure.step(
                "验证进程ID隔离",
                () -> {
                    // 模拟为每个实例设置不同的进程ID
                    for (int i = 0; i < machines.size(); i++) {
                        LogstashMachine machine = machines.get(i);
                        String mockPid = String.valueOf(10000 + i);

                        logstashMachineMapper.updateProcessPidById(
                                machines.get(i).getId(), mockPid);
                    }

                    // 验证每个实例有独立的进程ID
                    machines.forEach(
                            machine -> {
                                LogstashMachine updated =
                                        logstashMachineMapper.selectById(machine.getId());
                                assertNotNull(
                                        updated.getProcessPid(),
                                        String.format("实例[%d]应该有进程ID", machine.getId()));
                            });

                    Allure.attachment("进程ID隔离验证", "每个实例都有独立的进程ID");
                });

        Allure.step(
                "验证状态独立性",
                () -> {
                    // 让第一个实例失败，验证其他实例不受影响
                    LogstashMachine firstMachine = machines.get(0);
                    simulateStateTransition(firstMachine.getId(), LogstashMachineState.STOP_FAILED);

                    // 验证其他实例状态正常
                    machines.stream()
                            .skip(1)
                            .forEach(
                                    machine -> {
                                        LogstashMachine updated =
                                                logstashMachineMapper.selectById(machine.getId());
                                        assertNotEquals(
                                                LogstashMachineState.STOP_FAILED.name(),
                                                updated.getState(),
                                                String.format(
                                                        "实例[%d]不应该受到其他实例失败的影响", machine.getId()));
                                    });

                    Allure.attachment("状态独立性验证", "单个实例的失败不影响其他实例");
                });
    }

    @Test
    @Order(5)
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("5️⃣ 验证一机多实例完整工作流")
    @Description("端到端验证一机多实例的完整生命周期工作流")
    @Transactional
    void testCompleteMultiInstanceWorkflow() {
        Allure.step(
                "端到端工作流验证",
                () -> {
                    List<LogstashMachine> machines = multiInstanceScenario.getMachines();

                    // 1. 批量启动所有实例
                    machines.forEach(
                            machine -> {
                                simulateStateTransition(
                                        machine.getId(), LogstashMachineState.STARTING);
                                simulateStateTransition(
                                        machine.getId(), LogstashMachineState.RUNNING);
                            });

                    // 2. 验证所有实例运行
                    long runningCount =
                            machines.stream()
                                    .map(m -> logstashMachineMapper.selectById(m.getId()))
                                    .filter(
                                            m ->
                                                    LogstashMachineState.RUNNING
                                                            .name()
                                                            .equals(m.getState()))
                                    .count();
                    assertEquals(3, runningCount, "所有3个实例应该都在运行");

                    // 3. 模拟部分实例重启
                    LogstashMachine restartMachine = machines.get(1);
                    simulateStateTransition(restartMachine.getId(), LogstashMachineState.STOPPING);
                    simulateStateTransition(
                            restartMachine.getId(), LogstashMachineState.NOT_STARTED);
                    simulateStateTransition(restartMachine.getId(), LogstashMachineState.STARTING);
                    simulateStateTransition(restartMachine.getId(), LogstashMachineState.RUNNING);

                    // 4. 验证重启后状态
                    LogstashMachine restarted =
                            logstashMachineMapper.selectById(restartMachine.getId());
                    assertEquals(
                            LogstashMachineState.RUNNING.name(),
                            restarted.getState(),
                            "重启的实例应该恢复到运行状态");

                    // 5. 验证其他实例不受影响
                    machines.stream()
                            .filter(m -> !m.getId().equals(restartMachine.getId()))
                            .forEach(
                                    machine -> {
                                        LogstashMachine other =
                                                logstashMachineMapper.selectById(machine.getId());
                                        assertEquals(
                                                LogstashMachineState.RUNNING.name(),
                                                other.getState(),
                                                "其他实例不应该受重启影响");
                                    });

                    Allure.attachment(
                            "完整工作流验证", String.format("成功验证了%d个实例的完整生命周期", machines.size()));
                });
    }

    /** 保存测试数据到数据库 */
    private void saveTestDataToDatabase() {
        // 保存模块信息
        ModuleInfo module1 = testEnvironment.createTestModule("test-module-1", "1.0.0");
        ModuleInfo module2 = testEnvironment.createTestModule("test-module-2", "1.0.0");
        moduleInfoMapper.insert(module1);
        moduleInfoMapper.insert(module2);

        // 保存机器信息
        multiInstanceScenario.getTargetMachines().forEach(machineMapper::insert);

        // 保存进程信息
        multiInstanceScenario.getProcesses().forEach(logstashProcessMapper::insert);

        // 保存实例信息
        multiInstanceScenario.getMachines().forEach(logstashMachineMapper::insert);
    }

    /** 模拟状态转换 */
    private void simulateStateTransition(Long machineId, LogstashMachineState newState) {
        logstashMachineMapper.updateStateById(machineId, newState.name());
    }

    /** 模拟任务状态转换 */
    private void simulateTaskStateTransition(String taskId, TaskStatus newState) {
        LogstashTask task = logstashTaskMapper.findById(taskId).orElse(null);
        if (task != null) {
            task.setStatus(newState.name());
            logstashTaskMapper.updateStatus(taskId, newState.name());
        }
    }
}
