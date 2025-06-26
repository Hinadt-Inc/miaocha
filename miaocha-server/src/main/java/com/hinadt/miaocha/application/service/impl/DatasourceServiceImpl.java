package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.DatasourceService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.converter.DatasourceConverter;
import com.hinadt.miaocha.domain.dto.DatasourceConnectionTestResultDTO;
import com.hinadt.miaocha.domain.dto.DatasourceCreateDTO;
import com.hinadt.miaocha.domain.dto.DatasourceDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 数据源服务实现类 */
@Service
public class DatasourceServiceImpl implements DatasourceService {

    @Autowired private DatasourceMapper datasourceMapper;

    @Autowired private DatasourceConverter datasourceConverter;

    @Autowired private ModuleInfoMapper moduleInfoMapper;

    @Override
    @Transactional
    public DatasourceDTO createDatasource(DatasourceCreateDTO dto) {
        // 检查数据源名称是否已存在
        if (datasourceMapper.selectByName(dto.getName()) != null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NAME_EXISTS);
        }

        // 测试连接
        DatasourceConnectionTestResultDTO testResult = testConnection(dto);
        if (!testResult.isSuccess()) {
            throw new BusinessException(
                    ErrorCode.DATASOURCE_CONNECTION_FAILED, testResult.getErrorMessage());
        }

        // 转换为实体并保存
        DatasourceInfo datasourceInfo = datasourceConverter.toEntity(dto);
        datasourceMapper.insert(datasourceInfo);

        // 返回DTO
        return datasourceConverter.toDto(datasourceInfo);
    }

    @Override
    @Transactional
    public DatasourceDTO updateDatasource(Long id, DatasourceCreateDTO dto) {
        // 检查数据源是否存在
        DatasourceInfo existingDatasourceInfo = datasourceMapper.selectById(id);
        if (existingDatasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 检查名称是否与其他数据源重复
        DatasourceInfo sameNameDatasourceInfo = datasourceMapper.selectByName(dto.getName());
        if (sameNameDatasourceInfo != null && !sameNameDatasourceInfo.getId().equals(id)) {
            throw new BusinessException(ErrorCode.DATASOURCE_NAME_EXISTS);
        }

        // 测试连接
        DatasourceConnectionTestResultDTO testResult = testConnection(dto);
        if (!testResult.isSuccess()) {
            throw new BusinessException(
                    ErrorCode.DATASOURCE_CONNECTION_FAILED, testResult.getErrorMessage());
        }

        // 更新数据源
        DatasourceInfo datasourceInfo =
                datasourceConverter.updateEntity(existingDatasourceInfo, dto);
        datasourceInfo.setId(id);
        datasourceMapper.update(datasourceInfo);

        return datasourceConverter.toDto(datasourceInfo);
    }

    @Override
    @Transactional
    public void deleteDatasource(Long id) {
        // 检查数据源是否存在
        if (datasourceMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 检查数据源是否被模块引用
        if (moduleInfoMapper.existsByDatasourceId(id)) {
            throw new BusinessException(ErrorCode.DATASOURCE_IN_USE);
        }

        datasourceMapper.deleteById(id);
    }

    @Override
    public DatasourceDTO getDatasource(Long id) {
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(id);
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }
        return datasourceConverter.toDto(datasourceInfo);
    }

    @Override
    public List<DatasourceDTO> getAllDatasources() {
        return datasourceMapper.selectAll().stream()
                .map(datasourceConverter::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public DatasourceDTO getDatasourceByModule(String module) {
        // 参数验证
        if (module == null || module.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块名称不能为空");
        }

        // 根据模块名称查询模块信息
        ModuleInfo moduleInfo = moduleInfoMapper.selectByName(module.trim());
        if (moduleInfo == null) {
            throw new BusinessException(ErrorCode.MODULE_NOT_FOUND, "未找到模块: " + module);
        }

        // 获取数据源信息
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(moduleInfo.getDatasourceId());
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND, "模块关联的数据源不存在");
        }

        return datasourceConverter.toDto(datasourceInfo);
    }

    @Override
    public DatasourceInfo getDatasourceInfoByModule(String module) {
        // 参数验证
        if (module == null || module.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块名称不能为空");
        }

        // 根据模块名称查询模块信息
        ModuleInfo moduleInfo = moduleInfoMapper.selectByName(module.trim());
        if (moduleInfo == null) {
            throw new BusinessException(ErrorCode.MODULE_NOT_FOUND, "未找到模块: " + module);
        }

        // 获取数据源信息
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(moduleInfo.getDatasourceId());
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND, "模块关联的数据源不存在");
        }

        return datasourceInfo;
    }

    @Override
    public DatasourceConnectionTestResultDTO testConnection(DatasourceCreateDTO dto) {
        try {
            // 参数验证
            if (dto.getJdbcUrl() == null || dto.getJdbcUrl().trim().isEmpty()) {
                return DatasourceConnectionTestResultDTO.failure("JDBC URL不能为空");
            }

            if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
                return DatasourceConnectionTestResultDTO.failure("用户名不能为空");
            }

            // 直接使用传入的 JDBC URL
            String url = dto.getJdbcUrl().trim();

            try (Connection conn =
                    DriverManager.getConnection(url, dto.getUsername(), dto.getPassword())) {
                // 连接成功
                return DatasourceConnectionTestResultDTO.success();
            }
        } catch (SQLException e) {
            // 根据不同的SQL异常提供友好的错误信息
            return handleSQLException(e);
        } catch (Exception e) {
            // 处理其他异常
            return DatasourceConnectionTestResultDTO.failure("连接失败: " + getSimpleErrorMessage(e));
        }
    }

    @Override
    public DatasourceConnectionTestResultDTO testExistingConnection(Long id) {
        // 获取数据源
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(id);
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 创建一个DTO并填充已保存的连接参数
        DatasourceCreateDTO dto = new DatasourceCreateDTO();
        dto.setType(datasourceInfo.getType());
        dto.setJdbcUrl(datasourceInfo.getJdbcUrl());
        dto.setUsername(datasourceInfo.getUsername());
        dto.setPassword(datasourceInfo.getPassword());

        // 使用已保存的参数测试连接
        return testConnection(dto);
    }

    // =========== =========== 私有方法 =========== ===========

    /** 处理SQL异常，提供用户友好的错误信息 */
    private DatasourceConnectionTestResultDTO handleSQLException(SQLException e) {
        String errorMessage = e.getMessage().toLowerCase();
        String sqlState = e.getSQLState();

        // 连接超时或网络相关错误
        if (errorMessage.contains("timeout") || errorMessage.contains("connection timed out")) {
            return DatasourceConnectionTestResultDTO.failure("连接超时，请检查网络连接和数据库服务状态");
        }

        // 认证失败
        if (errorMessage.contains("access denied")
                || errorMessage.contains("authentication failed")
                || errorMessage.contains("login failed")
                || "28000".equals(sqlState)) {
            return DatasourceConnectionTestResultDTO.failure("用户名或密码错误，请检查认证信息");
        }

        // 数据库不存在
        if (errorMessage.contains("unknown database")
                || errorMessage.contains("database") && errorMessage.contains("does not exist")) {
            return DatasourceConnectionTestResultDTO.failure("指定的数据库不存在，请检查数据库名称");
        }

        // 主机无法连接 - 扩展检查条件
        if (errorMessage.contains("connection refused")
                || errorMessage.contains("no route to host")
                || errorMessage.contains("host is unreachable")
                || errorMessage.contains("connect failed")
                || errorMessage.contains("unknown host")
                || errorMessage.contains("name resolution failed")) {
            return DatasourceConnectionTestResultDTO.failure("无法连接到数据库服务器，请检查主机地址和端口");
        }

        // 端口相关错误 - 扩展检查条件
        if (errorMessage.contains("connection reset")
                || errorMessage.contains("connect timeout")
                || errorMessage.contains("network unreachable")
                || errorMessage.contains("port")) {
            return DatasourceConnectionTestResultDTO.failure("连接被拒绝，请检查端口是否正确且数据库服务正在运行");
        }

        // URL格式错误
        if (errorMessage.contains("malformed")
                || errorMessage.contains("invalid") && errorMessage.contains("url")) {
            return DatasourceConnectionTestResultDTO.failure("JDBC URL格式错误，请检查连接字符串格式");
        }

        // 驱动相关错误 - 只有明确是驱动问题时才返回驱动错误
        if (errorMessage.contains("no suitable driver found")
                || errorMessage.contains("driver class not found")
                || errorMessage.contains("driver not found")) {
            return DatasourceConnectionTestResultDTO.failure("找不到合适的数据库驱动，请检查数据库类型");
        }

        // 通用网络/连接错误 - 新增，在驱动错误之前检查
        if (errorMessage.contains("communications link failure")
                || errorMessage.contains("connection failure")
                || errorMessage.contains("network")
                || errorMessage.contains("socket")) {
            return DatasourceConnectionTestResultDTO.failure("网络连接失败，请检查数据库服务器地址和端口是否正确");
        }

        // 通用错误
        return DatasourceConnectionTestResultDTO.failure("数据库连接失败: " + getSimpleErrorMessage(e));
    }

    /** 获取简化的错误信息 */
    private String getSimpleErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }

        // 截取第一行错误信息，避免过长的堆栈信息
        String[] lines = message.split("\n");
        String firstLine = lines[0].trim();

        // 如果错误信息过长，截取前200个字符
        if (firstLine.length() > 200) {
            return firstLine.substring(0, 200) + "...";
        }

        return firstLine;
    }
}
