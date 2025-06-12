package com.hinadt.miaocha.config.mybatis;

import com.hinadt.miaocha.common.audit.UserAuditable;
import com.hinadt.miaocha.common.util.UserContextUtil;
import java.util.Properties;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * MyBatis用户审计拦截器 自动为实现了UserAuditable接口的实体设置创建人和修改人字段 时间字段由数据库的 DEFAULT CURRENT_TIMESTAMP 和 ON
 * UPDATE CURRENT_TIMESTAMP 自动处理
 */
@Component
@Intercepts({
    @Signature(
            type = Executor.class,
            method = "update",
            args = {MappedStatement.class, Object.class})
})
public class UserAuditInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(UserAuditInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object parameter = getParameter(invocation);

        if (isUserAuditableEntity(parameter)) {
            SqlCommandType commandType = getSqlCommandType(invocation);
            processUserAudit((UserAuditable) parameter, commandType);
        }

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 预留配置参数接口
    }

    /** 从调用参数中提取实体参数 */
    private Object getParameter(Invocation invocation) {
        return invocation.getArgs()[1];
    }

    /** 从调用参数中提取SQL命令类型 */
    private SqlCommandType getSqlCommandType(Invocation invocation) {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        return mappedStatement.getSqlCommandType();
    }

    /** 检查参数是否为UserAuditable实体 */
    private boolean isUserAuditableEntity(Object parameter) {
        return parameter instanceof UserAuditable;
    }

    /** 根据SQL命令类型处理用户审计字段 */
    private void processUserAudit(UserAuditable entity, SqlCommandType commandType) {
        try {
            String currentUserEmail = getCurrentUserEmail();

            switch (commandType) {
                case INSERT:
                    handleInsertAudit(entity, currentUserEmail);
                    break;
                case UPDATE:
                    handleUpdateAudit(entity, currentUserEmail);
                    break;
                default:
                    // 其他操作无需审计处理
                    break;
            }
        } catch (Exception e) {
            log.warn(
                    "设置用户审计字段失败，实体类型: [{}]，错误信息: {}",
                    entity.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    /** 处理INSERT操作的审计字段 */
    private void handleInsertAudit(UserAuditable entity, String currentUserEmail) {
        if (!StringUtils.hasText(entity.getCreateUser())) {
            entity.setCreateUser(currentUserEmail);
        }
        if (!StringUtils.hasText(entity.getUpdateUser())) {
            entity.setUpdateUser(currentUserEmail);
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "自动设置创建审计字段，实体类型: [{}]，用户邮箱: [{}]",
                    entity.getClass().getSimpleName(),
                    currentUserEmail);
        }
    }

    /** 处理UPDATE操作的审计字段 */
    private void handleUpdateAudit(UserAuditable entity, String currentUserEmail) {
        entity.setUpdateUser(currentUserEmail);

        if (log.isDebugEnabled()) {
            log.debug(
                    "自动设置更新审计字段，实体类型: [{}]，用户邮箱: [{}]",
                    entity.getClass().getSimpleName(),
                    currentUserEmail);
        }
    }

    /** 获取当前用户邮箱 */
    private String getCurrentUserEmail() {
        return UserContextUtil.getCurrentUserEmail();
    }
}
