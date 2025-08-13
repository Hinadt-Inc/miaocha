package com.hinadt.miaocha.domain.dto.logstash;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "Logstash process response DTO")
public class LogstashProcessResponseDTO {

    @Schema(description = "Logstash process ID")
    private Long id;

    @Schema(description = "Logstash process name")
    private String name;

    @Schema(description = "Associated module ID")
    private Long moduleId;

    @Schema(description = "Module name")
    private String moduleName;

    @Schema(description = "Datasource name")
    private String datasourceName;

    @Schema(description = "Table name")
    private String tableName;

    @Schema(description = "Logstash config content")
    private String configContent;

    @Schema(description = "JVM options file content")
    private String jvmOptions;

    @Schema(description = "Logstash YML file content")
    private String logstashYml;

    @Schema(description = "Alert recipients list (emails)")
    private List<String> alertRecipients;

    @Schema(description = "Create time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;

    @Schema(description = "Creator email")
    private String createUser;

    @Schema(description = "Creator nickname")
    private String createUserName;

    @Schema(description = "Updater email")
    private String updateUser;

    @Schema(description = "Updater nickname")
    private String updateUserName;

    @Schema(description = "Related LogstashMachine instance status list")
    private List<LogstashMachineStatusInfoDTO> logstashMachineStatusInfo;

    @Data
    @Schema(description = "Logstash machine status info DTO")
    public static class LogstashMachineStatusInfoDTO {
        @Schema(description = "LogstashMachine instance ID")
        private Long logstashMachineId;

        @Schema(description = "Machine ID")
        private Long machineId;

        @Schema(description = "Machine name")
        private String machineName;

        @Schema(description = "Machine IP")
        private String machineIp;

        @Schema(description = "Logstash state on the machine")
        private LogstashMachineState state;

        @Schema(description = "Logstash state description on the machine")
        private String stateDescription;

        @Schema(description = "Process PID on target machine")
        private String processPid;

        @Schema(description = "Deploy directory path")
        private String deployPath;

        @Schema(description = "Last update time of the instance state")
        private LocalDateTime lastUpdateTime;
    }
}
