package com.hinadt.miaocha.endpoint;

import com.hinadt.miaocha.application.service.LogSearchService;
import com.hinadt.miaocha.domain.dto.ApiResponse;
import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.dto.cache.BatchDeleteCacheDTO;
import com.hinadt.miaocha.domain.dto.cache.SystemCacheDTO;
import com.hinadt.miaocha.domain.dto.logsearch.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** 日志检索接口控制器 */
@RestController
@RequestMapping("/api/logs")
@Tag(name = "日志检索分析", description = "提供日志检索、查询")
public class LogSearchEndpoint {

    private final LogSearchService logSearchService;

    public LogSearchEndpoint(LogSearchService logSearchService) {
        this.logSearchService = logSearchService;
    }

    /**
     * 仅执行日志明细查询
     *
     * @param dto 日志检索请求参数
     * @return 日志明细查询结果
     */
    @PostMapping("/search/details")
    @Operation(summary = "执行日志明细查询", description = "仅查询日志详细行记录，不包含统计信息")
    public ApiResponse<LogDetailResultDTO> searchDetails(
            @Parameter(description = "日志检索请求", required = true) @Valid @RequestBody
                    LogSearchDTO dto) {
        LogDetailResultDTO result = logSearchService.searchDetails(dto);
        return ApiResponse.success(result);
    }

    /**
     * 仅执行日志时间分布查询（柱状图数据）
     *
     * @param dto 日志检索请求参数
     * @return 日志时间分布查询结果
     */
    @PostMapping("/search/histogram")
    @Operation(summary = "执行日志时间分布查询", description = "仅查询日志时间分布数据，用于生成柱状图")
    public ApiResponse<LogHistogramResultDTO> searchHistogram(
            @Parameter(description = "日志检索请求", required = true) @Valid @RequestBody
                    LogSearchDTO dto) {
        LogHistogramResultDTO result = logSearchService.searchHistogram(dto);
        return ApiResponse.success(result);
    }

    /**
     * 执行字段TOP5分布查询，使用Doris TOPN函数 使用LogSearchDTO中的fields字段指定需要查询分布的字段列表
     *
     * @param dto 日志检索请求参数，其中fields字段指定需要查询分布的字段列表
     * @return 字段分布查询结果
     */
    @PostMapping("/search/field-distributions")
    @Operation(
            summary = "执行字段分布查询",
            description = "仅查询指定字段的TOP5分布数据，使用Doris TOPN函数，字段列表由dto中的fields指定")
    public ApiResponse<LogFieldDistributionResultDTO> searchFieldDistributions(
            @Parameter(description = "日志检索请求", required = true) @Valid @RequestBody
                    LogSearchDTO dto) {
        LogFieldDistributionResultDTO result = logSearchService.searchFieldDistributions(dto);
        return ApiResponse.success(result);
    }

    /**
     * 获取日志表的字段列表
     *
     * @param module 模块名称
     * @return 字段详细信息列表
     */
    @GetMapping("/columns")
    @Operation(summary = "获取日志表字段", description = "获取指定模块的字段详细信息列表，包含字段名、类型、注释等信息，用于前端构建查询界面")
    public ApiResponse<List<SchemaInfoDTO.ColumnInfoDTO>> getTableColumns(
            @Parameter(description = "模块名称", required = true) @RequestParam("module")
                    String module) {
        List<SchemaInfoDTO.ColumnInfoDTO> columns = logSearchService.getTableColumns(module);
        return ApiResponse.success(columns);
    }

    /**
     * 保存用户个性化的日志搜索条件
     *
     * @param dto 日志搜索条件缓存
     * @return 生成的缓存键
     */
    @PostMapping("/search/save-condition")
    @Operation(summary = "保存搜索条件", description = "保存用户个性化的日志搜索条件，返回生成的缓存键")
    public ApiResponse<String> saveSearchCondition(
            @Parameter(description = "日志搜索条件缓存", required = true) @Valid @RequestBody
                    LogSearchCacheDTO dto) {
        String cacheKey = logSearchService.saveSearchCondition(dto);
        return ApiResponse.success(cacheKey);
    }

    /**
     * 获取用户个性化的日志搜索条件数据
     *
     * @return 用户的搜索条件缓存列表
     */
    @GetMapping("/search/conditions")
    @Operation(summary = "获取搜索条件列表", description = "获取当前用户保存的所有个性化日志搜索条件")
    public ApiResponse<List<SystemCacheDTO<LogSearchCacheDTO>>> getUserSearchConditions() {
        List<SystemCacheDTO<LogSearchCacheDTO>> conditions =
                logSearchService.getUserSearchConditions();
        return ApiResponse.success(conditions);
    }

    /**
     * 批量删除用户个性化的日志搜索条件
     *
     * @param deleteCacheDTO 批量删除请求
     * @return 删除的数量
     */
    @DeleteMapping("/search/conditions")
    @Operation(summary = "批量删除搜索条件", description = "批量删除用户个性化的日志搜索条件")
    public ApiResponse<Void> batchDeleteSearchConditions(
            @Parameter(description = "批量删除请求", required = true) @Valid @RequestBody
                    BatchDeleteCacheDTO deleteCacheDTO) {
        logSearchService.batchDeleteSearchConditions(deleteCacheDTO);
        return ApiResponse.success();
    }
}
