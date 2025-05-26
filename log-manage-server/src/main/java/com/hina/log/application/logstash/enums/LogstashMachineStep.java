package com.hina.log.application.logstash.enums;

import lombok.Getter;

/** Logstash进程操作步骤枚举 */
@Getter
public enum LogstashMachineStep {
    CREATE_REMOTE_DIR("创建远程目录"),
    UPLOAD_PACKAGE("上传Logstash安装包"),
    EXTRACT_PACKAGE("解压Logstash安装包"),
    CREATE_CONFIG("创建配置文件"), // Represents creation of main logstash .conf
    MODIFY_CONFIG("修改系统配置"), // Might be too generic, or could represent jvm/yml initial setup
    UPDATE_MAIN_CONFIG("更新Logstash主配置"), // New: For logstash .conf files
    UPDATE_JVM_CONFIG("更新JVM配置"), // New: For jvm.options
    UPDATE_SYSTEM_CONFIG("更新系统配置"), // New: For logstash.yml
    REFRESH_CONFIG("刷新配置"), // This usually implies a HUP signal or dynamic reload, not file change.
    START_PROCESS("启动Logstash进程"),
    VERIFY_PROCESS("验证进程状态"),
    STOP_PROCESS("停止Logstash进程");

    private final String name;

    LogstashMachineStep(String name) {
        this.name = name;
    }

    /**
     * 获取步骤ID
     *
     * @return 步骤ID，与枚举名称相同
     */
    public String getId() {
        return name();
    }
}
