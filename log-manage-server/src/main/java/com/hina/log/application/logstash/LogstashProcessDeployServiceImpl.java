package com.hina.log.application.logstash;

import com.hina.log.application.logstash.command.LogstashCommand;
import com.hina.log.application.logstash.command.LogstashCommandFactory;
import com.hina.log.application.logstash.enums.LogstashMachineStep;
import com.hina.log.application.logstash.enums.TaskOperationType;
import com.hina.log.application.logstash.state.LogstashMachineStateManager;
import com.hina.log.application.logstash.task.TaskService;
import com.hina.log.common.util.FutureUtils;
import com.hina.log.config.LogstashProperties;
import com.hina.log.domain.entity.LogstashProcess;
import com.hina.log.domain.entity.Machine;
import com.hina.log.domain.mapper.LogstashProcessMapper;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Logstash进程服务实现类 - 负责Logstash进程的部署、启动和停止 */
@Service("logstashDeployServiceImpl")
public class LogstashProcessDeployServiceImpl implements LogstashProcessDeployService {
    private static final Logger logger =
            LoggerFactory.getLogger(LogstashProcessDeployServiceImpl.class);

    private final LogstashProcessMapper logstashProcessMapper;
    private final TaskService taskService;
    private final LogstashMachineStateManager machineStateManager;
    private final LogstashCommandFactory commandFactory;
    private final LogstashProcessConfigService configService;
    private final LogstashProperties logstashProperties;
    private final LogstashMachineConnectionValidator connectionValidator;

    public LogstashProcessDeployServiceImpl(
            LogstashProcessMapper logstashProcessMapper,
            TaskService taskService,
            LogstashMachineStateManager machineStateManager,
            LogstashCommandFactory commandFactory,
            LogstashProcessConfigService configService,
            LogstashProperties logstashProperties,
            LogstashMachineConnectionValidator connectionValidator) {
        this.logstashProcessMapper = logstashProcessMapper;
        this.taskService = taskService;
        this.machineStateManager = machineStateManager;
        this.commandFactory = commandFactory;
        this.configService = configService;
        this.logstashProperties = logstashProperties;
        this.connectionValidator = connectionValidator;
    }

    @Override
    public void initializeProcess(LogstashProcess process, List<Machine> machines) {
        if (process == null || machines == null || machines.isEmpty()) {
            logger.error(
                    "初始化进程参数无效: process={}, machines是否为空={}",
                    process,
                    machines == null || machines.isEmpty());
            return;
        }

        Long processId = process.getId();

        // 创建任务
        String taskName = "初始化Logstash进程[" + process.getName() + "]";
        String taskDescription = "初始化Logstash进程环境";

        // 获取初始化步骤ID列表 - 只包括初始化相关的步骤，明确排除不需要的步骤
        List<String> stepIds =
                Arrays.stream(LogstashMachineStep.values())
                        .filter(
                                step ->
                                        step == LogstashMachineStep.CREATE_REMOTE_DIR
                                                || step == LogstashMachineStep.UPLOAD_PACKAGE
                                                || step == LogstashMachineStep.EXTRACT_PACKAGE
                                                || step == LogstashMachineStep.CREATE_CONFIG
                                                || step == LogstashMachineStep.MODIFY_CONFIG)
                        .map(LogstashMachineStep::getId)
                        .collect(Collectors.toList());

        // 为每台机器创建任务
        Map<Long, String> machineTaskMap =
                taskService.createMachineTasks(
                        processId,
                        taskName,
                        taskDescription,
                        TaskOperationType.INITIALIZE,
                        machines,
                        stepIds);

        // 为每台机器并行执行任务
        for (Machine machine : machines) {
            final Machine currentMachine = machine;
            // 初始化机器状态
            machineStateManager.initializeMachineState(processId, machine.getId());

            // 执行初始化操作
            String taskId = machineTaskMap.get(machine.getId());
            taskService.executeAsync(
                    taskId,
                    FutureUtils.toSyncRunnable(
                            () ->
                                    machineStateManager.initializeMachine(
                                            process, currentMachine, taskId),
                            "机器",
                            "初始化Logstash进程环境",
                            currentMachine.getId()),
                    null);
        }
    }

    @Override
    public void startProcess(LogstashProcess process, List<Machine> machines) {
        if (process == null || machines == null || machines.isEmpty()) {
            logger.error(
                    "启动进程参数无效: process={}, machines是否为空={}",
                    process,
                    machines == null || machines.isEmpty());
            return;
        }

        Long processId = process.getId();

        // 创建任务
        String taskName = "启动Logstash进程[" + process.getName() + "]";
        String taskDescription = "启动Logstash进程";

        List<String> stepIds =
                Arrays.asList(
                        LogstashMachineStep.START_PROCESS.getId(),
                        LogstashMachineStep.VERIFY_PROCESS.getId());

        // 为每台机器创建任务
        Map<Long, String> machineTaskMap =
                taskService.createMachineTasks(
                        processId,
                        taskName,
                        taskDescription,
                        TaskOperationType.START,
                        machines,
                        stepIds);

        // 为每台机器并行执行任务
        for (Machine machine : machines) {
            final Machine currentMachine = machine;
            String taskId = machineTaskMap.get(machine.getId());

            // 执行启动操作
            taskService.executeAsync(
                    taskId,
                    FutureUtils.toSyncRunnable(
                            () -> machineStateManager.startMachine(process, currentMachine, taskId),
                            "机器",
                            "启动Logstash进程",
                            currentMachine.getId()),
                    null);
        }
    }

    @Override
    public void stopProcess(Long processId, List<Machine> machines) {
        if (processId == null || machines == null || machines.isEmpty()) {
            logger.error(
                    "停止进程参数无效: processId={}, machines是否为空={}",
                    processId,
                    machines == null || machines.isEmpty());
            return;
        }

        // 查询进程信息
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            logger.error("找不到指定的Logstash进程: {}", processId);
            return;
        }

        // 创建任务
        String taskName = "停止Logstash进程[" + process.getName() + "]";
        String taskDescription = "停止Logstash进程";

        List<String> stepIds = Collections.singletonList(LogstashMachineStep.STOP_PROCESS.getId());

        // 为每台机器创建任务
        Map<Long, String> machineTaskMap =
                taskService.createMachineTasks(
                        processId,
                        taskName,
                        taskDescription,
                        TaskOperationType.STOP,
                        machines,
                        stepIds);

        // 为每台机器并行执行任务
        for (Machine machine : machines) {
            final Machine currentMachine = machine;
            String taskId = machineTaskMap.get(machine.getId());

            // 执行停止操作
            taskService.executeAsync(
                    taskId,
                    FutureUtils.toSyncRunnable(
                            () -> machineStateManager.stopMachine(process, currentMachine, taskId),
                            "机器",
                            "停止Logstash进程",
                            currentMachine.getId()),
                    null);
        }
    }

    @Override
    public CompletableFuture<Boolean> deleteProcessDirectory(
            Long processId, List<Machine> machines) {
        if (processId == null || machines == null || machines.isEmpty()) {
            logger.warn("无法删除进程目录: 进程ID或机器列表为空");
            return CompletableFuture.completedFuture(false);
        }

        logger.info("开始删除Logstash进程目录，进程ID: {}, 机器数量: {}", processId, machines.size());

        // 创建删除进程目录的命令
        LogstashCommand deleteCommand = commandFactory.deleteProcessDirectoryCommand(processId);

        // 收集所有机器上的删除操作的Future
        List<CompletableFuture<Boolean>> futures =
                machines.stream().map(deleteCommand::execute).toList();

        // 等待所有机器上的操作完成
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(
                        v -> {
                            // 检查所有机器是否都成功删除
                            boolean allSuccess =
                                    futures.stream()
                                            .allMatch(
                                                    future -> {
                                                        try {
                                                            return future.get();
                                                        } catch (Exception e) {
                                                            logger.error("获取删除命令执行结果时发生异常", e);
                                                            return false;
                                                        }
                                                    });

                            logger.info(
                                    "Logstash进程目录删除{}，进程ID: {}",
                                    allSuccess ? "成功" : "失败",
                                    processId);
                            return allSuccess;
                        });
    }

    @Override
    public void refreshConfig(Long processId, List<Machine> machines) {
        if (processId == null || machines == null || machines.isEmpty()) {
            logger.error(
                    "刷新配置参数无效: processId={}, machines是否为空={}",
                    processId,
                    machines == null || machines.isEmpty());
            return;
        }

        // 查询进程信息
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            logger.error("找不到指定的Logstash进程: {}", processId);
            return;
        }

        // 验证配置刷新条件
        if (machines.size() == 1) {
            // 单机刷新：验证机器连接
            Machine machine = machines.get(0);
            connectionValidator.validateSingleMachineConnection(machine);
            configService.validateConfigRefreshConditions(processId, machine.getId());
        } else {
            // 全局刷新
            configService.validateConfigRefreshConditions(processId, null);
        }

        // 创建任务
        String taskName = "刷新Logstash配置[" + process.getName() + "]";
        String taskDescription = "刷新Logstash配置到目标机器";

        List<String> stepIds =
                Collections.singletonList(LogstashMachineStep.REFRESH_CONFIG.getId());

        // 为每台机器创建任务
        Map<Long, String> machineTaskMap =
                taskService.createMachineTasks(
                        processId,
                        taskName,
                        taskDescription,
                        TaskOperationType.REFRESH_CONFIG,
                        machines,
                        stepIds);

        // 为每台机器并行执行任务
        for (Machine machine : machines) {
            final Machine currentMachine = machine;
            String taskId = machineTaskMap.get(machine.getId());

            // 执行配置刷新操作
            taskService.executeAsync(
                    taskId,
                    FutureUtils.toSyncRunnable(
                            () ->
                                    machineStateManager.refreshMachineConfig(
                                            process, currentMachine, taskId),
                            "机器",
                            "刷新Logstash配置",
                            currentMachine.getId()),
                    null);
        }
    }

    @Override
    public void startMachine(LogstashProcess process, Machine machine) {
        if (process == null || machine == null) {
            logger.error("启动机器参数无效: process={}, machine={}", process, machine);
            return;
        }

        // 验证机器连接
        connectionValidator.validateSingleMachineConnection(machine);

        Long processId = process.getId();

        // 创建任务
        String taskName = "启动Logstash进程[" + process.getName() + "]在机器[" + machine.getName() + "]上";
        String taskDescription = "启动单台机器上的Logstash进程";

        List<String> stepIds =
                Arrays.asList(
                        LogstashMachineStep.START_PROCESS.getId(),
                        LogstashMachineStep.VERIFY_PROCESS.getId());

        // 创建单机任务
        String taskId =
                taskService.createMachineTask(
                        processId,
                        machine.getId(),
                        taskName,
                        taskDescription,
                        TaskOperationType.START,
                        stepIds);

        // 执行启动操作
        taskService.executeAsync(
                taskId,
                FutureUtils.toSyncRunnable(
                        () -> machineStateManager.startMachine(process, machine, taskId),
                        "机器",
                        "启动Logstash进程",
                        machine.getId()),
                null);
    }

    @Override
    public void stopMachine(Long processId, Machine machine) {
        if (processId == null || machine == null) {
            logger.error("停止机器参数无效: processId={}, machine={}", processId, machine);
            return;
        }

        // 验证机器连接
        connectionValidator.validateSingleMachineConnection(machine);

        // 查询进程信息
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            logger.error("找不到指定的Logstash进程: {}", processId);
            return;
        }

        // 创建任务
        String taskName = "停止Logstash进程[" + process.getName() + "]在机器[" + machine.getName() + "]上";
        String taskDescription = "停止单台机器上的Logstash进程";

        List<String> stepIds = Collections.singletonList(LogstashMachineStep.STOP_PROCESS.getId());

        // 创建单机任务
        String taskId =
                taskService.createMachineTask(
                        processId,
                        machine.getId(),
                        taskName,
                        taskDescription,
                        TaskOperationType.STOP,
                        stepIds);

        // 执行停止操作
        taskService.executeAsync(
                taskId,
                FutureUtils.toSyncRunnable(
                        () -> machineStateManager.stopMachine(process, machine, taskId),
                        "机器",
                        "停止Logstash进程",
                        machine.getId()),
                null);
    }

    @Override
    public void restartMachine(Long processId, Machine machine) {
        if (processId == null || machine == null) {
            logger.error("重启机器参数无效: processId={}, machine={}", processId, machine);
            return;
        }

        // 验证机器连接
        connectionValidator.validateSingleMachineConnection(machine);

        // 查询进程信息
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            logger.error("找不到指定的Logstash进程: {}", processId);
            return;
        }

        // 创建任务
        String taskName = "重启Logstash进程[" + process.getName() + "]在机器[" + machine.getName() + "]上";
        String taskDescription = "重启单台机器上的Logstash进程";

        List<String> stepIds =
                Arrays.asList(
                        LogstashMachineStep.STOP_PROCESS.getId(),
                        LogstashMachineStep.START_PROCESS.getId(),
                        LogstashMachineStep.VERIFY_PROCESS.getId());

        // 创建单机任务
        String taskId =
                taskService.createMachineTask(
                        processId,
                        machine.getId(),
                        taskName,
                        taskDescription,
                        TaskOperationType.RESTART,
                        stepIds);

        // 执行重启操作
        taskService.executeAsync(
                taskId,
                FutureUtils.toSyncRunnable(
                        () -> {
                            // 先停止进程，然后在成功时启动进程
                            return machineStateManager
                                    .stopMachine(process, machine, taskId)
                                    .thenCompose(
                                            stopSuccess -> {
                                                if (!stopSuccess) {
                                                    logger.warn(
                                                            "机器 [{}] 上的Logstash进程停止失败，跳过启动步骤",
                                                            machine.getId());
                                                    return CompletableFuture.completedFuture(false);
                                                }

                                                // 停止成功后启动进程
                                                logger.info(
                                                        "机器 [{}] 上的Logstash进程停止成功，准备启动",
                                                        machine.getId());
                                                return machineStateManager.startMachine(
                                                        process, machine, taskId);
                                            });
                        },
                        "机器",
                        "重启Logstash进程",
                        machine.getId()),
                null);
    }

    @Override
    public void updateMultipleConfigs(
            Long processId,
            List<Machine> machines,
            String configContent,
            String jvmOptions,
            String logstashYml) {
        logger.info("开始更新进程 [{}] 的多种配置到 {} 台机器", processId, machines.size());

        // 检查输入参数
        if (processId == null || machines.isEmpty()) {
            logger.error("更新配置参数无效: processId={}, machines是否为空={}", processId, machines.isEmpty());
            return;
        }

        // 检查是否有任何需要更新的配置
        if (!StringUtils.hasText(configContent)
                && !StringUtils.hasText(jvmOptions)
                && !StringUtils.hasText(logstashYml)) {
            logger.warn("进程 [{}] 更新配置任务未提供任何配置内容，无需更新", processId);
            return;
        }

        // 检查所有机器的状态，确保没有正在运行的实例
        for (Machine machine : machines) {
            configService.validateConfigUpdateConditions(processId, machine.getId());
        }

        // 创建任务记录
        String taskName = "更新Logstash配置";
        String taskDescription = "更新Logstash配置到目标机器";

        // 确定需要执行的步骤
        List<String> stepIds = new ArrayList<>();
        if (StringUtils.hasText(configContent)) {
            stepIds.add(LogstashMachineStep.UPDATE_MAIN_CONFIG.getId());
        }
        if (StringUtils.hasText(jvmOptions)) {
            stepIds.add(LogstashMachineStep.UPDATE_JVM_CONFIG.getId());
        }
        if (StringUtils.hasText(logstashYml)) {
            stepIds.add(LogstashMachineStep.UPDATE_SYSTEM_CONFIG.getId());
        }

        // 为每台机器创建任务
        Map<Long, String> machineTaskMap =
                taskService.createMachineTasks(
                        processId,
                        taskName,
                        taskDescription,
                        TaskOperationType.UPDATE_CONFIG,
                        machines,
                        stepIds);

        // 为每台机器并行执行任务
        for (Machine machine : machines) {
            final Machine currentMachine = machine;
            String taskId = machineTaskMap.get(machine.getId());

            // 执行更新配置操作
            taskService.executeAsync(
                    taskId,
                    FutureUtils.toSyncRunnable(
                            () ->
                                    machineStateManager.updateMachineConfig(
                                            processId,
                                            currentMachine,
                                            configContent,
                                            jvmOptions,
                                            logstashYml,
                                            taskId),
                            "机器",
                            "更新Logstash配置",
                            currentMachine.getId()),
                    null);
        }
    }

    @Override
    public String getDeployBaseDir() {
        return logstashProperties.getDeployBaseDir();
    }

    @Override
    public LogstashProcessConfigService getConfigService() {
        return this.configService;
    }
}
