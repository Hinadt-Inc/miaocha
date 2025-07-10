package com.hinadt.miaocha.application.logstash.state;

import com.hinadt.miaocha.application.logstash.command.LogstashCommand;
import com.hinadt.miaocha.application.logstash.command.LogstashCommandFactory;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 启动中状态处理器 */
@Component
public class StartingStateHandler extends AbstractLogstashMachineStateHandler {

    private static final Logger logger = LoggerFactory.getLogger(StartingStateHandler.class);

    public StartingStateHandler(TaskService taskService, LogstashCommandFactory commandFactory) {
        super(taskService, commandFactory);
    }

    @Override
    public LogstashMachineState getState() {
        return LogstashMachineState.STARTING;
    }

    @Override
    public LogstashMachineState getNextState(OperationType operationType, boolean success) {
        // 启动中状态通常是由其他操作触发的，这里只是一个过渡状态
        // 实际状态转换应该由触发操作的处理器决定
        return getState();
    }

    @Override
    public boolean handleDelete(LogstashMachine logstashMachine, MachineInfo machineInfo) {
        Long logstashMachineId = logstashMachine.getId();
        Long machineId = machineInfo.getId();

        logger.info("删除机器 [{}] 上的LogstashMachine实例 [{}] 目录 (启动中状态)", machineId, logstashMachineId);

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
}
