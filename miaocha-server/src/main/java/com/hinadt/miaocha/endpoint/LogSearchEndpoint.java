package com.hinadt.miaocha.endpoint;

import com.hinadt.miaocha.application.service.LogSearchService;
import com.hinadt.miaocha.common.annotation.CurrentUser;
import com.hinadt.miaocha.domain.dto.ApiResponse;
import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogDetailResultDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogFieldDistributionResultDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogHistogramResultDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.user.UserDTO;
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
     * @param user 当前用户
     * @param dto 日志检索请求参数
     * @return 日志明细查询结果
     */
    @PostMapping("/search/details")
    @Operation(summary = "执行日志明细查询", description = "仅查询日志详细行记录，不包含统计信息")
    public ApiResponse<LogDetailResultDTO> searchDetails(
            @CurrentUser UserDTO user,
            @Parameter(description = "日志检索请求", required = true) @Valid @RequestBody
                    LogSearchDTO dto) {
        LogDetailResultDTO result = logSearchService.searchDetails(dto);
        return ApiResponse.success(result);
    }

    /**
     * 仅执行日志时间分布查询（柱状图数据）
     *
     * @param user 当前用户
     * @param dto 日志检索请求参数
     * @return 日志时间分布查询结果
     */
    @PostMapping("/search/histogram")
    @Operation(summary = "执行日志时间分布查询", description = "仅查询日志时间分布数据，用于生成柱状图")
    public ApiResponse<LogHistogramResultDTO> searchHistogram(
            @CurrentUser UserDTO user,
            @Parameter(description = "日志检索请求", required = true) @Valid @RequestBody
                    LogSearchDTO dto) {
        LogHistogramResultDTO result = logSearchService.searchHistogram(dto);
        return ApiResponse.success(result);
    }

    /**
     * 执行字段TOP5分布查询，使用Doris TOPN函数 使用LogSearchDTO中的fields字段指定需要查询分布的字段列表
     *
     * @param user 当前用户
     * @param dto 日志检索请求参数，其中fields字段指定需要查询分布的字段列表
     * @return 字段分布查询结果
     */
    @PostMapping("/search/field-distributions")
    @Operation(
            summary = "执行字段分布查询",
            description = "仅查询指定字段的TOP5分布数据，使用Doris TOPN函数，字段列表由dto中的fields指定")
    public ApiResponse<LogFieldDistributionResultDTO> searchFieldDistributions(
            @CurrentUser UserDTO user,
            @Parameter(description = "日志检索请求", required = true) @Valid @RequestBody
                    LogSearchDTO dto) {
        LogFieldDistributionResultDTO result = logSearchService.searchFieldDistributions(dto);
        return ApiResponse.success(result);
    }

    /**
     * 获取日志表的字段列表
     *
     * @param user 当前用户
     * @param module 模块名称
     * @return 字段详细信息列表
     */
    @GetMapping("/columns")
    @Operation(summary = "获取日志表字段", description = "获取指定模块的字段详细信息列表，包含字段名、类型、注释等信息，用于前端构建查询界面")
    public ApiResponse<List<SchemaInfoDTO.ColumnInfoDTO>> getTableColumns(
            @CurrentUser UserDTO user,
            @Parameter(description = "模块名称", required = true) @RequestParam("module")
                    String module) {
        List<SchemaInfoDTO.ColumnInfoDTO> columns = logSearchService.getTableColumns(module);
        return ApiResponse.success(columns);
    }
}
