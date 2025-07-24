package com.hinadt.miaocha.spi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth提供者元信息
 *
 * <p>定义OAuth提供者的完整信息，包含SPI标识信息和前端所需的URL配置。 既符合SPI设计原则，又满足前端展示和交互需求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthProviderInfo {

    // ========== SPI核心元数据 ==========

    /**
     * 提供者唯一标识符
     *
     * <p>用于在系统中唯一标识该OAuth提供者，建议使用小写字母和连字符
     *
     * <p>例如："mandao", "github", "google"
     */
    private String providerId;

    /**
     * 提供者显示名称
     *
     * <p>用于在用户界面中显示的友好名称
     *
     * <p>例如："曼道SSO", "GitHub", "Google"
     */
    private String displayName;

    /**
     * 提供者描述信息
     *
     * <p>简要描述该OAuth提供者的用途或特点
     */
    private String description;

    /**
     * 提供者版本
     *
     * <p>标识该SPI实现的版本，用于兼容性管理
     */
    private String version;

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

    /**
     * 是否启用
     *
     * <p>控制该提供者是否在前端显示和可用
     */
    private Boolean enabled;

    /**
     * 排序顺序
     *
     * <p>用于控制多个OAuth提供者在前端的显示顺序，数值越小越靠前
     */
    private Integer sortOrder;

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
