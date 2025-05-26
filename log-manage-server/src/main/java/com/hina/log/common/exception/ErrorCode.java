package com.hina.log.common.exception;

import lombok.Getter;

/** 错误码枚举 */
@Getter
public enum ErrorCode {
    // 通用错误码
    SUCCESS("0000", "操作成功"),
    INTERNAL_ERROR("9999", "服务器内部错误"),
    VALIDATION_ERROR("1000", "参数校验失败"),
    RESOURCE_NOT_FOUND("1001", "请求的资源不存在"),

    // 数据源相关错误码
    DATASOURCE_NOT_FOUND("2001", "数据源不存在"),
    DATASOURCE_NAME_EXISTS("2002", "数据源名称已存在"),
    DATASOURCE_CONNECTION_FAILED("2003", "数据源连接失败"),
    DATASOURCE_TYPE_NOT_SUPPORTED("2004", "不支持的数据源类型"),

    // 用户相关错误码
    USER_NOT_FOUND("3001", "用户不存在"),
    USER_NAME_EXISTS("3002", "用户名已存在"),
    USER_PASSWORD_ERROR("3003", "用户名或密码错误"),

    // 认证相关错误码
    INVALID_TOKEN("3101", "无效的令牌"),
    TOKEN_EXPIRED("3102", "令牌已过期，请刷新"),
    REFRESH_TOKEN_EXPIRED("3103", "刷新令牌已过期，请重新登录"),

    // 权限相关错误码
    PERMISSION_DENIED("4001", "权限不足"),
    PERMISSION_NOT_FOUND("4002", "权限不存在"),
    NO_ADMIN_PERMISSION("4003", "无管理员权限，无法访问"),

    // 导出相关错误码
    EXPORT_FAILED("5001", "导出失败"),

    // 机器相关错误码
    MACHINE_NOT_FOUND("6001", "机器不存在"),
    MACHINE_NAME_EXISTS("6002", "机器名称已存在"),
    MACHINE_CONNECTION_FAILED("6003", "机器连接失败"),

    // SSH相关错误码
    SSH_OPERATION_FAILED("6101", "SSH操作失败"),

    @Deprecated
    SSH_DEPENDENCY_MISSING("6102", "SSH依赖缺失"),

    SSH_COMMAND_FAILED("6103", "SSH命令执行失败"),
    SSH_FILE_TRANSFER_FAILED("6104", "SSH文件传输失败"),

    // Logstash相关错误码
    LOGSTASH_PROCESS_NOT_FOUND("7001", "Logstash进程不存在"),
    LOGSTASH_PROCESS_NAME_EXISTS("7002", "Logstash进程名称已存在"),
    LOGSTASH_DEPLOY_FAILED("7003", "Logstash部署失败"),
    LOGSTASH_START_FAILED("7004", "Logstash启动失败"),
    LOGSTASH_STOP_FAILED("7005", "Logstash停止失败"),
    LOGSTASH_CONFIG_NOT_FOUND("7006", "Logstash配置不存在"),
    LOGSTASH_CONFIG_INVALID("7007", "Logstash配置无效"),
    LOGSTASH_CONFIG_KAFKA_MISSING("7008", "Logstash配置缺少Kafka输入配置"),
    LOGSTASH_CONFIG_DORIS_MISSING("7009", "Logstash配置缺少Doris输出配置"),
    LOGSTASH_CONFIG_TABLE_MISSING("7010", "Logstash配置缺少表名"),
    LOGSTASH_TARGET_TABLE_NOT_FOUND("7011", "目标数据源中不存在指定的表"),
    MODULE_NOT_FOUND("7012", "未找到指定的模块"),
    LOGSTASH_MODULE_EXISTS("7013", "Logstash模块名称已存在"),

    // 任务相关错误码
    TASK_EXECUTION_FAILED("8001", "任务执行失败"),
    TASK_NOT_FOUND("8002", "任务不存在"),
    TASK_RETRY_FAILED("8003", "任务重试失败");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
