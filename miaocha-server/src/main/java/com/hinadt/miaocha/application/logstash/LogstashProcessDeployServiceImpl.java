package com.hinadt.miaocha.application.logstash;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineStep;
import com.hinadt.miaocha.application.logstash.enums.TaskOperationType;
import com.hinadt.miaocha.application.logstash.path.LogstashDeployPathManager;
import com.hinadt.miaocha.application.logstash.state.LogstashMachineStateManager;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.common.util.FutureUtil;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Logstash进程部署服务实现类 - 基于LogstashMachine实例级操作 */
@Slf4j
@Service
public class LogstashProcessDeployServiceImpl implements LogstashProcessDeployService {

    private final TaskService taskService;
    private final LogstashMachineStateManager machineStateManager;
    private final LogstashProcessConfigService configService;
    private final LogstashDeployPathManager deployPathManager;

    public LogstashProcessDeployServiceImpl(
            TaskService taskService,
            LogstashMachineStateManager machineStateManager,
            LogstashProcessConfigService configService,
            LogstashDeployPathManager deployPathManager) {
        this.taskService = taskService;
        this.machineStateManager = machineStateManager;
        this.configService = configService;
        this.deployPathManager = deployPathManager;
    }

    // ==================== 批量实例操作（基于LogstashMachine列表） ====================

    @Override
    public void initializeInstances(
            List<LogstashMachine> logstashMachines, LogstashProcess process) {
        if (!validateInstancesWithProcess(logstashMachines, process, "批量初始化实例")) {
            return;
        }

        var stepIds =
                Arrays.asList(
                        LogstashMachineStep.CREATE_REMOTE_DIR.getId(),
                        LogstashMachineStep.UPLOAD_PACKAGE.getId(),
                        LogstashMachineStep.EXTRACT_PACKAGE.getId(),
                        LogstashMachineStep.CREATE_CONFIG.getId(),
                        LogstashMachineStep.MODIFY_CONFIG.getId());

        executeInstancesOperation(
                logstashMachines,
                "批量初始化Logstash实例[" + process.getName() + "]",
                "批量初始化Logstash实例环境",
                TaskOperationType.INITIALIZE,
                stepIds,
                (logstashMachine, taskId) ->
                        machineStateManager.deployInstance(logstashMachine.getId(), taskId),
                "初始化Logstash实例环境");
    }

    @Override
    public void startInstances(List<LogstashMachine> logstashMachines, LogstashProcess process) {
        if (!validateInstancesWithProcess(logstashMachines, process, "批量启动实例")) {
            return;
        }

        var stepIds =
                Arrays.asList(
                        LogstashMachineStep.START_PROCESS.getId(),
                        LogstashMachineStep.VERIFY_PROCESS.getId());

        executeInstancesOperation(
                logstashMachines,
                "批量启动Logstash实例[" + process.getName() + "]",
                "批量启动Logstash实例",
                TaskOperationType.START,
                stepIds,
                (logstashMachine, taskId) ->
                        machineStateManager.startInstance(logstashMachine.getId(), taskId),
                "启动Logstash实例");
    }

    @Override
    public void stopInstances(List<LogstashMachine> logstashMachines) {
        if (!validateInstances(logstashMachines, "批量停止实例")) {
            return;
        }

        var stepIds = Collections.singletonList(LogstashMachineStep.STOP_PROCESS.getId());

        executeInstancesOperation(
                logstashMachines,
                "批量停止Logstash实例",
                "批量停止Logstash实例",
                TaskOperationType.STOP,
                stepIds,
                (logstashMachine, taskId) ->
                        machineStateManager.stopInstance(logstashMachine.getId(), taskId),
                "停止Logstash实例");
    }

    @Override
    public void forceStopInstances(List<LogstashMachine> logstashMachines) {
        if (!validateInstances(logstashMachines, "批量强制停止实例")) {
            return;
        }

        var stepIds = Collections.singletonList(LogstashMachineStep.STOP_PROCESS.getId());

        executeInstancesOperation(
                logstashMachines,
                "批量强制停止Logstash实例",
                "批量强制停止Logstash实例（应急操作）",
                TaskOperationType.FORCE_STOP,
                stepIds,
                (logstashMachine, taskId) ->
                        machineStateManager.forceStopInstance(logstashMachine.getId(), taskId),
                "强制停止Logstash实例");
    }

    @Override
    public void updateInstancesConfig(
            List<LogstashMachine> logstashMachines,
            String configContent,
            String jvmOptions,
            String logstashYml) {

        if (!validateInstances(logstashMachines, "批量更新实例配置")) {
            return;
        }
        var stepIds = new ArrayList<String>();

        if (StringUtils.hasText(configContent)) {
            stepIds.add(LogstashMachineStep.UPDATE_MAIN_CONFIG.getId());
        }
        if (StringUtils.hasText(jvmOptions)) {
            stepIds.add(LogstashMachineStep.UPDATE_JVM_CONFIG.getId());
        }
        if (StringUtils.hasText(logstashYml)) {
            stepIds.add(LogstashMachineStep.UPDATE_SYSTEM_CONFIG.getId());
        }

        executeInstancesOperation(
                logstashMachines,
                "批量更新Logstash实例配置",
                "批量更新Logstash实例配置文件",
                TaskOperationType.UPDATE_CONFIG,
                stepIds,
                (logstashMachine, taskId) ->
                        machineStateManager.updateInstanceConfig(
                                logstashMachine.getId(),
                                configContent,
                                jvmOptions,
                                logstashYml,
                                taskId),
                "更新Logstash实例配置");
    }

    @Override
    public void refreshInstancesConfig(
            List<LogstashMachine> logstashMachines, LogstashProcess process) {
        if (!validateInstancesWithProcess(logstashMachines, process, "批量刷新实例配置")) {
            return;
        }

        var stepIds = Collections.singletonList(LogstashMachineStep.REFRESH_CONFIG.getId());

        executeInstancesOperation(
                logstashMachines,
                "批量刷新Logstash实例配置[" + process.getName() + "]",
                "批量刷新Logstash实例配置文件",
                TaskOperationType.REFRESH_CONFIG,
                stepIds,
                (logstashMachine, taskId) ->
                        machineStateManager.refreshInstanceConfig(logstashMachine.getId(), taskId),
                "刷新Logstash实例配置");
    }

    @Override
    public boolean deleteInstancesDirectory(List<LogstashMachine> logstashMachines) {
        if (logstashMachines == null || logstashMachines.isEmpty()) {
            log.warn("批量删除实例目录参数无效: logstashMachines为空");
            return false;
        }

        // 并行执行删除操作
        List<CompletableFuture<Boolean>> futures =
                logstashMachines.stream()
                        .map(
                                logstashMachine ->
                                        FutureUtil.supplyAsync(
                                                () -> deleteInstanceDirectory(logstashMachine),
                                                e -> {
                                                    log.error(
                                                            "删除Logstash实例 [{}] 目录时发生异常: {}",
                                                            logstashMachine.getId(),
                                                            e.getMessage(),
                                                            e);
                                                    return false;
                                                }))
                        .toList();

        // 等待所有删除操作完成
        CompletableFuture<Void> allFutures =
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        try {
            allFutures.join();
            // 检查所有操作是否成功
            return futures.stream().allMatch(CompletableFuture::join);
        } catch (Exception e) {
            log.error("批量删除实例目录时发生异常: {}", e.getMessage(), e);
            return false;
        }
    }

    // ==================== 辅助方法 ====================

    @Override
    public String getDeployBaseDir() {
        return deployPathManager.getDeployBaseDir();
    }

    @Override
    public String getInstanceDeployPath(LogstashMachine logstashMachine) {
        return deployPathManager.getInstanceDeployPath(logstashMachine);
    }

    @Override
    public String generateDefaultInstancePath(MachineInfo machineInfo) {
        return deployPathManager.generateDefaultInstancePath(machineInfo);
    }

    @Override
    public LogstashProcessConfigService getConfigService() {
        return configService;
    }

    // ==================== 私有通用方法 ====================

    /** 验证实例列表有效性 */
    private boolean validateInstances(List<LogstashMachine> logstashMachines, String operation) {
        if (logstashMachines == null || logstashMachines.isEmpty()) {
            log.error("{}参数无效: logstashMachines为空", operation);
            return false;
        }
        return true;
    }

    /** 验证实例列表和进程有效性 */
    private boolean validateInstancesWithProcess(
            List<LogstashMachine> logstashMachines, LogstashProcess process, String operation) {

        if (logstashMachines == null || logstashMachines.isEmpty() || process == null) {
            log.error(
                    "{}参数无效: logstashMachines是否为空={}, process={}",
                    operation,
                    logstashMachines == null || logstashMachines.isEmpty(),
                    process);
            return false;
        }
        return true;
    }

    /** 函数式接口：实例操作 */
    @FunctionalInterface
    private interface InstanceOperation {
        boolean execute(LogstashMachine logstashMachine, String taskId);
    }

    /** 执行批量实例操作的通用方法 */
    private void executeInstancesOperation(
            List<LogstashMachine> logstashMachines,
            String taskName,
            String taskDescription,
            TaskOperationType operationType,
            List<String> stepIds,
            InstanceOperation operation,
            String operationDescription) {

        // 为每个实例创建任务
        Map<Long, String> instanceTaskMap =
                taskService.createInstanceTasks(
                        logstashMachines, taskName, taskDescription, operationType, stepIds);

        // 并行执行操作
        logstashMachines.forEach(
                logstashMachine -> {
                    String taskId = instanceTaskMap.get(logstashMachine.getId());

                    taskService.executeAsync(
                            taskId,
                            () -> {
                                try {
                                    boolean success = operation.execute(logstashMachine, taskId);
                                    log.info(
                                            "实例 [{}] {}{}",
                                            logstashMachine.getId(),
                                            operationDescription,
                                            success ? "成功" : "失败");

                                    if (!success) {
                                        throw new RuntimeException(
                                                String.format(
                                                        "实例 [%s] %s失败",
                                                        logstashMachine.getId(),
                                                        operationDescription));
                                    }
                                } catch (Exception e) {
                                    log.error(
                                            "实例 [{}] {}失败: {}",
                                            logstashMachine.getId(),
                                            operationDescription,
                                            e.getMessage(),
                                            e);
                                    throw e;
                                }
                            },
                            null);
                });
    }

    /** 删除单个实例目录 */
    private boolean deleteInstanceDirectory(LogstashMachine logstashMachine) {
        return machineStateManager.deleteInstance(logstashMachine.getId());
    }
}
