package com.hinadt.miaocha.spi;

import com.hinadt.miaocha.spi.model.LdapUserDTO;

/** LDAP认证服务SPI接口 用于加载外部LDAP认证插件，使用Java原生ServiceLoader机制 */
public interface LdapAuthenticationService {

    /**
     * LDAP用户认证
     *
     * @param loginIdentifier 登录标识符（用户名或邮箱）
     * @param password 密码
     * @return LDAP用户信息，认证失败返回null
     */
    LdapUserDTO authenticate(String loginIdentifier, String password);

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
        return "ldap";
    }
}
