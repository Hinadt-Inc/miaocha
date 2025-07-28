package com.hinadt.miaocha.ldap.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** LDAP配置属性 */
@Data
@ConfigurationProperties(prefix = "miaocha.ldap")
public class LdapProperties {

    /** 是否启用LDAP */
    private boolean enabled = false;

    /** LDAP服务器URL */
    private String url = "ldap://localhost:389";

    /** 基础DN */
    private String baseDn = "dc=example,dc=com";

    /** 用户搜索DN */
    private String userDn = "ou=users";

    /** 管理员DN，用于搜索和绑定 */
    private String managerDn = "cn=admin,dc=example,dc=com";

    /** 管理员密码 */
    private String managerPassword = "admin";

    /** 用户搜索过滤器 */
    private String userSearchFilter = "(uid={0})";

    /** 用户对象类 */
    private String userObjectClass = "inetOrgPerson";

    /** 邮箱属性名 */
    private String emailAttribute = "mail";

    /** 昵称属性名 */
    private String nicknameAttribute = "cn";

    /** 真实姓名属性名 */
    private String realNameAttribute = "displayName";

    /** 部门属性名 */
    private String departmentAttribute = "department";

    /** 职位属性名 */
    private String positionAttribute = "title";

    /** 组织单位属性名 */
    private String organizationalUnitAttribute = "ou";

    /** 连接超时（毫秒） */
    private int connectTimeout = 5000;

    /** 读取超时（毫秒） */
    private int readTimeout = 5000;

    /** 连接池配置 */
    private Pool pool = new Pool();

    @Data
    public static class Pool {
        /** 是否启用连接池 */
        private boolean enabled = true;

        /** 最小连接数 */
        private int minIdle = 1;

        /** 最大连接数 */
        private int maxActive = 8;

        /** 最大等待时间（毫秒） */
        private long maxWait = 5000;
    }
}
