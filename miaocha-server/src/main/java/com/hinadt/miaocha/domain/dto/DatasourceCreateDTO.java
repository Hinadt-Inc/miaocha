package com.hinadt.miaocha.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 数据源创建DTO，用于接收创建请求 */
@Data
@Schema(description = "数据源创建/更新请求对象")
public class DatasourceCreateDTO {
    @Schema(description = "数据源名称", example = "测试Doris", required = true)
    @NotBlank(message = "数据源名称不能为空")
    private String name;

    @Schema(description = "数据源类型", example = "DORIS", required = true)
    @NotBlank(message = "数据源类型不能为空")
    private String type;

    @Schema(description = "数据源描述", example = "用于测试的Doris数据库")
    private String description;

    @Schema(
            description = "JDBC连接URL",
            example = "jdbc:mysql://192.168.1.100:9030/logs_db?connectTimeout=3000",
            required = true)
    @NotBlank(message = "JDBC连接URL不能为空")
    private String jdbcUrl;

    @Schema(description = "数据源用户名", example = "admin", required = true)
    @NotBlank(message = "数据源用户名不能为空")
    private String username;

    @Schema(description = "数据源密码", example = "password123", required = true)
    @NotBlank(message = "数据源密码不能为空")
    private String password;
}
