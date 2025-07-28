package com.hinadt.miaocha.common.exception;

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
    DATASOURCE_IN_USE("2005", "数据源正在被模块使用，无法删除"),

    // 用户相关错误码
    USER_NOT_FOUND("3001", "用户不存在"),
    USER_NAME_EXISTS("3002", "用户名已存在"),
    USER_PASSWORD_ERROR("3003", "用户名或密码错误"),
    USER_FORBIDDEN("3004", "用户被禁用，无法登录"),

    // 认证相关错误码
    INVALID_TOKEN("3101", "无效的令牌"),
    TOKEN_EXPIRED("3102", "令牌已过期，请刷新"),
    REFRESH_TOKEN_EXPIRED("3103", "刷新令牌已过期，请重新登录"),
    UNAUTHORIZED("3104", "未授权的访问，请登录"),
    OAUTH_USER_CANNOT_REFRESH_TOKEN("3105", "第三方登录用户不支持刷新令牌，请重新登录"),

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
    MACHINE_IN_USE("6004", "机器正在被Logstash实例使用，无法删除"),

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
    LOGSTASH_MACHINE_ALREADY_ASSOCIATED("7014", "LogstashMachine实例已关联"),
    LOGSTASH_MACHINE_NOT_FOUND("7017", "LogstashMachine实例不存在"),
    LOGSTASH_CANNOT_SCALE_TO_ZERO("7015", "不能将进程缩容到零个实例"),
    LOGSTASH_MACHINE_RUNNING_CANNOT_REMOVE("7016", "运行中的LogstashMachine实例不能被移除"),

    // 任务相关错误码
    TASK_EXECUTION_FAILED("8001", "任务执行失败"),
    TASK_NOT_FOUND("8002", "任务不存在"),

    // SQL执行相关错误码
    SQL_EXECUTION_FAILED("9001", "SQL执行失败"),
    SQL_NOT_CREATE_TABLE("9002", "只允许执行CREATE TABLE语句"),
    SQL_TABLE_NAME_MISMATCH("9003", "SQL中的表名与模块配置的表名不一致"),

    // 日志查询相关错误码
    KEY_WORD_QUERY_SYNTAX_ERROR("9100", "关键字查询语法错误"),
    LOG_DETAIL_QUERY_FAILED("9101", "日志详情查询失败"),
    LOG_COUNT_QUERY_FAILED("9102", "日志总数查询失败"),
    LOG_HISTOGRAM_QUERY_FAILED("9103", "日志时间分布查询失败"),
    LOG_FIELD_DISTRIBUTION_QUERY_FAILED("9104", "日志字段分布查询失败"),

    // 表结构校验相关错误码
    TABLE_MESSAGE_FIELD_MISSING("7015", "表结构中缺少必需的message字段，无法进行关键字搜索"),
    TABLE_FIELD_VALIDATION_FAILED("7016", "表结构字段校验失败"),

    // 模块查询配置相关错误码
    MODULE_DORIS_SQL_NOT_CONFIGURED("7018", "模块尚未配置建表SQL语句，请先执行建表操作"),
    MODULE_QUERY_FIELD_NOT_EXISTS("7019", "配置的查询字段不存在于表结构中"),
    MODULE_QUERY_CONFIG_NOT_FOUND("7020", "模块未配置查询信息"),
    TIME_FIELD_NOT_CONFIGURED("7021", "模块未配置时间字段"),
    KEYWORD_FIELDS_NOT_CONFIGURED("7022", "模块未配置关键字查询字段"),
    KEYWORD_FIELD_NOT_ALLOWED("7023", "字段不允许进行关键字查询");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
