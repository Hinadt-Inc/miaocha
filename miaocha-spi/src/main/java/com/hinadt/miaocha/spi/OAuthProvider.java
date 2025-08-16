package com.hinadt.miaocha.spi;

import com.hinadt.miaocha.spi.model.OAuthUserInfo;

/** OAuth认证提供者SPI接口 用于加载外部OAuth认证插件，使用Java原生ServiceLoader机制 */
public interface OAuthProvider extends AuthenticationProvider {

    @Override
    default AuthenticationType getAuthenticationType() {
        return AuthenticationType.OAUTH;
    }

    @Override
    default OAuthUserInfo authenticateWithCode(String code, String redirectUri) {
        return authenticate(code, redirectUri);
    }

    /**
     * 验证认证凭据并获取用户信息
     *
     * @param code 授权码
     * @param redirectUri 重定向URI
     * @return 用户信息对象
     * @deprecated 使用 authenticateWithCode 替代
     */
    @Deprecated
    OAuthUserInfo authenticate(String code, String redirectUri);
}
