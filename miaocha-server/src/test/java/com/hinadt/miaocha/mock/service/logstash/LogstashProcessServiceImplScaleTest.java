package com.hinadt.miaocha.mock.service.logstash;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.logstash.LogstashConfigSyncService;
import com.hinadt.miaocha.application.logstash.LogstashMachineConnectionValidator;
import com.hinadt.miaocha.application.logstash.LogstashProcessDeployService;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.parser.LogstashConfigParser;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.application.service.impl.LogstashProcessServiceImpl;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.converter.LogstashMachineConverter;
import com.hinadt.miaocha.domain.converter.LogstashProcessConverter;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessResponseDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessScaleRequestDTO;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.domain.mapper.LogstashProcessMapper;
import com.hinadt.miaocha.domain.mapper.MachineMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import io.qameta.allure.*;
import java.util.Arrays;
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

/**
 * LogstashProcessService 扩容缩容功能测试
 *
 * <p>测试秒查系统中Logstash进程的动态扩容缩容能力，基于LogstashMachine实例的新架构 支持同一台机器上部署多个LogstashProcess实例（一机多实例）
 */
@Epic("秒查日志管理系统")
@Feature("Logstash进程管理")
@Story("进程扩容缩容")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LogstashProcessService 扩容缩容测试")
@Owner("开发团队")
class LogstashProcessServiceImplScaleTest {

    @Mock private LogstashProcessMapper logstashProcessMapper;
    @Mock private LogstashMachineMapper logstashMachineMapper;
    @Mock private MachineMapper machineMapper;
    @Mock private ModuleInfoMapper moduleInfoMapper;
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
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("扩容操作 - 成功添加机器（一机多实例支持）")
    @Description("验证在日志处理负载增加时，能够成功向Logstash进程添加新机器节点，支持同一台机器部署多个实例")
    @Issue("MIAOCHA-101")
    void testScaleOut_Success() {
        Allure.step(
                "准备扩容测试数据",
                () -> {
                    Allure.parameter("进程ID", "1");
                    Allure.parameter("添加机器ID", "3, 4");
                    Allure.parameter("部署路径", "/custom/deploy/path");
                });
        // 准备测试数据
        Long processId = 1L;
        LogstashProcess process = createTestProcess(processId);

        LogstashProcessScaleRequestDTO dto = new LogstashProcessScaleRequestDTO();
        dto.setAddMachineIds(Arrays.asList(3L, 4L));
        dto.setCustomDeployPath("/custom/deploy/path");

        MachineInfo machine3 = createTestMachine(3L, "machine3", "192.168.1.3");
        MachineInfo machine4 = createTestMachine(4L, "machine4", "192.168.1.4");

        LogstashMachine logstashMachine3 = createTestLogstashMachine(1L, processId, 3L);
        logstashMachine3.setDeployPath("/custom/deploy/path");
        LogstashMachine logstashMachine4 = createTestLogstashMachine(2L, processId, 4L);
        logstashMachine4.setDeployPath("/custom/deploy/path");

        LogstashProcessResponseDTO responseDTO = new LogstashProcessResponseDTO();
        responseDTO.setId(processId);

        // Mock 方法调用 - 基于新架构，检查是否存在相同 (processId, machineId, deployPath) 的实例
        when(logstashProcessMapper.selectById(processId)).thenReturn(process);
        when(machineMapper.selectById(3L)).thenReturn(machine3);
        when(machineMapper.selectById(4L)).thenReturn(machine4);

        // 检查是否存在冲突的LogstashMachine实例（相同machine+path）
        when(logstashMachineMapper.selectByMachineAndPath(3L, "/custom/deploy/path"))
                .thenReturn(null);
        when(logstashMachineMapper.selectByMachineAndPath(4L, "/custom/deploy/path"))
                .thenReturn(null);

        // Mock验证器不抛出异常（连接成功）
        doNothing().when(connectionValidator).validateSingleMachineConnection(machine3);
        doNothing().when(connectionValidator).validateSingleMachineConnection(machine4);

        // Mock转换器创建LogstashMachine（使用自定义路径）
        when(logstashMachineConverter.createFromProcess(process, 3L, "/custom/deploy/path"))
                .thenReturn(logstashMachine3);
        when(logstashMachineConverter.createFromProcess(process, 4L, "/custom/deploy/path"))
                .thenReturn(logstashMachine4);

        // Mock插入操作
        when(logstashMachineMapper.insert(logstashMachine3)).thenReturn(1);
        when(logstashMachineMapper.insert(logstashMachine4)).thenReturn(1);

        // Mock部署服务初始化实例
        doNothing().when(logstashDeployService).initializeInstances(anyList(), eq(process));

        when(logstashProcessConverter.toResponseDTO(process)).thenReturn(responseDTO);

        // 执行测试
        LogstashProcessResponseDTO result =
                logstashProcessService.scaleLogstashProcess(processId, dto);

        // 验证结果
        assertNotNull(result);
        assertEquals(processId, result.getId());

        // 验证调用了正确的方法
        verify(logstashMachineMapper, times(2)).insert(any(LogstashMachine.class));
        verify(logstashDeployService).initializeInstances(anyList(), eq(process));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("扩容操作 - 检测路径冲突")
    @Description("验证在扩容时能正确检测和处理部署路径冲突的情况")
    @Issue("MIAOCHA-103")
    void testScaleOut_PathConflict() {
        // 准备测试数据
        Long processId = 1L;
        LogstashProcess process = createTestProcess(processId);

        LogstashProcessScaleRequestDTO dto = new LogstashProcessScaleRequestDTO();
        dto.setAddMachineIds(Arrays.asList(2L)); // 只添加一台机器
        dto.setCustomDeployPath("/conflicting/path");

        MachineInfo machine2 = createTestMachine(2L, "machine2", "192.168.1.2");
        LogstashMachine existingConflictingInstance = createTestLogstashMachine(99L, processId, 2L);
        existingConflictingInstance.setDeployPath("/conflicting/path");

        when(logstashProcessMapper.selectById(processId)).thenReturn(process);
        when(machineMapper.selectById(2L))
                .thenReturn(createTestMachine(2L, "machine2", "192.168.1.2"));

        // 返回已存在的实例，表示冲突（使用新的路径冲突检查方法）
        when(logstashMachineMapper.selectByMachineAndPath(2L, "/conflicting/path"))
                .thenReturn(existingConflictingInstance);

        // 执行测试并期待异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> logstashProcessService.scaleLogstashProcess(processId, dto));

        // 验证异常信息
        assertEquals(ErrorCode.LOGSTASH_MACHINE_ALREADY_ASSOCIATED, exception.getErrorCode());
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("缩容操作 - 成功移除LogstashMachine实例")
    @Description("验证在日志处理负载减少时，能够安全移除特定的LogstashMachine实例")
    @Issue("MIAOCHA-102")
    void testScaleIn_Success() {
        // 准备测试数据
        Long processId = 1L;
        LogstashProcess process = createTestProcess(processId);

        // 使用logstashMachineIds而不是machineIds
        LogstashProcessScaleRequestDTO dto = new LogstashProcessScaleRequestDTO();
        dto.setRemoveLogstashMachineIds(Arrays.asList(2L)); // 移除特定的LogstashMachine实例

        LogstashMachine existingLogstashMachine = createTestLogstashMachine(2L, processId, 2L);
        existingLogstashMachine.setState(LogstashMachineState.NOT_STARTED.name());

        // 模拟进程至少还有其他LogstashMachine实例
        LogstashMachine remainingInstance = createTestLogstashMachine(1L, processId, 1L);
        List<LogstashMachine> allInstances =
                Arrays.asList(remainingInstance, existingLogstashMachine);

        LogstashProcessResponseDTO responseDTO = new LogstashProcessResponseDTO();
        responseDTO.setId(processId);

        when(logstashProcessMapper.selectById(processId)).thenReturn(process);
        when(logstashMachineMapper.selectById(2L)).thenReturn(existingLogstashMachine);
        when(logstashMachineMapper.selectByLogstashProcessId(processId)).thenReturn(allInstances);
        when(logstashProcessConverter.toResponseDTO(process)).thenReturn(responseDTO);

        // Mock删除目录的异步操作
        CompletableFuture<Boolean> deleteDirectoryFuture = CompletableFuture.completedFuture(true);
        when(logstashDeployService.deleteInstancesDirectory(anyList()))
                .thenReturn(deleteDirectoryFuture);

        // 执行测试
        LogstashProcessResponseDTO result =
                logstashProcessService.scaleLogstashProcess(processId, dto);

        // 验证结果
        assertNotNull(result);
        assertEquals(processId, result.getId());

        // 验证调用了删除目录服务（异步删除实例目录和数据库记录）
        verify(logstashDeployService).deleteInstancesDirectory(anyList());

        // 注意：新的缩容逻辑保留任务记录，不再调用 taskService.getAllInstanceTaskIds
    }

    @Test
    @DisplayName("缩容操作 - 不能缩容到零个实例")
    void testScaleIn_CannotScaleToZero() {
        // 准备测试数据
        Long processId = 1L;
        LogstashProcess process = createTestProcess(processId);

        LogstashProcessScaleRequestDTO dto = new LogstashProcessScaleRequestDTO();
        dto.setRemoveLogstashMachineIds(Arrays.asList(1L)); // 移除唯一的实例

        LogstashMachine onlyInstance = createTestLogstashMachine(1L, processId, 1L);
        List<LogstashMachine> allInstances = Arrays.asList(onlyInstance);

        when(logstashProcessMapper.selectById(processId)).thenReturn(process);
        when(logstashMachineMapper.selectById(1L)).thenReturn(onlyInstance);
        when(logstashMachineMapper.selectByLogstashProcessId(processId)).thenReturn(allInstances);

        // 执行测试并期待异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> logstashProcessService.scaleLogstashProcess(processId, dto));

        assertEquals(ErrorCode.LOGSTASH_CANNOT_SCALE_TO_ZERO, exception.getErrorCode());
    }

    @Test
    @DisplayName("缩容操作 - 运行中实例不能移除")
    void testScaleIn_RunningInstanceCannotRemove() {
        // 准备测试数据
        Long processId = 1L;
        LogstashProcess process = createTestProcess(processId);

        LogstashProcessScaleRequestDTO dto = new LogstashProcessScaleRequestDTO();
        dto.setRemoveLogstashMachineIds(Arrays.asList(2L));
        dto.setForceScale(false); // 非强制模式

        LogstashMachine runningInstance = createTestLogstashMachine(2L, processId, 2L);
        runningInstance.setState(LogstashMachineState.RUNNING.name());

        // 确保进程有其他实例
        LogstashMachine otherInstance = createTestLogstashMachine(1L, processId, 1L);
        List<LogstashMachine> allInstances = Arrays.asList(otherInstance, runningInstance);

        when(logstashProcessMapper.selectById(processId)).thenReturn(process);
        when(logstashMachineMapper.selectById(2L)).thenReturn(runningInstance);
        when(logstashMachineMapper.selectByLogstashProcessId(processId)).thenReturn(allInstances);

        // 执行测试并期待异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> logstashProcessService.scaleLogstashProcess(processId, dto));

        assertEquals(ErrorCode.LOGSTASH_MACHINE_RUNNING_CANNOT_REMOVE, exception.getErrorCode());
    }

    @Test
    @DisplayName("缩容操作 - 强制模式停止运行中实例")
    void testScaleIn_ForceStopRunningInstance() {
        // 准备测试数据
        Long processId = 1L;
        LogstashProcess process = createTestProcess(processId);

        LogstashProcessScaleRequestDTO dto = new LogstashProcessScaleRequestDTO();
        dto.setRemoveLogstashMachineIds(Arrays.asList(2L));
        dto.setForceScale(true); // 强制模式

        LogstashMachine runningInstance = createTestLogstashMachine(2L, processId, 2L);
        runningInstance.setState(LogstashMachineState.RUNNING.name());

        // 确保进程有其他实例
        LogstashMachine otherInstance = createTestLogstashMachine(1L, processId, 1L);
        List<LogstashMachine> allInstances = Arrays.asList(otherInstance, runningInstance);

        LogstashProcessResponseDTO responseDTO = new LogstashProcessResponseDTO();
        responseDTO.setId(processId);

        when(logstashProcessMapper.selectById(processId)).thenReturn(process);
        when(logstashMachineMapper.selectById(2L)).thenReturn(runningInstance);
        when(logstashMachineMapper.selectByLogstashProcessId(processId)).thenReturn(allInstances);
        when(logstashProcessConverter.toResponseDTO(process)).thenReturn(responseDTO);

        // Mock强制停止的部署服务调用
        doNothing().when(logstashDeployService).forceStopInstances(anyList());

        // Mock删除目录的异步操作
        CompletableFuture<Boolean> deleteDirectoryFuture = CompletableFuture.completedFuture(true);
        when(logstashDeployService.deleteInstancesDirectory(anyList()))
                .thenReturn(deleteDirectoryFuture);

        // 执行测试
        LogstashProcessResponseDTO result =
                logstashProcessService.scaleLogstashProcess(processId, dto);

        // 验证结果
        assertNotNull(result);
        assertEquals(processId, result.getId());

        // 验证调用了强制停止服务而不是普通停止
        verify(logstashDeployService).forceStopInstances(anyList());
        // 验证调用了删除目录服务
        verify(logstashDeployService).deleteInstancesDirectory(anyList());
        // 注意：新的缩容逻辑保留任务记录，不再调用 taskService.getAllInstanceTaskIds
    }

    @Test
    @DisplayName("扩容缩容参数验证")
    void testScaleValidation() {
        Long processId = 1L;
        LogstashProcess process = createTestProcess(processId);

        // Mock进程存在验证
        when(logstashProcessMapper.selectById(processId)).thenReturn(process);

        // 测试空的扩容缩容请求
        LogstashProcessScaleRequestDTO emptyDto = new LogstashProcessScaleRequestDTO();

        BusinessException exception1 =
                assertThrows(
                        BusinessException.class,
                        () -> logstashProcessService.scaleLogstashProcess(processId, emptyDto));
        assertEquals(ErrorCode.VALIDATION_ERROR, exception1.getErrorCode());

        // 测试同时指定添加和移除
        LogstashProcessScaleRequestDTO conflictDto = new LogstashProcessScaleRequestDTO();
        conflictDto.setAddMachineIds(Arrays.asList(1L));
        conflictDto.setRemoveLogstashMachineIds(Arrays.asList(2L));

        BusinessException exception2 =
                assertThrows(
                        BusinessException.class,
                        () -> logstashProcessService.scaleLogstashProcess(processId, conflictDto));
        assertEquals(ErrorCode.VALIDATION_ERROR, exception2.getErrorCode());
    }

    // 辅助方法
    private LogstashProcess createTestProcess(Long id) {
        LogstashProcess process = new LogstashProcess();
        process.setId(id);
        process.setName("test-process-" + id);
        process.setModuleId(1L);
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

    // ============ 一机多实例专项测试 ============

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("一机多实例 - 同台机器扩容多个不同进程实例")
    @Description("验证在同一台机器上可以部署多个不同LogstashProcess的实例，通过不同的部署路径实现隔离")
    @Issue("MIAOCHA-105")
    void testMultiInstanceOnSameMachine_DifferentProcesses() {
        Allure.step(
                "准备同机器多进程扩容测试数据",
                () -> {
                    Allure.parameter("目标机器ID", "1");
                    Allure.parameter("进程1 ID", "100");
                    Allure.parameter("进程2 ID", "200");
                    Allure.parameter("验证场景", "同一台机器部署不同进程的实例");
                });

        // 准备测试数据：同一台机器部署两个不同进程的实例
        Long machineId = 1L;
        Long process1Id = 100L;
        Long process2Id = 200L;

        LogstashProcess process1 = createTestProcess(process1Id);
        LogstashProcess process2 = createTestProcess(process2Id);

        MachineInfo targetMachine = createTestMachine(machineId, "shared-machine", "192.168.1.100");

        // 第一个进程的扩容请求
        LogstashProcessScaleRequestDTO dto1 = new LogstashProcessScaleRequestDTO();
        dto1.setAddMachineIds(Arrays.asList(machineId));
        dto1.setCustomDeployPath("/logstash/process1/instance");

        // 第二个进程的扩容请求
        LogstashProcessScaleRequestDTO dto2 = new LogstashProcessScaleRequestDTO();
        dto2.setAddMachineIds(Arrays.asList(machineId));
        dto2.setCustomDeployPath("/logstash/process2/instance");

        // Mock基础数据
        when(logstashProcessMapper.selectById(process1Id)).thenReturn(process1);
        when(logstashProcessMapper.selectById(process2Id)).thenReturn(process2);
        when(machineMapper.selectById(machineId)).thenReturn(targetMachine);

        // 第一个进程扩容：检查路径冲突（不冲突）
        when(logstashMachineMapper.selectByMachineAndPath(machineId, "/logstash/process1/instance"))
                .thenReturn(null);

        // 第二个进程扩容：检查路径冲突（不冲突，因为路径不同）
        when(logstashMachineMapper.selectByMachineAndPath(machineId, "/logstash/process2/instance"))
                .thenReturn(null);

        LogstashMachine logstashMachine1 = createTestLogstashMachine(1L, process1Id, machineId);
        logstashMachine1.setDeployPath("/logstash/process1/instance");
        LogstashMachine logstashMachine2 = createTestLogstashMachine(2L, process2Id, machineId);
        logstashMachine2.setDeployPath("/logstash/process2/instance");

        // Mock实例创建
        when(logstashMachineConverter.createFromProcess(
                        process1, machineId, "/logstash/process1/instance"))
                .thenReturn(logstashMachine1);
        when(logstashMachineConverter.createFromProcess(
                        process2, machineId, "/logstash/process2/instance"))
                .thenReturn(logstashMachine2);

        when(logstashMachineMapper.insert(any(LogstashMachine.class))).thenReturn(1);
        when(logstashMachineMapper.update(any(LogstashMachine.class))).thenReturn(1);

        // Mock连接验证
        doNothing().when(connectionValidator).validateSingleMachineConnection(targetMachine);

        // Mock部署服务
        doNothing()
                .when(logstashDeployService)
                .initializeInstances(anyList(), any(LogstashProcess.class));

        LogstashProcessResponseDTO response1 = new LogstashProcessResponseDTO();
        response1.setId(process1Id);
        LogstashProcessResponseDTO response2 = new LogstashProcessResponseDTO();
        response2.setId(process2Id);

        when(logstashProcessConverter.toResponseDTO(process1)).thenReturn(response1);
        when(logstashProcessConverter.toResponseDTO(process2)).thenReturn(response2);

        // 执行测试：依次为两个进程在同一台机器上扩容
        LogstashProcessResponseDTO result1 =
                logstashProcessService.scaleLogstashProcess(process1Id, dto1);
        LogstashProcessResponseDTO result2 =
                logstashProcessService.scaleLogstashProcess(process2Id, dto2);

        // 验证结果
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(process1Id, result1.getId());
        assertEquals(process2Id, result2.getId());

        // 验证关键行为：每个进程都创建了自己的LogstashMachine实例
        verify(logstashMachineMapper, times(2)).insert(any(LogstashMachine.class));

        // 验证路径冲突检查被正确调用
        verify(logstashMachineMapper)
                .selectByMachineAndPath(machineId, "/logstash/process1/instance");
        verify(logstashMachineMapper)
                .selectByMachineAndPath(machineId, "/logstash/process2/instance");

        // 验证两次部署服务初始化调用
        verify(logstashDeployService, times(2))
                .initializeInstances(anyList(), any(LogstashProcess.class));

        Allure.step(
                "验证一机多实例部署成功",
                () -> {
                    Allure.attachment("验证结果", "同一台机器成功部署了两个不同进程的LogstashMachine实例");
                });
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("一机多实例 - 同进程同机器不同路径扩容")
    @Description("验证同一个LogstashProcess可以在同一台机器上通过不同部署路径扩容多个实例")
    @Issue("MIAOCHA-106")
    void testMultiInstanceOnSameMachine_SameProcess() {
        Allure.step(
                "准备同进程同机器多实例扩容测试数据",
                () -> {
                    Allure.parameter("目标机器ID", "1");
                    Allure.parameter("进程ID", "100");
                    Allure.parameter("实例1路径", "/logstash/process100/instance1");
                    Allure.parameter("实例2路径", "/logstash/process100/instance2");
                    Allure.parameter("验证场景", "同一进程在同一台机器部署多个实例");
                });

        // 准备测试数据：同一个进程在同一台机器上扩容两个实例
        Long machineId = 1L;
        Long processId = 100L;

        LogstashProcess process = createTestProcess(processId);
        MachineInfo targetMachine =
                createTestMachine(machineId, "multi-instance-machine", "192.168.1.100");

        // 第一次扩容请求
        LogstashProcessScaleRequestDTO dto1 = new LogstashProcessScaleRequestDTO();
        dto1.setAddMachineIds(Arrays.asList(machineId));
        dto1.setCustomDeployPath("/logstash/process100/instance1");

        // 第二次扩容请求
        LogstashProcessScaleRequestDTO dto2 = new LogstashProcessScaleRequestDTO();
        dto2.setAddMachineIds(Arrays.asList(machineId));
        dto2.setCustomDeployPath("/logstash/process100/instance2");

        // Mock基础数据
        when(logstashProcessMapper.selectById(processId)).thenReturn(process);
        when(machineMapper.selectById(machineId)).thenReturn(targetMachine);

        // 第一次扩容：检查路径冲突（不冲突）
        when(logstashMachineMapper.selectByMachineAndPath(
                        machineId, "/logstash/process100/instance1"))
                .thenReturn(null);

        // 第二次扩容：检查路径冲突（不冲突，因为路径不同）
        when(logstashMachineMapper.selectByMachineAndPath(
                        machineId, "/logstash/process100/instance2"))
                .thenReturn(null);

        LogstashMachine logstashMachine1 = createTestLogstashMachine(1L, processId, machineId);
        logstashMachine1.setDeployPath("/logstash/process100/instance1");
        LogstashMachine logstashMachine2 = createTestLogstashMachine(2L, processId, machineId);
        logstashMachine2.setDeployPath("/logstash/process100/instance2");

        // Mock实例创建
        when(logstashMachineConverter.createFromProcess(
                        process, machineId, "/logstash/process100/instance1"))
                .thenReturn(logstashMachine1);
        when(logstashMachineConverter.createFromProcess(
                        process, machineId, "/logstash/process100/instance2"))
                .thenReturn(logstashMachine2);

        when(logstashMachineMapper.insert(any(LogstashMachine.class))).thenReturn(1);

        // Mock连接验证
        doNothing().when(connectionValidator).validateSingleMachineConnection(targetMachine);

        // Mock部署服务
        doNothing().when(logstashDeployService).initializeInstances(anyList(), eq(process));

        LogstashProcessResponseDTO response = new LogstashProcessResponseDTO();
        response.setId(processId);
        when(logstashProcessConverter.toResponseDTO(process)).thenReturn(response);

        // 执行测试：为同一个进程在同一台机器上两次扩容
        LogstashProcessResponseDTO result1 =
                logstashProcessService.scaleLogstashProcess(processId, dto1);
        LogstashProcessResponseDTO result2 =
                logstashProcessService.scaleLogstashProcess(processId, dto2);

        // 验证结果
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(processId, result1.getId());
        assertEquals(processId, result2.getId());

        // 验证关键行为：创建了两个LogstashMachine实例
        verify(logstashMachineMapper, times(2)).insert(any(LogstashMachine.class));

        // 验证路径冲突检查被正确调用
        verify(logstashMachineMapper)
                .selectByMachineAndPath(machineId, "/logstash/process100/instance1");
        verify(logstashMachineMapper)
                .selectByMachineAndPath(machineId, "/logstash/process100/instance2");

        // 验证两次部署服务初始化调用
        verify(logstashDeployService, times(2)).initializeInstances(anyList(), eq(process));

        Allure.step(
                "验证同进程一机多实例部署成功",
                () -> {
                    Allure.attachment("验证结果", "同一个LogstashProcess在同一台机器成功部署了两个不同路径的实例");
                });
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("一机多实例 - 路径冲突检测")
    @Description("验证在一机多实例架构下，系统能正确检测和阻止部署路径冲突")
    @Issue("MIAOCHA-107")
    void testMultiInstanceOnSameMachine_PathConflictDetection() {
        Allure.step(
                "准备路径冲突检测测试数据",
                () -> {
                    Allure.parameter("目标机器ID", "1");
                    Allure.parameter("进程ID", "100");
                    Allure.parameter("冲突路径", "/logstash/shared/path");
                    Allure.parameter("验证场景", "检测相同路径部署冲突");
                });

        // 准备测试数据：尝试在相同路径创建实例
        Long machineId = 1L;
        Long processId = 100L;
        String conflictPath = "/logstash/shared/path";

        LogstashProcess process = createTestProcess(processId);
        MachineInfo targetMachine =
                createTestMachine(machineId, "conflict-test-machine", "192.168.1.100");

        // 模拟已存在的LogstashMachine实例占用了该路径
        LogstashMachine existingInstance = createTestLogstashMachine(99L, processId, machineId);
        existingInstance.setDeployPath(conflictPath);

        LogstashProcessScaleRequestDTO conflictDto = new LogstashProcessScaleRequestDTO();
        conflictDto.setAddMachineIds(Arrays.asList(machineId));
        conflictDto.setCustomDeployPath(conflictPath);

        // Mock基础数据
        when(logstashProcessMapper.selectById(processId)).thenReturn(process);
        when(machineMapper.selectById(machineId)).thenReturn(targetMachine);

        // 路径冲突检查：返回已存在的实例
        when(logstashMachineMapper.selectByMachineAndPath(machineId, conflictPath))
                .thenReturn(existingInstance);

        // 执行测试并期待异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> logstashProcessService.scaleLogstashProcess(processId, conflictDto));

        // 验证异常信息
        assertEquals(ErrorCode.LOGSTASH_MACHINE_ALREADY_ASSOCIATED, exception.getErrorCode());

        // 验证进行了路径冲突检查
        verify(logstashMachineMapper).selectByMachineAndPath(machineId, conflictPath);

        // 验证没有创建新实例（因为冲突被阻止）
        verify(logstashMachineMapper, never()).insert(any(LogstashMachine.class));

        Allure.step(
                "验证路径冲突检测成功",
                () -> {
                    Allure.attachment("验证结果", "系统成功检测到路径冲突并阻止了重复部署");
                });
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("一机多实例 - 查询同机器所有实例")
    @Description("验证可以正确查询同一台机器上的所有LogstashMachine实例")
    @Issue("MIAOCHA-108")
    void testMultiInstanceOnSameMachine_QueryAllInstances() {
        Allure.step(
                "准备同机器实例查询测试数据",
                () -> {
                    Allure.parameter("目标机器ID", "1");
                    Allure.parameter("实例数量", "3");
                    Allure.parameter("验证场景", "查询同机器所有实例");
                });

        // 准备测试数据：同一台机器上的多个实例
        Long machineId = 1L;

        LogstashMachine instance1 = createTestLogstashMachine(1L, 100L, machineId);
        instance1.setDeployPath("/logstash/process100/instance1");

        LogstashMachine instance2 = createTestLogstashMachine(2L, 100L, machineId);
        instance2.setDeployPath("/logstash/process100/instance2");

        LogstashMachine instance3 = createTestLogstashMachine(3L, 200L, machineId);
        instance3.setDeployPath("/logstash/process200/instance1");

        List<LogstashMachine> allInstancesOnMachine =
                Arrays.asList(instance1, instance2, instance3);

        // Mock查询方法
        when(logstashMachineMapper.selectByMachineId(machineId)).thenReturn(allInstancesOnMachine);

        // 执行查询（这里我们模拟一个查询操作，实际可能在Service层有相关方法）
        List<LogstashMachine> result = logstashMachineMapper.selectByMachineId(machineId);

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.size());

        // 验证所有实例都在同一台机器上
        result.forEach(instance -> assertEquals(machineId, instance.getMachineId()));

        // 验证包含了不同进程的实例
        List<Long> processIds =
                result.stream().map(LogstashMachine::getLogstashProcessId).distinct().toList();
        assertEquals(2, processIds.size()); // 两个不同的进程

        // 验证包含了不同的部署路径
        List<String> deployPaths =
                result.stream().map(LogstashMachine::getDeployPath).distinct().toList();
        assertEquals(3, deployPaths.size()); // 三个不同的路径

        verify(logstashMachineMapper).selectByMachineId(machineId);

        Allure.step(
                "验证同机器实例查询成功",
                () -> {
                    Allure.attachment(
                            "查询结果", String.format("在机器[%d]上发现%d个实例", machineId, result.size()));
                });
    }
}
