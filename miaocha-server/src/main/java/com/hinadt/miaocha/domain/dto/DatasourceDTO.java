package com.hinadt.miaocha.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/** 数据源DTO，用于接口返回，不包含敏感信息 */
@Data
@Schema(description = "数据源响应对象")
public class DatasourceDTO {
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

    @Schema(description = "数据库名称", example = "logs_db")
    private String database;

    @Schema(description = "JDBC连接参数，JSON格式", example = "{\"connectTimeout\":3000}")
    private String jdbcParams;

    @Schema(description = "创建时间", example = "2023-06-01T10:30:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2023-06-01T10:30:00")
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
