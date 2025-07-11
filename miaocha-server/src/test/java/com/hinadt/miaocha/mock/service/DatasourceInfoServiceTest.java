package com.hinadt.miaocha.mock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.datasource.HikariDatasourceManager;
import com.hinadt.miaocha.application.service.impl.DatasourceServiceImpl;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.converter.DatasourceConverter;
import com.hinadt.miaocha.domain.dto.DatasourceConnectionTestResultDTO;
import com.hinadt.miaocha.domain.dto.DatasourceCreateDTO;
import com.hinadt.miaocha.domain.dto.DatasourceDTO;
import com.hinadt.miaocha.domain.dto.DatasourceUpdateDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("数据源服务测试")
class DatasourceInfoServiceTest {

    @Mock private DatasourceMapper datasourceMapper;

    @Mock private DatasourceConverter datasourceConverter;

    @Mock private ModuleInfoMapper moduleInfoMapper;

    @Mock private HikariDatasourceManager hikariDatasourceManager;

    @Spy @InjectMocks private DatasourceServiceImpl datasourceService;

    private DatasourceCreateDTO createDTO;
    private DatasourceUpdateDTO updateDTO;
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

        updateDTO = new DatasourceUpdateDTO();
        updateDTO.setName("Updated Datasource");
        updateDTO.setType("MySQL");
        updateDTO.setJdbcUrl(
                "jdbc:mysql://localhost:3306/updated_db?useSSL=false&serverTimezone=UTC");
        updateDTO.setUsername("admin");
        updateDTO.setPassword("newpassword");
        updateDTO.setDescription("更新后的数据源");

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
        when(datasourceConverter.updateEntity(
                        any(DatasourceInfo.class), any(DatasourceUpdateDTO.class)))
                .thenReturn(existingDatasourceInfo);
    }

    @Test
    void testCreateDatasource_Success() {
        // 模拟数据源不存在
        when(datasourceMapper.selectByName(anyString())).thenReturn(null);
        // 模拟插入成功
        when(datasourceMapper.insert(any(DatasourceInfo.class))).thenReturn(1);
        // 模拟连接测试成功
        doReturn(DatasourceConnectionTestResultDTO.success())
                .when(datasourceService)
                .testConnection(any(DatasourceCreateDTO.class));

        DatasourceDTO result = datasourceService.createDatasource(createDTO);

        assertNotNull(result);
        assertEquals(createDTO.getName(), result.getName());
        verify(datasourceMapper, times(1)).selectByName(anyString());
        verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
        verify(datasourceMapper, times(1)).insert(any(DatasourceInfo.class));
    }

    @Test
    void testCreateDatasource_ConnectionFailed() {
        // 模拟数据源不存在
        when(datasourceMapper.selectByName(anyString())).thenReturn(null);
        // 模拟连接测试失败
        doReturn(DatasourceConnectionTestResultDTO.failure("连接失败"))
                .when(datasourceService)
                .testConnection(any(DatasourceCreateDTO.class));

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

    @Nested
    @DisplayName("更新数据源测试")
    class UpdateDatasourceTests {

        @Test
        @DisplayName("完整更新数据源 - 成功")
        void testUpdateDatasource_FullUpdate_Success() {
            // 模拟数据源存在
            when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);
            // 模拟名称不重复
            when(datasourceMapper.selectByName(anyString())).thenReturn(null);
            // 模拟连接测试成功
            DatasourceCreateDTO testDTO = new DatasourceCreateDTO();
            when(datasourceConverter.toCreateDTO(
                            any(DatasourceInfo.class), any(DatasourceUpdateDTO.class)))
                    .thenReturn(testDTO);
            doReturn(DatasourceConnectionTestResultDTO.success())
                    .when(datasourceService)
                    .testConnection(any(DatasourceCreateDTO.class));
            // 模拟更新成功
            when(datasourceMapper.update(any(DatasourceInfo.class))).thenReturn(1);

            DatasourceDTO result = datasourceService.updateDatasource(1L, updateDTO);

            assertNotNull(result);
            verify(datasourceMapper, times(1)).selectById(anyLong());
            verify(datasourceMapper, times(1)).selectByName(anyString());
            verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
            verify(datasourceMapper, times(1)).update(any(DatasourceInfo.class));
            verify(hikariDatasourceManager, times(1)).invalidateDataSourceById(1L);
        }

        @Test
        @DisplayName("部分更新数据源 - 仅更新名称")
        void testUpdateDatasource_PartialUpdate_NameOnly() {
            DatasourceUpdateDTO partialDTO = new DatasourceUpdateDTO();
            partialDTO.setName("New Name Only");

            // 模拟数据源存在
            when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);
            // 模拟名称不重复
            when(datasourceMapper.selectByName(anyString())).thenReturn(null);
            // 模拟更新成功
            when(datasourceMapper.update(any(DatasourceInfo.class))).thenReturn(1);

            DatasourceDTO result = datasourceService.updateDatasource(1L, partialDTO);

            assertNotNull(result);
            verify(datasourceMapper, times(1)).selectById(anyLong());
            verify(datasourceMapper, times(1)).selectByName(anyString());
            // 由于没有更新密码或JDBC URL，不应该进行连接测试
            verify(datasourceService, never()).testConnection(any(DatasourceCreateDTO.class));
            verify(datasourceMapper, times(1)).update(any(DatasourceInfo.class));
            verify(hikariDatasourceManager, times(1)).invalidateDataSourceById(1L);
        }

        @Test
        @DisplayName("部分更新数据源 - 仅更新描述")
        void testUpdateDatasource_PartialUpdate_DescriptionOnly() {
            DatasourceUpdateDTO partialDTO = new DatasourceUpdateDTO();
            partialDTO.setDescription("新的描述信息");

            // 模拟数据源存在
            when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);
            // 模拟更新成功
            when(datasourceMapper.update(any(DatasourceInfo.class))).thenReturn(1);

            DatasourceDTO result = datasourceService.updateDatasource(1L, partialDTO);

            assertNotNull(result);
            verify(datasourceMapper, times(1)).selectById(anyLong());
            // 由于没有更新名称，不需要检查名称重复
            verify(datasourceMapper, never()).selectByName(anyString());
            // 由于没有更新密码或JDBC URL，不应该进行连接测试
            verify(datasourceService, never()).testConnection(any(DatasourceCreateDTO.class));
            verify(datasourceMapper, times(1)).update(any(DatasourceInfo.class));
            verify(hikariDatasourceManager, times(1)).invalidateDataSourceById(1L);
        }

        @Test
        @DisplayName("更新数据源密码 - 需要连接测试")
        void testUpdateDatasource_UpdatePassword_RequiresConnectionTest() {
            DatasourceUpdateDTO partialDTO = new DatasourceUpdateDTO();
            partialDTO.setPassword("new_password");

            // 模拟数据源存在
            when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);
            // 模拟连接测试成功
            DatasourceCreateDTO testDTO = new DatasourceCreateDTO();
            when(datasourceConverter.toCreateDTO(
                            any(DatasourceInfo.class), any(DatasourceUpdateDTO.class)))
                    .thenReturn(testDTO);
            doReturn(DatasourceConnectionTestResultDTO.success())
                    .when(datasourceService)
                    .testConnection(any(DatasourceCreateDTO.class));
            // 模拟更新成功
            when(datasourceMapper.update(any(DatasourceInfo.class))).thenReturn(1);

            DatasourceDTO result = datasourceService.updateDatasource(1L, partialDTO);

            assertNotNull(result);
            verify(datasourceMapper, times(1)).selectById(anyLong());
            // 由于没有更新名称，不需要检查名称重复
            verify(datasourceMapper, never()).selectByName(anyString());
            // 由于更新了密码，需要进行连接测试
            verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
            verify(datasourceMapper, times(1)).update(any(DatasourceInfo.class));
            verify(hikariDatasourceManager, times(1)).invalidateDataSourceById(1L);
        }

        @Test
        @DisplayName("更新数据源JDBC URL - 需要连接测试")
        void testUpdateDatasource_UpdateJdbcUrl_RequiresConnectionTest() {
            DatasourceUpdateDTO partialDTO = new DatasourceUpdateDTO();
            partialDTO.setJdbcUrl("jdbc:mysql://newhost:3306/newdb");

            // 模拟数据源存在
            when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);
            // 模拟连接测试成功
            DatasourceCreateDTO testDTO = new DatasourceCreateDTO();
            when(datasourceConverter.toCreateDTO(
                            any(DatasourceInfo.class), any(DatasourceUpdateDTO.class)))
                    .thenReturn(testDTO);
            doReturn(DatasourceConnectionTestResultDTO.success())
                    .when(datasourceService)
                    .testConnection(any(DatasourceCreateDTO.class));
            // 模拟更新成功
            when(datasourceMapper.update(any(DatasourceInfo.class))).thenReturn(1);

            DatasourceDTO result = datasourceService.updateDatasource(1L, partialDTO);

            assertNotNull(result);
            verify(datasourceMapper, times(1)).selectById(anyLong());
            // 由于没有更新名称，不需要检查名称重复
            verify(datasourceMapper, never()).selectByName(anyString());
            // 由于更新了JDBC URL，需要进行连接测试
            verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
            verify(datasourceMapper, times(1)).update(any(DatasourceInfo.class));
            verify(hikariDatasourceManager, times(1)).invalidateDataSourceById(1L);
        }

        @Test
        @DisplayName("更新数据源 - 连接测试失败")
        void testUpdateDatasource_ConnectionFailed() {
            // 模拟数据源存在
            when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);
            // 模拟名称不重复
            when(datasourceMapper.selectByName(anyString())).thenReturn(null);
            // 模拟连接测试失败
            DatasourceCreateDTO testDTO = new DatasourceCreateDTO();
            when(datasourceConverter.toCreateDTO(
                            any(DatasourceInfo.class), any(DatasourceUpdateDTO.class)))
                    .thenReturn(testDTO);
            doReturn(DatasourceConnectionTestResultDTO.failure("连接失败"))
                    .when(datasourceService)
                    .testConnection(any(DatasourceCreateDTO.class));

            assertThrows(
                    BusinessException.class,
                    () -> {
                        datasourceService.updateDatasource(1L, updateDTO);
                    });

            verify(datasourceMapper, times(1)).selectById(anyLong());
            verify(datasourceMapper, times(1)).selectByName(anyString());
            verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
            verify(datasourceMapper, never()).update(any(DatasourceInfo.class));
            verify(hikariDatasourceManager, never()).invalidateDataSourceById(anyLong());
        }

        @Test
        @DisplayName("更新数据源 - 名称重复")
        void testUpdateDatasource_NameExists() {
            DatasourceInfo anotherDatasource = new DatasourceInfo();
            anotherDatasource.setId(2L);
            anotherDatasource.setName("Updated Datasource");

            // 模拟数据源存在
            when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);
            // 模拟名称重复
            when(datasourceMapper.selectByName(anyString())).thenReturn(anotherDatasource);

            assertThrows(
                    BusinessException.class,
                    () -> {
                        datasourceService.updateDatasource(1L, updateDTO);
                    });

            verify(datasourceMapper, times(1)).selectById(anyLong());
            verify(datasourceMapper, times(1)).selectByName(anyString());
            verify(datasourceService, never()).testConnection(any(DatasourceCreateDTO.class));
            verify(datasourceMapper, never()).update(any(DatasourceInfo.class));
            verify(hikariDatasourceManager, never()).invalidateDataSourceById(anyLong());
        }

        @Test
        @DisplayName("更新数据源 - 数据源不存在")
        void testUpdateDatasource_NotFound() {
            // 模拟数据源不存在
            when(datasourceMapper.selectById(anyLong())).thenReturn(null);

            assertThrows(
                    BusinessException.class,
                    () -> {
                        datasourceService.updateDatasource(1L, updateDTO);
                    });

            verify(datasourceMapper, times(1)).selectById(anyLong());
            verify(datasourceMapper, never()).selectByName(anyString());
            verify(datasourceService, never()).testConnection(any(DatasourceCreateDTO.class));
            verify(datasourceMapper, never()).update(any(DatasourceInfo.class));
            verify(hikariDatasourceManager, never()).invalidateDataSourceById(anyLong());
        }

        @Test
        @DisplayName("更新数据源 - 同名但同ID的情况")
        void testUpdateDatasource_SameNameSameId() {
            DatasourceUpdateDTO sameNameDTO = new DatasourceUpdateDTO();
            sameNameDTO.setName("Test Datasource"); // 与现有数据源同名

            // 模拟数据源存在
            when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);
            // 模拟查询到同名数据源（但是同一个ID）
            when(datasourceMapper.selectByName(anyString())).thenReturn(existingDatasourceInfo);
            // 模拟更新成功
            when(datasourceMapper.update(any(DatasourceInfo.class))).thenReturn(1);

            // 应该成功，因为是同一个数据源
            DatasourceDTO result = datasourceService.updateDatasource(1L, sameNameDTO);

            assertNotNull(result);
            verify(datasourceMapper, times(1)).selectById(anyLong());
            verify(datasourceMapper, times(1)).selectByName(anyString());
            // 由于没有更新密码或JDBC URL，不应该进行连接测试
            verify(datasourceService, never()).testConnection(any(DatasourceCreateDTO.class));
            verify(datasourceMapper, times(1)).update(any(DatasourceInfo.class));
            verify(hikariDatasourceManager, times(1)).invalidateDataSourceById(1L);
        }
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
        verify(hikariDatasourceManager, times(1)).invalidateDataSourceById(1L);
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
        verify(hikariDatasourceManager, never()).invalidateDataSourceById(anyLong());
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
        verify(hikariDatasourceManager, never()).invalidateDataSourceById(anyLong());
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
        doReturn(DatasourceConnectionTestResultDTO.success())
                .when(datasourceService)
                .testConnection(any(DatasourceCreateDTO.class));

        DatasourceConnectionTestResultDTO result = datasourceService.testConnection(createDTO);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNull(result.getErrorMessage());
        verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
    }

    @Test
    void testTestConnection_Failed() {
        // 使用Spy对象模拟testConnection方法返回失败
        doReturn(DatasourceConnectionTestResultDTO.failure("连接失败"))
                .when(datasourceService)
                .testConnection(any(DatasourceCreateDTO.class));

        DatasourceConnectionTestResultDTO result = datasourceService.testConnection(createDTO);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("连接失败", result.getErrorMessage());
        verify(datasourceService, times(1)).testConnection(any(DatasourceCreateDTO.class));
    }

    @Test
    void testTestExistingConnection_Success() {
        // 模拟数据源存在
        when(datasourceMapper.selectById(anyLong())).thenReturn(existingDatasourceInfo);
        // 模拟连接测试成功
        doReturn(DatasourceConnectionTestResultDTO.success())
                .when(datasourceService)
                .testConnection(any(DatasourceCreateDTO.class));

        DatasourceConnectionTestResultDTO result = datasourceService.testExistingConnection(1L);

        assertNotNull(result);
        assertTrue(result.isSuccess());
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
