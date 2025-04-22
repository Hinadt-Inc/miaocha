package com.hina.log.service.impl;

import com.hina.log.converter.UserDatasourcePermissionConverter;
import com.hina.log.dto.permission.UserDatasourcePermissionDTO;
import com.hina.log.dto.permission.UserPermissionTableStructureDTO;
import com.hina.log.dto.permission.UserPermissionTableStructureDTO.TableInfoDTO;
import com.hina.log.entity.Datasource;
import com.hina.log.entity.User;
import com.hina.log.entity.UserDatasourcePermission;
import com.hina.log.entity.enums.UserRole;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.DatasourceMapper;
import com.hina.log.mapper.UserDatasourcePermissionMapper;
import com.hina.log.mapper.UserMapper;
import com.hina.log.service.UserDatasourcePermissionService;
import com.hina.log.service.database.DatabaseMetadataService;
import com.hina.log.service.database.DatabaseMetadataServiceFactory;
import com.hina.log.service.sql.JdbcQueryExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户数据源权限服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDatasourcePermissionServiceImpl implements UserDatasourcePermissionService {

    private final UserDatasourcePermissionMapper permissionMapper;
    private final UserMapper userMapper;
    private final DatasourceMapper datasourceMapper;
    private final UserDatasourcePermissionConverter permissionConverter;
    private final JdbcQueryExecutor jdbcQueryExecutor;
    private final DatabaseMetadataServiceFactory metadataServiceFactory;

    @Override
    public List<UserDatasourcePermissionDTO> getUserDatasourcePermissions(Long userId, Long datasourceId) {
        List<UserDatasourcePermission> permissions = permissionMapper.selectByUserAndDatasource(userId, datasourceId);
        return permissions.stream()
                .map(permissionConverter::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasTablePermission(Long userId, Long datasourceId, String tableName) {
        // 先查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 超级管理员和管理员拥有所有表的权限
        String role = user.getRole();
        if (UserRole.SUPER_ADMIN.name().equals(role) || UserRole.ADMIN.name().equals(role)) {
            return true;
        }

        // 检查用户是否拥有此表的权限
        UserDatasourcePermission permission = getPermissionEntity(userId, datasourceId, tableName);
        if (permission != null) {
            return true;
        }

        // 检查用户是否拥有此数据源下所有表的权限
        UserDatasourcePermission wildcard = permissionMapper.selectAllTablesPermission(userId, datasourceId);
        return wildcard != null;
    }

    @Override
    @Transactional
    public UserDatasourcePermissionDTO grantTablePermission(Long userId, Long datasourceId, String tableName) {
        // 检查是否已存在相同权限
        UserDatasourcePermission existPermission = getPermissionEntity(userId, datasourceId, tableName);

        if (existPermission != null) {
            return permissionConverter.toDto(existPermission);
        }

        // 创建新权限
        UserDatasourcePermission permission = permissionConverter.createEntity(userId, datasourceId, tableName);
        permissionMapper.insert(permission);

        return permissionConverter.toDto(permission);
    }

    @Override
    @Transactional
    public void revokeTablePermission(Long permissionId) {
        permissionMapper.deleteById(permissionId);
    }

    @Override
    @Transactional
    public void revokeTablePermission(Long userId, Long datasourceId, String tableName) {
        UserDatasourcePermission permission = getPermissionEntity(userId, datasourceId, tableName);

        if (permission != null) {
            permissionMapper.deleteById(permission.getId());
        }
    }

    @Override
    public UserDatasourcePermission getPermissionEntityById(Long permissionId) {
        UserDatasourcePermission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        return permission;
    }

    @Override
    public UserDatasourcePermission getPermissionEntity(Long userId, Long datasourceId, String tableName) {
        return permissionMapper.selectByUserDatasourceAndTable(userId, datasourceId, tableName);
    }

    @Override
    public List<UserPermissionTableStructureDTO> getUserAccessibleTables(Long userId) {
        // 先查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 获取所有数据源
        List<Datasource> allDatasources = datasourceMapper.selectAll();
        if (allDatasources.isEmpty()) {
            return new ArrayList<>();
        }

        // 数据源ID -> 数据源对象的映射
        Map<Long, Datasource> datasourceMap = new HashMap<>();
        allDatasources.forEach(ds -> datasourceMap.put(ds.getId(), ds));

        // 结果容器
        Map<Long, UserPermissionTableStructureDTO> resultMap = new HashMap<>();

        // 超级管理员和管理员拥有所有表的权限
        String role = user.getRole();
        boolean isAdmin = UserRole.SUPER_ADMIN.name().equals(role) || UserRole.ADMIN.name().equals(role);

        if (isAdmin) {
            // 管理员拥有所有数据源的权限，要从数据库中获取表名列表
            for (Datasource ds : allDatasources) {
                UserPermissionTableStructureDTO structureDTO = new UserPermissionTableStructureDTO();
                structureDTO.setDatasourceId(ds.getId());
                structureDTO.setDatasourceName(ds.getName());
                structureDTO.setDatabaseName(ds.getDatabase());

                List<TableInfoDTO> tables = fetchTablesFromDatabase(ds);
                structureDTO.setTables(tables);
                resultMap.put(ds.getId(), structureDTO);
            }
        } else {
            // 非管理员，查询用户所有的数据源权限
            for (Datasource ds : allDatasources) {
                Long datasourceId = ds.getId();

                // 检查用户是否拥有此数据源下所有表的权限
                UserDatasourcePermission wildcardPermission = permissionMapper.selectAllTablesPermission(userId,
                        datasourceId);

                if (wildcardPermission != null) {
                    // 用户拥有此数据源所有表的权限
                    UserPermissionTableStructureDTO structureDTO = new UserPermissionTableStructureDTO();
                    structureDTO.setDatasourceId(datasourceId);
                    structureDTO.setDatasourceName(ds.getName());
                    structureDTO.setDatabaseName(ds.getDatabase());

                    // 获取数据库中的实际表列表
                    List<TableInfoDTO> tables = fetchTablesFromDatabase(ds);
                    structureDTO.setTables(tables);
                    resultMap.put(datasourceId, structureDTO);
                } else {
                    // 获取用户在此数据源上的表权限
                    List<UserDatasourcePermission> permissions = permissionMapper.selectByUserAndDatasource(userId,
                            datasourceId);

                    if (permissions != null && !permissions.isEmpty()) {
                        UserPermissionTableStructureDTO structureDTO = new UserPermissionTableStructureDTO();
                        structureDTO.setDatasourceId(datasourceId);
                        structureDTO.setDatasourceName(ds.getName());
                        structureDTO.setDatabaseName(ds.getDatabase());
                        structureDTO.setTables(new ArrayList<>());

                        // 添加表信息
                        for (UserDatasourcePermission permission : permissions) {
                            TableInfoDTO tableInfo = new TableInfoDTO();
                            tableInfo.setTableName(permission.getTableName());
                            tableInfo.setPermissionId(permission.getId());
                            structureDTO.getTables().add(tableInfo);
                        }

                        resultMap.put(datasourceId, structureDTO);
                    }
                }
            }
        }

        return new ArrayList<>(resultMap.values());
    }

    /**
     * 从数据库中获取表列表
     * 
     * @param datasource 数据源
     * @return 表名列表
     */
    private List<TableInfoDTO> fetchTablesFromDatabase(Datasource datasource) {
        List<TableInfoDTO> tables = new ArrayList<>();

        try {
            // 获取数据库连接
            Connection connection = jdbcQueryExecutor.getConnection(datasource);

            // 获取对应数据库类型的元数据服务
            DatabaseMetadataService metadataService = metadataServiceFactory.getService(datasource.getType());

            // 获取所有表名
            List<String> tableNames = metadataService.getAllTables(connection);

            // 转换为DTO对象
            for (String tableName : tableNames) {
                TableInfoDTO tableInfo = new TableInfoDTO();
                tableInfo.setTableName(tableName);
                // 通配符权限没有特定的权限ID
                tableInfo.setPermissionId(null);
                tables.add(tableInfo);
            }

            // 关闭连接
            connection.close();
        } catch (SQLException e) {
            log.error("获取数据源表列表失败", e);
            // 连接失败时返回空列表，不应该中断整个权限获取流程
        }

        return tables;
    }
}