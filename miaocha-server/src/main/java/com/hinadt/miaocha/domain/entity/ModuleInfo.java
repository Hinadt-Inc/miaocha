package com.hinadt.miaocha.domain.entity;

import com.hinadt.miaocha.common.audit.UserAuditable;
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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人邮箱")
    private String createUser;

    /** 修改人邮箱 */
    private String updateUser;
}
