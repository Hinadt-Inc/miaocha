package com.hinadt.miaocha.common.ssh;

import lombok.Builder;
import lombok.Data;

/** SSH连接配置 */
@Data
@Builder
public class SshConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private String privateKey;

    /** 连接超时时间（秒） */
    @Builder.Default private int connectTimeout = 60;

    /** 命令执行超时时间（分钟） */
    @Builder.Default private int commandTimeout = 10;

    /** 文件传输超时时间（分钟） */
    @Builder.Default private int transferTimeout = 30;
}
