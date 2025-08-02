package com.hinadt.miaocha.spi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * OAuth提供者元信息
 *
 * <p>定义OAuth提供者的完整信息，包含SPI标识信息和前端所需的URL配置。 既符合SPI设计原则，又满足前端展示和交互需求。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OAuthProviderInfo extends BaseProviderInfo {

    // ========== OAuth2标准端点信息 ==========

    /**
     * 授权端点URL
     *
     * <p>用户进行OAuth授权的URL，前端需要此URL构建登录链接
     */
    private String authorizationEndpoint;

    /**
     * 令牌端点URL
     *
     * <p>用于交换授权码获取访问令牌的URL
     */
    private String tokenEndpoint;

    /**
     * 用户信息端点URL
     *
     * <p>用于获取用户基本信息的URL
     */
    private String userinfoEndpoint;

    /**
     * 撤销端点URL
     *
     * <p>用于撤销访问令牌的URL（可选）
     */
    private String revocationEndpoint;

    // ========== 前端展示配置 ==========

    /**
     * 提供者图标URL
     *
     * <p>用于在前端显示的图标地址
     */
    private String iconUrl;

    // ========== OAuth2功能支持信息 ==========

    /**
     * 支持的作用域
     *
     * <p>该OAuth提供者支持的权限范围，以逗号分隔
     *
     * <p>例如："openid,profile,email"
     */
    private String supportedScopes;

    /**
     * 支持的响应类型
     *
     * <p>该OAuth提供者支持的响应类型，以逗号分隔
     *
     * <p>例如："code,token"
     */
    private String supportedResponseTypes;

    /**
     * 支持的授权类型
     *
     * <p>该OAuth提供者支持的授权类型，以逗号分隔
     *
     * <p>例如："authorization_code,refresh_token"
     */
    private String supportedGrantTypes;
}
