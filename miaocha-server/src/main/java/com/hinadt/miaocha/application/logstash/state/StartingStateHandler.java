package com.hinadt.miaocha.application.logstash.state;

import com.hinadt.miaocha.application.logstash.command.LogstashCommandFactory;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import org.springframework.stereotype.Component;

/** 启动中状态处理器 */
@Component
public class StartingStateHandler extends AbstractLogstashMachineStateHandler {

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
}
