package com.hina.log.logstash.task;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hina.log.application.logstash.enums.LogstashMachineState;
import com.hina.log.application.logstash.task.LogstashProcessMonitorTask;
import com.hina.log.common.exception.SshException;
import com.hina.log.common.ssh.SshClient;
import com.hina.log.domain.entity.LogstashMachine;
import com.hina.log.domain.entity.LogstashProcess;
import com.hina.log.domain.entity.Machine;
import com.hina.log.domain.mapper.LogstashMachineMapper;
import com.hina.log.domain.mapper.LogstashProcessMapper;
import com.hina.log.domain.mapper.MachineMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LogstashProcessMonitorTaskTest {

    @Mock private LogstashMachineMapper logstashMachineMapper;

    @Mock private LogstashProcessMapper logstashProcessMapper;

    @Mock private MachineMapper machineMapper;

    @Mock private SshClient sshClient;

    @InjectMocks private LogstashProcessMonitorTask monitorTask;

    private LogstashMachine logstashMachine;
    private LogstashProcess logstashProcess;
    private Machine machine;

    @BeforeEach
    void setUp() {
        // 创建测试数据
        logstashMachine = new LogstashMachine();
        logstashMachine.setId(1L);
        logstashMachine.setLogstashProcessId(100L);
        logstashMachine.setMachineId(200L);
        logstashMachine.setProcessPid("12345");
        logstashMachine.setState(LogstashMachineState.RUNNING.name());

        logstashProcess = new LogstashProcess();
        logstashProcess.setId(100L);
        logstashProcess.setName("test-logstash");
        logstashProcess.setUpdateTime(LocalDateTime.now().minusMinutes(10)); // 已运行10分钟

        machine = new Machine();
        machine.setId(200L);
        machine.setIp("192.168.1.100");
        machine.setPort(22);
        machine.setUsername("user");
        machine.setPassword("password");
    }

    @Test
    void testMonitorLogstashProcesses_NoRunningProcesses() {
        // 模拟没有运行中的进程
        when(logstashMachineMapper.selectAllWithProcessPid()).thenReturn(Collections.emptyList());

        // 执行监控任务
        monitorTask.monitorLogstashProcesses();

        // 验证不会调用其他方法
        verify(logstashMachineMapper).selectAllWithProcessPid();
        verifyNoMoreInteractions(logstashProcessMapper, machineMapper, sshClient);
    }

    @Test
    void testMonitorLogstashProcesses_ProcessStillRunning() throws SshException {
        // 模拟有运行中的进程
        when(logstashMachineMapper.selectAllWithProcessPid()).thenReturn(List.of(logstashMachine));
        when(logstashProcessMapper.selectById(anyLong())).thenReturn(logstashProcess);
        when(machineMapper.selectById(anyLong())).thenReturn(machine);

        // 模拟进程仍在运行
        when(sshClient.executeCommand(any(Machine.class), anyString())).thenReturn("12345");

        // 执行监控任务
        monitorTask.monitorLogstashProcesses();

        // 验证调用的方法和参数
        verify(logstashMachineMapper).selectAllWithProcessPid();
        verify(logstashProcessMapper).selectById(eq(100L));
        verify(machineMapper).selectById(eq(200L));
        verify(sshClient).executeCommand(eq(machine), contains("ps -p 12345"));

        // 确认状态没有被更新
        verify(logstashMachineMapper, never()).updateState(anyLong(), anyLong(), anyString());
        verify(logstashMachineMapper, never()).updateProcessPid(anyLong(), anyLong(), anyString());
    }

    @Test
    void testMonitorLogstashProcesses_ProcessDied() throws SshException {
        // 模拟有运行中的进程
        when(logstashMachineMapper.selectAllWithProcessPid()).thenReturn(List.of(logstashMachine));
        when(logstashProcessMapper.selectById(anyLong())).thenReturn(logstashProcess);
        when(machineMapper.selectById(anyLong())).thenReturn(machine);
        when(logstashMachineMapper.updateState(anyLong(), anyLong(), anyString())).thenReturn(1);
        when(logstashMachineMapper.updateProcessPid(anyLong(), anyLong(), isNull())).thenReturn(1);

        // 模拟进程已死亡
        when(sshClient.executeCommand(any(Machine.class), anyString()))
                .thenReturn("Process not found");

        // 执行监控任务
        monitorTask.monitorLogstashProcesses();

        // 验证调用的方法和参数
        verify(logstashMachineMapper).selectAllWithProcessPid();
        verify(logstashProcessMapper).selectById(eq(100L));
        verify(machineMapper).selectById(eq(200L));
        verify(sshClient).executeCommand(eq(machine), contains("ps -p 12345"));

        // 确认状态被更新
        verify(logstashMachineMapper)
                .updateState(eq(100L), eq(200L), eq(LogstashMachineState.NOT_STARTED.name()));
        verify(logstashMachineMapper).updateProcessPid(eq(100L), eq(200L), isNull());
    }

    @Test
    void testMonitorLogstashProcesses_SshError() throws SshException {
        // 模拟有运行中的进程
        when(logstashMachineMapper.selectAllWithProcessPid()).thenReturn(List.of(logstashMachine));
        when(logstashProcessMapper.selectById(anyLong())).thenReturn(logstashProcess);
        when(machineMapper.selectById(anyLong())).thenReturn(machine);

        // 模拟SSH连接错误
        when(sshClient.executeCommand(any(Machine.class), anyString()))
                .thenThrow(new SshException("Connection failed"));

        // 执行监控任务
        monitorTask.monitorLogstashProcesses();

        // 验证调用的方法和参数
        verify(logstashMachineMapper).selectAllWithProcessPid();
        verify(logstashProcessMapper).selectById(eq(100L));
        verify(machineMapper).selectById(eq(200L));
        verify(sshClient).executeCommand(eq(machine), contains("ps -p 12345"));

        // 确认状态没有被更新 (安全起见，SSH错误时不更改状态)
        verify(logstashMachineMapper, never()).updateState(anyLong(), anyLong(), anyString());
        verify(logstashMachineMapper, never()).updateProcessPid(anyLong(), anyLong(), anyString());
    }

    @Test
    void testMonitorLogstashProcesses_MachineNotRunning() {
        // 模拟有记录但机器状态不是运行状态
        logstashMachine.setState(LogstashMachineState.NOT_STARTED.name());
        when(logstashMachineMapper.selectAllWithProcessPid()).thenReturn(List.of(logstashMachine));
        when(logstashProcessMapper.selectById(anyLong())).thenReturn(logstashProcess);

        // 执行监控任务
        monitorTask.monitorLogstashProcesses();

        // 验证不会调用SSH检查
        verify(logstashMachineMapper).selectAllWithProcessPid();
        verify(logstashProcessMapper).selectById(eq(100L));
        verifyNoInteractions(sshClient);

        // 确认状态没有被更新
        verify(logstashMachineMapper, never()).updateState(anyLong(), anyLong(), anyString());
        verify(logstashMachineMapper, never()).updateProcessPid(anyLong(), anyLong(), anyString());
    }

    @Test
    void testMonitorLogstashProcesses_ProcessTooNew() {
        // 模拟有记录但进程运行时间不足5分钟
        when(logstashMachineMapper.selectAllWithProcessPid()).thenReturn(List.of(logstashMachine));

        // 设置进程最后更新时间为3分钟前，不满足最小运行时间要求
        logstashProcess.setUpdateTime(LocalDateTime.now().minusMinutes(3));
        when(logstashProcessMapper.selectById(anyLong())).thenReturn(logstashProcess);

        // 执行监控任务
        monitorTask.monitorLogstashProcesses();

        // 验证不会调用SSH检查
        verify(logstashMachineMapper).selectAllWithProcessPid();
        verify(logstashProcessMapper).selectById(eq(100L));
        verifyNoInteractions(sshClient);
        verifyNoInteractions(machineMapper);

        // 确认状态没有被更新
        verify(logstashMachineMapper, never()).updateState(anyLong(), anyLong(), anyString());
        verify(logstashMachineMapper, never()).updateProcessPid(anyLong(), anyLong(), anyString());
    }

    @Test
    void testMonitorLogstashProcesses_NoUpdateTime() {
        // 模拟有记录但进程没有更新时间
        when(logstashMachineMapper.selectAllWithProcessPid()).thenReturn(List.of(logstashMachine));

        // 设置进程没有更新时间
        logstashProcess.setUpdateTime(null);
        when(logstashProcessMapper.selectById(anyLong())).thenReturn(logstashProcess);

        // 执行监控任务
        monitorTask.monitorLogstashProcesses();

        // 验证不会调用SSH检查
        verify(logstashMachineMapper).selectAllWithProcessPid();
        verify(logstashProcessMapper).selectById(eq(100L));
        verifyNoInteractions(sshClient);
        verifyNoInteractions(machineMapper);

        // 确认状态没有被更新
        verify(logstashMachineMapper, never()).updateState(anyLong(), anyLong(), anyString());
        verify(logstashMachineMapper, never()).updateProcessPid(anyLong(), anyLong(), anyString());
    }
}
