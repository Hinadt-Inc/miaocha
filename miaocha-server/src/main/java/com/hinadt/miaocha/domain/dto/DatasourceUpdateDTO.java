package com.hinadt.miaocha.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 数据源更新DTO，用于接收更新请求 */
@Data
@Schema(description = "数据源更新请求对象")
@SuperBuilder
@NoArgsConstructor
public class DatasourceUpdateDTO {
    @Schema(description = "新的数据源名称", example = "测试Doris-更新")
    private String name;

    @Schema(description = "新的数据源类型", example = "DORIS")
    private String type;

    @Schema(description = "新的数据源描述", example = "更新后的Doris数据库")
    private String description;

    @Schema(
            description = "新的JDBC连接URL",
            example = "jdbc:mysql://192.168.1.101:9030/logs_db?connectTimeout=5000")
    private String jdbcUrl;

    @Schema(description = "新的数据源用户名", example = "new_admin")
    private String username;

    @Schema(description = "新的数据源密码，不提供则不更新", example = "new_password123")
    private String password;
}
