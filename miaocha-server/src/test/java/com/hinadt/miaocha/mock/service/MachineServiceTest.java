package com.hinadt.miaocha.mock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.impl.MachineServiceImpl;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.converter.MachineConverter;
import com.hinadt.miaocha.domain.dto.MachineConnectionTestResultDTO;
import com.hinadt.miaocha.domain.dto.MachineCreateDTO;
import com.hinadt.miaocha.domain.dto.MachineDTO;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.domain.mapper.MachineMapper;
import io.qameta.allure.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Machine服务测试类 */
@ExtendWith(MockitoExtension.class)
@Feature("机器管理")
@DisplayName("机器管理服务测试")
class MachineServiceTest {

    @Mock private MachineMapper machineMapper;
    @Mock private LogstashMachineMapper logstashMachineMapper;
    @Mock private MachineConverter machineConverter;
    @Mock private SshClient sshClient;

    private MachineServiceImpl machineService;

    private MachineInfo sampleMachine;
    private MachineDTO sampleMachineDTO;

    @BeforeEach
    void setUp() {
        // 手动创建服务实例
        machineService =
                new MachineServiceImpl(
                        machineMapper, sshClient, machineConverter, logstashMachineMapper);

        setupTestData();
    }

    private void setupTestData() {
        // 机器信息
        sampleMachine = new MachineInfo();
        sampleMachine.setId(1L);
        sampleMachine.setName("test-machine");
        sampleMachine.setIp("192.168.1.100");
        sampleMachine.setPort(22);
        sampleMachine.setUsername("admin");
        sampleMachine.setPassword("password");
        sampleMachine.setCreateTime(LocalDateTime.now());
        sampleMachine.setUpdateTime(LocalDateTime.now());
        sampleMachine.setCreateUser("admin@test.com");
        sampleMachine.setUpdateUser("admin@test.com");

        // 机器DTO
        sampleMachineDTO = new MachineDTO();
        sampleMachineDTO.setId(1L);
        sampleMachineDTO.setName("test-machine");
        sampleMachineDTO.setIp("192.168.1.100");
        sampleMachineDTO.setPort(22);
        sampleMachineDTO.setUsername("admin");
        sampleMachineDTO.setCreateTime(LocalDateTime.now());
        sampleMachineDTO.setUpdateTime(LocalDateTime.now());
        sampleMachineDTO.setCreateUser("admin@test.com");
        sampleMachineDTO.setUpdateUser("admin@test.com");
        sampleMachineDTO.setCreateUserName("管理员");
        sampleMachineDTO.setUpdateUserName("管理员");
    }

    @Test
    @Story("创建机器")
    @Description("测试成功创建机器")
    @Severity(SeverityLevel.CRITICAL)
    void testCreateMachine_Success() {
        // 准备测试数据
        MachineCreateDTO createDTO = new MachineCreateDTO();
        createDTO.setName("new-machine");
        createDTO.setIp("192.168.1.101");
        createDTO.setPort(22);
        createDTO.setUsername("admin");
        createDTO.setPassword("password");

        // Mock 行为
        when(machineMapper.selectByName("new-machine")).thenReturn(null);
        when(machineConverter.toEntity(createDTO)).thenReturn(sampleMachine);
        when(machineMapper.insert(sampleMachine)).thenReturn(1);
        when(machineConverter.toDto(sampleMachine)).thenReturn(sampleMachineDTO);

        // 执行测试
        MachineDTO result = machineService.createMachine(createDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals("test-machine", result.getName());
        assertEquals("192.168.1.100", result.getIp());

        // 验证调用
        verify(machineMapper).selectByName("new-machine");
        verify(machineConverter).toEntity(createDTO);
        verify(machineMapper).insert(sampleMachine);
        verify(machineConverter).toDto(sampleMachine);
    }

    @Test
    @Story("创建机器")
    @Description("测试机器名称已存在时抛出异常")
    @Severity(SeverityLevel.NORMAL)
    void testCreateMachine_NameExists() {
        // 准备测试数据
        MachineCreateDTO createDTO = new MachineCreateDTO();
        createDTO.setName("existing-machine");

        // Mock 行为
        when(machineMapper.selectByName("existing-machine")).thenReturn(sampleMachine);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> machineService.createMachine(createDTO));

        assertEquals(ErrorCode.MACHINE_NAME_EXISTS, exception.getErrorCode());

        // 验证只调用了名称检查
        verify(machineMapper).selectByName("existing-machine");
        verify(machineMapper, never()).insert(any());
    }

    @Test
    @Story("更新机器")
    @Description("测试成功更新机器")
    @Severity(SeverityLevel.CRITICAL)
    void testUpdateMachine_Success() {
        // 准备测试数据
        MachineCreateDTO updateDTO = new MachineCreateDTO();
        updateDTO.setName("updated-machine");
        updateDTO.setIp("192.168.1.102");

        MachineInfo updatedMachine = new MachineInfo();
        updatedMachine.setId(1L);
        updatedMachine.setName("updated-machine");

        // Mock 行为
        when(machineMapper.selectById(1L)).thenReturn(sampleMachine);
        when(machineMapper.selectByName("updated-machine")).thenReturn(null);
        when(machineConverter.updateEntity(sampleMachine, updateDTO)).thenReturn(updatedMachine);
        when(machineMapper.update(updatedMachine)).thenReturn(1);
        when(machineConverter.toDto(updatedMachine)).thenReturn(sampleMachineDTO);

        // 执行测试
        MachineDTO result = machineService.updateMachine(1L, updateDTO);

        // 验证结果
        assertNotNull(result);

        // 验证调用顺序
        verify(machineMapper).selectById(1L);
        verify(machineMapper).selectByName("updated-machine");
        verify(machineMapper).update(updatedMachine);
    }

    @Test
    @Story("更新机器")
    @Description("测试更新不存在的机器")
    @Severity(SeverityLevel.NORMAL)
    void testUpdateMachine_MachineNotFound() {
        // 准备测试数据
        MachineCreateDTO updateDTO = new MachineCreateDTO();
        updateDTO.setName("updated-machine");

        // Mock 行为
        when(machineMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> machineService.updateMachine(999L, updateDTO));

        assertEquals(ErrorCode.MACHINE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @Story("更新机器")
    @Description("测试更新时机器名称重复")
    @Severity(SeverityLevel.NORMAL)
    void testUpdateMachine_NameExists() {
        // 准备测试数据
        MachineCreateDTO updateDTO = new MachineCreateDTO();
        updateDTO.setName("existing-name");

        MachineInfo existingMachine = new MachineInfo();
        existingMachine.setId(2L);
        existingMachine.setName("existing-name");

        // Mock 行为
        when(machineMapper.selectById(1L)).thenReturn(sampleMachine);
        when(machineMapper.selectByName("existing-name")).thenReturn(existingMachine);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> machineService.updateMachine(1L, updateDTO));

        assertEquals(ErrorCode.MACHINE_NAME_EXISTS, exception.getErrorCode());

        // 验证不会更新
        verify(machineMapper, never()).update(any());
    }

    @Test
    @Story("删除机器")
    @Description("测试成功删除机器")
    @Severity(SeverityLevel.CRITICAL)
    void testDeleteMachine_Success() {
        // Mock 行为 - 根据实际业务逻辑，使用countByMachineId而不是selectByMachineId
        when(machineMapper.selectById(1L)).thenReturn(sampleMachine);
        when(logstashMachineMapper.countByMachineId(1L)).thenReturn(0);
        when(machineMapper.deleteById(1L)).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> machineService.deleteMachine(1L));

        // 验证调用
        verify(machineMapper).selectById(1L);
        verify(logstashMachineMapper).countByMachineId(1L);
        verify(machineMapper).deleteById(1L);
    }

    @Test
    @Story("删除机器")
    @Description("测试删除不存在的机器")
    @Severity(SeverityLevel.NORMAL)
    void testDeleteMachine_MachineNotFound() {
        // Mock 行为
        when(machineMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(BusinessException.class, () -> machineService.deleteMachine(999L));

        assertEquals(ErrorCode.MACHINE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @Story("删除机器")
    @Description("测试删除被使用的机器")
    @Severity(SeverityLevel.CRITICAL)
    void testDeleteMachine_MachineInUse() {
        // Mock 行为：机器被Logstash使用 - 根据实际业务逻辑，使用countByMachineId返回大于0的数量
        when(machineMapper.selectById(1L)).thenReturn(sampleMachine);
        when(logstashMachineMapper.countByMachineId(1L)).thenReturn(2);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(BusinessException.class, () -> machineService.deleteMachine(1L));

        assertEquals(ErrorCode.MACHINE_IN_USE, exception.getErrorCode());

        // 验证不会删除
        verify(machineMapper, never()).deleteById(any());
    }

    @Test
    @Story("连接测试")
    @Description("测试机器连接成功")
    @Severity(SeverityLevel.CRITICAL)
    void testTestConnection_Success() {
        // Mock 行为
        when(machineMapper.selectById(1L)).thenReturn(sampleMachine);
        when(sshClient.testConnection(sampleMachine))
                .thenReturn(MachineConnectionTestResultDTO.success());

        // 执行测试
        MachineConnectionTestResultDTO result = machineService.testConnection(1L);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(machineMapper).selectById(1L);
        verify(sshClient).testConnection(sampleMachine);
    }

    @Test
    @Story("连接测试")
    @Description("测试机器连接失败")
    @Severity(SeverityLevel.NORMAL)
    void testTestConnection_Failed() {
        // Mock 行为
        when(machineMapper.selectById(1L)).thenReturn(sampleMachine);
        when(sshClient.testConnection(sampleMachine))
                .thenReturn(MachineConnectionTestResultDTO.failure("连接失败，请检查配置"));

        // 执行测试
        MachineConnectionTestResultDTO result = machineService.testConnection(1L);

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @Story("连接测试")
    @Description("测试通过连接信息直接测试连接")
    @Severity(SeverityLevel.NORMAL)
    void testTestConnectionWithParams_Success() {
        // 准备测试数据
        MachineCreateDTO connectionDTO = new MachineCreateDTO();
        connectionDTO.setIp("192.168.1.100");
        connectionDTO.setPort(22);
        connectionDTO.setUsername("admin");
        connectionDTO.setPassword("password");

        // Mock 行为
        when(machineConverter.toEntity(connectionDTO)).thenReturn(sampleMachine);
        when(sshClient.testConnection(sampleMachine))
                .thenReturn(MachineConnectionTestResultDTO.success());

        // 执行测试
        MachineConnectionTestResultDTO result =
                machineService.testConnectionWithParams(connectionDTO);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(machineConverter).toEntity(connectionDTO);
        verify(sshClient).testConnection(sampleMachine);
    }

    @Test
    @Story("查询机器")
    @Description("测试根据ID查询单个机器")
    @Severity(SeverityLevel.CRITICAL)
    void testGetMachine_Success() {
        // Mock 行为
        when(machineMapper.selectById(1L)).thenReturn(sampleMachine);
        when(machineConverter.toDto(sampleMachine)).thenReturn(sampleMachineDTO);

        // 执行测试
        MachineDTO result = machineService.getMachine(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test-machine", result.getName());

        // 验证调用
        verify(machineMapper).selectById(1L);
        verify(machineConverter).toDto(sampleMachine);
    }

    @Test
    @Story("查询机器")
    @Description("测试查询不存在的机器")
    @Severity(SeverityLevel.NORMAL)
    void testGetMachine_NotFound() {
        // Mock 行为
        when(machineMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(BusinessException.class, () -> machineService.getMachine(999L));

        assertEquals(ErrorCode.MACHINE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @Story("查询机器")
    @Description("测试获取所有机器列表")
    @Severity(SeverityLevel.NORMAL)
    void testGetAllMachines_Success() {
        // 准备数据
        List<MachineInfo> machineList = Collections.singletonList(sampleMachine);

        // Mock 行为
        when(machineMapper.selectAll()).thenReturn(machineList);
        when(machineConverter.toDto(sampleMachine)).thenReturn(sampleMachineDTO);

        // 执行测试
        List<MachineDTO> result = machineService.getAllMachines();

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test-machine", result.get(0).getName());

        // 验证调用
        verify(machineMapper).selectAll();
        verify(machineConverter).toDto(sampleMachine);
    }
}
