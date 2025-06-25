package com.hinadt.miaocha.domain.entity;

import com.hinadt.miaocha.common.annotation.UserAuditable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/** 机器元信息实体类 */
@Data
@Schema(description = "机器元信息实体")
public class MachineInfo implements UserAuditable {
    @Schema(description = "机器ID", example = "1")
    private Long id;

    @Schema(description = "机器名称", example = "测试服务器")
    private String name;

    @Schema(description = "机器IP", example = "192.168.1.100")
    private String ip;

    @Schema(description = "机器端口", example = "22")
    private Integer port;

    @Schema(description = "机器用户名", example = "root")
    private String username;

    @Schema(description = "机器密码", example = "password123")
    private String password;

    @Schema(description = "机器SSH密钥")
    private String sshKey;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人邮箱")
    private String createUser;

    /** 修改人邮箱 */
    private String updateUser;
}
