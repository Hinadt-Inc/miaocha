package com.hinadt.miaocha.spi;

import com.hinadt.miaocha.spi.model.LdapUserInfo;
import java.util.List;

/** LDAP用户同步提供者SPI接口 用于从LDAP系统同步用户信息，使用Java原生ServiceLoader机制 */
public interface LdapUserSyncProvider {

    /**
     * 同步所有LDAP用户
     *
     * @return 同步的用户列表
     */
    List<LdapUserInfo> syncAllUsers();

    /**
     * 根据过滤条件同步用户
     *
     * @param filter LDAP过滤条件，例如："(department=IT)"
     * @return 匹配条件的用户列表
     */
    List<LdapUserInfo> syncUsersByFilter(String filter);

    /**
     * 根据用户标识符获取单个用户信息
     *
     * @param loginIdentifier 登录标识符（用户名或邮箱）
     * @return LDAP用户信息，未找到返回null
     */
    LdapUserInfo getUserByIdentifier(String loginIdentifier);

    /**
     * 检查LDAP服务是否可用
     *
     * @return true表示可用，false表示不可用
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 获取提供者名称
     *
     * @return 提供者名称
     */
    default String getProviderName() {
        return "ldap-sync";
    }

    /**
     * 测试LDAP连接
     *
     * @return true表示连接成功，false表示连接失败
     */
    boolean testConnection();
}
