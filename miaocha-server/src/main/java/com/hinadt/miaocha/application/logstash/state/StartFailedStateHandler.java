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

/** 启动失败状态处理器 */
@Component
public class StartFailedStateHandler extends AbstractLogstashMachineStateHandler {

    private static final Logger logger = LoggerFactory.getLogger(StartFailedStateHandler.class);

    public StartFailedStateHandler(TaskService taskService, LogstashCommandFactory commandFactory) {
        super(taskService, commandFactory);
    }

    @Override
    public LogstashMachineState getState() {
        return LogstashMachineState.START_FAILED;
    }

    @Override
    public boolean handleStart(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String taskId) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("重新启动机器 [{}] 上的LogstashMachine实例 [{}]", machineId, logstashMachineId);

        // 重置所有步骤的状态
        taskService.resetStepStatuses(taskId, StepStatus.PENDING);

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
    public boolean canStart() {
        return true; // 允许重新启动
    }

    @Override
    public boolean handleDelete(LogstashMachine logstashMachine, MachineInfo machineInfo) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("删除机器 [{}] 上的LogstashMachine实例 [{}] 目录 (启动失败状态)", machineId, logstashMachineId);

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
