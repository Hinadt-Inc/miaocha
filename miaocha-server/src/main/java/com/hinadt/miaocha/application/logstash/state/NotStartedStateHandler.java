package com.hinadt.miaocha.application.logstash.state;

import com.hinadt.miaocha.application.logstash.command.LogstashCommand;
import com.hinadt.miaocha.application.logstash.command.LogstashCommandFactory;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineStep;
import com.hinadt.miaocha.application.logstash.enums.StepStatus;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 未启动状态处理器 */
@Component
public class NotStartedStateHandler extends AbstractLogstashMachineStateHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotStartedStateHandler.class);

    public NotStartedStateHandler(TaskService taskService, LogstashCommandFactory commandFactory) {
        super(taskService, commandFactory);
    }

    @Override
    public LogstashMachineState getState() {
        return LogstashMachineState.NOT_STARTED;
    }

    @Override
    public boolean handleStart(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("启动机器 [{}] 上的LogstashMachine实例 [{}]", machineId, logstashMachineId);

        // 1. 启动Logstash进程
        if (!startProcess(logstashMachine, machineInfo, taskId)) {
            return false;
        }

        // 2. 验证进程状态
        return verifyProcess(logstashMachine, machineInfo, taskId);
    }

    /**
     * 启动Logstash进程
     *
     * @param logstashMachine Logstash实例
     * @param machineInfo 机器信息
     * @param taskId 任务ID
     * @return 操作是否成功
     */
    private boolean startProcess(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        taskService.updateStepStatus(
                taskId,
                logstashMachineId,
                LogstashMachineStep.START_PROCESS.getId(),
                StepStatus.RUNNING);

        LogstashCommand startCommand = commandFactory.startProcessCommand(logstashMachineId);

        try {
            boolean startSuccess = startCommand.execute(machineInfo);
            StepStatus status = startSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
            String errorMessage = startSuccess ? null : "启动Logstash进程失败";
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.START_PROCESS.getId(),
                    status,
                    errorMessage);

            if (!startSuccess) {
                throw new RuntimeException("启动Logstash进程失败");
            }

            return startSuccess;
        } catch (Exception ex) {
            // 更新任务状态为失败，不记录详细日志（由外层处理）
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.START_PROCESS.getId(),
                    StepStatus.FAILED,
                    ex.getMessage());

            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * 验证Logstash进程状态
     *
     * @param logstashMachine Logstash实例
     * @param machineInfo 机器信息
     * @param taskId 任务ID
     * @return 操作是否成功
     */
    private boolean verifyProcess(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        Long logstashMachineId = logstashMachine.getId();

        taskService.updateStepStatus(
                taskId,
                logstashMachineId,
                LogstashMachineStep.VERIFY_PROCESS.getId(),
                StepStatus.RUNNING);

        LogstashCommand verifyCommand = commandFactory.verifyProcessCommand(logstashMachineId);

        try {
            boolean verifySuccess = verifyCommand.execute(machineInfo);
            StepStatus status = verifySuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
            String errorMessage = verifySuccess ? null : "验证Logstash进程失败，进程可能未正常运行";
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.VERIFY_PROCESS.getId(),
                    status,
                    errorMessage);

            if (!verifySuccess) {
                throw new RuntimeException("验证Logstash进程失败，进程可能未正常运行");
            }

            return verifySuccess;
        } catch (Exception ex) {
            // 更新任务状态为失败，不记录详细日志（由外层处理）
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.VERIFY_PROCESS.getId(),
                    StepStatus.FAILED,
                    ex.getMessage());

            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean handleUpdateConfig(
            LogstashMachine logstashMachine,
            String configContent,
            String jvmOptions,
            String logstashYml,
            MachineInfo machineInfo,
            String taskId) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("更新机器 [{}] 上的LogstashMachine实例 [{}] 配置", machineId, logstashMachineId);

        // 更新相关步骤的状态为运行中
        updateStepStatusToRunning(logstashMachine, taskId, configContent, jvmOptions, logstashYml);

        // 创建一个命令来同时更新所有配置
        LogstashCommand updateCommand =
                commandFactory.updateConfigCommand(
                        logstashMachineId, configContent, jvmOptions, logstashYml);

        try {
            boolean success = updateCommand.execute(machineInfo);
            handleUpdateConfigResult(
                    logstashMachine, taskId, configContent, jvmOptions, logstashYml, success, null);
            return success;
        } catch (Exception ex) {
            handleUpdateConfigResult(
                    logstashMachine,
                    taskId,
                    configContent,
                    jvmOptions,
                    logstashYml,
                    false,
                    ex.getMessage());
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * 更新配置步骤状态为运行中
     *
     * @param logstashMachine Logstash实例
     * @param taskId 任务ID
     * @param configContent 主配置内容
     * @param jvmOptions JVM配置
     * @param logstashYml 系统配置
     */
    private void updateStepStatusToRunning(
            LogstashMachine logstashMachine,
            String taskId,
            String configContent,
            String jvmOptions,
            String logstashYml) {
        Long logstashMachineId = logstashMachine.getId();

        if (configContent != null) {
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.UPDATE_MAIN_CONFIG.getId(),
                    StepStatus.RUNNING);
        }
        if (jvmOptions != null) {
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.UPDATE_JVM_CONFIG.getId(),
                    StepStatus.RUNNING);
        }
        if (logstashYml != null) {
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.UPDATE_SYSTEM_CONFIG.getId(),
                    StepStatus.RUNNING);
        }
    }

    /**
     * 处理更新配置的结果
     *
     * @param logstashMachine Logstash实例
     * @param taskId 任务ID
     * @param configContent 主配置内容
     * @param jvmOptions JVM配置
     * @param logstashYml 系统配置
     * @param success 是否成功
     * @param errorMessage 错误信息
     */
    private void handleUpdateConfigResult(
            LogstashMachine logstashMachine,
            String taskId,
            String configContent,
            String jvmOptions,
            String logstashYml,
            boolean success,
            String errorMessage) {
        Long logstashMachineId = logstashMachine.getId();
        StepStatus status = success ? StepStatus.COMPLETED : StepStatus.FAILED;
        String message = success ? null : (errorMessage != null ? errorMessage : "配置更新失败");

        if (configContent != null) {
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.UPDATE_MAIN_CONFIG.getId(),
                    status,
                    message);
        }
        if (jvmOptions != null) {
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.UPDATE_JVM_CONFIG.getId(),
                    status,
                    message);
        }
        if (logstashYml != null) {
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.UPDATE_SYSTEM_CONFIG.getId(),
                    status,
                    message);
        }

        if (!success && errorMessage == null) {
            throw new RuntimeException("配置更新失败");
        }
    }

    @Override
    public boolean handleRefreshConfig(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("刷新机器 [{}] 上的LogstashMachine实例 [{}] 配置", machineId, logstashMachineId);

        taskService.updateStepStatus(
                taskId,
                logstashMachineId,
                LogstashMachineStep.REFRESH_CONFIG.getId(),
                StepStatus.RUNNING);

        // 使用支持所有配置类型的刷新命令
        // 传递null将从数据库中获取最新配置
        LogstashCommand refreshConfigCommand =
                commandFactory.refreshConfigCommand(logstashMachineId);

        try {
            boolean success = refreshConfigCommand.execute(machineInfo);
            StepStatus status = success ? StepStatus.COMPLETED : StepStatus.FAILED;
            String errorMessage = success ? null : "刷新配置失败";
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.REFRESH_CONFIG.getId(),
                    status,
                    errorMessage);

            if (!success) {
                throw new RuntimeException("刷新配置失败");
            }

            return success;
        } catch (Exception ex) {
            // 更新任务状态为失败，不记录详细日志（由外层处理）
            taskService.updateStepStatus(
                    taskId,
                    logstashMachineId,
                    LogstashMachineStep.REFRESH_CONFIG.getId(),
                    StepStatus.FAILED,
                    ex.getMessage());

            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean handleDelete(LogstashMachine logstashMachine, MachineInfo machineInfo) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("删除机器 [{}] 上的LogstashMachine实例 [{}] 目录", machineId, logstashMachineId);

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
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean canStart() {
        return true;
    }

    @Override
    public boolean canUpdateConfig() {
        return true;
    }

    @Override
    public boolean canRefreshConfig() {
        return true;
    }

    @Override
    public boolean canDelete() {
        return true;
    }

    @Override
    public LogstashMachineState getNextState(OperationType operationType, boolean success) {
        if (operationType == OperationType.START) {
            return success ? LogstashMachineState.RUNNING : LogstashMachineState.START_FAILED;
        }
        return getState(); // 默认保持当前状态
    }
}
