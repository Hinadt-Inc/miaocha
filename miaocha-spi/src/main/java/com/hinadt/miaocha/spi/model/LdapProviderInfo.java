package com.hinadt.miaocha.spi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * LDAP提供者信息
 *
 * <p>定义LDAP提供者的完整信息，继承基础提供者信息
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LdapProviderInfo extends BaseProviderInfo {

    /** LDAP服务器地址 */
    private String serverUrl;

    /** 基础DN */
    private String baseDn;

    /** 连接状态 */
    private String connectionStatus;

    /** 最后连接测试时间 */
    private Long lastConnectionTest;
}
