package com.hinadt.miaocha.application.logstash;

import com.hinadt.miaocha.application.logstash.command.LogstashCommand;
import com.hinadt.miaocha.application.logstash.command.LogstashCommandFactory;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineStep;
import com.hinadt.miaocha.application.logstash.enums.TaskOperationType;
import com.hinadt.miaocha.application.logstash.state.LogstashMachineStateManager;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.common.util.FutureUtils;
import com.hinadt.miaocha.config.LogstashProperties;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.domain.mapper.LogstashProcessMapper;
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
    private final LogstashMachineMapper logstashMachineMapper;
    private final TaskService taskService;
    private final LogstashMachineStateManager machineStateManager;
    private final LogstashCommandFactory commandFactory;
    private final LogstashProcessConfigService configService;
    private final LogstashProperties logstashProperties;
    private final LogstashMachineConnectionValidator connectionValidator;

    public LogstashProcessDeployServiceImpl(
            LogstashProcessMapper logstashProcessMapper,
            LogstashMachineMapper logstashMachineMapper,
            TaskService taskService,
            LogstashMachineStateManager machineStateManager,
            LogstashCommandFactory commandFactory,
            LogstashProcessConfigService configService,
            LogstashProperties logstashProperties,
            LogstashMachineConnectionValidator connectionValidator) {
        this.logstashProcessMapper = logstashProcessMapper;
        this.logstashMachineMapper = logstashMachineMapper;
        this.taskService = taskService;
        this.machineStateManager = machineStateManager;
        this.commandFactory = commandFactory;
        this.configService = configService;
        this.logstashProperties = logstashProperties;
        this.connectionValidator = connectionValidator;
    }

    @Override
    public void initializeProcess(LogstashProcess process, List<MachineInfo> machineInfos) {
        if (process == null || machineInfos == null || machineInfos.isEmpty()) {
            logger.error(
                    "初始化进程参数无效: process={}, machines是否为空={}",
                    process,
                    machineInfos == null || machineInfos.isEmpty());
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
                        machineInfos,
                        stepIds);

        // 为每台机器并行执行任务
        for (MachineInfo machineInfo : machineInfos) {
            final MachineInfo currentMachineInfo = machineInfo;
            // 初始化机器状态
            machineStateManager.initializeMachineState(processId, machineInfo.getId());

            // 执行初始化操作
            String taskId = machineTaskMap.get(machineInfo.getId());
            taskService.executeAsync(
                    taskId,
                    FutureUtils.toSyncRunnable(
                            () ->
                                    machineStateManager.initializeMachine(
                                            process, currentMachineInfo, taskId),
                            "机器",
                            "初始化Logstash进程环境",
                            currentMachineInfo.getId()),
                    null);
        }
    }

    @Override
    public void startProcess(LogstashProcess process, List<MachineInfo> machineInfos) {
        if (process == null || machineInfos == null || machineInfos.isEmpty()) {
            logger.error(
                    "启动进程参数无效: process={}, machines是否为空={}",
                    process,
                    machineInfos == null || machineInfos.isEmpty());
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
                        machineInfos,
                        stepIds);

        // 为每台机器并行执行任务
        for (MachineInfo machineInfo : machineInfos) {
            final MachineInfo currentMachineInfo = machineInfo;
            String taskId = machineTaskMap.get(machineInfo.getId());

            // 执行启动操作
            taskService.executeAsync(
                    taskId,
                    FutureUtils.toSyncRunnable(
                            () ->
                                    machineStateManager.startMachine(
                                            process, currentMachineInfo, taskId),
                            "机器",
                            "启动Logstash进程",
                            currentMachineInfo.getId()),
                    null);
        }
    }

    @Override
    public void stopProcess(Long processId, List<MachineInfo> machineInfos) {
        if (processId == null || machineInfos == null || machineInfos.isEmpty()) {
            logger.error(
                    "停止进程参数无效: processId={}, machines是否为空={}",
                    processId,
                    machineInfos == null || machineInfos.isEmpty());
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
                        machineInfos,
                        stepIds);

        // 为每台机器并行执行任务
        for (MachineInfo machineInfo : machineInfos) {
            final MachineInfo currentMachineInfo = machineInfo;
            String taskId = machineTaskMap.get(machineInfo.getId());

            // 执行停止操作
            taskService.executeAsync(
                    taskId,
                    FutureUtils.toSyncRunnable(
                            () ->
                                    machineStateManager.stopMachine(
                                            process, currentMachineInfo, taskId),
                            "机器",
                            "停止Logstash进程",
                            currentMachineInfo.getId()),
                    null);
        }
    }

    @Override
    public void forceStopProcess(Long processId, List<MachineInfo> machineInfos) {
        if (processId == null || machineInfos == null || machineInfos.isEmpty()) {
            logger.error(
                    "强制停止进程参数无效: processId={}, machines是否为空={}",
                    processId,
                    machineInfos == null || machineInfos.isEmpty());
            return;
        }

        // 查询进程信息
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            logger.error("找不到指定的Logstash进程: {}", processId);
            return;
        }

        // 创建任务
        String taskName = "强制停止Logstash进程[" + process.getName() + "]";
        String taskDescription = "强制停止Logstash进程（应急操作）";

        List<String> stepIds = Collections.singletonList(LogstashMachineStep.STOP_PROCESS.getId());

        // 为每台机器创建任务
        Map<Long, String> machineTaskMap =
                taskService.createMachineTasks(
                        processId,
                        taskName,
                        taskDescription,
                        TaskOperationType.FORCE_STOP,
                        machineInfos,
                        stepIds);

        // 为每台机器并行执行任务
        for (MachineInfo machineInfo : machineInfos) {
            final MachineInfo currentMachineInfo = machineInfo;
            String taskId = machineTaskMap.get(machineInfo.getId());

            // 执行强制停止操作
            taskService.executeAsync(
                    taskId,
                    FutureUtils.toSyncRunnable(
                            () ->
                                    machineStateManager.forceStopMachine(
                                            process, currentMachineInfo, taskId),
                            "机器",
                            "强制停止Logstash进程",
                            currentMachineInfo.getId()),
                    null);
        }
    }

    @Override
    public CompletableFuture<Boolean> deleteProcessDirectory(
            Long processId, List<MachineInfo> machineInfos) {
        if (processId == null || machineInfos == null || machineInfos.isEmpty()) {
            logger.warn("无法删除进程目录: 进程ID或机器列表为空");
            return CompletableFuture.completedFuture(false);
        }

        logger.info("开始删除Logstash进程目录，进程ID: {}, 机器数量: {}", processId, machineInfos.size());

        // 创建删除进程目录的命令
        LogstashCommand deleteCommand = commandFactory.deleteProcessDirectoryCommand(processId);

        // 收集所有机器上的删除操作的Future
        List<CompletableFuture<Boolean>> futures =
                machineInfos.stream().map(deleteCommand::execute).toList();

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
    public void refreshConfig(Long processId, List<MachineInfo> machineInfos) {
        if (processId == null || machineInfos == null || machineInfos.isEmpty()) {
            logger.error(
                    "刷新配置参数无效: processId={}, machines是否为空={}",
                    processId,
                    machineInfos == null || machineInfos.isEmpty());
            return;
        }

        // 查询进程信息
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            logger.error("找不到指定的Logstash进程: {}", processId);
            return;
        }

        // 验证配置刷新条件
        if (machineInfos.size() == 1) {
            // 单机刷新：验证机器连接
            MachineInfo machineInfo = machineInfos.get(0);
            connectionValidator.validateSingleMachineConnection(machineInfo);
            configService.validateConfigRefreshConditions(processId, machineInfo.getId());
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
                        machineInfos,
                        stepIds);

        // 为每台机器并行执行任务
        for (MachineInfo machineInfo : machineInfos) {
            final MachineInfo currentMachineInfo = machineInfo;
            String taskId = machineTaskMap.get(machineInfo.getId());

            // 执行配置刷新操作
            taskService.executeAsync(
                    taskId,
                    FutureUtils.toSyncRunnable(
                            () ->
                                    machineStateManager.refreshMachineConfig(
                                            process, currentMachineInfo, taskId),
                            "机器",
                            "刷新Logstash配置",
                            currentMachineInfo.getId()),
                    null);
        }
    }

    @Override
    public void startMachine(LogstashProcess process, MachineInfo machineInfo) {
        if (process == null || machineInfo == null) {
            logger.error("启动机器参数无效: process={}, machine={}", process, machineInfo);
            return;
        }

        // 验证机器连接
        connectionValidator.validateSingleMachineConnection(machineInfo);

        Long processId = process.getId();

        // 创建任务
        String taskName =
                "启动Logstash进程[" + process.getName() + "]在机器[" + machineInfo.getName() + "]上";
        String taskDescription = "启动单台机器上的Logstash进程";

        List<String> stepIds =
                Arrays.asList(
                        LogstashMachineStep.START_PROCESS.getId(),
                        LogstashMachineStep.VERIFY_PROCESS.getId());

        // 创建单机任务
        String taskId =
                taskService.createMachineTask(
                        processId,
                        machineInfo.getId(),
                        taskName,
                        taskDescription,
                        TaskOperationType.START,
                        stepIds);

        // 执行启动操作
        taskService.executeAsync(
                taskId,
                FutureUtils.toSyncRunnable(
                        () -> machineStateManager.startMachine(process, machineInfo, taskId),
                        "机器",
                        "启动Logstash进程",
                        machineInfo.getId()),
                null);
    }

    @Override
    public void stopMachine(Long processId, MachineInfo machineInfo) {
        if (processId == null || machineInfo == null) {
            logger.error("停止机器参数无效: processId={}, machine={}", processId, machineInfo);
            return;
        }

        // 验证机器连接
        connectionValidator.validateSingleMachineConnection(machineInfo);

        // 查询进程信息
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            logger.error("找不到指定的Logstash进程: {}", processId);
            return;
        }

        // 创建任务
        String taskName =
                "停止Logstash进程[" + process.getName() + "]在机器[" + machineInfo.getName() + "]上";
        String taskDescription = "停止单台机器上的Logstash进程";

        List<String> stepIds = Collections.singletonList(LogstashMachineStep.STOP_PROCESS.getId());

        // 创建单机任务
        String taskId =
                taskService.createMachineTask(
                        processId,
                        machineInfo.getId(),
                        taskName,
                        taskDescription,
                        TaskOperationType.STOP,
                        stepIds);

        // 执行停止操作
        taskService.executeAsync(
                taskId,
                FutureUtils.toSyncRunnable(
                        () -> machineStateManager.stopMachine(process, machineInfo, taskId),
                        "机器",
                        "停止Logstash进程",
                        machineInfo.getId()),
                null);
    }

    @Override
    public void forceStopMachine(Long processId, MachineInfo machineInfo) {
        if (processId == null || machineInfo == null) {
            logger.error("强制停止机器参数无效: processId={}, machine={}", processId, machineInfo);
            return;
        }

        // 查询进程信息
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            logger.error("找不到指定的Logstash进程: {}", processId);
            return;
        }

        // 创建任务
        String taskName =
                "强制停止Logstash进程[" + process.getName() + "]在机器[" + machineInfo.getName() + "]上";
        String taskDescription = "强制停止单台机器上的Logstash进程（应急操作）";

        List<String> stepIds = Collections.singletonList(LogstashMachineStep.STOP_PROCESS.getId());

        // 创建单机任务
        String taskId =
                taskService.createMachineTask(
                        processId,
                        machineInfo.getId(),
                        taskName,
                        taskDescription,
                        TaskOperationType.FORCE_STOP,
                        stepIds);

        // 执行强制停止操作
        taskService.executeAsync(
                taskId,
                FutureUtils.toSyncRunnable(
                        () -> machineStateManager.forceStopMachine(process, machineInfo, taskId),
                        "机器",
                        "强制停止Logstash进程",
                        machineInfo.getId()),
                null);
    }

    @Override
    public void restartMachine(Long processId, MachineInfo machineInfo) {
        if (processId == null || machineInfo == null) {
            logger.error("重启机器参数无效: processId={}, machine={}", processId, machineInfo);
            return;
        }

        // 验证机器连接
        connectionValidator.validateSingleMachineConnection(machineInfo);

        // 查询进程信息
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            logger.error("找不到指定的Logstash进程: {}", processId);
            return;
        }

        // 创建任务
        String taskName =
                "重启Logstash进程[" + process.getName() + "]在机器[" + machineInfo.getName() + "]上";
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
                        machineInfo.getId(),
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
                                    .stopMachine(process, machineInfo, taskId)
                                    .thenCompose(
                                            stopSuccess -> {
                                                if (!stopSuccess) {
                                                    logger.warn(
                                                            "机器 [{}] 上的Logstash进程停止失败，跳过启动步骤",
                                                            machineInfo.getId());
                                                    return CompletableFuture.completedFuture(false);
                                                }

                                                // 停止成功后启动进程
                                                logger.info(
                                                        "机器 [{}] 上的Logstash进程停止成功，准备启动",
                                                        machineInfo.getId());
                                                return machineStateManager.startMachine(
                                                        process, machineInfo, taskId);
                                            });
                        },
                        "机器",
                        "重启Logstash进程",
                        machineInfo.getId()),
                null);
    }

    @Override
    public void updateMultipleConfigs(
            Long processId,
            List<MachineInfo> machineInfos,
            String configContent,
            String jvmOptions,
            String logstashYml) {
        logger.info("开始更新进程 [{}] 的多种配置到 {} 台机器", processId, machineInfos.size());

        // 检查输入参数
        if (processId == null || machineInfos.isEmpty()) {
            logger.error(
                    "更新配置参数无效: processId={}, machines是否为空={}", processId, machineInfos.isEmpty());
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
        for (MachineInfo machineInfo : machineInfos) {
            configService.validateConfigUpdateConditions(processId, machineInfo.getId());
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
                        machineInfos,
                        stepIds);

        // 为每台机器并行执行任务
        for (MachineInfo machineInfo : machineInfos) {
            final MachineInfo currentMachineInfo = machineInfo;
            String taskId = machineTaskMap.get(machineInfo.getId());

            // 执行更新配置操作
            taskService.executeAsync(
                    taskId,
                    FutureUtils.toSyncRunnable(
                            () ->
                                    machineStateManager.updateMachineConfig(
                                            processId,
                                            currentMachineInfo,
                                            configContent,
                                            jvmOptions,
                                            logstashYml,
                                            taskId),
                            "机器",
                            "更新Logstash配置",
                            currentMachineInfo.getId()),
                    null);
        }
    }

    @Override
    public String getDeployBaseDir() {
        return logstashProperties.getDeployBaseDir();
    }

    @Override
    public String getProcessDeployPath(Long processId, MachineInfo machineInfo) {
        try {
            // 优先从数据库获取机器特定的部署路径
            LogstashMachine logstashMachine =
                    logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                            processId, machineInfo.getId());
            if (logstashMachine != null && StringUtils.hasText(logstashMachine.getDeployPath())) {
                // 数据库中存储的是完整的部署路径，直接使用
                return logstashMachine.getDeployPath();
            }
        } catch (Exception e) {
            logger.warn("无法从数据库获取部署路径，使用默认路径: {}", e.getMessage());
        }

        // 使用默认路径
        return generateDefaultProcessPath(processId, machineInfo);
    }

    @Override
    public String generateDefaultProcessPath(Long processId, MachineInfo machineInfo) {
        // 规范化基础部署目录
        String baseDeployDir = getDeployBaseDir();
        String actualDeployDir;

        if (baseDeployDir.startsWith("/")) {
            // 已经是绝对路径，直接使用
            actualDeployDir = baseDeployDir;
        } else {
            // 相对路径，转换为用户家目录下的路径
            String username = machineInfo.getUsername();
            actualDeployDir = String.format("/home/%s/%s", username, baseDeployDir);
        }

        // 拼接进程ID
        return String.format("%s/logstash-%d", actualDeployDir, processId);
    }

    @Override
    public LogstashProcessConfigService getConfigService() {
        return this.configService;
    }
}
