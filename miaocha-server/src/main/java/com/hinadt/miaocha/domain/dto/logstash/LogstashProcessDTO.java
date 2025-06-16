package com.hinadt.miaocha.domain.dto.logstash;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** Logstash进程信息DTO */
@Data
@Schema(description = "Logstash进程信息对象")
public class LogstashProcessDTO {
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

    @Schema(description = "部署的Logstash实例列表")
    private List<LogstashMachineDTO> instances;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
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
