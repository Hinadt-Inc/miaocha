package com.hina.log.service.impl;

import com.hina.log.dto.DatasourceCreateDTO;
import com.hina.log.dto.DatasourceDTO;
import com.hina.log.entity.Datasource;
import com.hina.log.entity.enums.DatasourceType;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.DatasourceMapper;
import com.hina.log.service.DatasourceService;
import com.hina.log.converter.DatasourceConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据源服务实现类
 */
@Service
public class DatasourceServiceImpl implements DatasourceService {

    @Autowired
    private DatasourceMapper datasourceMapper;

    @Autowired
    private DatasourceConverter datasourceConverter;

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
        Datasource datasource = datasourceConverter.toEntity(dto);
        datasourceMapper.insert(datasource);

        // 返回DTO
        return datasourceConverter.toDto(datasource);
    }

    @Override
    @Transactional
    public DatasourceDTO updateDatasource(Long id, DatasourceCreateDTO dto) {
        // 检查数据源是否存在
        Datasource existingDatasource = datasourceMapper.selectById(id);
        if (existingDatasource == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 检查名称是否与其他数据源重复
        Datasource sameNameDatasource = datasourceMapper.selectByName(dto.getName());
        if (sameNameDatasource != null && !sameNameDatasource.getId().equals(id)) {
            throw new BusinessException(ErrorCode.DATASOURCE_NAME_EXISTS);
        }

        // 测试连接
        if (!testConnection(dto)) {
            throw new BusinessException(ErrorCode.DATASOURCE_CONNECTION_FAILED);
        }

        // 更新数据源
        Datasource datasource = datasourceConverter.updateEntity(existingDatasource, dto);
        datasource.setId(id);
        datasourceMapper.update(datasource);

        return datasourceConverter.toDto(datasource);
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
        Datasource datasource = datasourceMapper.selectById(id);
        if (datasource == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }
        return datasourceConverter.toDto(datasource);
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
            String url = datasourceType.buildJdbcUrl(
                    dto.getIp(),
                    dto.getPort(),
                    dto.getDatabase(),
                    dto.getJdbcParams());

            try (Connection conn = DriverManager.getConnection(
                    url,
                    dto.getUsername(),
                    dto.getPassword())) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean testExistingConnection(Long id) {
        // 获取数据源
        Datasource datasource = datasourceMapper.selectById(id);
        if (datasource == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 创建一个DTO并填充已保存的连接参数
        DatasourceCreateDTO dto = new DatasourceCreateDTO();
        dto.setType(datasource.getType());
        dto.setIp(datasource.getIp());
        dto.setPort(datasource.getPort());
        dto.setUsername(datasource.getUsername());
        dto.setPassword(datasource.getPassword());
        dto.setDatabase(datasource.getDatabase());
        dto.setJdbcParams(datasource.getJdbcParams());

        // 使用已保存的参数测试连接
        return testConnection(dto);
    }
}