package com.hinadt.miaocha.mock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.application.service.impl.ModuleInfoServiceImpl;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.converter.ModuleInfoConverter;
import com.hinadt.miaocha.domain.converter.ModulePermissionConverter;
import com.hinadt.miaocha.domain.dto.SqlQueryResultDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoCreateDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoUpdateDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoWithPermissionsDTO;
import com.hinadt.miaocha.domain.dto.module.QueryConfigDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.entity.UserModulePermission;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.LogstashProcessMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import com.hinadt.miaocha.domain.mapper.UserModulePermissionMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ModuleInfo服务测试类 */
@ExtendWith(MockitoExtension.class)
@DisplayName("模块管理服务测试")
class ModuleInfoServiceTest {

    @Mock private ModuleInfoMapper moduleInfoMapper;
    @Mock private DatasourceMapper datasourceMapper;
    @Mock private JdbcQueryExecutor jdbcQueryExecutor;
    @Mock private ModuleInfoConverter moduleInfoConverter;
    @Mock private LogstashProcessMapper logstashProcessMapper;
    @Mock private UserModulePermissionMapper userModulePermissionMapper;
    @Mock private UserMapper userMapper;
    @Mock private ModulePermissionConverter modulePermissionConverter;
    @Mock private TableValidationService tableValidationService;
    @Mock private ObjectMapper objectMapper;

    private ModuleInfoServiceImpl moduleInfoService;

    private ModuleInfo sampleModuleInfo;
    private ModuleInfoDTO sampleModuleInfoDTO;
    private DatasourceInfo sampleDatasourceInfo;

    @BeforeEach
    void setUp() {
        // 手动创建服务实例，使用构造函数注入
        moduleInfoService =
                new ModuleInfoServiceImpl(
                        moduleInfoMapper,
                        datasourceMapper,
                        jdbcQueryExecutor,
                        moduleInfoConverter,
                        logstashProcessMapper,
                        userModulePermissionMapper,
                        userMapper,
                        modulePermissionConverter,
                        tableValidationService,
                        objectMapper);

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
    void testDeleteModule_Success() {
        // Mock 行为
        when(moduleInfoMapper.selectById(1L)).thenReturn(sampleModuleInfo);
        when(logstashProcessMapper.countByModuleId(1L)).thenReturn(0);
        when(moduleInfoMapper.deleteById(1L)).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> moduleInfoService.deleteModule(1L, false));

        // 验证调用
        verify(moduleInfoMapper).selectById(1L);
        verify(logstashProcessMapper).countByModuleId(1L);
        verify(moduleInfoMapper).deleteById(1L);
    }

    @Test
    void testCreateModule_DatasourceNotFound() {
        // 准备测试数据
        ModuleInfoCreateDTO createDTO = new ModuleInfoCreateDTO();
        createDTO.setName("New Module");
        createDTO.setDatasourceId(999L);

        // Mock 行为
        when(moduleInfoMapper.existsByName("New Module", null)).thenReturn(false);
        when(datasourceMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> moduleInfoService.createModule(createDTO));

        assertEquals(ErrorCode.DATASOURCE_NOT_FOUND, exception.getErrorCode());

        // 验证调用顺序
        verify(moduleInfoMapper).existsByName("New Module", null);
        verify(datasourceMapper).selectById(999L);
        verify(moduleInfoMapper, never()).insert(any());
    }

    @Test
    void testUpdateModule_Success() {
        // 准备测试数据
        ModuleInfoUpdateDTO updateDTO = new ModuleInfoUpdateDTO();
        updateDTO.setId(1L);
        updateDTO.setName("Updated Module");
        updateDTO.setDatasourceId(1L);

        ModuleInfo updatedModule = new ModuleInfo();
        updatedModule.setId(1L);
        updatedModule.setName("Updated Module");

        // Mock 行为
        when(moduleInfoMapper.selectById(1L)).thenReturn(sampleModuleInfo);
        when(moduleInfoMapper.existsByName("Updated Module", 1L)).thenReturn(false);
        when(datasourceMapper.selectById(1L)).thenReturn(sampleDatasourceInfo);
        when(moduleInfoConverter.updateEntity(any(ModuleInfo.class), eq(updateDTO)))
                .thenReturn(updatedModule);
        when(moduleInfoMapper.update(updatedModule)).thenReturn(1);
        when(moduleInfoMapper.selectById(1L)).thenReturn(updatedModule);
        when(moduleInfoConverter.toDto(updatedModule, sampleDatasourceInfo))
                .thenReturn(sampleModuleInfoDTO);

        // 执行测试
        ModuleInfoDTO result = moduleInfoService.updateModule(updateDTO);

        // 验证结果
        assertNotNull(result);

        // 验证调用顺序
        verify(moduleInfoMapper, times(2)).selectById(1L); // 检查存在性 + 重新查询
        verify(moduleInfoMapper).existsByName("Updated Module", 1L);
        verify(datasourceMapper).selectById(1L);
        verify(moduleInfoMapper).update(updatedModule);
    }

    @Test
    void testUpdateModule_ModuleNotFound() {
        // 准备测试数据
        ModuleInfoUpdateDTO updateDTO = new ModuleInfoUpdateDTO();
        updateDTO.setId(999L);

        // Mock 行为
        when(moduleInfoMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> moduleInfoService.updateModule(updateDTO));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void testDeleteModule_ModuleNotFound() {
        // Mock 行为
        when(moduleInfoMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> moduleInfoService.deleteModule(999L, false));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void testDeleteModule_ModuleInUse() {
        // Mock 行为
        when(moduleInfoMapper.selectById(1L)).thenReturn(sampleModuleInfo);
        when(logstashProcessMapper.countByModuleId(1L)).thenReturn(2);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> moduleInfoService.deleteModule(1L, false));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());

        // 验证不会删除
        verify(moduleInfoMapper, never()).deleteById(any());
    }

    @Test
    void testDeleteModule_WithDorisTable() {
        // 准备有Doris SQL的模块
        ModuleInfo moduleWithDoris = new ModuleInfo();
        moduleWithDoris.setId(1L);
        moduleWithDoris.setName("Test Module");
        moduleWithDoris.setTableName("test_logs");
        moduleWithDoris.setDorisSql("CREATE TABLE test_logs ...");
        moduleWithDoris.setDatasourceId(1L);

        // Mock 行为
        when(moduleInfoMapper.selectById(1L)).thenReturn(moduleWithDoris);
        when(logstashProcessMapper.countByModuleId(1L)).thenReturn(0);
        when(datasourceMapper.selectById(1L)).thenReturn(sampleDatasourceInfo);
        // jdbcQueryExecutor.executeQuery没有返回值，不需要mock
        when(moduleInfoMapper.deleteById(1L)).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> moduleInfoService.deleteModule(1L, true));

        // 验证SQL执行：TRUNCATE和DROP
        verify(jdbcQueryExecutor)
                .executeQuery(eq(sampleDatasourceInfo), eq("TRUNCATE TABLE test_logs"));
        verify(jdbcQueryExecutor)
                .executeQuery(eq(sampleDatasourceInfo), eq("DROP TABLE IF EXISTS test_logs"));
        verify(moduleInfoMapper).deleteById(1L);
    }

    @Test
    void testExecuteDorisSql_Success() {
        // 准备没有Doris SQL的模块
        ModuleInfo moduleWithoutDoris = new ModuleInfo();
        moduleWithoutDoris.setId(1L);
        moduleWithoutDoris.setDorisSql(null); // 没有执行过SQL
        moduleWithoutDoris.setDatasourceId(1L);

        String sql = "CREATE TABLE test_logs ...";

        // 准备executeQuery的返回值
        SqlQueryResultDTO executeResult = new SqlQueryResultDTO();
        executeResult.setAffectedRows(1);

        // Mock 行为
        when(moduleInfoMapper.selectById(1L)).thenReturn(moduleWithoutDoris);
        doNothing().when(tableValidationService).validateDorisSql(moduleWithoutDoris, sql);
        when(datasourceMapper.selectById(1L)).thenReturn(sampleDatasourceInfo);
        when(jdbcQueryExecutor.executeQuery(eq(sampleDatasourceInfo), eq(sql)))
                .thenReturn(executeResult);
        when(moduleInfoMapper.update(any(ModuleInfo.class))).thenReturn(1);
        when(moduleInfoConverter.toDto(any(ModuleInfo.class), eq(sampleDatasourceInfo)))
                .thenReturn(sampleModuleInfoDTO);

        // 执行测试
        ModuleInfoDTO result = moduleInfoService.executeDorisSql(1L, sql);

        // 验证结果
        assertNotNull(result);

        // 验证调用顺序
        verify(moduleInfoMapper).selectById(1L);
        verify(tableValidationService).validateDorisSql(moduleWithoutDoris, sql);
        verify(datasourceMapper).selectById(1L);
        verify(jdbcQueryExecutor).executeQuery(eq(sampleDatasourceInfo), eq(sql));
        verify(moduleInfoMapper).update(any(ModuleInfo.class));
    }

    @Test
    void testExecuteDorisSql_AlreadyExecuted() {
        // 准备已有Doris SQL的模块
        ModuleInfo moduleWithDoris = new ModuleInfo();
        moduleWithDoris.setId(1L);
        moduleWithDoris.setDorisSql("CREATE TABLE existing ...");

        // Mock 行为
        when(moduleInfoMapper.selectById(1L)).thenReturn(moduleWithDoris);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> moduleInfoService.executeDorisSql(1L, "CREATE TABLE new ..."));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());

        // 验证不会执行SQL
        verify(jdbcQueryExecutor, never()).executeQuery(any(DatasourceInfo.class), anyString());
    }

    @Test
    void testConfigureQueryConfig_Success() throws Exception {
        // 准备查询配置
        QueryConfigDTO queryConfig = new QueryConfigDTO();
        queryConfig.setTimeField("timestamp");

        String configJson = "{\"timeField\":\"timestamp\"}";

        // 准备有Doris SQL的模块
        ModuleInfo moduleWithDoris = new ModuleInfo();
        moduleWithDoris.setId(1L);
        moduleWithDoris.setDatasourceId(1L); // 设置datasourceId避免null
        moduleWithDoris.setDorisSql("CREATE TABLE test (id INT, timestamp DATETIME)");

        // Mock 行为
        when(moduleInfoMapper.selectById(1L)).thenReturn(moduleWithDoris);
        when(objectMapper.writeValueAsString(queryConfig)).thenReturn(configJson);
        when(moduleInfoMapper.update(any(ModuleInfo.class))).thenReturn(1);
        when(datasourceMapper.selectById(1L)).thenReturn(sampleDatasourceInfo);
        when(moduleInfoConverter.toDto(any(ModuleInfo.class), eq(sampleDatasourceInfo)))
                .thenReturn(sampleModuleInfoDTO);

        // 执行测试
        ModuleInfoDTO result = moduleInfoService.configureQueryConfig(1L, queryConfig);

        // 验证结果
        assertNotNull(result);

        // 验证调用 - 验证关键的业务逻辑调用
        verify(objectMapper).writeValueAsString(queryConfig);
        verify(moduleInfoMapper).update(any(ModuleInfo.class));
        // 验证字段验证方法被调用，包含时间字段
        verify(tableValidationService)
                .validateQueryConfigFields(
                        eq(moduleWithDoris), argThat(fields -> fields.contains("timestamp")));
    }

    @Test
    void testConfigureQueryConfig_WithExcludeFields() throws Exception {
        // 准备查询配置 - 包含排除字段
        QueryConfigDTO queryConfig = new QueryConfigDTO();
        queryConfig.setTimeField("timestamp");
        queryConfig.setExcludeFields(Arrays.asList("password", "secret_key"));

        String configJson =
                "{\"timeField\":\"timestamp\",\"excludeFields\":[\"password\",\"secret_key\"]}";

        // 准备有Doris SQL的模块
        ModuleInfo moduleWithDoris = new ModuleInfo();
        moduleWithDoris.setId(1L);
        moduleWithDoris.setDatasourceId(1L);
        moduleWithDoris.setDorisSql(
                "CREATE TABLE test (id INT, timestamp DATETIME, password VARCHAR(255), secret_key"
                        + " VARCHAR(255))");

        // Mock 行为
        when(moduleInfoMapper.selectById(1L)).thenReturn(moduleWithDoris);
        when(objectMapper.writeValueAsString(queryConfig)).thenReturn(configJson);
        when(moduleInfoMapper.update(any(ModuleInfo.class))).thenReturn(1);
        when(datasourceMapper.selectById(1L)).thenReturn(sampleDatasourceInfo);
        when(moduleInfoConverter.toDto(any(ModuleInfo.class), eq(sampleDatasourceInfo)))
                .thenReturn(sampleModuleInfoDTO);

        // 执行测试
        ModuleInfoDTO result = moduleInfoService.configureQueryConfig(1L, queryConfig);

        // 验证结果
        assertNotNull(result);

        // 验证调用 - 验证关键的业务逻辑调用
        verify(objectMapper).writeValueAsString(queryConfig);
        verify(moduleInfoMapper).update(any(ModuleInfo.class));
        // 验证字段验证方法被调用，包含排除字段
        verify(tableValidationService)
                .validateQueryConfigFields(
                        eq(moduleWithDoris),
                        argThat(
                                fields ->
                                        fields.containsAll(
                                                Arrays.asList(
                                                        "timestamp", "password", "secret_key"))));
    }

    @Test
    void testConfigureQueryConfig_WithKeywordFieldsAndExcludeFields() throws Exception {
        // 准备查询配置 - 包含关键词字段和排除字段
        QueryConfigDTO queryConfig = new QueryConfigDTO();
        queryConfig.setTimeField("timestamp");

        List<QueryConfigDTO.KeywordFieldConfigDTO> keywordFields = new ArrayList<>();
        QueryConfigDTO.KeywordFieldConfigDTO keywordField =
                new QueryConfigDTO.KeywordFieldConfigDTO();
        keywordField.setFieldName("message");
        keywordField.setSearchMethod("LIKE");
        keywordFields.add(keywordField);
        queryConfig.setKeywordFields(keywordFields);

        queryConfig.setExcludeFields(Arrays.asList("password", "secret_key"));

        String configJson =
                "{\"timeField\":\"timestamp\",\"keywordFields\":[{\"fieldName\":\"message\",\"searchMethod\":\"LIKE\"}],\"excludeFields\":[\"password\",\"secret_key\"]}";

        // 准备有Doris SQL的模块
        ModuleInfo moduleWithDoris = new ModuleInfo();
        moduleWithDoris.setId(1L);
        moduleWithDoris.setDatasourceId(1L);
        moduleWithDoris.setDorisSql(
                "CREATE TABLE test (id INT, timestamp DATETIME, message TEXT, password"
                        + " VARCHAR(255), secret_key VARCHAR(255))");

        // Mock 行为
        when(moduleInfoMapper.selectById(1L)).thenReturn(moduleWithDoris);
        when(objectMapper.writeValueAsString(queryConfig)).thenReturn(configJson);
        when(moduleInfoMapper.update(any(ModuleInfo.class))).thenReturn(1);
        when(datasourceMapper.selectById(1L)).thenReturn(sampleDatasourceInfo);
        when(moduleInfoConverter.toDto(any(ModuleInfo.class), eq(sampleDatasourceInfo)))
                .thenReturn(sampleModuleInfoDTO);

        // 执行测试
        ModuleInfoDTO result = moduleInfoService.configureQueryConfig(1L, queryConfig);

        // 验证结果
        assertNotNull(result);

        // 验证调用 - 验证关键的业务逻辑调用
        verify(objectMapper).writeValueAsString(queryConfig);
        verify(moduleInfoMapper).update(any(ModuleInfo.class));
        // 验证字段验证方法被调用，包含时间字段、关键词字段和排除字段
        verify(tableValidationService)
                .validateQueryConfigFields(
                        eq(moduleWithDoris),
                        argThat(
                                fields ->
                                        fields.containsAll(
                                                Arrays.asList(
                                                        "timestamp",
                                                        "message",
                                                        "password",
                                                        "secret_key"))));
    }

    @Test
    void testConfigureQueryConfig_ExcludeFieldsContainsTimeField() throws Exception {
        // 准备查询配置 - 排除字段包含时间字段
        QueryConfigDTO queryConfig = new QueryConfigDTO();
        queryConfig.setTimeField("timestamp");
        queryConfig.setExcludeFields(Arrays.asList("password", "timestamp", "secret_key"));

        // 准备有Doris SQL的模块
        ModuleInfo moduleWithDoris = new ModuleInfo();
        moduleWithDoris.setId(1L);
        moduleWithDoris.setDatasourceId(1L);
        moduleWithDoris.setDorisSql(
                "CREATE TABLE test (id INT, timestamp DATETIME, password VARCHAR(255), secret_key"
                        + " VARCHAR(255))");

        // Mock 行为
        when(moduleInfoMapper.selectById(1L)).thenReturn(moduleWithDoris);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> moduleInfoService.configureQueryConfig(1L, queryConfig));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("排除字段列表不能包含时间字段", exception.getMessage());

        // 验证不会执行后续的操作
        verify(objectMapper, never()).writeValueAsString(any());
        verify(moduleInfoMapper, never()).update(any());
        verify(tableValidationService, never()).validateQueryConfigFields(any(), any());
    }

    @Test
    void testGetTableNameByModule_Success() {
        // Mock 行为
        when(moduleInfoMapper.selectByName("test_module")).thenReturn(sampleModuleInfo);

        // 执行测试
        String tableName = moduleInfoService.getTableNameByModule("test_module");

        // 验证结果
        assertEquals("test_logs", tableName);
        verify(moduleInfoMapper).selectByName("test_module");
    }

    @Test
    void testGetTableNameByModule_ModuleNotFound() {
        // Mock 行为
        when(moduleInfoMapper.selectByName("non_exist")).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> moduleInfoService.getTableNameByModule("non_exist"));

        assertEquals(ErrorCode.MODULE_NOT_FOUND, exception.getErrorCode());
        // 仅验证异常类型和错误码，不依赖具体错误消息
    }

    @Test
    void testGetQueryConfigByModule_Success() throws Exception {
        // 准备模块和配置
        ModuleInfo moduleWithConfig = new ModuleInfo();
        moduleWithConfig.setId(1L);
        moduleWithConfig.setQueryConfig("{\"timeField\":\"timestamp\"}");

        QueryConfigDTO expectedConfig = new QueryConfigDTO();
        expectedConfig.setTimeField("timestamp");

        // Mock 行为
        when(moduleInfoMapper.selectByName("test_module")).thenReturn(moduleWithConfig);
        when(objectMapper.readValue("{\"timeField\":\"timestamp\"}", QueryConfigDTO.class))
                .thenReturn(expectedConfig);

        // 执行测试
        QueryConfigDTO result = moduleInfoService.getQueryConfigByModule("test_module");

        // 验证结果
        assertNotNull(result);
        assertEquals("timestamp", result.getTimeField());
    }

    @Test
    void testGetAllModules_Success() {
        // 准备数据
        List<ModuleInfo> moduleList = Arrays.asList(sampleModuleInfo);

        // Mock 行为
        when(moduleInfoMapper.selectAll()).thenReturn(moduleList);
        when(datasourceMapper.selectById(1L)).thenReturn(sampleDatasourceInfo);
        when(moduleInfoConverter.toDto(sampleModuleInfo, sampleDatasourceInfo))
                .thenReturn(sampleModuleInfoDTO);

        // 执行测试
        List<ModuleInfoDTO> result = moduleInfoService.getAllModules();

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Module", result.get(0).getName());
    }

    @Test
    void testGetAllModulesWithPermissions_Success() {
        // 准备权限数据
        UserModulePermission permission = new UserModulePermission();
        permission.setUserId(1L);
        permission.setModule("Test Module");

        User user = new User();
        user.setId(1L);
        user.setNickname("testuser");

        // Mock 行为
        when(moduleInfoMapper.selectAll()).thenReturn(Arrays.asList(sampleModuleInfo));
        when(datasourceMapper.selectById(1L)).thenReturn(sampleDatasourceInfo);
        when(userModulePermissionMapper.selectAll()).thenReturn(Arrays.asList(permission));
        when(userMapper.selectByIds(Arrays.asList(1L))).thenReturn(Arrays.asList(user));
        when(modulePermissionConverter.createUserPermissionInfoDTO(permission, user))
                .thenReturn(
                        new com.hinadt.miaocha.domain.dto.permission.ModuleUsersPermissionDTO
                                .UserPermissionInfoDTO());
        when(moduleInfoConverter.toWithPermissionsDto(
                        eq(sampleModuleInfo), eq(sampleDatasourceInfo), any()))
                .thenReturn(new ModuleInfoWithPermissionsDTO());

        // 执行测试
        List<ModuleInfoWithPermissionsDTO> result =
                moduleInfoService.getAllModulesWithPermissions();

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
