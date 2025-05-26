package com.hina.log.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 创建机器信息DTO */
@Data
@Schema(description = "创建机器信息对象")
public class MachineCreateDTO {
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

    @Schema(description = "机器密码", example = "password123")
    private String password;

    @Schema(description = "机器SSH密钥")
    private String sshKey;
}
