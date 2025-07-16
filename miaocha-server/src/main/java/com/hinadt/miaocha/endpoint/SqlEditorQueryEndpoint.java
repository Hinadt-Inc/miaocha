package com.hinadt.miaocha.endpoint;

import com.hinadt.miaocha.application.service.SqlQueryService;
import com.hinadt.miaocha.common.annotation.CurrentUser;
import com.hinadt.miaocha.domain.dto.*;
import com.hinadt.miaocha.domain.dto.user.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** SQL查询接口控制器 */
@RestController
@RequestMapping("/api/sql")
@Tag(name = "自定义SQL查询", description = "提供SQL查询、数据库结构获取和文件导出等功能")
@RequiredArgsConstructor
public class SqlEditorQueryEndpoint {

    private final SqlQueryService sqlQueryService;

    /**
     * 执行SQL查询
     *
     * @param user 当前用户
     * @param dto SQL查询请求参数
     * @return 查询结果
     */
    @PostMapping("/execute")
    @Operation(summary = "执行SQL查询", description = "执行用户输入的SQL查询语句，返回查询结果，可选择导出为文件")
    public ApiResponse<SqlQueryResultDTO> executeQuery(
            @CurrentUser UserDTO user,
            @Parameter(description = "SQL查询请求", required = true) @Valid @RequestBody
                    SqlQueryDTO dto) {
        SqlQueryResultDTO result = sqlQueryService.executeQuery(user.getId(), dto);
        return ApiResponse.success(result);
    }

    /**
     * 获取数据库表列表
     *
     * @param user 当前用户
     * @param datasourceId 数据源ID
     * @return 数据库表列表
     */
    @GetMapping("/tables/{datasourceId}")
    @Operation(summary = "获取数据库表列表", description = "获取指定数据源的数据库表列表，不包含字段信息")
    public ApiResponse<DatabaseTableListDTO> getDatabaseTableList(
            @CurrentUser UserDTO user,
            @Parameter(description = "数据源ID", required = true) @PathVariable("datasourceId")
                    Long datasourceId) {
        DatabaseTableListDTO tableList =
                sqlQueryService.getDatabaseTableList(user.getId(), datasourceId);
        return ApiResponse.success(tableList);
    }

    /**
     * 获取指定表的字段信息
     *
     * @param user 当前用户
     * @param datasourceId 数据源ID
     * @param tableName 表名
     * @return 表字段信息
     */
    @GetMapping("/table-schema/{datasourceId}")
    @Operation(summary = "获取表字段信息", description = "获取指定数据源和表名的字段信息")
    public ApiResponse<TableSchemaDTO> getTableSchema(
            @CurrentUser UserDTO user,
            @Parameter(description = "数据源ID", required = true) @PathVariable("datasourceId")
                    Long datasourceId,
            @Parameter(description = "表名", required = true) @RequestParam("tableName")
                    String tableName) {
        TableSchemaDTO tableSchema =
                sqlQueryService.getTableSchema(user.getId(), datasourceId, tableName);
        return ApiResponse.success(tableSchema);
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
            @Parameter(description = "查询历史ID", required = true) @PathVariable("queryId")
                    Long queryId) {
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
                mediaType =
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(mediaType)
                .body(resource);
    }

    /**
     * 查询SQL执行历史
     *
     * @param user 当前用户
     * @param dto 查询参数
     * @return 分页查询结果
     */
    @GetMapping("/history")
    @Operation(summary = "查询SQL执行历史", description = "分页查询用户的SQL执行历史记录，支持按数据源、表名和查询关键字筛选")
    public ApiResponse<SqlHistoryResponseDTO> getQueryHistory(
            @CurrentUser UserDTO user,
            @Parameter(description = "查询参数") @Valid SqlHistoryQueryDTO dto) {
        SqlHistoryResponseDTO result = sqlQueryService.getQueryHistory(user.getId(), dto);
        return ApiResponse.success(result);
    }
}
