package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.DatasourceService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.converter.DatasourceConverter;
import com.hinadt.miaocha.domain.dto.DatasourceCreateDTO;
import com.hinadt.miaocha.domain.dto.DatasourceDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.enums.DatasourceType;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import java.sql.Connection;
import java.sql.DriverManager;
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

    @Override
    @Transactional
    public DatasourceDTO createDatasource(DatasourceCreateDTO dto) {
        // 检查数据源名称是否已存在
        if (datasourceMapper.selectByName(dto.getName()) != null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NAME_EXISTS);
        }

        // 测试连接
        if (!testConnection(dto)) {
            throw new BusinessException(ErrorCode.DATASOURCE_CONNECTION_FAILED);
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
        if (!testConnection(dto)) {
            throw new BusinessException(ErrorCode.DATASOURCE_CONNECTION_FAILED);
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
    public boolean testConnection(DatasourceCreateDTO dto) {
        try {
            // 根据类型获取数据源类型枚举
            DatasourceType datasourceType = DatasourceType.fromType(dto.getType());
            if (datasourceType == null) {
                throw new BusinessException(ErrorCode.DATASOURCE_TYPE_NOT_SUPPORTED);
            }

            // 使用枚举构建JDBC URL（包含JDBC参数）
            String url =
                    datasourceType.buildJdbcUrl(
                            dto.getIp(), dto.getPort(), dto.getDatabase(), dto.getJdbcParams());

            try (Connection conn =
                    DriverManager.getConnection(url, dto.getUsername(), dto.getPassword())) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean testExistingConnection(Long id) {
        // 获取数据源
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(id);
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 创建一个DTO并填充已保存的连接参数
        DatasourceCreateDTO dto = new DatasourceCreateDTO();
        dto.setType(datasourceInfo.getType());
        dto.setIp(datasourceInfo.getIp());
        dto.setPort(datasourceInfo.getPort());
        dto.setUsername(datasourceInfo.getUsername());
        dto.setPassword(datasourceInfo.getPassword());
        dto.setDatabase(datasourceInfo.getDatabase());
        dto.setJdbcParams(datasourceInfo.getJdbcParams());

        // 使用已保存的参数测试连接
        return testConnection(dto);
    }
}
