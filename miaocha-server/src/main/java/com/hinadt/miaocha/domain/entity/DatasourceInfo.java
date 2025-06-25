package com.hinadt.miaocha.domain.entity;

import com.hinadt.miaocha.common.annotation.UserAuditable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/** 数据源实体类 */
@Data
@Schema(description = "数据源实体")
public class DatasourceInfo implements UserAuditable {
    @Schema(description = "数据源ID", example = "1")
    private Long id;

    @Schema(description = "数据源名称", example = "测试Doris")
    private String name;

    @Schema(description = "数据源类型", example = "DORIS")
    private String type;

    @Schema(description = "数据源描述", example = "用于测试的Doris数据库")
    private String description;

    @Schema(
            description = "JDBC连接URL",
            example = "jdbc:mysql://192.168.1.100:9030/logs_db?connectTimeout=3000")
    private String jdbcUrl;

    @Schema(description = "数据源用户名", example = "admin")
    private String username;

    @Schema(description = "数据源密码", example = "password123")
    private String password;

    @Schema(description = "创建时间", example = "2023-06-01T10:30:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2023-06-01T10:30:00")
    private LocalDateTime updateTime;

    @Schema(description = "创建人邮箱")
    private String createUser;

    /** 修改人邮箱 */
    private String updateUser;
}
