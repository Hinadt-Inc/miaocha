package com.hinadt.miaocha.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.impl.ModuleInfoServiceImpl;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.converter.ModuleInfoConverter;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoCreateDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.LogstashProcessMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import io.qameta.allure.*;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ModuleInfo服务测试类 */
@ExtendWith(MockitoExtension.class)
@Feature("模块管理")
@DisplayName("模块管理服务测试")
class ModuleInfoServiceTest {

    @Mock private ModuleInfoMapper moduleInfoMapper;
    @Mock private DatasourceMapper datasourceMapper;
    @Mock private JdbcQueryExecutor jdbcQueryExecutor;
    @Mock private ModuleInfoConverter moduleInfoConverter;
    @Mock private LogstashProcessMapper logstashProcessMapper;

    @org.mockito.InjectMocks private ModuleInfoServiceImpl moduleInfoService;

    private ModuleInfo sampleModuleInfo;
    private ModuleInfoDTO sampleModuleInfoDTO;
    private DatasourceInfo sampleDatasourceInfo;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        // 数据源信息
        sampleDatasourceInfo = new DatasourceInfo();
        sampleDatasourceInfo.setId(1L);
        sampleDatasourceInfo.setName("Test Doris");
        sampleDatasourceInfo.setType("DORIS");

        // 模块信息
        sampleModuleInfo = new ModuleInfo();
        sampleModuleInfo.setId(1L);
        sampleModuleInfo.setName("Test Module");
        sampleModuleInfo.setDatasourceId(1L);
        sampleModuleInfo.setTableName("test_logs");
        sampleModuleInfo.setCreateTime(LocalDateTime.now());
        sampleModuleInfo.setUpdateTime(LocalDateTime.now());
        sampleModuleInfo.setCreateUser("admin@test.com");
        sampleModuleInfo.setUpdateUser("admin@test.com");

        // 模块DTO
        sampleModuleInfoDTO = new ModuleInfoDTO();
        sampleModuleInfoDTO.setId(1L);
        sampleModuleInfoDTO.setName("Test Module");
        sampleModuleInfoDTO.setDatasourceId(1L);
        sampleModuleInfoDTO.setTableName("test_logs");
        sampleModuleInfoDTO.setDatasourceName("Test Doris");
        sampleModuleInfoDTO.setCreateTime(LocalDateTime.now());
        sampleModuleInfoDTO.setUpdateTime(LocalDateTime.now());
        sampleModuleInfoDTO.setCreateUser("admin@test.com");
        sampleModuleInfoDTO.setUpdateUser("admin@test.com");
        sampleModuleInfoDTO.setCreateUserName("管理员");
        sampleModuleInfoDTO.setUpdateUserName("管理员");
    }

    @Test
    @Story("创建模块")
    @Description("测试成功创建模块的功能")
    @Severity(SeverityLevel.CRITICAL)
    void testCreateModule_Success() {
        // 准备测试数据
        ModuleInfoCreateDTO createDTO = new ModuleInfoCreateDTO();
        createDTO.setName("New Module");
        createDTO.setDatasourceId(1L);
        createDTO.setTableName("new_logs");
        // 注意：不再设置dorisSql字段，因为创建时不应该设置

        // Mock 行为
        when(moduleInfoMapper.existsByName("New Module", null)).thenReturn(false);
        when(datasourceMapper.selectById(1L)).thenReturn(sampleDatasourceInfo);
        when(moduleInfoConverter.toEntity(createDTO)).thenReturn(sampleModuleInfo);
        when(moduleInfoMapper.insert(sampleModuleInfo)).thenReturn(1);
        when(moduleInfoConverter.toDto(sampleModuleInfo, sampleDatasourceInfo))
                .thenReturn(sampleModuleInfoDTO);

        // 执行测试
        ModuleInfoDTO result = moduleInfoService.createModule(createDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals("Test Module", result.getName());
        assertEquals(1L, result.getDatasourceId());
        assertEquals("test_logs", result.getTableName());

        // 验证调用
        verify(moduleInfoMapper).existsByName("New Module", null);
        verify(datasourceMapper).selectById(1L);
        verify(moduleInfoConverter).toEntity(createDTO);
        verify(moduleInfoMapper).insert(sampleModuleInfo);
        verify(moduleInfoConverter).toDto(sampleModuleInfo, sampleDatasourceInfo);
    }

    @Test
    @Story("创建模块")
    @Description("测试模块名称已存在时抛出异常")
    @Severity(SeverityLevel.NORMAL)
    void testCreateModule_NameExists() {
        // 准备测试数据
        ModuleInfoCreateDTO createDTO = new ModuleInfoCreateDTO();
        createDTO.setName("Existing Module");

        // Mock 行为
        when(moduleInfoMapper.existsByName("Existing Module", null)).thenReturn(true);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> moduleInfoService.createModule(createDTO));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("模块名称已存在", exception.getMessage());

        // 验证只调用了名称检查
        verify(moduleInfoMapper).existsByName("Existing Module", null);
        verify(datasourceMapper, never()).selectById(any());
    }

    @Test
    @Story("查询模块")
    @Description("测试根据ID查询单个模块")
    @Severity(SeverityLevel.CRITICAL)
    void testGetModuleById_Success() {
        // Mock 行为
        when(moduleInfoMapper.selectById(1L)).thenReturn(sampleModuleInfo);
        when(datasourceMapper.selectById(1L)).thenReturn(sampleDatasourceInfo);
        when(moduleInfoConverter.toDto(sampleModuleInfo, sampleDatasourceInfo))
                .thenReturn(sampleModuleInfoDTO);

        // 执行测试
        ModuleInfoDTO result = moduleInfoService.getModuleById(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Module", result.getName());

        // 验证调用
        verify(moduleInfoMapper).selectById(1L);
        verify(datasourceMapper).selectById(1L);
    }

    @Test
    @Story("删除模块")
    @Description("测试成功删除模块")
    @Severity(SeverityLevel.CRITICAL)
    void testDeleteModule_Success() {
        // Mock 行为
        when(moduleInfoMapper.selectById(1L)).thenReturn(sampleModuleInfo);
        when(logstashProcessMapper.countByModuleId(1L)).thenReturn(0);
        when(moduleInfoMapper.deleteById(1L)).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> moduleInfoService.deleteModule(1L));

        // 验证调用
        verify(moduleInfoMapper).selectById(1L);
        verify(logstashProcessMapper).countByModuleId(1L);
        verify(moduleInfoMapper).deleteById(1L);
    }

    @Test
    @Story("执行Doris SQL")
    @Description("测试成功执行Doris SQL")
    @Severity(SeverityLevel.CRITICAL)
    void testExecuteDorisSql_Success() throws Exception {
        // 准备测试数据
        String sql = "CREATE TABLE test_table (id INT, name VARCHAR(100))";
        ModuleInfo moduleWithoutSql = new ModuleInfo();
        moduleWithoutSql.setId(1L);
        moduleWithoutSql.setDatasourceId(1L);
        moduleWithoutSql.setDorisSql(null); // 还没有执行过SQL

        Connection mockConnection = mock(Connection.class);

        // Mock 行为
        when(moduleInfoMapper.selectById(1L)).thenReturn(moduleWithoutSql);
        when(datasourceMapper.selectById(1L)).thenReturn(sampleDatasourceInfo);
        when(jdbcQueryExecutor.getConnection(sampleDatasourceInfo)).thenReturn(mockConnection);
        when(jdbcQueryExecutor.executeRawQuery(mockConnection, sql)).thenReturn(new HashMap<>());
        when(moduleInfoMapper.update(any(ModuleInfo.class))).thenReturn(1);
        when(moduleInfoConverter.toDto(any(ModuleInfo.class), eq(sampleDatasourceInfo)))
                .thenReturn(sampleModuleInfoDTO);

        // 执行测试
        ModuleInfoDTO result = moduleInfoService.executeDorisSql(1L, sql);

        // 验证结果
        assertNotNull(result);

        // 验证调用
        verify(moduleInfoMapper).selectById(1L);
        verify(datasourceMapper).selectById(1L);
        verify(jdbcQueryExecutor).getConnection(sampleDatasourceInfo);
        verify(jdbcQueryExecutor).executeRawQuery(mockConnection, sql);
        verify(moduleInfoMapper).update(any(ModuleInfo.class));
    }
}
