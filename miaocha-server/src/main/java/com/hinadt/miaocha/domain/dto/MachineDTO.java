package com.hinadt.miaocha.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/** 机器信息DTO */
@Data
@Schema(description = "机器信息对象")
public class MachineDTO {
    @Schema(description = "机器ID", example = "1")
    private Long id;

    @Schema(description = "机器名称", example = "测试服务器")
    @NotBlank(message = "机器名称不能为空")
    private String name;

    @Schema(description = "机器IP", example = "192.168.1.100")
    @NotBlank(message = "机器IP不能为空")
    private String ip;

    @Schema(description = "机器端口", example = "22")
    @NotNull(message = "机器端口不能为空") private Integer port;

    @Schema(description = "机器用户名", example = "root")
    @NotBlank(message = "机器用户名不能为空")
    private String username;

    @Schema(description = "Logstash进程实例数量", example = "2")
    private Integer logstashMachineCount;

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
