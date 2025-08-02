package com.hinadt.miaocha.spi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 提供者基础信息
 *
 * <p>定义所有类型提供者的通用信息，包含 SPI 标识信息和基本元数据
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseProviderInfo {

    /**
     * 提供者唯一标识符
     *
     * <p>用于在系统中唯一标识该提供者，建议使用小写字母和连字符
     *
     * <p>例如："ldap", "mandao", "github", "google"
     */
    private String providerId;

    /**
     * 提供者显示名称
     *
     * <p>用于在用户界面中显示的友好名称
     *
     * <p>例如："LDAP认证", "曼道SSO", "GitHub", "Google"
     */
    private String displayName;

    /**
     * 提供者描述信息
     *
     * <p>简要描述该提供者的用途或特点
     */
    private String description;

    /**
     * 提供者版本
     *
     * <p>标识该 SPI 实现的版本，用于兼容性管理
     */
    private String version;

    /**
     * 提供者类型
     *
     * <p>标识提供者的类型，如："oauth", "ldap", "saml" 等
     */
    private String providerType;

    /**
     * 排序顺序
     *
     * <p>用于在前端显示时的排序，数值越小越靠前
     */
    private int sortOrder;

    /**
     * 是否启用
     *
     * <p>标识该提供者是否当前可用
     */
    private boolean enabled;
}
