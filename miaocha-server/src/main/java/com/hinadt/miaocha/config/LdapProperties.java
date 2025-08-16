package com.hinadt.miaocha.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** LDAP配置属性类 */
@Data
@Component
@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {

    /** 是否启用LDAP认证 */
    private Boolean enabled = false;

    /** LDAP服务器URL */
    private String serverUrl = "ldap://localhost:389";

    /** LDAP基础DN */
    private String baseDn = "dc=company,dc=com";

    /** 用户DN模式 {0}会被替换为用户名 */
    private String userDnPattern = "uid={0},ou=people,dc=company,dc=com";

    /** 用户搜索基础DN */
    private String userSearchBase = "ou=people,dc=company,dc=com";

    /** 用户搜索过滤器 {0}会被替换为用户名 */
    private String userSearchFilter = "uid={0}";

    /** 组搜索基础DN */
    private String groupSearchBase = "ou=groups,dc=company,dc=com";

    /** 组搜索过滤器 {0}会被替换为用户DN */
    private String groupSearchFilter = "member={0}";

    /** 管理员DN（用于搜索） */
    private String managerDn;

    /** 管理员密码 */
    private String managerPassword;

    /** 连接超时时间（毫秒） */
    private Integer connectTimeout = 5000;

    /** 读取超时时间（毫秒） */
    private Integer readTimeout = 5000;

    /** 用户名属性名 */
    private String usernameAttribute = "uid";

    /** 邮箱属性名 */
    private String emailAttribute = "mail";

    /** 显示名称属性名 */
    private String displayNameAttribute = "displayName";

    /** 真实姓名属性名 */
    private String realNameAttribute = "cn";

    /** 部门属性名 */
    private String departmentAttribute = "departmentNumber";

    /** 职位属性名 */
    private String positionAttribute = "title";
}
