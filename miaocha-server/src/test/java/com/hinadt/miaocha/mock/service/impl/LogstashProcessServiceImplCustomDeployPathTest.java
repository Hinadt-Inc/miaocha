package com.hinadt.miaocha.mock.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.logstash.LogstashConfigSyncService;
import com.hinadt.miaocha.application.logstash.LogstashMachineConnectionValidator;
import com.hinadt.miaocha.application.logstash.LogstashProcessDeployService;
import com.hinadt.miaocha.application.logstash.parser.LogstashConfigParser;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.application.service.impl.LogstashProcessServiceImpl;
import com.hinadt.miaocha.domain.converter.LogstashMachineConverter;
import com.hinadt.miaocha.domain.converter.LogstashProcessConverter;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessCreateDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessResponseDTO;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
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
 * <p>测试秒查系统中Logstash进程自定义部署路径功能，基于一机多实例架构 验证用户可以为不同的LogstashMachine实例指定特定的部署路径，支持同一台机器上部署多个实例
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
    @Mock private com.hinadt.miaocha.domain.mapper.ModuleInfoMapper moduleInfoMapper;
    @Mock private LogstashProcessDeployService logstashDeployService;
    @Mock private LogstashProcessConverter logstashProcessConverter;
    @Mock private LogstashMachineConverter logstashMachineConverter;
    @Mock private LogstashConfigParser logstashConfigParser;
    @Mock private TaskService taskService;
    @Mock private LogstashConfigSyncService configSyncService;
    @Mock private LogstashMachineConnectionValidator connectionValidator;

    private LogstashProcessServiceImpl logstashProcessService;

    @BeforeEach
    void setUp() {
        logstashProcessService =
                new LogstashProcessServiceImpl(
                        logstashProcessMapper,
                        logstashMachineMapper,
                        machineMapper,
                        moduleInfoMapper,
                        logstashDeployService,
                        logstashProcessConverter,
                        logstashMachineConverter,
                        logstashConfigParser,
                        taskService,
                        configSyncService,
                        connectionValidator);
    }

    @Test
    @DisplayName("创建进程 - 使用自定义部署路径（一机多实例支持）")
    @Description("验证创建LogstashProcess时可以指定自定义部署路径，支持在同一台机器上部署多个实例")
    void testCreateLogstashProcessWithCustomDeployPath() {
        // 准备测试数据
        LogstashProcessCreateDTO createDTO = new LogstashProcessCreateDTO();
        createDTO.setName("Test Process");
        createDTO.setModuleId(1L);
        createDTO.setCustomDeployPath("/custom/deploy/path");
        createDTO.setMachineIds(List.of(1L, 2L));

        LogstashProcess mockProcess = new LogstashProcess();
        mockProcess.setId(100L);
        mockProcess.setName("Test Process");
        mockProcess.setModuleId(1L);

        MachineInfo mockMachine1 = new MachineInfo();
        mockMachine1.setId(1L);
        mockMachine1.setName("Machine1");

        MachineInfo mockMachine2 = new MachineInfo();
        mockMachine2.setId(2L);
        mockMachine2.setName("Machine2");

        LogstashMachine mockLogstashMachine1 = new LogstashMachine();
        mockLogstashMachine1.setId(1L);
        mockLogstashMachine1.setLogstashProcessId(100L);
        mockLogstashMachine1.setMachineId(1L);
        mockLogstashMachine1.setDeployPath("/custom/deploy/path");

        LogstashMachine mockLogstashMachine2 = new LogstashMachine();
        mockLogstashMachine2.setId(2L);
        mockLogstashMachine2.setLogstashProcessId(100L);
        mockLogstashMachine2.setMachineId(2L);
        mockLogstashMachine2.setDeployPath("/custom/deploy/path");

        // Mock 行为
        when(logstashProcessMapper.selectByName(anyString())).thenReturn(null);
        when(moduleInfoMapper.selectById(1L))
                .thenReturn(new com.hinadt.miaocha.domain.entity.ModuleInfo());
        when(machineMapper.selectById(1L)).thenReturn(mockMachine1);
        when(machineMapper.selectById(2L)).thenReturn(mockMachine2);

        // Mock LogstashMachine创建
        when(logstashMachineConverter.createFromProcess(mockProcess, 1L, "/custom/deploy/path"))
                .thenReturn(mockLogstashMachine1);
        when(logstashMachineConverter.createFromProcess(mockProcess, 2L, "/custom/deploy/path"))
                .thenReturn(mockLogstashMachine2);

        // 检查路径冲突
        when(logstashMachineMapper.selectByMachineAndPath(1L, "/custom/deploy/path"))
                .thenReturn(null);
        when(logstashMachineMapper.selectByMachineAndPath(2L, "/custom/deploy/path"))
                .thenReturn(null);

        when(logstashMachineMapper.insert(any(LogstashMachine.class))).thenReturn(1);
        when(logstashMachineMapper.selectByLogstashProcessId(100L))
                .thenReturn(List.of(mockLogstashMachine1, mockLogstashMachine2));

        when(machineMapper.selectByIds(List.of(1L, 2L)))
                .thenReturn(List.of(mockMachine1, mockMachine2));

        when(logstashProcessConverter.toEntity(createDTO)).thenReturn(mockProcess);
        when(logstashProcessMapper.insert(mockProcess))
                .thenAnswer(
                        invocation -> {
                            mockProcess.setId(100L); // 模拟数据库插入后设置ID
                            return 1;
                        });

        // Mock连接验证
        doNothing().when(connectionValidator).validateSingleMachineConnection(mockMachine1);
        doNothing().when(connectionValidator).validateSingleMachineConnection(mockMachine2);

        // Mock部署服务初始化
        doNothing().when(logstashDeployService).initializeInstances(anyList(), eq(mockProcess));

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

        // 验证检查了路径冲突
        verify(logstashMachineMapper).selectByMachineAndPath(1L, "/custom/deploy/path");
        verify(logstashMachineMapper).selectByMachineAndPath(2L, "/custom/deploy/path");

        // 验证插入了两个LogstashMachine记录
        verify(logstashMachineMapper, times(2)).insert(any(LogstashMachine.class));

        // 验证调用了初始化
        verify(logstashDeployService).initializeInstances(anyList(), eq(mockProcess));
    }

    @Test
    @DisplayName("创建进程 - 使用默认部署路径")
    @Description("验证创建LogstashProcess时不指定自定义路径，系统自动生成默认路径")
    void testCreateLogstashProcessWithDefaultDeployPath() {
        // 准备测试数据 - 不设置customDeployPath
        LogstashProcessCreateDTO createDTO = new LogstashProcessCreateDTO();
        createDTO.setName("Test Process Default");
        createDTO.setModuleId(1L);
        // customDeployPath为null，应该使用默认路径
        createDTO.setMachineIds(List.of(1L));

        LogstashProcess mockProcess = new LogstashProcess();
        mockProcess.setId(200L);
        mockProcess.setName("Test Process Default");
        mockProcess.setModuleId(1L);

        MachineInfo mockMachine = new MachineInfo();
        mockMachine.setId(1L);
        mockMachine.setName("Machine1");

        LogstashMachine mockLogstashMachine = new LogstashMachine();
        mockLogstashMachine.setId(1L);
        mockLogstashMachine.setLogstashProcessId(200L);
        mockLogstashMachine.setMachineId(1L);
        mockLogstashMachine.setDeployPath("/opt/logstash/logstash-200"); // 默认路径

        // Mock 行为
        when(logstashProcessMapper.selectByName(anyString())).thenReturn(null);
        when(moduleInfoMapper.selectById(1L))
                .thenReturn(new com.hinadt.miaocha.domain.entity.ModuleInfo());
        when(machineMapper.selectById(1L)).thenReturn(mockMachine);
        when(logstashProcessConverter.toEntity(createDTO)).thenReturn(mockProcess);
        when(logstashProcessMapper.insert(mockProcess))
                .thenAnswer(
                        invocation -> {
                            mockProcess.setId(200L); // 模拟数据库插入后设置ID
                            return 1;
                        });
        when(logstashDeployService.generateDefaultInstancePath(eq(mockMachine)))
                .thenReturn("/opt/logstash/logstash-200");

        // 检查生成的默认路径是否冲突
        when(logstashMachineMapper.selectByMachineAndPath(eq(1L), eq("/opt/logstash/logstash-200")))
                .thenReturn(null);

        when(logstashMachineConverter.createFromProcess(
                        eq(mockProcess), eq(1L), eq("/opt/logstash/logstash-200")))
                .thenReturn(mockLogstashMachine);
        when(logstashMachineMapper.insert(any(LogstashMachine.class))).thenReturn(1);
        when(logstashMachineMapper.selectByLogstashProcessId(200L))
                .thenReturn(List.of(mockLogstashMachine));
        when(machineMapper.selectByIds(List.of(1L))).thenReturn(List.of(mockMachine));

        // Mock连接验证
        doNothing().when(connectionValidator).validateSingleMachineConnection(mockMachine);

        // Mock部署服务初始化
        doNothing().when(logstashDeployService).initializeInstances(anyList(), eq(mockProcess));

        LogstashProcessResponseDTO mockResponse = new LogstashProcessResponseDTO();
        when(logstashProcessConverter.toResponseDTO(mockProcess)).thenReturn(mockResponse);
        when(logstashProcessMapper.selectById(200L)).thenReturn(mockProcess);

        // 执行测试
        LogstashProcessResponseDTO result = logstashProcessService.createLogstashProcess(createDTO);

        // 验证结果
        assertNotNull(result);

        // 验证LogstashMachine创建时使用了默认部署路径
        verify(logstashMachineConverter)
                .createFromProcess(mockProcess, 1L, "/opt/logstash/logstash-200");

        // 验证调用了generateDefaultInstancePath生成默认路径
        verify(logstashDeployService).generateDefaultInstancePath(eq(mockMachine));

        // 验证检查了路径冲突
        verify(logstashMachineMapper).selectByMachineAndPath(1L, "/opt/logstash/logstash-200");
    }

    @Test
    @DisplayName("创建进程 - 空字符串自定义路径使用默认路径")
    @Description("验证当customDeployPath为空字符串时，系统使用默认路径而不是空字符串")
    void testCreateLogstashProcessWithEmptyCustomDeployPath() {
        // 准备测试数据 - customDeployPath为空字符串，应该使用默认路径
        LogstashProcessCreateDTO createDTO = new LogstashProcessCreateDTO();
        createDTO.setName("Test Process Empty");
        createDTO.setModuleId(1L);
        createDTO.setCustomDeployPath(""); // 空字符串
        createDTO.setMachineIds(List.of(1L));

        LogstashProcess mockProcess = new LogstashProcess();
        mockProcess.setId(300L);

        MachineInfo mockMachine = new MachineInfo();
        mockMachine.setId(1L);

        LogstashMachine mockLogstashMachine = new LogstashMachine();
        mockLogstashMachine.setId(1L);
        mockLogstashMachine.setLogstashProcessId(300L);
        mockLogstashMachine.setMachineId(1L);
        mockLogstashMachine.setDeployPath("/opt/logstash/logstash-300");

        // Mock 行为
        when(logstashProcessMapper.selectByName(anyString())).thenReturn(null);
        when(moduleInfoMapper.selectById(1L))
                .thenReturn(new com.hinadt.miaocha.domain.entity.ModuleInfo());
        when(machineMapper.selectById(1L)).thenReturn(mockMachine);
        when(logstashProcessConverter.toEntity(createDTO)).thenReturn(mockProcess);
        when(logstashProcessMapper.insert(mockProcess))
                .thenAnswer(
                        invocation -> {
                            mockProcess.setId(300L); // 模拟数据库插入后设置ID
                            return 1;
                        });
        when(logstashDeployService.generateDefaultInstancePath(eq(mockMachine)))
                .thenReturn("/opt/logstash/logstash-300");

        // 检查生成的默认路径是否冲突
        when(logstashMachineMapper.selectByMachineAndPath(eq(1L), eq("/opt/logstash/logstash-300")))
                .thenReturn(null);

        when(logstashMachineConverter.createFromProcess(
                        eq(mockProcess), eq(1L), eq("/opt/logstash/logstash-300")))
                .thenReturn(mockLogstashMachine);
        when(logstashMachineMapper.insert(any(LogstashMachine.class))).thenReturn(1);
        when(logstashMachineMapper.selectByLogstashProcessId(300L))
                .thenReturn(List.of(mockLogstashMachine));
        when(machineMapper.selectByIds(List.of(1L))).thenReturn(List.of(mockMachine));

        // Mock连接验证
        doNothing().when(connectionValidator).validateSingleMachineConnection(mockMachine);

        // Mock部署服务初始化
        doNothing().when(logstashDeployService).initializeInstances(anyList(), eq(mockProcess));

        LogstashProcessResponseDTO mockResponse = new LogstashProcessResponseDTO();
        when(logstashProcessConverter.toResponseDTO(mockProcess)).thenReturn(mockResponse);
        when(logstashProcessMapper.selectById(300L)).thenReturn(mockProcess);

        // 执行测试
        LogstashProcessResponseDTO result = logstashProcessService.createLogstashProcess(createDTO);

        // 验证使用了默认路径而不是空字符串
        verify(logstashMachineConverter)
                .createFromProcess(mockProcess, 1L, "/opt/logstash/logstash-300");
        verify(logstashDeployService).generateDefaultInstancePath(eq(mockMachine));

        // 验证检查了路径冲突
        verify(logstashMachineMapper).selectByMachineAndPath(1L, "/opt/logstash/logstash-300");
    }

    @Test
    @DisplayName("创建进程 - 同台机器不同路径支持多实例")
    @Description("验证可以在同一台机器上使用不同部署路径创建多个LogstashProcess实例")
    void testCreateMultipleInstancesOnSameMachine() {
        // 第一个实例 - 使用自定义路径
        LogstashProcessCreateDTO createDTO1 = new LogstashProcessCreateDTO();
        createDTO1.setName("Process Instance 1");
        createDTO1.setModuleId(1L);
        createDTO1.setCustomDeployPath("/custom/path/instance1");
        createDTO1.setMachineIds(List.of(1L));

        LogstashProcess mockProcess1 = new LogstashProcess();
        mockProcess1.setId(100L);
        mockProcess1.setName("Process Instance 1");
        mockProcess1.setModuleId(1L);

        // 第二个实例 - 使用不同的自定义路径
        LogstashProcessCreateDTO createDTO2 = new LogstashProcessCreateDTO();
        createDTO2.setName("Process Instance 2");
        createDTO2.setModuleId(1L);
        createDTO2.setCustomDeployPath("/custom/path/instance2"); // 不同的路径
        createDTO2.setMachineIds(List.of(1L));

        LogstashProcess mockProcess2 = new LogstashProcess();
        mockProcess2.setId(200L);
        mockProcess2.setName("Process Instance 2");
        mockProcess2.setModuleId(1L);

        MachineInfo mockMachine1 = new MachineInfo();
        mockMachine1.setId(1L);
        mockMachine1.setName("Machine1");

        LogstashMachine mockLogstashMachine1 = new LogstashMachine();
        mockLogstashMachine1.setId(1L);
        mockLogstashMachine1.setLogstashProcessId(100L);
        mockLogstashMachine1.setMachineId(1L);
        mockLogstashMachine1.setDeployPath("/custom/path/instance1");

        LogstashMachine mockLogstashMachine2 = new LogstashMachine();
        mockLogstashMachine2.setId(2L);
        mockLogstashMachine2.setLogstashProcessId(200L);
        mockLogstashMachine2.setMachineId(1L);
        mockLogstashMachine2.setDeployPath("/custom/path/instance2");

        // Mock第一个实例的创建
        when(logstashProcessMapper.selectByName("Process Instance 1")).thenReturn(null);
        when(logstashProcessMapper.selectByName("Process Instance 2")).thenReturn(null);
        when(moduleInfoMapper.selectById(1L))
                .thenReturn(new com.hinadt.miaocha.domain.entity.ModuleInfo());
        when(machineMapper.selectById(1L)).thenReturn(mockMachine1);

        // 检查路径冲突 - 两个实例都不冲突（因为路径不同）
        when(logstashMachineMapper.selectByMachineAndPath(eq(1L), eq("/custom/path/instance1")))
                .thenReturn(null);
        when(logstashMachineMapper.selectByMachineAndPath(eq(1L), eq("/custom/path/instance2")))
                .thenReturn(null);

        when(logstashProcessConverter.toEntity(createDTO1)).thenReturn(mockProcess1);
        when(logstashProcessConverter.toEntity(createDTO2)).thenReturn(mockProcess2);

        when(logstashProcessMapper.insert(mockProcess1))
                .thenAnswer(
                        invocation -> {
                            mockProcess1.setId(100L);
                            return 1;
                        });
        when(logstashProcessMapper.insert(mockProcess2))
                .thenAnswer(
                        invocation -> {
                            mockProcess2.setId(200L);
                            return 1;
                        });

        when(logstashMachineConverter.createFromProcess(
                        eq(mockProcess1), eq(1L), eq("/custom/path/instance1")))
                .thenReturn(mockLogstashMachine1);
        when(logstashMachineConverter.createFromProcess(
                        eq(mockProcess2), eq(1L), eq("/custom/path/instance2")))
                .thenReturn(mockLogstashMachine2);

        // Mock数据库操作
        when(logstashMachineMapper.insert(any(LogstashMachine.class))).thenReturn(1);
        when(logstashMachineMapper.selectByLogstashProcessId(100L))
                .thenReturn(List.of(mockLogstashMachine1));
        when(logstashMachineMapper.selectByLogstashProcessId(200L))
                .thenReturn(List.of(mockLogstashMachine2));
        when(machineMapper.selectByIds(List.of(1L))).thenReturn(List.of(mockMachine1));

        // Mock部署服务初始化实例
        doNothing().when(logstashDeployService).initializeInstances(anyList(), eq(mockProcess1));
        doNothing().when(logstashDeployService).initializeInstances(anyList(), eq(mockProcess2));

        LogstashProcessResponseDTO mockResponse1 = new LogstashProcessResponseDTO();
        mockResponse1.setId(100L);
        LogstashProcessResponseDTO mockResponse2 = new LogstashProcessResponseDTO();
        mockResponse2.setId(200L);

        when(logstashProcessConverter.toResponseDTO(mockProcess1)).thenReturn(mockResponse1);
        when(logstashProcessConverter.toResponseDTO(mockProcess2)).thenReturn(mockResponse2);
        when(logstashProcessMapper.selectById(100L)).thenReturn(mockProcess1);
        when(logstashProcessMapper.selectById(200L)).thenReturn(mockProcess2);

        // Mock连接验证
        doNothing().when(connectionValidator).validateSingleMachineConnection(mockMachine1);

        // 执行两个实例创建
        LogstashProcessResponseDTO result1 =
                logstashProcessService.createLogstashProcess(createDTO1);
        LogstashProcessResponseDTO result2 =
                logstashProcessService.createLogstashProcess(createDTO2);

        // 验证两个实例都创建成功
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(100L, result1.getId());
        assertEquals(200L, result2.getId());

        // 验证使用了正确的部署路径（不同路径）
        verify(logstashMachineConverter)
                .createFromProcess(mockProcess1, 1L, "/custom/path/instance1");
        verify(logstashMachineConverter)
                .createFromProcess(mockProcess2, 1L, "/custom/path/instance2");

        // 验证路径冲突检查被调用
        verify(logstashMachineMapper).selectByMachineAndPath(1L, "/custom/path/instance1");
        verify(logstashMachineMapper).selectByMachineAndPath(1L, "/custom/path/instance2");
    }
}
