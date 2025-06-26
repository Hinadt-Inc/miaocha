package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.dto.DatasourceConnectionTestResultDTO;
import com.hinadt.miaocha.domain.dto.DatasourceCreateDTO;
import com.hinadt.miaocha.domain.dto.DatasourceDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import java.util.List;

/** 数据源服务接口 */
public interface DatasourceService {

    /** 创建数据源 */
    DatasourceDTO createDatasource(DatasourceCreateDTO dto);

    /** 更新数据源 */
    DatasourceDTO updateDatasource(Long id, DatasourceCreateDTO dto);

    /** 删除数据源 */
    void deleteDatasource(Long id);

    /** 获取数据源详情 */
    DatasourceDTO getDatasource(Long id);

    /** 获取所有数据源 */
    List<DatasourceDTO> getAllDatasources();

    /** 根据模块名称获取数据源 */
    DatasourceDTO getDatasourceByModule(String module);

    /** 根据模块名称获取数据源详细信息（包含敏感信息，仅供内部使用） */
    DatasourceInfo getDatasourceInfoByModule(String module);

    /** 测试数据源连接 */
    DatasourceConnectionTestResultDTO testConnection(DatasourceCreateDTO dto);

    /** 测试已保存数据源的连接 */
    DatasourceConnectionTestResultDTO testExistingConnection(Long id);
}
