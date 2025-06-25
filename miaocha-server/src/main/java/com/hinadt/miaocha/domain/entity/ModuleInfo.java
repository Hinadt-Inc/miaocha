package com.hinadt.miaocha.domain.entity;

import com.hinadt.miaocha.common.annotation.UserAuditable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/** 模块信息实体类 */
@Data
@Schema(description = "模块信息实体")
public class ModuleInfo implements UserAuditable {
    @Schema(description = "模块ID", example = "1")
    private Long id;

    @Schema(description = "模块名称", example = "Nginx日志模块")
    private String name;

    @Schema(description = "数据源ID", example = "1")
    private Long datasourceId;

    @Schema(description = "表名", example = "nginx_logs")
    private String tableName;

    @Schema(description = "Doris SQL语句")
    private String dorisSql;

    /**
     * 查询配置JSON字符串 用于配置模块的查询相关设置，包括： 1. timeField: 指定用于时间分析的字段名 2. keywordFields: 配置关键词检索字段及其检索方法
     *
     * <p>JSON格式示例： { "timeField": "log_time", "keywordFields": [ { "fieldName": "message",
     * "searchMethod": "LIKE" }, { "fieldName": "level", "searchMethod": "MATCH_ALL" } ] }
     *
     * <p>searchMethod支持的值： - LIKE: 模糊匹配 - MATCH_ALL: 全词匹配 - MATCH_ANY: 任意词匹配 - MATCH_PHRASE: 短语匹配
     */
    @Schema(description = "查询配置JSON，包含时间字段和关键词检索字段配置")
    private String queryConfig;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人邮箱")
    private String createUser;

    /** 修改人邮箱 */
    private String updateUser;
}
