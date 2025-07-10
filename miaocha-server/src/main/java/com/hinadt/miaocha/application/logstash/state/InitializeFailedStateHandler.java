package com.hinadt.miaocha.application.logstash.state;

import com.hinadt.miaocha.application.logstash.command.LogstashCommand;
import com.hinadt.miaocha.application.logstash.command.LogstashCommandFactory;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineStep;
import com.hinadt.miaocha.application.logstash.enums.StepStatus;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import org.springframework.stereotype.Component;

/** 初始化失败状态处理器 允许重新初始化进程 */
@Component
public class InitializeFailedStateHandler extends AbstractLogstashMachineStateHandler {

    public InitializeFailedStateHandler(
            TaskService taskService, LogstashCommandFactory commandFactory) {
        super(taskService, commandFactory);
    }

    @Override
    public LogstashMachineState getState() {
        return LogstashMachineState.INITIALIZE_FAILED;
    }

    @Override
    public boolean handleInitialize(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("重新初始化机器 [{}] 上的LogstashMachine实例 [{}]", machineId, logstashMachineId);

        // 重置所有初始化步骤的状态
        taskService.resetStepStatuses(taskId, StepStatus.PENDING);

        // 先删除进程目录（不计入步骤状态）
        if (!cleanupProcessDirectory(logstashMachine, machineInfo)) {
            return false;
        }

        // 1. 创建远程目录
        if (!createRemoteDirectory(logstashMachine, machineInfo, taskId)) {
            return false;
        }

        // 2. 上传Logstash压缩包
        if (!uploadLogstashPackage(logstashMachine, machineInfo, taskId)) {
            return false;
        }

        // 3. 解压Logstash包
        if (!extractLogstashPackage(logstashMachine, machineInfo, taskId)) {
            return false;
        }

        // 4. 创建配置文件
        if (!createConfigFiles(logstashMachine, machineInfo, taskId)) {
            return false;
        }

        // 5. 修改系统配置
        return modifySystemConfig(logstashMachine, machineInfo, taskId);
    }

    /**
     * 清理进程目录
     *
     * @param logstashMachine LogstashMachine实例
     * @param machineInfo 目标机器
     * @return 操作是否成功
     */
    private boolean cleanupProcessDirectory(
            LogstashMachine logstashMachine, MachineInfo machineInfo) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("删除机器 [{}] 上的LogstashMachine实例 [{}] 目录", machineId, logstashMachineId);
        LogstashCommand deleteCommand =
                commandFactory.deleteProcessDirectoryCommand(logstashMachineId);

        try {
            boolean success = deleteCommand.execute(machineInfo);
            if (!success) {
                // 即使删除失败也继续执行（可能目录不存在）
                logger.warn("删除进程目录失败，将继续执行初始化");
            }
            return true;
        } catch (Exception ex) {
            // 即使删除失败也继续执行（可能目录不存在）
            logger.warn("删除进程目录失败，将继续执行初始化: {}", ex.getMessage());
            return true;
        }
    }

    /**
     * 创建远程目录
     *
     * @param logstashMachine LogstashMachine实例
     * @param machineInfo 目标机器
     * @param taskId 任务ID
     * @return 操作是否成功
     */
    private boolean createRemoteDirectory(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        return executeStep(
                logstashMachine,
                machineInfo,
                taskId,
                LogstashMachineStep.CREATE_REMOTE_DIR,
                "创建远程目录",
                () -> commandFactory.createDirectoryCommand(logstashMachine.getId()));
    }

    /**
     * 上传Logstash压缩包
     *
     * @param logstashMachine LogstashMachine实例
     * @param machineInfo 目标机器
     * @param taskId 任务ID
     * @return 操作是否成功
     */
    private boolean uploadLogstashPackage(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        return executeStep(
                logstashMachine,
                machineInfo,
                taskId,
                LogstashMachineStep.UPLOAD_PACKAGE,
                "上传Logstash安装包",
                () -> commandFactory.uploadPackageCommand(logstashMachine.getId()));
    }

    /**
     * 解压Logstash包
     *
     * @param logstashMachine LogstashMachine实例
     * @param machineInfo 目标机器
     * @param taskId 任务ID
     * @return 操作是否成功
     */
    private boolean extractLogstashPackage(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        return executeStep(
                logstashMachine,
                machineInfo,
                taskId,
                LogstashMachineStep.EXTRACT_PACKAGE,
                "解压Logstash安装包",
                () -> commandFactory.extractPackageCommand(logstashMachine.getId()));
    }

    /**
     * 创建配置文件
     *
     * @param logstashMachine LogstashMachine实例
     * @param machineInfo 目标机器
     * @param taskId 任务ID
     * @return 操作是否成功
     */
    private boolean createConfigFiles(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        return executeStep(
                logstashMachine,
                machineInfo,
                taskId,
                LogstashMachineStep.CREATE_CONFIG,
                "创建配置文件",
                () -> commandFactory.createConfigCommand(logstashMachine));
    }

    /**
     * 修改系统配置
     *
     * @param logstashMachine LogstashMachine实例
     * @param machineInfo 目标机器
     * @param taskId 任务ID
     * @return 操作是否成功
     */
    private boolean modifySystemConfig(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        String jvmOptions = logstashMachine.getJvmOptions();
        String logstashYml = logstashMachine.getLogstashYml();
        return executeStep(
                logstashMachine,
                machineInfo,
                taskId,
                LogstashMachineStep.MODIFY_CONFIG,
                "修改系统配置",
                () ->
                        commandFactory.modifySystemConfigCommand(
                                logstashMachine.getId(), jvmOptions, logstashYml));
    }

    /**
     * 执行初始化步骤
     *
     * @param logstashMachine LogstashMachine实例
     * @param machineInfo 目标机器
     * @param taskId 任务ID
     * @param step 步骤枚举
     * @param operationName 操作名称（用于日志和错误消息）
     * @param commandSupplier 命令提供者
     * @return 操作是否成功
     */
    private boolean executeStep(
            LogstashMachine logstashMachine,
            MachineInfo machineInfo,
            String taskId,
            LogstashMachineStep step,
            String operationName,
            CommandSupplier commandSupplier) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        taskService.updateStepStatus(taskId, logstashMachineId, step.getId(), StepStatus.RUNNING);

        logger.info(
                "{}在机器 [{}] LogstashMachine实例 [{}]", operationName, machineId, logstashMachineId);

        try {
            LogstashCommand command = commandSupplier.get();
            boolean success = command.execute(machineInfo);
            StepStatus status = success ? StepStatus.COMPLETED : StepStatus.FAILED;
            String errorMessage = success ? null : operationName + "失败";
            taskService.updateStepStatus(
                    taskId, logstashMachineId, step.getId(), status, errorMessage);

            if (!success) {
                throw new RuntimeException(operationName + "失败");
            }

            return true;
        } catch (Exception ex) {
            // 更新任务状态为失败，不记录详细日志（由外层处理）
            taskService.updateStepStatus(
                    taskId, logstashMachineId, step.getId(), StepStatus.FAILED, ex.getMessage());

            // 重新抛出异常，确保异常传递到外层
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public boolean canInitialize() {
        return true; // 允许重新初始化
    }

    @Override
    public LogstashMachineState getNextState(OperationType operationType, boolean success) {
        if (operationType == OperationType.INITIALIZE) {
            return success
                    ? LogstashMachineState.NOT_STARTED
                    : LogstashMachineState.INITIALIZE_FAILED;
        }
        return getState(); // 默认保持当前状态
    }

    @Override
    public boolean handleDelete(LogstashMachine logstashMachine, MachineInfo machineInfo) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info(
                "删除机器 [{}] 上的LogstashMachine实例 [{}] 目录 (初始化失败状态)", machineId, logstashMachineId);

        LogstashCommand deleteCommand =
                commandFactory.deleteProcessDirectoryCommand(logstashMachineId);

        try {
            boolean success = deleteCommand.execute(machineInfo);
            if (success) {
                logger.info(
                        "成功删除机器 [{}] 上的LogstashMachine实例 [{}] 目录", machineId, logstashMachineId);
            } else {
                logger.error(
                        "删除机器 [{}] 上的LogstashMachine实例 [{}] 目录失败", machineId, logstashMachineId);
            }
            return success;
        } catch (Exception ex) {
            logger.error(
                    "删除机器 [{}] 上的LogstashMachine实例 [{}] 目录时发生异常: {}",
                    machineId,
                    logstashMachineId,
                    ex.getMessage(),
                    ex);
            return false;
        }
    }

    @Override
    public boolean canDelete() {
        return true;
    }

    /** 命令提供者函数式接口 */
    @FunctionalInterface
    private interface CommandSupplier {
        LogstashCommand get();
    }
}
