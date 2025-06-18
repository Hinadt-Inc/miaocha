package com.hinadt.miaocha.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.impl.DatasourceServiceImpl;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.converter.DatasourceConverter;
import com.hinadt.miaocha.domain.dto.DatasourceCreateDTO;
import com.hinadt.miaocha.domain.dto.DatasourceDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import io.qameta.allure.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 数据源服务单元测试
 *
 * <p>测试秒查系统的数据源管理功能，包括数据源的创建、查询、更新等操作
 */
@Epic("秒查日志管理系统")
@Feature("数据源管理")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("数据源服务测试")
@Owner("开发团队")
class DatasourceInfoServiceTest {

    @Mock private DatasourceMapper datasourceMapper;

    @Mock private DatasourceConverter datasourceConverter;

    @Mock private ModuleInfoMapper moduleInfoMapper;

    @Spy @InjectMocks private DatasourceServiceImpl datasourceService;

    private DatasourceCreateDTO createDTO;
    private DatasourceInfo existingDatasourceInfo;
    private DatasourceDTO datasourceDTO;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        createDTO = new DatasourceCreateDTO();
        createDTO.setName("Test Datasource");
        createDTO.setType("MySQL");
        createDTO.setJdbcUrl("jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC");
        createDTO.setUsername("root");
        createDTO.setPassword("password");

        existingDatasourceInfo = new DatasourceInfo();
        existingDatasourceInfo.setId(1L);
        existingDatasourceInfo.setName("Test Datasource");
        existingDatasourceInfo.setType("MySQL");
        existingDatasourceInfo.setJdbcUrl(
                "jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC");
        existingDatasourceInfo.setUsername("root");
        existingDatasourceInfo.setPassword("password");
        existingDatasourceInfo.setCreateTime(LocalDateTime.now());
        existingDatasourceInfo.setUpdateTime(LocalDateTime.now());

        datasourceDTO = new DatasourceDTO();
        datasourceDTO.setId(existingDatasourceInfo.getId());
        datasourceDTO.setName(existingDatasourceInfo.getName());
        datasourceDTO.setType(existingDatasourceInfo.getType());
        datasourceDTO.setJdbcUrl(existingDatasourceInfo.getJdbcUrl());

        // 设置转换器行为
        when(datasourceConverter.toDto(any(DatasourceInfo.class))).thenReturn(datasourceDTO);
        when(datasourceConverter.toEntity(any(DatasourceCreateDTO.class)))
                .thenReturn(existingDatasourceInfo);
        when(datasourceConverter.updateEntity(
                        any(DatasourceInfo.class), any(DatasourceCreateDTO.class)))
                .thenReturn(existingDatasourceInfo);
    }

    @Test
    @Story("数据源创建")
    @Description("测试成功创建数据源的功能")
    @Severity(SeverityLevel.CRITICAL)
    void testCreateDatasource_Success() {
        Allure.step(
                "准备测试数据",
                () -> {
                    Allure.parameter("数据源名称", createDTO.getName());
                    Allure.parameter("数据源类型", createDTO.getType());
                    Allure.parameter("连接地址", createDTO.getJdbcUrl());
                });

        Allure.step(
                "模拟外部依赖",
                () -> {
                    // 模拟数据源不存在
                    when(datasourceMapper.selectByName(anyString())).thenReturn(null);
                    // 模拟插入成功
                    when(datasourceMapper.insert(any(DatasourceInfo.class))).thenReturn(1);
                    // 模拟连接测试成功
                    doReturn(true)
                            .when(datasourceService)
                            .testConnection(any(DatasourceCreateDTO.class));
                });

        DatasourceDTO result =
                Allure.step(
                        "执行数据源创建",
                        () -> {
                            return datasourceService.createDatasource(createDTO);
                        });

        Allure.step(
                "验证创建结果",
                () -> {
                    assertNotNull(result);
                    assertEquals(createDTO.getName(), result.getName());
                    verify(datasourceMapper, times(1)).selectByName(anyString());
                    verify(datasourceMapper, times(1)).insert(any(DatasourceInfo.class));
                    verify(datasourceService, times(1))
                            .testConnection(any(DatasourceCreateDTO.class));

                    Allure.attachment(
                            "创建结果", "数据源ID: " + result.getId() + ", 名称: " + result.getName());
                });
    }

    @Test
    void testCreateDatasource_ConnectionFailed() {
        // 模拟数据源不存在
        when(datasourceMapper.selectByName(anyString())).thenReturn(null);
        // 模拟连接测试失败
        doReturn(false).when(datasourceService).testConnection(any(DatasourceCreateDTO.class));

        assertThrows(
                BusinessException.class,
                () -> {
                    datasourceService.createDatasource(createDTO);
                });

        verify(datasourceMapper, times(1)).selectByName(anyString());
        verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
        verify(datasourceMapper, never()).insert(any(DatasourceInfo.class));
    }

    @Test
    void testCreateDatasource_NameExists() {
        // 模拟数据源已存在
        when(datasourceMapper.selectByName(anyString())).thenReturn(existingDatasourceInfo);

        assertThrows(
                BusinessException.class,
                () -> {
                    datasourceService.createDatasource(createDTO);
                });

        verify(datasourceMapper, times(1)).selectByName(anyString());
        verify(datasourceService, never()).testConnection(any(DatasourceCreateDTO.class));
        verify(datasourceMapper, never()).insert(any(DatasourceInfo.class));
    }

    @Test
    void testUpdateDatasource_Success() {
        // 模拟数据源存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);
        // 模拟名称不重复
        when(datasourceMapper.selectByName(anyString())).thenReturn(null);
        // 模拟连接测试成功
        doReturn(true).when(datasourceService).testConnection(any(DatasourceCreateDTO.class));
        // 模拟更新成功
        when(datasourceMapper.update(any(DatasourceInfo.class))).thenReturn(1);

        DatasourceDTO result = datasourceService.updateDatasource(1L, createDTO);

        assertNotNull(result);
        assertEquals(createDTO.getName(), result.getName());
        verify(datasourceMapper, times(1)).selectById(anyLong());
        verify(datasourceMapper, times(1)).selectByName(anyString());
        verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
        verify(datasourceMapper, times(1)).update(any(DatasourceInfo.class));
    }

    @Test
    void testUpdateDatasource_ConnectionFailed() {
        // 模拟数据源存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);
        // 模拟名称不重复
        when(datasourceMapper.selectByName(anyString())).thenReturn(null);
        // 模拟连接测试失败
        doReturn(false).when(datasourceService).testConnection(any(DatasourceCreateDTO.class));

        assertThrows(
                BusinessException.class,
                () -> {
                    datasourceService.updateDatasource(1L, createDTO);
                });

        verify(datasourceMapper, times(1)).selectById(anyLong());
        verify(datasourceMapper, times(1)).selectByName(anyString());
        verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
        verify(datasourceMapper, never()).update(any(DatasourceInfo.class));
    }

    @Test
    void testUpdateDatasource_NotFound() {
        // 模拟数据源不存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(null);

        assertThrows(
                BusinessException.class,
                () -> {
                    datasourceService.updateDatasource(1L, createDTO);
                });

        verify(datasourceMapper, times(1)).selectById(anyLong());
        verify(datasourceMapper, never()).selectByName(anyString());
        verify(datasourceService, never()).testConnection(any(DatasourceCreateDTO.class));
        verify(datasourceMapper, never()).update(any(DatasourceInfo.class));
    }

    @Test
    void testDeleteDatasource_Success() {
        // 模拟数据源存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);
        // 模拟数据源未被模块引用
        when(moduleInfoMapper.existsByDatasourceId(anyLong())).thenReturn(false);
        // 模拟删除成功
        when(datasourceMapper.deleteById(anyLong())).thenReturn(1);

        assertDoesNotThrow(
                () -> {
                    datasourceService.deleteDatasource(1L);
                });

        verify(datasourceMapper, times(1)).selectById(anyLong());
        verify(moduleInfoMapper, times(1)).existsByDatasourceId(anyLong());
        verify(datasourceMapper, times(1)).deleteById(anyLong());
    }

    @Test
    void testDeleteDatasource_NotFound() {
        // 模拟数据源不存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(null);

        assertThrows(
                BusinessException.class,
                () -> {
                    datasourceService.deleteDatasource(1L);
                });

        verify(datasourceMapper, times(1)).selectById(anyLong());
        verify(moduleInfoMapper, never()).existsByDatasourceId(anyLong());
        verify(datasourceMapper, never()).deleteById(anyLong());
    }

    @Test
    void testDeleteDatasource_InUse() {
        // 模拟数据源存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);
        // 模拟数据源被模块引用
        when(moduleInfoMapper.existsByDatasourceId(anyLong())).thenReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            datasourceService.deleteDatasource(1L);
                        });

        assertEquals(ErrorCode.DATASOURCE_IN_USE, exception.getErrorCode());
        verify(datasourceMapper, times(1)).selectById(anyLong());
        verify(moduleInfoMapper, times(1)).existsByDatasourceId(anyLong());
        verify(datasourceMapper, never()).deleteById(anyLong());
    }

    @Test
    void testGetDatasource_Success() {
        // 模拟数据源存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);

        DatasourceDTO result = datasourceService.getDatasource(1L);

        assertNotNull(result);
        assertEquals(existingDatasourceInfo.getName(), result.getName());
        verify(datasourceMapper, times(1)).selectById(anyLong());
    }

    @Test
    void testGetDatasource_NotFound() {
        // 模拟数据源不存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(null);

        assertThrows(
                BusinessException.class,
                () -> {
                    datasourceService.getDatasource(1L);
                });

        verify(datasourceMapper, times(1)).selectById(anyLong());
    }

    @Test
    void testGetAllDatasources() {
        // 模拟返回数据源列表
        List<DatasourceInfo> datasourceInfos = Arrays.asList(existingDatasourceInfo);
        when(datasourceMapper.selectAll()).thenReturn(datasourceInfos);

        List<DatasourceDTO> result = datasourceService.getAllDatasources();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(existingDatasourceInfo.getName(), result.get(0).getName());
        verify(datasourceMapper, times(1)).selectAll();
    }

    @Test
    void testTestConnection() {
        // 使用Spy对象模拟testConnection方法返回成功
        doReturn(true).when(datasourceService).testConnection(any(DatasourceCreateDTO.class));

        boolean result = datasourceService.testConnection(createDTO);

        assertTrue(result);
        verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
    }

    @Test
    void testTestExistingConnection_Success() {
        // 模拟数据源存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);
        // 模拟连接测试成功
        doReturn(true).when(datasourceService).testConnection(any(DatasourceCreateDTO.class));

        boolean result = datasourceService.testExistingConnection(1L);

        assertTrue(result);
        verify(datasourceMapper, times(1)).selectById(anyLong());
        verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
    }

    @Test
    void testTestExistingConnection_NotFound() {
        // 模拟数据源不存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(null);

        assertThrows(
                BusinessException.class,
                () -> {
                    datasourceService.testExistingConnection(1L);
                });

        verify(datasourceMapper, times(1)).selectById(anyLong());
        verify(datasourceService, never()).testConnection(any(DatasourceCreateDTO.class));
    }
}
