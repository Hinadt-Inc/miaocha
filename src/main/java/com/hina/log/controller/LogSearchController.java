package com.hina.log.controller;

import com.hina.log.dto.ApiResponse;
import com.hina.log.dto.LogSearchDTO;
import com.hina.log.dto.LogSearchResultDTO;
import com.hina.log.dto.SchemaInfoDTO;
import com.hina.log.service.LogSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 日志检索接口控制器
 */
@RestController
@RequestMapping("/api/logs")
@Tag(name = "日志检索分析", description = "提供日志检索、查询")
public class LogSearchController {

    @Autowired
    private LogSearchService logSearchService;

    /**
     * 执行日志检索
     *
     * @param dto 日志检索请求参数
     * @return 日志检索结果
     */
    @PostMapping("/search")
    @Operation(summary = "执行日志检索", description = "根据时间范围、关键字等条件查询日志，返回日志数据及统计信息")
    public ApiResponse<LogSearchResultDTO> search(
            @Parameter(description = "日志检索请求", required = true) @Valid @RequestBody LogSearchDTO dto) {
        // 实际项目中需要从安全上下文获取用户ID，这里先mock
        Long userId = 1L; // 假设是ID为1的用户

        LogSearchResultDTO result = logSearchService.search(userId, dto);
        return ApiResponse.success(result);
    }

    /**
     * 获取日志表的字段列表
     *
     * @param datasourceId 数据源ID
     * @param tableName    表名
     * @return 字段详细信息列表
     */
    @GetMapping("/columns")
    @Operation(summary = "获取日志表字段", description = "获取指定日志表的字段详细信息列表，包含字段名、类型、注释等信息，用于前端构建查询界面")
    public ApiResponse<List<SchemaInfoDTO.ColumnInfoDTO>> getTableColumns(
            @Parameter(description = "数据源ID", required = true) @RequestParam("datasourceId") Long datasourceId,
            @Parameter(description = "表名", required = true) @RequestParam("tableName") String tableName) {
        // 实际项目中需要从安全上下文获取用户ID，这里先mock
        Long userId = 1L; // 假设是ID为1的用户

        List<SchemaInfoDTO.ColumnInfoDTO> columns = logSearchService.getTableColumns(userId, datasourceId, tableName);
        return ApiResponse.success(columns);
    }

}