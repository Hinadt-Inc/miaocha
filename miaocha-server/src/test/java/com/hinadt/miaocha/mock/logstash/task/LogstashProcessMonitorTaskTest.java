package com.hinadt.miaocha.mock.logstash.task;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.reset;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.task.LogstashProcessMonitorTask;
import com.hinadt.miaocha.common.exception.SshException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.infrastructure.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.infrastructure.mapper.LogstashProcessMapper;
import com.hinadt.miaocha.infrastructure.mapper.MachineMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LogstashProcessMonitorTaskTest {

    @Mock private LogstashMachineMapper logstashMachineMapper;

    @Mock private LogstashProcessMapper logstashProcessMapper;

    @Mock private MachineMapper machineMapper;

    @Mock private SshClient sshClient;

    private LogstashProcessMonitorTask monitorTask;

    private LogstashMachine logstashMachine;
    private LogstashProcess logstashProcess;
    private MachineInfo machineInfo;

    @BeforeEach
    void setUp() {
        reset(logstashMachineMapper, logstashProcessMapper, machineMapper, sshClient);
        // 手动创建LogstashProcessMonitorTask，注入所有mock对象
        monitorTask =
                new LogstashProcessMonitorTask(
                        logstashMachineMapper, logstashProcessMapper, machineMapper, sshClient);

        // 创建测试数据 - 基于新的logstashMachine架构
        logstashMachine = new LogstashMachine();
        logstashMachine.setId(1L); // logstashMachine的ID
        logstashMachine.setLogstashProcessId(100L);
        logstashMachine.setMachineId(200L);
        logstashMachine.setProcessPid("12345");
        logstashMachine.setState(LogstashMachineState.RUNNING.name());
        logstashMachine.setDeployPath("/opt/logstash/instance-1");

        logstashProcess = new LogstashProcess();
        logstashProcess.setId(100L);
        logstashProcess.setName("test-logstash");
        logstashProcess.setUpdateTime(LocalDateTime.now().minusMinutes(10)); // 已运行10分钟

        machineInfo = new MachineInfo();
        machineInfo.setId(200L);
        machineInfo.setIp("192.168.1.100");
        machineInfo.setPort(22);
        machineInfo.setUsername("user");
        machineInfo.setPassword("password");
    }

    @Test
    void testMonitorLogstashProcesses_NoRunningProcesses() {
        // 模拟没有运行中的LogstashMachine实例
        when(logstashMachineMapper.selectAllWithProcessPid()).thenReturn(Collections.emptyList());

        // 执行监控任务
        monitorTask.monitorLogstashProcesses();

        // 验证不会调用其他方法
        verify(logstashMachineMapper).selectAllWithProcessPid();
        verifyNoMoreInteractions(logstashProcessMapper, machineMapper, sshClient);
    }

    @Test
    void testMonitorLogstashProcesses_ProcessStillRunning() throws SshException {
        // 模拟有运行中的LogstashMachine实例
        when(logstashMachineMapper.selectAllWithProcessPid()).thenReturn(List.of(logstashMachine));
        when(logstashProcessMapper.selectById(anyLong())).thenReturn(logstashProcess);
        when(machineMapper.selectById(anyLong())).thenReturn(machineInfo);

        // 模拟进程仍在运行
        when(sshClient.executeCommand(any(MachineInfo.class), anyString())).thenReturn("12345");

        // 执行监控任务
        monitorTask.monitorLogstashProcesses();

        // 验证调用的方法和参数
        verify(logstashMachineMapper).selectAllWithProcessPid();
        verify(logstashProcessMapper).selectById(eq(100L));
        verify(machineMapper).selectById(eq(200L));
        verify(sshClient).executeCommand(eq(machineInfo), contains("ps -p 12345"));

        // 确认状态没有被更新（因为进程仍在运行）
        verify(logstashMachineMapper, never()).updateStateById(anyLong(), anyString());
        verify(logstashMachineMapper, never()).updateProcessPidById(anyLong(), anyString());
    }

    @Test
    void testMonitorLogstashProcesses_ProcessDied() throws SshException {
        // 模拟有运行中的LogstashMachine实例
        when(logstashMachineMapper.selectAllWithProcessPid()).thenReturn(List.of(logstashMachine));
        when(logstashProcessMapper.selectById(anyLong())).thenReturn(logstashProcess);
        when(machineMapper.selectById(anyLong())).thenReturn(machineInfo);
        when(logstashMachineMapper.updateStateById(anyLong(), anyString())).thenReturn(1);
        when(logstashMachineMapper.updateProcessPidById(anyLong(), isNull())).thenReturn(1);

        // 模拟进程已死亡
        when(sshClient.executeCommand(any(MachineInfo.class), anyString()))
                .thenReturn("Process not found");

        // 执行监控任务
        monitorTask.monitorLogstashProcesses();

        // 验证调用的方法和参数
        verify(logstashMachineMapper).selectAllWithProcessPid();
        verify(logstashProcessMapper).selectById(eq(100L));
        verify(machineMapper).selectById(eq(200L));
        verify(sshClient).executeCommand(eq(machineInfo), contains("ps -p 12345"));

        // 确认状态被更新 - 使用新的基于logstashMachineId的方法
        verify(logstashMachineMapper)
                .updateStateById(eq(1L), eq(LogstashMachineState.NOT_STARTED.name()));
        verify(logstashMachineMapper).updateProcessPidById(eq(1L), isNull());
    }

    @Test
    void testMonitorLogstashProcesses_SshError() throws SshException {
        // 模拟有运行中的LogstashMachine实例
        when(logstashMachineMapper.selectAllWithProcessPid()).thenReturn(List.of(logstashMachine));
        when(logstashProcessMapper.selectById(anyLong())).thenReturn(logstashProcess);
        when(machineMapper.selectById(anyLong())).thenReturn(machineInfo);

        // 模拟SSH连接错误
        when(sshClient.executeCommand(any(MachineInfo.class), anyString()))
                .thenThrow(new SshException("Connection failed"));

        // 执行监控任务
        monitorTask.monitorLogstashProcesses();

        // 验证调用的方法和参数
        verify(logstashMachineMapper).selectAllWithProcessPid();
        verify(logstashProcessMapper).selectById(eq(100L));
        verify(machineMapper).selectById(eq(200L));
        verify(sshClient).executeCommand(eq(machineInfo), contains("ps -p 12345"));

        // 确认状态没有被更新 (安全起见，SSH错误时不更改状态)
        verify(logstashMachineMapper, never()).updateStateById(anyLong(), anyString());
        verify(logstashMachineMapper, never()).updateProcessPidById(anyLong(), anyString());
    }

    @Test
    void testMonitorLogstashProcesses_MachineNotRunning() {
        // 模拟LogstashMachine实例状态不是运行状态
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
        verify(logstashMachineMapper, never()).updateStateById(anyLong(), anyString());
        verify(logstashMachineMapper, never()).updateProcessPidById(anyLong(), anyString());
    }

    @Test
    void testMonitorLogstashProcesses_ProcessTooNew() {
        // 模拟LogstashMachine实例对应的进程运行时间不足5分钟
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
        verify(logstashMachineMapper, never()).updateStateById(anyLong(), anyString());
        verify(logstashMachineMapper, never()).updateProcessPidById(anyLong(), anyString());
    }

    @Test
    void testMonitorLogstashProcesses_NoUpdateTime() {
        // 模拟LogstashMachine实例对应的进程没有更新时间
        when(logstashMachineMapper.selectAllWithProcessPid()).thenReturn(List.of(logstashMachine));

        // 设置进程更新时间为null
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
        verify(logstashMachineMapper, never()).updateStateById(anyLong(), anyString());
        verify(logstashMachineMapper, never()).updateProcessPidById(anyLong(), anyString());
    }

    @Test
    void testMonitorLogstashProcesses_MultipleInstances() throws SshException {
        // 模拟同一台机器上的多个LogstashMachine实例
        LogstashMachine logstashMachine2 = new LogstashMachine();
        logstashMachine2.setId(2L);
        logstashMachine2.setLogstashProcessId(100L);
        logstashMachine2.setMachineId(200L); // 同一台机器
        logstashMachine2.setProcessPid("67890");
        logstashMachine2.setState(LogstashMachineState.RUNNING.name());
        logstashMachine2.setDeployPath("/opt/logstash/instance-2"); // 不同部署路径

        when(logstashMachineMapper.selectAllWithProcessPid())
                .thenReturn(List.of(logstashMachine, logstashMachine2));
        when(logstashProcessMapper.selectById(anyLong())).thenReturn(logstashProcess);
        when(machineMapper.selectById(anyLong())).thenReturn(machineInfo);

        // 第一个实例仍在运行，第二个实例已死亡
        when(sshClient.executeCommand(eq(machineInfo), contains("ps -p 12345")))
                .thenReturn("12345");
        when(sshClient.executeCommand(eq(machineInfo), contains("ps -p 67890")))
                .thenReturn("Process not found");

        when(logstashMachineMapper.updateStateById(eq(2L), anyString())).thenReturn(1);
        when(logstashMachineMapper.updateProcessPidById(eq(2L), isNull())).thenReturn(1);

        // 执行监控任务
        monitorTask.monitorLogstashProcesses();

        // 验证每个实例都被检查
        verify(sshClient).executeCommand(eq(machineInfo), contains("ps -p 12345"));
        verify(sshClient).executeCommand(eq(machineInfo), contains("ps -p 67890"));

        // 确认只有死亡的实例状态被更新
        verify(logstashMachineMapper, never()).updateStateById(eq(1L), anyString());
        verify(logstashMachineMapper)
                .updateStateById(eq(2L), eq(LogstashMachineState.NOT_STARTED.name()));
        verify(logstashMachineMapper).updateProcessPidById(eq(2L), isNull());
    }
}
