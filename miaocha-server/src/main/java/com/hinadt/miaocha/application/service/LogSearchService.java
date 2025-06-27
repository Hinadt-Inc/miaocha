package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.dto.LogDetailResultDTO;
import com.hinadt.miaocha.domain.dto.LogFieldDistributionResultDTO;
import com.hinadt.miaocha.domain.dto.LogHistogramResultDTO;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import java.util.List;

/** 日志检索服务接口 */
public interface LogSearchService {

    /**
     * 仅执行日志明细查询
     *
     * @param dto 检索请求参数
     * @return 日志明细查询结果
     */
    LogDetailResultDTO searchDetails(LogSearchDTO dto);

    /**
     * 仅执行日志时间分布查询（柱状图数据）
     *
     * @param dto 检索请求参数
     * @return 日志时间分布查询结果
     */
    LogHistogramResultDTO searchHistogram(LogSearchDTO dto);

    /**
     * 执行字段TOP5分布查询，使用Doris TOPN函数 使用LogSearchDTO中的fields字段指定需要查询分布的字段列表
     *
     * @param dto 检索请求参数，其中fields字段指定需要查询分布的字段列表
     * @return 字段分布查询结果
     */
    LogFieldDistributionResultDTO searchFieldDistributions(LogSearchDTO dto);

    /**
     * 获取日志检索表结构信息
     *
     * @param module 模块名称
     * @return 表结构信息（包含列名、数据类型、是否主键等详细信息）
     */
    List<SchemaInfoDTO.ColumnInfoDTO> getTableColumns(String module);
}
