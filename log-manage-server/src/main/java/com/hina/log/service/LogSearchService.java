package com.hina.log.service;

import com.hina.log.dto.LogSearchDTO;
import com.hina.log.dto.LogSearchResultDTO;
import com.hina.log.dto.SchemaInfoDTO;

import java.util.List;

/**
 * 日志检索服务接口
 */
public interface LogSearchService {
    /**
     * 执行日志检索
     *
     * @param userId 用户ID
     * @param dto    检索请求参数
     * @return 检索结果
     */
    LogSearchResultDTO search(Long userId, LogSearchDTO dto);

    /**
     * 获取日志检索表结构信息
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID
     * @param module       模块名称
     * @return 表结构信息（包含列名、数据类型、是否主键等详细信息）
     */
    List<SchemaInfoDTO.ColumnInfoDTO> getTableColumns(Long userId, Long datasourceId, String module);

}