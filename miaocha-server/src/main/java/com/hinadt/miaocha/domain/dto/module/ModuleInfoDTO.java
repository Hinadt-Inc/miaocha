package com.hinadt.miaocha.domain.dto.module;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/** 模块信息响应DTO */
@Data
@Schema(description = "模块信息响应")
public class ModuleInfoDTO {

    @Schema(description = "模块ID", example = "1")
    private Long id;

    @Schema(description = "模块名称", example = "Nginx日志模块")
    private String name;

    @Schema(description = "数据源ID", example = "1")
    private Long datasourceId;

    @Schema(description = "数据源名称", example = "生产环境MySQL")
    private String datasourceName;

    @Schema(description = "表名", example = "nginx_logs")
    private String tableName;

    @Schema(description = "Doris SQL语句")
    private String dorisSql;

    @Schema(description = "查询配置")
    private QueryConfigDTO queryConfig;

    @Schema(description = "模块状态：1-启用，0-禁用", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "创建人邮箱")
    private String createUser;

    @Schema(description = "创建人昵称")
    private String createUserName;

    @Schema(description = "修改人邮箱")
    private String updateUser;

    @Schema(description = "修改人昵称")
    private String updateUserName;
}
