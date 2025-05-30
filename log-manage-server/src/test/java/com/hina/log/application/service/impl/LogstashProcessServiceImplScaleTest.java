package com.hina.log.application.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hina.log.application.logstash.LogstashConfigSyncService;
import com.hina.log.application.logstash.LogstashMachineConnectionValidator;
import com.hina.log.application.logstash.LogstashProcessDeployService;
import com.hina.log.application.logstash.command.LogstashCommandFactory;
import com.hina.log.application.logstash.enums.LogstashMachineState;
import com.hina.log.application.logstash.parser.LogstashConfigParser;
import com.hina.log.application.logstash.task.TaskService;
import com.hina.log.application.service.TableValidationService;
import com.hina.log.application.service.sql.JdbcQueryExecutor;
import com.hina.log.common.exception.BusinessException;
import com.hina.log.common.exception.ErrorCode;
import com.hina.log.domain.converter.LogstashMachineConverter;
import com.hina.log.domain.converter.LogstashProcessConverter;
import com.hina.log.domain.dto.logstash.LogstashProcessResponseDTO;
import com.hina.log.domain.dto.logstash.LogstashProcessScaleRequestDTO;
import com.hina.log.domain.entity.LogstashMachine;
import com.hina.log.domain.entity.LogstashProcess;
import com.hina.log.domain.entity.MachineInfo;
import com.hina.log.domain.mapper.DatasourceMapper;
import com.hina.log.domain.mapper.LogstashMachineMapper;
import com.hina.log.domain.mapper.LogstashProcessMapper;
import com.hina.log.domain.mapper.MachineMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LogstashProcessService 扩容缩容测试")
class LogstashProcessServiceImplScaleTest {

    @Mock private LogstashProcessMapper logstashProcessMapper;
    @Mock private LogstashMachineMapper logstashMachineMapper;
    @Mock private MachineMapper machineMapper;
    @Mock private DatasourceMapper datasourceMapper;
    @Mock private LogstashProcessDeployService logstashDeployService;
    @Mock private LogstashProcessConverter logstashProcessConverter;
    @Mock private LogstashMachineConverter logstashMachineConverter;
    @Mock private LogstashConfigParser logstashConfigParser;
    @Mock private TableValidationService tableValidationService;
    @Mock private JdbcQueryExecutor jdbcQueryExecutor;
    @Mock private TaskService taskService;
    @Mock private LogstashConfigSyncService configSyncService;
    @Mock private LogstashMachineConnectionValidator connectionValidator;
    @Mock private LogstashCommandFactory commandFactory;

    private LogstashProcessServiceImpl logstashProcessService;

    @BeforeEach
    void setUp() {
        logstashProcessService =
                new LogstashProcessServiceImpl(
                        logstashProcessMapper,
                        logstashMachineMapper,
                        machineMapper,
                        datasourceMapper,
                        logstashDeployService,
                        logstashProcessConverter,
                        logstashMachineConverter,
                        logstashConfigParser,
                        tableValidationService,
                        jdbcQueryExecutor,
                        taskService,
                        configSyncService,
                        connectionValidator,
                        commandFactory);
    }

    @Test
    @DisplayName("扩容操作 - 成功添加机器")
    void testScaleOut_Success() {
        // 准备测试数据
        Long processId = 1L;
        LogstashProcess process = createTestProcess(processId);

        LogstashProcessScaleRequestDTO dto = new LogstashProcessScaleRequestDTO();
        dto.setAddMachineIds(Arrays.asList(3L, 4L));
        dto.setCustomDeployPath("/custom/deploy/path");

        MachineInfo machine3 = createTestMachine(3L, "machine3", "192.168.1.3");
        MachineInfo machine4 = createTestMachine(4L, "machine4", "192.168.1.4");

        LogstashMachine logstashMachine3 = createTestLogstashMachine(1L, processId, 3L);
        LogstashMachine logstashMachine4 = createTestLogstashMachine(2L, processId, 4L);

        LogstashProcessResponseDTO responseDTO = new LogstashProcessResponseDTO();
        responseDTO.setId(processId);

        // Mock 方法调用
        when(logstashProcessMapper.selectById(processId)).thenReturn(process);
        when(machineMapper.selectById(3L)).thenReturn(machine3);
        when(machineMapper.selectById(4L)).thenReturn(machine4);
        when(logstashMachineMapper.selectByLogstashProcessIdAndMachineId(processId, 3L))
                .thenReturn(null);
        when(logstashMachineMapper.selectByLogstashProcessIdAndMachineId(processId, 4L))
                .thenReturn(null);
        when(logstashDeployService.getDeployBaseDir()).thenReturn("/default/deploy/path");
        when(logstashMachineConverter.createFromProcess(
                        eq(process), eq(3L), eq("/custom/deploy/path")))
                .thenReturn(logstashMachine3);
        when(logstashMachineConverter.createFromProcess(
                        eq(process), eq(4L), eq("/custom/deploy/path")))
                .thenReturn(logstashMachine4);
        when(logstashProcessConverter.toResponseDTO(process)).thenReturn(responseDTO);

        // 执行测试
        LogstashProcessResponseDTO result =
                logstashProcessService.scaleLogstashProcess(processId, dto);

        // 验证结果
        assertNotNull(result);
        assertEquals(processId, result.getId());

        // 验证方法调用
        verify(machineMapper, times(2)).selectById(anyLong());
        verify(logstashMachineMapper, times(2)).insert(any(LogstashMachine.class));
        verify(logstashDeployService).initializeProcess(eq(process), anyList());
        verify(connectionValidator, times(2))
                .validateSingleMachineConnection(any(MachineInfo.class));
    }

    @Test
    @DisplayName("扩容操作 - 机器已存在关联")
    void testScaleOut_MachineAlreadyAssociated() {
        // 准备测试数据
        Long processId = 1L;
        LogstashProcess process = createTestProcess(processId);

        LogstashProcessScaleRequestDTO dto = new LogstashProcessScaleRequestDTO();
        dto.setAddMachineIds(Arrays.asList(2L));

        MachineInfo machine2 = createTestMachine(2L, "machine2", "192.168.1.2");
        LogstashMachine existingRelation = createTestLogstashMachine(1L, processId, 2L);

        // Mock 方法调用
        when(logstashProcessMapper.selectById(processId)).thenReturn(process);
        when(machineMapper.selectById(2L)).thenReturn(machine2);
        when(logstashMachineMapper.selectByLogstashProcessIdAndMachineId(processId, 2L))
                .thenReturn(existingRelation);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            logstashProcessService.scaleLogstashProcess(processId, dto);
                        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("已经与进程"));
    }

    @Test
    @DisplayName("缩容操作 - 成功移除机器")
    void testScaleIn_Success() {
        // 准备测试数据
        Long processId = 1L;
        LogstashProcess process = createTestProcess(processId);

        LogstashProcessScaleRequestDTO dto = new LogstashProcessScaleRequestDTO();
        dto.setRemoveMachineIds(Arrays.asList(2L));
        dto.setForceScale(false);

        MachineInfo machine2 = createTestMachine(2L, "machine2", "192.168.1.2");
        LogstashMachine relation2 = createTestLogstashMachine(2L, processId, 2L);
        relation2.setState(LogstashMachineState.NOT_STARTED.name());

        // 模拟当前有3台机器，缩容1台后还剩2台
        List<LogstashMachine> allRelations =
                Arrays.asList(
                        createTestLogstashMachine(1L, processId, 1L),
                        relation2,
                        createTestLogstashMachine(3L, processId, 3L));

        LogstashProcessResponseDTO responseDTO = new LogstashProcessResponseDTO();
        responseDTO.setId(processId);

        // Mock 方法调用
        when(logstashProcessMapper.selectById(processId)).thenReturn(process);
        when(logstashMachineMapper.selectByLogstashProcessId(processId)).thenReturn(allRelations);
        when(machineMapper.selectById(2L)).thenReturn(machine2);
        when(logstashMachineMapper.selectByLogstashProcessIdAndMachineId(processId, 2L))
                .thenReturn(relation2);
        when(logstashDeployService.deleteProcessDirectory(eq(processId), anyList()))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(taskService.getAllMachineTaskIds(processId, 2L))
                .thenReturn(Arrays.asList("task1", "task2"));
        when(logstashProcessConverter.toResponseDTO(process)).thenReturn(responseDTO);

        // 执行测试
        LogstashProcessResponseDTO result =
                logstashProcessService.scaleLogstashProcess(processId, dto);

        // 验证结果
        assertNotNull(result);
        assertEquals(processId, result.getId());

        // 验证方法调用
        verify(logstashMachineMapper).deleteById(relation2.getId());
        verify(logstashDeployService).deleteProcessDirectory(eq(processId), anyList());
        verify(taskService).getAllMachineTaskIds(processId, 2L);
        verify(taskService, times(2)).deleteTaskSteps(anyString());
        verify(taskService, times(2)).deleteTask(anyString());
    }

    @Test
    @DisplayName("缩容操作 - 无法缩容到0台机器")
    void testScaleIn_CannotScaleToZero() {
        // 准备测试数据
        Long processId = 1L;
        LogstashProcess process = createTestProcess(processId);

        LogstashProcessScaleRequestDTO dto = new LogstashProcessScaleRequestDTO();
        dto.setRemoveMachineIds(Arrays.asList(1L, 2L)); // 尝试移除所有机器

        // 模拟当前只有2台机器
        List<LogstashMachine> allRelations =
                Arrays.asList(
                        createTestLogstashMachine(1L, processId, 1L),
                        createTestLogstashMachine(2L, processId, 2L));

        // Mock 方法调用
        when(logstashProcessMapper.selectById(processId)).thenReturn(process);
        when(logstashMachineMapper.selectByLogstashProcessId(processId)).thenReturn(allRelations);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            logstashProcessService.scaleLogstashProcess(processId, dto);
                        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("至少保留一台机器"));
    }

    @Test
    @DisplayName("缩容操作 - 运行中机器非强制模式拒绝")
    void testScaleIn_RunningMachineNotForced() {
        // 准备测试数据
        Long processId = 1L;
        LogstashProcess process = createTestProcess(processId);

        LogstashProcessScaleRequestDTO dto = new LogstashProcessScaleRequestDTO();
        dto.setRemoveMachineIds(Arrays.asList(2L));
        dto.setForceScale(false);

        MachineInfo machine2 = createTestMachine(2L, "machine2", "192.168.1.2");
        LogstashMachine relation2 = createTestLogstashMachine(2L, processId, 2L);
        relation2.setState(LogstashMachineState.RUNNING.name()); // 设置为运行状态

        // 模拟当前有3台机器
        List<LogstashMachine> allRelations =
                Arrays.asList(
                        createTestLogstashMachine(1L, processId, 1L),
                        relation2,
                        createTestLogstashMachine(3L, processId, 3L));

        // Mock 方法调用
        when(logstashProcessMapper.selectById(processId)).thenReturn(process);
        when(logstashMachineMapper.selectByLogstashProcessId(processId)).thenReturn(allRelations);
        when(machineMapper.selectById(2L)).thenReturn(machine2);
        when(logstashMachineMapper.selectByLogstashProcessIdAndMachineId(processId, 2L))
                .thenReturn(relation2);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            logstashProcessService.scaleLogstashProcess(processId, dto);
                        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("不能在非强制模式下缩容运行中的机器"));
    }

    @Test
    @DisplayName("缩容操作 - 强制模式停止运行中机器")
    void testScaleIn_ForceStopRunningMachine() {
        // 准备测试数据
        Long processId = 1L;
        LogstashProcess process = createTestProcess(processId);

        LogstashProcessScaleRequestDTO dto = new LogstashProcessScaleRequestDTO();
        dto.setRemoveMachineIds(Arrays.asList(2L));
        dto.setForceScale(true);

        MachineInfo machine2 = createTestMachine(2L, "machine2", "192.168.1.2");
        LogstashMachine relation2 = createTestLogstashMachine(2L, processId, 2L);
        relation2.setState(LogstashMachineState.RUNNING.name());

        // 模拟当前有3台机器
        List<LogstashMachine> allRelations =
                Arrays.asList(
                        createTestLogstashMachine(1L, processId, 1L),
                        relation2,
                        createTestLogstashMachine(3L, processId, 3L));

        LogstashProcessResponseDTO responseDTO = new LogstashProcessResponseDTO();
        responseDTO.setId(processId);

        // Mock 方法调用
        when(logstashProcessMapper.selectById(processId)).thenReturn(process);
        when(logstashMachineMapper.selectByLogstashProcessId(processId)).thenReturn(allRelations);
        when(machineMapper.selectById(2L)).thenReturn(machine2);
        when(logstashMachineMapper.selectByLogstashProcessIdAndMachineId(processId, 2L))
                .thenReturn(relation2);
        when(logstashDeployService.deleteProcessDirectory(eq(processId), anyList()))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(taskService.getAllMachineTaskIds(processId, 2L)).thenReturn(Collections.emptyList());
        when(logstashProcessConverter.toResponseDTO(process)).thenReturn(responseDTO);

        // 执行测试
        LogstashProcessResponseDTO result =
                logstashProcessService.scaleLogstashProcess(processId, dto);

        // 验证结果
        assertNotNull(result);
        assertEquals(processId, result.getId());

        // 验证强制停止被调用
        verify(logstashDeployService).stopProcess(eq(processId), anyList());
        verify(logstashMachineMapper).deleteById(relation2.getId());
    }

    @Test
    @DisplayName("扩容缩容参数验证")
    void testScaleValidation() {
        final Long processId = 1L;

        // 测试空请求
        assertThrows(
                BusinessException.class,
                () -> {
                    logstashProcessService.scaleLogstashProcess(processId, null);
                });

        // 测试同时扩容和缩容
        LogstashProcessScaleRequestDTO dto = new LogstashProcessScaleRequestDTO();
        dto.setAddMachineIds(Arrays.asList(1L));
        dto.setRemoveMachineIds(Arrays.asList(2L));

        final LogstashProcessScaleRequestDTO finalDto1 = dto;
        assertThrows(
                BusinessException.class,
                () -> {
                    logstashProcessService.scaleLogstashProcess(processId, finalDto1);
                });

        // 测试既不扩容也不缩容
        LogstashProcessScaleRequestDTO dto2 = new LogstashProcessScaleRequestDTO();
        final LogstashProcessScaleRequestDTO finalDto2 = dto2;
        assertThrows(
                BusinessException.class,
                () -> {
                    logstashProcessService.scaleLogstashProcess(processId, finalDto2);
                });
    }

    // 辅助方法
    private LogstashProcess createTestProcess(Long id) {
        LogstashProcess process = new LogstashProcess();
        process.setId(id);
        process.setName("test-process");
        process.setModule("test-module");
        process.setConfigContent("input {} output {}");
        return process;
    }

    private MachineInfo createTestMachine(Long id, String name, String ip) {
        MachineInfo machine = new MachineInfo();
        machine.setId(id);
        machine.setName(name);
        machine.setIp(ip);
        machine.setPort(22);
        machine.setUsername("test");
        return machine;
    }

    private LogstashMachine createTestLogstashMachine(Long id, Long processId, Long machineId) {
        LogstashMachine logstashMachine = new LogstashMachine();
        logstashMachine.setId(id);
        logstashMachine.setLogstashProcessId(processId);
        logstashMachine.setMachineId(machineId);
        logstashMachine.setState(LogstashMachineState.NOT_STARTED.name());
        return logstashMachine;
    }
}
