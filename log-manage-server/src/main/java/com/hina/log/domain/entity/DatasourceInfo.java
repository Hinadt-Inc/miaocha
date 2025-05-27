package com.hina.log.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/** 数据源实体类 */
@Data
@Schema(description = "数据源实体")
public class DatasourceInfo {
    @Schema(description = "数据源ID", example = "1")
    private Long id;

    @Schema(description = "数据源名称", example = "测试Doris")
    private String name;

    @Schema(description = "数据源类型", example = "DORIS")
    private String type;

    @Schema(description = "数据源描述", example = "用于测试的Doris数据库")
    private String description;

    @Schema(description = "数据源IP地址", example = "192.168.1.100")
    private String ip;

    @Schema(description = "数据源端口", example = "9030")
    private Integer port;

    @Schema(description = "数据源用户名", example = "admin")
    private String username;

    @Schema(description = "数据源密码", example = "password123")
    private String password;

    @Schema(description = "数据库名称", example = "logs_db")
    private String database;

    @Schema(description = "JDBC连接参数，JSON格式", example = "{\"connectTimeout\":3000}")
    private String jdbcParams;

    @Schema(description = "创建时间", example = "2023-06-01T10:30:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2023-06-01T10:30:00")
    private LocalDateTime updateTime;
}
