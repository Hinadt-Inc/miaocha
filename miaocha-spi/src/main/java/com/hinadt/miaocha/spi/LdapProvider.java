package com.hinadt.miaocha.spi;

import java.util.List;

/** LDAP认证提供者SPI接口 用于加载外部LDAP认证插件，使用Java原生ServiceLoader机制 */
public interface LdapProvider extends AuthenticationProvider {

    @Override
    default AuthenticationType getAuthenticationType() {
        return AuthenticationType.CREDENTIALS;
    }

    /**
     * 测试LDAP连接是否正常
     *
     * @return true表示连接正常，false表示连接失败
     */
    boolean testConnection();

    /**
     * 获取用户所属的组信息
     *
     * @param username 用户名
     * @return 用户组列表
     */
    default List<String> getUserGroups(String username) {
        return java.util.Collections.emptyList();
    }
}
