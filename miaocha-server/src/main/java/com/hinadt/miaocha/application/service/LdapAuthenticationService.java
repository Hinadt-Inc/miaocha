package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.dto.auth.LdapUserDTO;

/** LDAP认证服务接口 */
public interface LdapAuthenticationService {

    /**
     * LDAP用户认证
     *
     * @param username 用户名
     * @param password 密码
     * @return LDAP用户信息，认证失败返回null
     */
    LdapUserDTO authenticate(String username, String password);

    /**
     * 根据用户名获取LDAP用户信息
     *
     * @param username 用户名
     * @return LDAP用户信息，不存在返回null
     */
    LdapUserDTO getLdapUser(String username);
}
