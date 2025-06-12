package com.hinadt.miaocha.domain.entity;

import com.hinadt.miaocha.common.audit.UserAuditable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/** Logstash进程任务实体类 */
@Data
@Schema(description = "Logstash进程任务实体")
public class LogstashProcess implements UserAuditable {
    @Schema(description = "进程ID", example = "1")
    private Long id;

    @Schema(description = "进程名称", example = "Nginx日志收集")
    private String name;

    @Schema(description = "关联的模块ID", example = "1")
    private Long moduleId;

    @Schema(description = "Logstash配置文件内容")
    private String configContent;

    @Schema(description = "JVM配置选项模板")
    private String jvmOptions;

    @Schema(description = "Logstash系统配置模板")
    private String logstashYml;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "修改人")
    private String updateUser;
}
