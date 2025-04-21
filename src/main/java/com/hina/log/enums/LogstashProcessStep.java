package com.hina.log.enums;

import lombok.Getter;

/**
 * Logstash进程操作步骤枚举
 */
@Getter
public enum LogstashProcessStep {
    CREATE_REMOTE_DIR("创建远程目录"),
    UPLOAD_PACKAGE("上传Logstash安装包"),
    EXTRACT_PACKAGE("解压Logstash安装包"),
    CREATE_CONFIG("创建配置文件"),
    START_PROCESS("启动Logstash进程"),
    VERIFY_PROCESS("验证进程状态"),
    STOP_PROCESS("停止Logstash进程");

    private final String name;

    LogstashProcessStep(String name) {
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