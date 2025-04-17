package com.hina.log.controller;

import com.hina.log.dto.ApiResponse;
import com.hina.log.dto.SchemaInfoDTO;
import com.hina.log.dto.SqlQueryDTO;
import com.hina.log.dto.SqlQueryResultDTO;
import com.hina.log.service.SqlQueryService;
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

/**
 * SQL查询接口控制器
 */
@RestController
@RequestMapping("/api/sql")
@Tag(name = "自定义SQL查询", description = "提供SQL查询、数据库结构获取和文件导出等功能")
public class SqlEditorQueryController {

    @Autowired
    private SqlQueryService sqlQueryService;

    /**
     * 执行SQL查询
     * 
     * @param dto SQL查询请求参数
     * @return 查询结果
     */
    @PostMapping("/execute")
    @Operation(summary = "执行SQL查询", description = "执行用户输入的SQL查询语句，返回查询结果，可选择导出为文件")
    public ApiResponse<SqlQueryResultDTO> executeQuery(
            @Parameter(description = "SQL查询请求", required = true) @Valid @RequestBody SqlQueryDTO dto) {
        // 从安全上下文获取当前用户ID，这里简化处理，假设为1
        Long userId = 1L;

        SqlQueryResultDTO result = sqlQueryService.executeQuery(userId, dto);
        return ApiResponse.success(result);
    }

    /**
     * 获取数据库表结构
     * 
     * @param datasourceId 数据源ID
     * @return 表结构信息
     */
    @GetMapping("/schema/{datasourceId}")
    @Operation(summary = "获取数据库结构", description = "获取指定数据源的数据库结构信息，包括表和字段")
    public ApiResponse<SchemaInfoDTO> getSchemaInfo(
            @Parameter(description = "数据源ID", required = true) @PathVariable("datasourceId") Long datasourceId) {
        // 从安全上下文获取当前用户ID，这里简化处理，假设为1
        Long userId = 1L;

        SchemaInfoDTO schema = sqlQueryService.getSchemaInfo(userId, datasourceId);
        return ApiResponse.success(schema);
    }

    /**
     * 下载查询结果文件
     * 
     * @param queryId SQL查询历史ID
     * @return 结果文件
     */
    @GetMapping("/result/{queryId}")
    @Operation(summary = "下载查询结果", description = "根据查询历史ID下载保存的查询结果文件")
    public ResponseEntity<Resource> downloadQueryResult(
            @Parameter(description = "查询历史ID", required = true) @PathVariable("queryId") Long queryId) {
        Resource resource = sqlQueryService.getQueryResult(queryId);

        // 获取文件名和媒体类型
        String filename = "query_result_" + queryId;
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

        // 根据资源名称设置正确的媒体类型和文件名
        String resourceName = resource.getFilename();
        if (resourceName != null) {
            if (resourceName.toLowerCase().endsWith(".csv")) {
                filename += ".csv";
                mediaType = MediaType.parseMediaType("text/csv");
            } else if (resourceName.toLowerCase().endsWith(".xlsx")) {
                filename += ".xlsx";
                mediaType = MediaType
                        .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(mediaType)
                .body(resource);
    }
}