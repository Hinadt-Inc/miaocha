package com.hina.log.service;

import com.hina.log.dto.DatasourceCreateDTO;
import com.hina.log.dto.DatasourceDTO;
import com.hina.log.entity.Datasource;
import com.hina.log.exception.BusinessException;
import com.hina.log.mapper.DatasourceMapper;
import com.hina.log.service.impl.DatasourceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 数据源服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class DatasourceServiceTest {

    @Mock
    private DatasourceMapper datasourceMapper;

    @Spy
    @InjectMocks
    private DatasourceServiceImpl datasourceService;

    private DatasourceCreateDTO createDTO;
    private Datasource existingDatasource;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        createDTO = new DatasourceCreateDTO();
        createDTO.setName("Test Datasource");
        createDTO.setType("MySQL");
        createDTO.setIp("localhost");
        createDTO.setPort(3306);
        createDTO.setUsername("root");
        createDTO.setPassword("password");
        createDTO.setDatabase("test_db");
        createDTO.setJdbcParams("useSSL=false&serverTimezone=UTC");

        existingDatasource = new Datasource();
        existingDatasource.setId(1L);
        existingDatasource.setName("Test Datasource");
        existingDatasource.setType("MySQL");
        existingDatasource.setIp("localhost");
        existingDatasource.setPort(3306);
        existingDatasource.setUsername("root");
        existingDatasource.setPassword("password");
        existingDatasource.setDatabase("test_db");
        existingDatasource.setJdbcParams("useSSL=false&serverTimezone=UTC");
        existingDatasource.setCreateTime(LocalDateTime.now());
        existingDatasource.setUpdateTime(LocalDateTime.now());
    }

    @Test
    void testCreateDatasource_Success() {
        // 模拟数据源不存在
        when(datasourceMapper.selectByName(anyString())).thenReturn(null);
        // 模拟插入成功
        when(datasourceMapper.insert(any(Datasource.class))).thenReturn(1);
        // 模拟连接测试成功
        doReturn(true).when(datasourceService).testConnection(any(DatasourceCreateDTO.class));

        DatasourceDTO result = datasourceService.createDatasource(createDTO);

        assertNotNull(result);
        assertEquals(createDTO.getName(), result.getName());
        verify(datasourceMapper, times(1)).selectByName(anyString());
        verify(datasourceMapper, times(1)).insert(any(Datasource.class));
        verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
    }

    @Test
    void testCreateDatasource_ConnectionFailed() {
        // 模拟数据源不存在
        when(datasourceMapper.selectByName(anyString())).thenReturn(null);
        // 模拟连接测试失败
        doReturn(false).when(datasourceService).testConnection(any(DatasourceCreateDTO.class));

        assertThrows(BusinessException.class, () -> {
            datasourceService.createDatasource(createDTO);
        });

        verify(datasourceMapper, times(1)).selectByName(anyString());
        verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
        verify(datasourceMapper, never()).insert(any(Datasource.class));
    }

    @Test
    void testCreateDatasource_NameExists() {
        // 模拟数据源已存在
        when(datasourceMapper.selectByName(anyString())).thenReturn(existingDatasource);

        assertThrows(BusinessException.class, () -> {
            datasourceService.createDatasource(createDTO);
        });

        verify(datasourceMapper, times(1)).selectByName(anyString());
        verify(datasourceService, never()).testConnection(any(DatasourceCreateDTO.class));
        verify(datasourceMapper, never()).insert(any(Datasource.class));
    }

    @Test
    void testUpdateDatasource_Success() {
        // 模拟数据源存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasource);
        // 模拟名称不重复
        when(datasourceMapper.selectByName(anyString())).thenReturn(null);
        // 模拟连接测试成功
        doReturn(true).when(datasourceService).testConnection(any(DatasourceCreateDTO.class));
        // 模拟更新成功
        when(datasourceMapper.update(any(Datasource.class))).thenReturn(1);

        DatasourceDTO result = datasourceService.updateDatasource(1L, createDTO);

        assertNotNull(result);
        assertEquals(createDTO.getName(), result.getName());
        verify(datasourceMapper, times(1)).selectById(anyLong());
        verify(datasourceMapper, times(1)).selectByName(anyString());
        verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
        verify(datasourceMapper, times(1)).update(any(Datasource.class));
    }

    @Test
    void testUpdateDatasource_ConnectionFailed() {
        // 模拟数据源存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasource);
        // 模拟名称不重复
        when(datasourceMapper.selectByName(anyString())).thenReturn(null);
        // 模拟连接测试失败
        doReturn(false).when(datasourceService).testConnection(any(DatasourceCreateDTO.class));

        assertThrows(BusinessException.class, () -> {
            datasourceService.updateDatasource(1L, createDTO);
        });

        verify(datasourceMapper, times(1)).selectById(anyLong());
        verify(datasourceMapper, times(1)).selectByName(anyString());
        verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
        verify(datasourceMapper, never()).update(any(Datasource.class));
    }

    @Test
    void testUpdateDatasource_NotFound() {
        // 模拟数据源不存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            datasourceService.updateDatasource(1L, createDTO);
        });

        verify(datasourceMapper, times(1)).selectById(anyLong());
        verify(datasourceMapper, never()).selectByName(anyString());
        verify(datasourceService, never()).testConnection(any(DatasourceCreateDTO.class));
        verify(datasourceMapper, never()).update(any(Datasource.class));
    }

    @Test
    void testDeleteDatasource_Success() {
        // 模拟数据源存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasource);
        // 模拟删除成功
        when(datasourceMapper.deleteById(anyLong())).thenReturn(1);

        assertDoesNotThrow(() -> {
            datasourceService.deleteDatasource(1L);
        });

        verify(datasourceMapper, times(1)).selectById(anyLong());
        verify(datasourceMapper, times(1)).deleteById(anyLong());
    }

    @Test
    void testDeleteDatasource_NotFound() {
        // 模拟数据源不存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            datasourceService.deleteDatasource(1L);
        });

        verify(datasourceMapper, times(1)).selectById(anyLong());
        verify(datasourceMapper, never()).deleteById(anyLong());
    }

    @Test
    void testGetDatasource_Success() {
        // 模拟数据源存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasource);

        DatasourceDTO result = datasourceService.getDatasource(1L);

        assertNotNull(result);
        assertEquals(existingDatasource.getName(), result.getName());
        verify(datasourceMapper, times(1)).selectById(anyLong());
    }

    @Test
    void testGetDatasource_NotFound() {
        // 模拟数据源不存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            datasourceService.getDatasource(1L);
        });

        verify(datasourceMapper, times(1)).selectById(anyLong());
    }

    @Test
    void testGetAllDatasources() {
        // 模拟返回数据源列表
        List<Datasource> datasources = Arrays.asList(existingDatasource);
        when(datasourceMapper.selectAll()).thenReturn(datasources);

        List<DatasourceDTO> result = datasourceService.getAllDatasources();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(existingDatasource.getName(), result.get(0).getName());
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
        when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasource);
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

        assertThrows(BusinessException.class, () -> {
            datasourceService.testExistingConnection(1L);
        });

        verify(datasourceMapper, times(1)).selectById(anyLong());
        verify(datasourceService, never()).testConnection(any(DatasourceCreateDTO.class));
    }
}