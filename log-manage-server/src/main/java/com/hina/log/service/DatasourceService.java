package com.hina.log.service;

import com.hina.log.dto.DatasourceCreateDTO;
import com.hina.log.dto.DatasourceDTO;
import java.util.List;

/**
 * 数据源服务接口
 */
public interface DatasourceService {

    /**
     * 创建数据源
     */
    DatasourceDTO createDatasource(DatasourceCreateDTO dto);

    /**
     * 更新数据源
     */
    DatasourceDTO updateDatasource(Long id, DatasourceCreateDTO dto);

    /**
     * 删除数据源
     */
    void deleteDatasource(Long id);

    /**
     * 获取数据源详情
     */
    DatasourceDTO getDatasource(Long id);

    /**
     * 获取所有数据源
     */
    List<DatasourceDTO> getAllDatasources();

    /**
     * 测试数据源连接
     */
    boolean testConnection(DatasourceCreateDTO dto);

    /**
     * 测试已保存数据源的连接
     */
    boolean testExistingConnection(Long id);
}