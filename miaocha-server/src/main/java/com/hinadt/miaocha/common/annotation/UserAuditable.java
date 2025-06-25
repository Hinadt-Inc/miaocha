package com.hinadt.miaocha.common.annotation;

/**
 * 用户审计字段接口 实现此接口的实体将自动设置创建人和修改人字段 时间字段由数据库的 DEFAULT CURRENT_TIMESTAMP 和 ON UPDATE CURRENT_TIMESTAMP
 * 自动处理
 */
public interface UserAuditable {

    /** 获取创建人 */
    String getCreateUser();

    /** 设置创建人 */
    void setCreateUser(String createUser);

    /** 获取更新人 */
    String getUpdateUser();

    /** 设置更新人 */
    void setUpdateUser(String updateUser);
}
