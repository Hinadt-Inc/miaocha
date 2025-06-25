package com.hinadt.miaocha.common.exception;

/**
 * 日志查询异常
 *
 * <p>专门用于日志查询过程中的异常处理，保留原始异常信息和查询上下文
 */
public class LogQueryException extends BusinessException {

    /** 查询类型 */
    private final String queryType;

    /** 查询SQL（可选，用于调试） */
    private final String sql;

    public LogQueryException(ErrorCode errorCode, String queryType) {
        super(errorCode);
        this.queryType = queryType;
        this.sql = null;
    }

    public LogQueryException(ErrorCode errorCode, String queryType, String message) {
        super(errorCode, message);
        this.queryType = queryType;
        this.sql = null;
    }

    public LogQueryException(
            ErrorCode errorCode, String queryType, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.queryType = queryType;
        this.sql = null;
    }

    public LogQueryException(
            ErrorCode errorCode, String queryType, String sql, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.queryType = queryType;
        this.sql = sql;
    }

    public String getQueryType() {
        return queryType;
    }

    public String getSql() {
        return sql;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LogQueryException{");
        sb.append("queryType='").append(queryType).append('\'');
        sb.append(", errorCode=").append(getErrorCode());
        sb.append(", message='").append(getMessage()).append('\'');
        if (sql != null) {
            sb.append(", sql='").append(sql).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }
}
