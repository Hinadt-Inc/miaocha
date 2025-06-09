package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.dto.SqlHistoryQueryDTO;
import com.hinadt.miaocha.domain.dto.SqlHistoryResponseDTO;
import com.hinadt.miaocha.domain.dto.SqlQueryDTO;
import com.hinadt.miaocha.domain.dto.SqlQueryResultDTO;
import org.springframework.core.io.Resource;

/** SQL查询服务接口 */
public interface SqlQueryService {

    /**
     * 执行SQL查询
     *
     * @param userId 用户ID
     * @param dto 查询请求DTO
     * @return 查询结果
     */
    SqlQueryResultDTO executeQuery(Long userId, SqlQueryDTO dto);

    /**
     * 获取指定数据源的表结构信息
     *
     * @param userId 用户ID
     * @param datasourceId 数据源ID
     * @return 表结构信息列表
     */
    SchemaInfoDTO getSchemaInfo(Long userId, Long datasourceId);

    /**
     * 获取查询结果文件
     *
     * @param queryId 查询ID
     * @return 查询结果文件
     */
    Resource getQueryResult(Long queryId);

    /**
     * 分页查询SQL查询历史
     *
     * @param userId 用户ID
     * @param dto 查询参数
     * @return 分页查询结果
     */
    SqlHistoryResponseDTO getQueryHistory(Long userId, SqlHistoryQueryDTO dto);
}
