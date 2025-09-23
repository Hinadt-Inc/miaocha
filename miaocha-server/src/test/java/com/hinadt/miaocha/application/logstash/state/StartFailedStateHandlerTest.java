package com.hinadt.miaocha.application.logstash.state;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hinadt.miaocha.application.logstash.command.LogstashCommand;
import com.hinadt.miaocha.application.logstash.command.LogstashCommandFactory;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StartFailedStateHandlerTest {

    @Mock private TaskService taskService;
    @Mock private LogstashCommandFactory commandFactory;
    @Mock private LogstashCommand deleteCommand;

    private StartFailedStateHandler handler;

    @BeforeEach
    void setUp() {
        handler = new StartFailedStateHandler(taskService, commandFactory);
    }

    @Test
    @DisplayName("handleDelete should remove directory when command succeeds")
    void handleDeleteShouldRemoveDirectory() {
        LogstashMachine logstashMachine = new LogstashMachine();
        logstashMachine.setId(10L);
        MachineInfo machineInfo = new MachineInfo();
        machineInfo.setId(20L);

        when(commandFactory.deleteProcessDirectoryCommand(10L)).thenReturn(deleteCommand);
        when(deleteCommand.execute(machineInfo)).thenReturn(true);

        boolean result = handler.handleDelete(logstashMachine, machineInfo);

        assertTrue(result);
        verify(commandFactory).deleteProcessDirectoryCommand(10L);
        verify(deleteCommand).execute(machineInfo);
    }

    @Test
    @DisplayName("handleDelete should return false when command reports failure")
    void handleDeleteShouldReturnFalseWhenCommandFails() {
        LogstashMachine logstashMachine = new LogstashMachine();
        logstashMachine.setId(11L);
        MachineInfo machineInfo = new MachineInfo();
        machineInfo.setId(21L);

        when(commandFactory.deleteProcessDirectoryCommand(11L)).thenReturn(deleteCommand);
        when(deleteCommand.execute(machineInfo)).thenReturn(false);

        boolean result = handler.handleDelete(logstashMachine, machineInfo);

        assertFalse(result);
        verify(commandFactory).deleteProcessDirectoryCommand(11L);
        verify(deleteCommand).execute(machineInfo);
    }

    @Test
    @DisplayName("canDelete should return true in start failed state")
    void canDeleteShouldReturnTrue() {
        assertTrue(handler.canDelete());
    }
}
