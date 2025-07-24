package com.hinadt.miaocha.spi;

import com.hinadt.miaocha.spi.model.OAuthProviderInfo;
import com.hinadt.miaocha.spi.model.OAuthUserInfo;

/** OAuth认证提供者SPI接口 用于加载外部OAuth认证插件，使用Java原生ServiceLoader机制 */
public interface OAuthProvider {

    /**
     * 获取提供者元信息
     *
     * @return 提供者元信息对象
     */
    OAuthProviderInfo getProviderInfo();

    /**
     * 验证认证凭据并获取用户信息
     *
     * @param code 授权码
     * @param redirectUri 重定向URI
     * @return 用户信息对象
     */
    OAuthUserInfo authenticate(String code, String redirectUri);

    /**
     * 检查提供者是否可用
     *
     * @return true表示可用，false表示不可用
     */
    default boolean isAvailable() {
        return true;
    }
}
