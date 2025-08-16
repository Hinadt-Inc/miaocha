package com.hinadt.miaocha.spi;

import com.hinadt.miaocha.spi.model.OAuthProviderInfo;
import com.hinadt.miaocha.spi.model.OAuthUserInfo;

/** 统一认证提供者SPI接口 支持OAuth2和用户名密码两种认证方式 */
public interface AuthenticationProvider {

    /**
     * 获取提供者元信息
     *
     * @return 提供者元信息对象
     */
    OAuthProviderInfo getProviderInfo();

    /**
     * 使用用户名密码进行认证 适用于LDAP、数据库等传统认证方式
     *
     * @param username 用户名
     * @param password 密码
     * @return 用户信息对象
     * @throws UnsupportedOperationException 如果提供者不支持此认证方式
     */
    default OAuthUserInfo authenticateWithCredentials(String username, String password) {
        throw new UnsupportedOperationException(
                "This provider does not support credential authentication");
    }

    /**
     * 使用授权码进行认证 适用于OAuth2、OIDC等授权码流程
     *
     * @param code 授权码
     * @param redirectUri 重定向URI
     * @return 用户信息对象
     * @throws UnsupportedOperationException 如果提供者不支持此认证方式
     */
    default OAuthUserInfo authenticateWithCode(String code, String redirectUri) {
        throw new UnsupportedOperationException(
                "This provider does not support code authentication");
    }

    /**
     * 获取认证类型
     *
     * @return 认证类型枚举
     */
    AuthenticationType getAuthenticationType();

    /**
     * 检查提供者是否可用
     *
     * @return true表示可用，false表示不可用
     */
    default boolean isAvailable() {
        return true;
    }
}
