package com.hinadt.miaocha.domain.converter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.domain.dto.module.ModuleInfoCreateDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoUpdateDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ModuleInfoConverter测试类 */
@ExtendWith(MockitoExtension.class)
@Feature("模块转换器")
@DisplayName("模块信息转换器测试")
class ModuleInfoConverterTest {

    @Mock private UserMapper userMapper;

    private ModuleInfoConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ModuleInfoConverter(userMapper);
    }

    @Test
    @Story("CreateDTO转Entity")
    @Description("测试将创建DTO转换为实体")
    @Severity(SeverityLevel.CRITICAL)
    void testToEntityFromCreateDTO() {
        // 准备测试数据
        ModuleInfoCreateDTO createDTO = new ModuleInfoCreateDTO();
        createDTO.setName("Test Module");
        createDTO.setDatasourceId(1L);
        createDTO.setTableName("test_logs");
        // 注意：不再设置dorisSql字段，因为创建时不应该设置

        // 执行转换
        ModuleInfo result = converter.toEntity(createDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals("Test Module", result.getName());
        assertEquals(1L, result.getDatasourceId());
        assertEquals("test_logs", result.getTableName());
        assertNull(result.getDorisSql()); // 创建时dorisSql应该为null
        assertNull(result.getId()); // ID在创建时应该为null
    }

    @Test
    @Story("CreateDTO转Entity")
    @Description("测试空CreateDTO转换返回null")
    @Severity(SeverityLevel.NORMAL)
    void testToEntityFromCreateDTO_Null() {
        ModuleInfo result = converter.toEntity((ModuleInfoCreateDTO) null);
        assertNull(result);
    }

    @Test
    @Story("UpdateDTO转Entity")
    @Description("测试将更新DTO转换为实体")
    @Severity(SeverityLevel.CRITICAL)
    void testToEntityFromUpdateDTO() {
        // 准备测试数据
        ModuleInfoUpdateDTO updateDTO = new ModuleInfoUpdateDTO();
        updateDTO.setId(1L);
        updateDTO.setName("Updated Module");
        updateDTO.setDatasourceId(2L);
        updateDTO.setTableName("updated_logs");

        // 执行转换
        ModuleInfo result = converter.toEntity(updateDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Updated Module", result.getName());
        assertEquals(2L, result.getDatasourceId());
        assertEquals("updated_logs", result.getTableName());
        assertNull(result.getDorisSql()); // UpdateDTO不应该设置dorisSql
    }

    @Test
    @Story("Entity转DTO")
    @Description("测试将实体转换为DTO")
    @Severity(SeverityLevel.CRITICAL)
    void testToDto() {
        ModuleInfo entity = new ModuleInfo();
        entity.setId(1L);
        entity.setName("Test Module");
        entity.setDatasourceId(1L);
        entity.setTableName("test_logs");
        entity.setCreateUser("admin@test.com");
        entity.setUpdateUser("user@test.com");

        when(userMapper.selectNicknameByEmail("admin@test.com")).thenReturn("管理员");
        when(userMapper.selectNicknameByEmail("user@test.com")).thenReturn("普通用户");

        ModuleInfoDTO result = converter.toDto(entity);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Module", result.getName());
        assertEquals("管理员", result.getCreateUserName());
        assertEquals("普通用户", result.getUpdateUserName());
    }

    @Test
    @Story("Entity转DTO")
    @Description("测试将实体转换为DTO，包含数据源名称")
    @Severity(SeverityLevel.CRITICAL)
    void testToDtoWithDatasource() {
        // 准备测试数据
        ModuleInfo entity = new ModuleInfo();
        entity.setId(1L);
        entity.setName("Test Module");
        entity.setDatasourceId(1L);
        entity.setCreateUser("admin@test.com");
        entity.setUpdateUser("admin@test.com");

        DatasourceInfo datasourceInfo = new DatasourceInfo();
        datasourceInfo.setId(1L);
        datasourceInfo.setName("Test Doris");

        // Mock用户昵称查询
        when(userMapper.selectNicknameByEmail("admin@test.com")).thenReturn("管理员");

        // 执行转换
        ModuleInfoDTO result = converter.toDto(entity, datasourceInfo);

        // 验证结果
        assertNotNull(result);
        assertEquals("Test Module", result.getName());
        assertEquals("Test Doris", result.getDatasourceName());
    }

    @Test
    @Story("Entity转DTO")
    @Description("测试空实体转换返回null")
    @Severity(SeverityLevel.NORMAL)
    void testToDto_NullEntity() {
        ModuleInfoDTO result = converter.toDto(null);
        assertNull(result);
    }

    @Test
    @Story("Entity转DTO")
    @Description("测试实体转换时用户字段为null的情况")
    @Severity(SeverityLevel.NORMAL)
    void testToDto_NullUserFields() {
        // 准备测试数据
        ModuleInfo entity = new ModuleInfo();
        entity.setId(1L);
        entity.setName("Test Module");
        entity.setCreateUser(null);
        entity.setUpdateUser(null);

        // 执行转换
        ModuleInfoDTO result = converter.toDto(entity);

        // 验证结果
        assertNotNull(result);
        assertEquals("Test Module", result.getName());
        assertNull(result.getCreateUserName());
        assertNull(result.getUpdateUserName());

        // 验证没有调用用户昵称查询
        verify(userMapper, never()).selectNicknameByEmail(anyString());
    }

    @Test
    @Story("Entity更新")
    @Description("测试使用CreateDTO更新实体")
    @Severity(SeverityLevel.CRITICAL)
    void testUpdateEntityWithCreateDTO() {
        // 准备现有实体
        ModuleInfo existingEntity = new ModuleInfo();
        existingEntity.setId(1L);
        existingEntity.setName("Old Module");
        existingEntity.setDatasourceId(1L);

        // 准备更新DTO
        ModuleInfoCreateDTO updateDTO = new ModuleInfoCreateDTO();
        updateDTO.setName("New Module");
        updateDTO.setDatasourceId(2L);
        updateDTO.setTableName("new_logs");
        // 注意：不再设置dorisSql字段

        // 执行更新
        ModuleInfo result = converter.updateEntity(existingEntity, updateDTO);

        // 验证结果
        assertSame(existingEntity, result); // 应该返回同一个对象
        assertEquals("New Module", result.getName());
        assertEquals(2L, result.getDatasourceId());
        assertEquals("new_logs", result.getTableName());
        assertNull(result.getDorisSql()); // dorisSql应该保持原有值（此例中为null）
        assertEquals(1L, result.getId()); // ID应该保持不变
    }

    @Test
    @Story("Entity更新")
    @Description("测试使用UpdateDTO更新实体")
    @Severity(SeverityLevel.CRITICAL)
    void testUpdateEntityWithUpdateDTO() {
        // 准备现有实体
        ModuleInfo existingEntity = new ModuleInfo();
        existingEntity.setId(1L);
        existingEntity.setName("Old Module");
        existingEntity.setDorisSql("OLD SQL");

        // 准备更新DTO
        ModuleInfoUpdateDTO updateDTO = new ModuleInfoUpdateDTO();
        updateDTO.setId(1L);
        updateDTO.setName("New Module");
        updateDTO.setDatasourceId(2L);
        updateDTO.setTableName("new_logs");

        // 执行更新
        ModuleInfo result = converter.updateEntity(existingEntity, updateDTO);

        // 验证结果
        assertSame(existingEntity, result);
        assertEquals("New Module", result.getName());
        assertEquals(2L, result.getDatasourceId());
        assertEquals("new_logs", result.getTableName());
        assertEquals("OLD SQL", result.getDorisSql()); // UpdateDTO不应该修改dorisSql
    }

    @Test
    @Story("Entity更新")
    @Description("测试使用null参数更新实体")
    @Severity(SeverityLevel.NORMAL)
    void testUpdateEntity_NullParameters() {
        ModuleInfo entity = new ModuleInfo();

        // 测试null实体
        assertNull(converter.updateEntity(null, new ModuleInfoCreateDTO()));

        // 测试null DTO
        assertSame(entity, converter.updateEntity(entity, (ModuleInfoCreateDTO) null));
        assertSame(entity, converter.updateEntity(entity, (ModuleInfoUpdateDTO) null));
    }
}
