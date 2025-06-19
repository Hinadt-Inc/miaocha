package com.hinadt.miaocha.domain.dto.logstash;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "Logstash进程响应DTO")
public class LogstashProcessResponseDTO {

    @Schema(description = "Logstash进程ID")
    private Long id;

    @Schema(description = "Logstash进程名称")
    private String name;

    @Schema(description = "关联的模块ID")
    private Long moduleId;

    @Schema(description = "模块名称")
    private String moduleName;

    @Schema(description = "数据源名称")
    private String datasourceName;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "Logstash配置文件内容")
    private String configContent;

    @Schema(description = "JVM配置文件内容")
    private String jvmOptions;

    @Schema(description = "Logstash YML配置文件内容")
    private String logstashYml;

    @Schema(description = "自定义的Logstash安装包路径")
    private String customPackagePath;

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

    @Schema(description = "关联的LogstashMachine实例状态列表")
    private List<LogstashMachineStatusInfoDTO> logstashMachineStatusInfo;

    @Data
    @Schema(description = "Logstash机器状态信息DTO")
    public static class LogstashMachineStatusInfoDTO {
        @Schema(description = "LogstashMachine实例ID")
        private Long logstashMachineId;

        @Schema(description = "机器ID")
        private Long machineId;

        @Schema(description = "机器名称")
        private String machineName;

        @Schema(description = "机器IP")
        private String machineIp;

        @Schema(description = "Logstash在该机器上的状态")
        private LogstashMachineState state;

        @Schema(description = "Logstash在该机器上的状态描述")
        private String stateDescription;

        @Schema(description = "目标机器上的进程PID")
        private String processPid;

        @Schema(description = "部署目录路径")
        private String deployPath;
    }
}
