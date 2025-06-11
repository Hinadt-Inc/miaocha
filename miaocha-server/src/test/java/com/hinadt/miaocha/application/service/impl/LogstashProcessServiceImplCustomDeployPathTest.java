package com.hinadt.miaocha.application.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.logstash.LogstashConfigSyncService;
import com.hinadt.miaocha.application.logstash.LogstashMachineConnectionValidator;
import com.hinadt.miaocha.application.logstash.LogstashProcessDeployService;
import com.hinadt.miaocha.application.logstash.command.LogstashCommandFactory;
import com.hinadt.miaocha.application.logstash.parser.LogstashConfigParser;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.domain.converter.LogstashMachineConverter;
import com.hinadt.miaocha.domain.converter.LogstashProcessConverter;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessCreateDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessResponseDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.domain.mapper.LogstashProcessMapper;
import com.hinadt.miaocha.domain.mapper.MachineMapper;
import io.qameta.allure.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * LogstashProcessService 自定义部署路径测试
 *
 * <p>测试秒查系统中Logstash进程自定义部署路径功能 验证用户可以为不同的进程指定特定的部署路径
 */
@Epic("秒查日志管理系统")
@Feature("Logstash进程管理")
@Story("自定义部署路径")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LogstashProcessService 自定义部署路径测试")
@Owner("开发团队")
class LogstashProcessServiceImplCustomDeployPathTest {

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
    void testCreateLogstashProcessWithCustomDeployPath() {
        // 准备测试数据
        LogstashProcessCreateDTO createDTO = new LogstashProcessCreateDTO();
        createDTO.setName("Test Process");
        createDTO.setModule("test");
        createDTO.setCustomDeployPath("/custom/deploy/path");
        createDTO.setDatasourceId(1L);
        createDTO.setMachineIds(List.of(1L, 2L));

        LogstashProcess mockProcess = new LogstashProcess();
        mockProcess.setId(100L);
        mockProcess.setName("Test Process");
        mockProcess.setModule("test");

        MachineInfo mockMachine1 = new MachineInfo();
        mockMachine1.setId(1L);
        mockMachine1.setName("Machine1");

        MachineInfo mockMachine2 = new MachineInfo();
        mockMachine2.setId(2L);
        mockMachine2.setName("Machine2");

        LogstashMachine mockLogstashMachine1 = new LogstashMachine();
        mockLogstashMachine1.setLogstashProcessId(100L);
        mockLogstashMachine1.setMachineId(1L);
        mockLogstashMachine1.setDeployPath("/custom/deploy/path");

        LogstashMachine mockLogstashMachine2 = new LogstashMachine();
        mockLogstashMachine2.setLogstashProcessId(100L);
        mockLogstashMachine2.setMachineId(2L);
        mockLogstashMachine2.setDeployPath("/custom/deploy/path");

        // Mock 行为
        when(logstashProcessMapper.selectByName(anyString())).thenReturn(null);
        when(logstashProcessMapper.selectByModule(anyString())).thenReturn(null);
        when(datasourceMapper.selectById(1L)).thenReturn(new DatasourceInfo());
        when(machineMapper.selectById(1L)).thenReturn(mockMachine1);
        when(machineMapper.selectById(2L)).thenReturn(mockMachine2);
        when(logstashProcessConverter.toEntity(createDTO)).thenReturn(mockProcess);
        when(logstashProcessMapper.insert(mockProcess))
                .thenAnswer(
                        invocation -> {
                            mockProcess.setId(100L); // 模拟数据库插入后设置ID
                            return 1;
                        });
        when(logstashMachineConverter.createFromProcess(
                        eq(mockProcess), eq(1L), eq("/custom/deploy/path")))
                .thenReturn(mockLogstashMachine1);
        when(logstashMachineConverter.createFromProcess(
                        eq(mockProcess), eq(2L), eq("/custom/deploy/path")))
                .thenReturn(mockLogstashMachine2);
        when(logstashMachineMapper.insert(any(LogstashMachine.class))).thenReturn(1);
        when(logstashMachineMapper.selectByLogstashProcessId(100L))
                .thenReturn(List.of(mockLogstashMachine1, mockLogstashMachine2));
        when(machineMapper.selectByIds(List.of(1L, 2L)))
                .thenReturn(List.of(mockMachine1, mockMachine2));

        LogstashProcessResponseDTO mockResponse = new LogstashProcessResponseDTO();
        when(logstashProcessConverter.toResponseDTO(mockProcess)).thenReturn(mockResponse);
        when(logstashProcessMapper.selectById(100L)).thenReturn(mockProcess);

        // 执行测试
        LogstashProcessResponseDTO result = logstashProcessService.createLogstashProcess(createDTO);

        // 验证结果
        assertNotNull(result);

        // 验证LogstashMachine创建时使用了自定义部署路径
        verify(logstashMachineConverter).createFromProcess(mockProcess, 1L, "/custom/deploy/path");
        verify(logstashMachineConverter).createFromProcess(mockProcess, 2L, "/custom/deploy/path");

        // 验证插入了两个LogstashMachine记录
        verify(logstashMachineMapper, times(2)).insert(any(LogstashMachine.class));

        // 验证调用了初始化
        verify(logstashDeployService).initializeProcess(eq(mockProcess), anyList());
    }

    @Test
    void testCreateLogstashProcessWithDefaultDeployPath() {
        // 准备测试数据 - 不设置customDeployPath
        LogstashProcessCreateDTO createDTO = new LogstashProcessCreateDTO();
        createDTO.setName("Test Process Default");
        createDTO.setModule("test-default");
        // customDeployPath为null，应该使用默认路径
        createDTO.setDatasourceId(1L);
        createDTO.setMachineIds(List.of(1L));

        LogstashProcess mockProcess = new LogstashProcess();
        mockProcess.setId(200L);
        mockProcess.setName("Test Process Default");
        mockProcess.setModule("test-default");

        MachineInfo mockMachine = new MachineInfo();
        mockMachine.setId(1L);
        mockMachine.setName("Machine1");

        LogstashMachine mockLogstashMachine = new LogstashMachine();
        mockLogstashMachine.setLogstashProcessId(200L);
        mockLogstashMachine.setMachineId(1L);
        mockLogstashMachine.setDeployPath("/opt/logstash"); // 默认路径

        // Mock 行为
        when(logstashProcessMapper.selectByName(anyString())).thenReturn(null);
        when(logstashProcessMapper.selectByModule(anyString())).thenReturn(null);
        when(datasourceMapper.selectById(1L)).thenReturn(new DatasourceInfo());
        when(machineMapper.selectById(1L)).thenReturn(mockMachine);
        when(logstashProcessConverter.toEntity(createDTO)).thenReturn(mockProcess);
        when(logstashProcessMapper.insert(mockProcess))
                .thenAnswer(
                        invocation -> {
                            mockProcess.setId(200L); // 模拟数据库插入后设置ID
                            return 1;
                        });
        when(logstashDeployService.generateDefaultProcessPath(eq(200L), eq(mockMachine)))
                .thenReturn("/opt/logstash/logstash-200");
        when(logstashMachineConverter.createFromProcess(
                        eq(mockProcess), eq(1L), eq("/opt/logstash/logstash-200")))
                .thenReturn(mockLogstashMachine);
        when(logstashMachineMapper.insert(any(LogstashMachine.class))).thenReturn(1);
        when(logstashMachineMapper.selectByLogstashProcessId(200L))
                .thenReturn(List.of(mockLogstashMachine));
        when(machineMapper.selectByIds(List.of(1L))).thenReturn(List.of(mockMachine));

        LogstashProcessResponseDTO mockResponse = new LogstashProcessResponseDTO();
        when(logstashProcessConverter.toResponseDTO(mockProcess)).thenReturn(mockResponse);
        when(logstashProcessMapper.selectById(200L)).thenReturn(mockProcess);

        // 执行测试
        LogstashProcessResponseDTO result = logstashProcessService.createLogstashProcess(createDTO);

        // 验证结果
        assertNotNull(result);

        // 验证LogstashMachine创建时使用了默认部署路径加进程ID
        verify(logstashMachineConverter)
                .createFromProcess(mockProcess, 1L, "/opt/logstash/logstash-200");

        // 验证调用了generateDefaultProcessPath生成默认路径
        verify(logstashDeployService).generateDefaultProcessPath(eq(200L), eq(mockMachine));
    }

    @Test
    void testCreateLogstashProcessWithEmptyCustomDeployPath() {
        // 准备测试数据 - customDeployPath为空字符串，应该使用默认路径
        LogstashProcessCreateDTO createDTO = new LogstashProcessCreateDTO();
        createDTO.setName("Test Process Empty");
        createDTO.setModule("test-empty");
        createDTO.setCustomDeployPath(""); // 空字符串
        createDTO.setDatasourceId(1L);
        createDTO.setMachineIds(List.of(1L));

        LogstashProcess mockProcess = new LogstashProcess();
        mockProcess.setId(300L);

        MachineInfo mockMachine = new MachineInfo();
        mockMachine.setId(1L);

        LogstashMachine mockLogstashMachine = new LogstashMachine();
        mockLogstashMachine.setDeployPath("/opt/logstash");

        // Mock 行为
        when(logstashProcessMapper.selectByName(anyString())).thenReturn(null);
        when(logstashProcessMapper.selectByModule(anyString())).thenReturn(null);
        when(datasourceMapper.selectById(1L)).thenReturn(new DatasourceInfo());
        when(machineMapper.selectById(1L)).thenReturn(mockMachine);
        when(logstashProcessConverter.toEntity(createDTO)).thenReturn(mockProcess);
        when(logstashProcessMapper.insert(mockProcess))
                .thenAnswer(
                        invocation -> {
                            mockProcess.setId(300L); // 模拟数据库插入后设置ID
                            return 1;
                        });
        when(logstashDeployService.generateDefaultProcessPath(eq(300L), eq(mockMachine)))
                .thenReturn("/opt/logstash/logstash-300");
        when(logstashMachineConverter.createFromProcess(
                        eq(mockProcess), eq(1L), eq("/opt/logstash/logstash-300")))
                .thenReturn(mockLogstashMachine);
        when(logstashMachineMapper.insert(any(LogstashMachine.class))).thenReturn(1);
        when(logstashMachineMapper.selectByLogstashProcessId(300L))
                .thenReturn(List.of(mockLogstashMachine));
        when(machineMapper.selectByIds(List.of(1L))).thenReturn(List.of(mockMachine));

        LogstashProcessResponseDTO mockResponse = new LogstashProcessResponseDTO();
        when(logstashProcessConverter.toResponseDTO(mockProcess)).thenReturn(mockResponse);
        when(logstashProcessMapper.selectById(300L)).thenReturn(mockProcess);

        // 执行测试
        LogstashProcessResponseDTO result = logstashProcessService.createLogstashProcess(createDTO);

        // 验证使用了默认路径加进程ID而不是空字符串
        verify(logstashMachineConverter)
                .createFromProcess(mockProcess, 1L, "/opt/logstash/logstash-300");
        verify(logstashDeployService).generateDefaultProcessPath(eq(300L), eq(mockMachine));
    }
}
