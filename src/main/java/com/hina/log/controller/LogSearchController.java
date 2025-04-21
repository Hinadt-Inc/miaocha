package com.hina.log.controller;

import com.hina.log.annotation.CurrentUser;
import com.hina.log.dto.ApiResponse;
import com.hina.log.dto.LogSearchDTO;
import com.hina.log.dto.LogSearchResultDTO;
import com.hina.log.dto.SchemaInfoDTO;
import com.hina.log.dto.user.UserDTO;
import com.hina.log.service.LogSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 日志检索接口控制器
 */
@RestController
@RequestMapping("/api/logs")
@Tag(name = "日志检索分析", description = "提供日志检索、查询")
@RequiredArgsConstructor
public class LogSearchController {

    private final LogSearchService logSearchService;

    /**
     * 执行日志检索
     *
     * @param user 当前用户
     * @param dto  日志检索请求参数
     * @return 日志检索结果
     */
    @PostMapping("/search")
    @Operation(summary = "执行日志检索", description = "根据时间范围、关键字等条件查询日志，返回日志数据及统计信息")
    public ApiResponse<LogSearchResultDTO> search(
            @CurrentUser UserDTO user,
            @Parameter(description = "日志检索请求", required = true) @Valid @RequestBody LogSearchDTO dto) {
        LogSearchResultDTO result = logSearchService.search(user.getId(), dto);
        return ApiResponse.success(result);
    }

    /**
     * 获取日志表的字段列表
     *
     * @param user         当前用户
     * @param datasourceId 数据源ID
     * @param tableName    表名
     * @return 字段详细信息列表
     */
    @GetMapping("/columns")
    @Operation(summary = "获取日志表字段", description = "获取指定日志表的字段详细信息列表，包含字段名、类型、注释等信息，用于前端构建查询界面")
    public ApiResponse<List<SchemaInfoDTO.ColumnInfoDTO>> getTableColumns(
            @CurrentUser UserDTO user,
            @Parameter(description = "数据源ID", required = true) @RequestParam("datasourceId") Long datasourceId,
            @Parameter(description = "表名", required = true) @RequestParam("tableName") String tableName) {
        List<SchemaInfoDTO.ColumnInfoDTO> columns = logSearchService.getTableColumns(user.getId(), datasourceId,
                tableName);
        return ApiResponse.success(columns);
    }

}