package com.hinadt.miaocha.integration.logstash.support;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.enums.TaskOperationType;
import com.hinadt.miaocha.application.logstash.enums.TaskStatus;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.LogstashTask;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Logstash集成测试环境管理器
 *
 * <p>提供功能： 1. 测试数据创建和管理 2. 状态跟踪和验证 3. 资源清理和回收 4. 测试场景构建
 */
@Component
public class LogstashTestEnvironment {

    private final AtomicLong idGenerator = new AtomicLong(1000);
    private final Map<String, TestScenario> scenarios = new ConcurrentHashMap<>();
    private final Map<Long, LogstashMachine> machines = new ConcurrentHashMap<>();
    private final Map<Long, LogstashProcess> processes = new ConcurrentHashMap<>();
    private final Map<Long, LogstashTask> tasks = new ConcurrentHashMap<>();

    /** 测试场景定义 */
    public static class TestScenario {
        private final String name;
        private final List<LogstashProcess> processes = new ArrayList<>();
        private final List<LogstashMachine> machines = new ArrayList<>();
        private final List<MachineInfo> targetMachines = new ArrayList<>();
        private final Map<String, Object> context = new HashMap<>();

        public TestScenario(String name) {
            this.name = name;
        }

        // Getters and fluent methods
        public String getName() {
            return name;
        }

        public List<LogstashProcess> getProcesses() {
            return processes;
        }

        public List<LogstashMachine> getMachines() {
            return machines;
        }

        public List<MachineInfo> getTargetMachines() {
            return targetMachines;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        public TestScenario addProcess(LogstashProcess process) {
            processes.add(process);
            return this;
        }

        public TestScenario addMachine(LogstashMachine machine) {
            machines.add(machine);
            return this;
        }

        public TestScenario addTargetMachine(MachineInfo machine) {
            targetMachines.add(machine);
            return this;
        }

        public TestScenario putContext(String key, Object value) {
            context.put(key, value);
            return this;
        }
    }

    /** 创建基础的一机多实例测试场景 */
    public TestScenario createMultiInstanceScenario() {
        TestScenario scenario = new TestScenario("multi-instance-scenario");

        // 创建目标机器
        MachineInfo targetMachine = createTestMachine("multi-instance-server", "192.168.1.100");
        scenario.addTargetMachine(targetMachine);

        // 创建第一个进程和实例
        LogstashProcess process1 = createTestProcess("process-app1", 1L);
        LogstashMachine machine1 =
                createTestLogstashMachine(
                        process1.getId(), targetMachine.getId(), "/opt/logstash/app1");
        scenario.addProcess(process1).addMachine(machine1);

        // 创建第二个进程和实例（同一台机器）
        LogstashProcess process2 = createTestProcess("process-app2", 2L);
        LogstashMachine machine2 =
                createTestLogstashMachine(
                        process2.getId(), targetMachine.getId(), "/opt/logstash/app2");
        scenario.addProcess(process2).addMachine(machine2);

        // 创建同一进程的第二个实例（同一台机器，不同路径）
        LogstashMachine machine3 =
                createTestLogstashMachine(
                        process1.getId(), targetMachine.getId(), "/opt/logstash/app1-backup");
        scenario.addMachine(machine3);

        scenarios.put(scenario.getName(), scenario);
        return scenario;
    }

    /** 创建状态机测试场景 */
    public TestScenario createStateMachineScenario() {
        TestScenario scenario = new TestScenario("state-machine-scenario");

        MachineInfo targetMachine = createTestMachine("state-test-server", "192.168.1.200");
        scenario.addTargetMachine(targetMachine);

        LogstashProcess process = createTestProcess("state-test-process", 1L);
        LogstashMachine machine =
                createTestLogstashMachine(
                        process.getId(), targetMachine.getId(), "/opt/logstash/state-test");

        scenario.addProcess(process).addMachine(machine);
        scenario.putContext(
                "expectedStates",
                Arrays.asList(
                        LogstashMachineState.NOT_STARTED,
                        LogstashMachineState.STARTING,
                        LogstashMachineState.RUNNING,
                        LogstashMachineState.STOPPING,
                        LogstashMachineState.STOP_FAILED));

        scenarios.put(scenario.getName(), scenario);
        return scenario;
    }

    /** 创建任务生命周期测试场景 */
    public TestScenario createTaskLifecycleScenario() {
        TestScenario scenario = new TestScenario("task-lifecycle-scenario");

        MachineInfo targetMachine = createTestMachine("task-test-server", "192.168.1.300");
        scenario.addTargetMachine(targetMachine);

        LogstashProcess process = createTestProcess("task-test-process", 1L);
        LogstashMachine machine =
                createTestLogstashMachine(
                        process.getId(), targetMachine.getId(), "/opt/logstash/task-test");

        scenario.addProcess(process).addMachine(machine);
        scenario.putContext(
                "taskTypes",
                Arrays.asList(
                        TaskOperationType.START,
                        TaskOperationType.STOP,
                        TaskOperationType.RESTART,
                        TaskOperationType.INITIALIZE));

        scenarios.put(scenario.getName(), scenario);
        return scenario;
    }

    /** 创建测试用的MachineInfo */
    public MachineInfo createTestMachine(String name, String ip) {
        MachineInfo machine = new MachineInfo();
        machine.setId(idGenerator.getAndIncrement());
        machine.setName(name);
        machine.setIp(ip);
        machine.setPort(22);
        machine.setUsername("test");
        machine.setPassword("test123");
        machine.setCreateTime(LocalDateTime.now());
        return machine;
    }

    /** 创建测试用的LogstashProcess */
    public LogstashProcess createTestProcess(String name, Long moduleId) {
        LogstashProcess process = new LogstashProcess();
        process.setId(idGenerator.getAndIncrement());
        process.setName(name);
        process.setModuleId(moduleId);
        process.setCreateTime(LocalDateTime.now());
        processes.put(process.getId(), process);
        return process;
    }

    /** 创建测试用的LogstashMachine */
    public LogstashMachine createTestLogstashMachine(
            Long processId, Long machineId, String deployPath) {
        LogstashMachine machine = new LogstashMachine();
        machine.setId(idGenerator.getAndIncrement());
        machine.setLogstashProcessId(processId);
        machine.setMachineId(machineId);
        machine.setDeployPath(deployPath);
        machine.setState(LogstashMachineState.NOT_STARTED.name());
        machine.setCreateTime(LocalDateTime.now());
        machines.put(machine.getId(), machine);
        return machine;
    }

    /** 创建测试用的LogstashTask 注意：由于这个方法在测试中使用，需要传入必要的参数来设置processId */
    public LogstashTask createTestTask(Long machineId, TaskOperationType taskType, Long processId) {
        LogstashTask task = new LogstashTask();
        task.setId(java.util.UUID.randomUUID().toString());
        task.setProcessId(processId);
        task.setMachineId(machineId);
        task.setLogstashMachineId(machineId);
        task.setOperationType(taskType.name());
        task.setStatus(TaskStatus.PENDING.name());
        task.setName("测试任务-" + taskType.name());
        task.setDescription("集成测试任务");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        tasks.put(Long.valueOf(task.getId().hashCode()), task);
        return task;
    }

    /** 创建测试用的LogstashTask（重载方法，自动从内存中查找processId） */
    public LogstashTask createTestTask(Long machineId, TaskOperationType taskType) {
        // 从LogstashMachine找到对应的processId
        LogstashMachine machine = machines.get(machineId);
        Long processId = machine != null ? machine.getLogstashProcessId() : 1001L;
        return createTestTask(machineId, taskType, processId);
    }

    /** 创建测试用的ModuleInfo */
    public ModuleInfo createTestModule(String name, String version) {
        ModuleInfo module = new ModuleInfo();
        module.setId(idGenerator.getAndIncrement());
        module.setName(name);
        module.setDatasourceId(1L);
        module.setTableName("test_table_" + idGenerator.get());
        module.setDorisSql("SELECT * FROM test_table_" + idGenerator.get());
        module.setCreateTime(LocalDateTime.now());
        module.setUpdateTime(LocalDateTime.now());
        return module;
    }

    /** 获取场景 */
    public TestScenario getScenario(String name) {
        return scenarios.get(name);
    }

    /** 清理测试环境 */
    public void cleanup() {
        scenarios.clear();
        machines.clear();
        processes.clear();
        tasks.clear();
        idGenerator.set(1000);
    }

    /** 验证一机多实例状态 */
    public boolean validateMultiInstanceStates(TestScenario scenario) {
        return scenario.getMachines().stream()
                .collect(java.util.stream.Collectors.groupingBy(LogstashMachine::getMachineId))
                .entrySet()
                .stream()
                .allMatch(entry -> entry.getValue().size() > 1); // 每台机器有多个实例
    }

    /** 获取实例统计信息 */
    public Map<String, Object> getInstanceStats(TestScenario scenario) {
        Map<String, Object> stats = new HashMap<>();

        Map<Long, Long> instancesPerMachine =
                scenario.getMachines().stream()
                        .collect(
                                java.util.stream.Collectors.groupingBy(
                                        LogstashMachine::getMachineId,
                                        java.util.stream.Collectors.counting()));

        Map<Long, Long> instancesPerProcess =
                scenario.getMachines().stream()
                        .collect(
                                java.util.stream.Collectors.groupingBy(
                                        LogstashMachine::getLogstashProcessId,
                                        java.util.stream.Collectors.counting()));

        stats.put("totalMachines", scenario.getTargetMachines().size());
        stats.put("totalProcesses", scenario.getProcesses().size());
        stats.put("totalInstances", scenario.getMachines().size());
        stats.put("instancesPerMachine", instancesPerMachine);
        stats.put("instancesPerProcess", instancesPerProcess);
        stats.put(
                "maxInstancesOnSingleMachine",
                instancesPerMachine.values().stream().max(Long::compareTo).orElse(0L));

        return stats;
    }
}
