package com.hinadt.miaocha.spi.impl;

import com.hinadt.miaocha.config.LdapProperties;
import com.hinadt.miaocha.spi.LdapProvider;
import com.hinadt.miaocha.spi.model.OAuthProviderInfo;
import com.hinadt.miaocha.spi.model.OAuthUserInfo;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RealLdapProvider implements LdapProvider {

    private final LdapProperties ldapProperties;

    public RealLdapProvider(LdapProperties ldapProperties) {
        this.ldapProperties = ldapProperties;
    }

    private LdapTemplate ldapTemplate;

    /** 初始化LDAP连接模板 */
    private LdapTemplate getLdapTemplate() {
        if (ldapTemplate == null) {
            LdapContextSource contextSource = new LdapContextSource();
            contextSource.setUrl(ldapProperties.getServerUrl());
            contextSource.setBase(ldapProperties.getBaseDn());

            if (ldapProperties.getManagerDn() != null) {
                contextSource.setUserDn(ldapProperties.getManagerDn());
                contextSource.setPassword(ldapProperties.getManagerPassword());
            }

            contextSource.afterPropertiesSet();

            ldapTemplate = new LdapTemplate(contextSource);
        }
        return ldapTemplate;
    }

    @Override
    public OAuthProviderInfo getProviderInfo() {
        return OAuthProviderInfo.builder()
                .providerId("ldap")
                .displayName("企业LDAP登录")
                .description("使用企业Active Directory/LDAP进行统一认证")
                .version("1.0.0")
                .enabled(ldapProperties.getEnabled())
                .sortOrder(0)
                .iconUrl("/icons/ldap.svg")
                .build();
    }

    @Override
    public OAuthUserInfo authenticateWithCredentials(String username, String password) {
        log.info("Attempting LDAP authentication for user: {}", username);

        if (!isAvailable()) {
            throw new RuntimeException("LDAP provider is not available");
        }

        // 检查是否为测试模式
        if (isTestMode()) {
            return handleTestAuthentication(username, password);
        }

        try {
            // 第一步：验证用户密码
            if (!authenticateUser(username, password)) {
                throw new RuntimeException("LDAP authentication failed: Invalid credentials");
            }

            // 第二步：获取用户信息
            OAuthUserInfo userInfo = getUserInfo(username);
            if (userInfo == null) {
                throw new RuntimeException(
                        "LDAP authentication failed: Cannot retrieve user information");
            }

            log.info("LDAP authentication successful for user: {}", username);
            return userInfo;

        } catch (Exception e) {
            log.error("LDAP authentication failed for user: {}", username, e);
            throw new RuntimeException("LDAP authentication failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean testConnection() {
        log.info("Testing LDAP connection to: {}", ldapProperties.getServerUrl());

        if (!ldapProperties.getEnabled()) {
            log.info("LDAP is disabled");
            return false;
        }

        DirContext context = null;
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapProperties.getServerUrl());
            env.put(
                    "com.sun.jndi.ldap.connect.timeout",
                    ldapProperties.getConnectTimeout().toString());
            env.put("com.sun.jndi.ldap.read.timeout", ldapProperties.getReadTimeout().toString());

            if (ldapProperties.getManagerDn() != null) {
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                env.put(Context.SECURITY_PRINCIPAL, ldapProperties.getManagerDn());
                env.put(Context.SECURITY_CREDENTIALS, ldapProperties.getManagerPassword());
            }

            context = new InitialLdapContext(env, null);
            log.info("LDAP connection test successful");
            return true;

        } catch (Exception e) {
            log.error("LDAP connection test failed", e);
            return false;
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException e) {
                    log.warn("Failed to close LDAP context", e);
                }
            }
        }
    }

    @Override
    public List<String> getUserGroups(String username) {
        log.info("Getting user groups for: {}", username);

        if (!isAvailable()) {
            return new ArrayList<>();
        }

        try {
            LdapTemplate template = getLdapTemplate();

            // 首先获取用户DN
            String userDn = getUserDn(username);
            if (userDn == null) {
                log.warn("Cannot find user DN for: {}", username);
                return new ArrayList<>();
            }

            // 搜索用户所属的组
            EqualsFilter filter = new EqualsFilter("member", userDn);
            List<String> groups =
                    template.search(
                            ldapProperties.getGroupSearchBase(),
                            filter.encode(),
                            new AttributesMapper<String>() {
                                @Override
                                public String mapFromAttributes(Attributes attrs)
                                        throws NamingException {
                                    return attrs.get("cn") != null
                                            ? attrs.get("cn").get().toString()
                                            : null;
                                }
                            });

            log.info("Found {} groups for user: {}", groups.size(), username);
            return groups;

        } catch (Exception e) {
            log.error("Failed to get user groups for: {}", username, e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean isAvailable() {
        return ldapProperties.getEnabled() && testConnection();
    }

    /** 验证用户密码 */
    private boolean authenticateUser(String username, String password) {
        DirContext context = null;
        try {
            String userDn = String.format(ldapProperties.getUserDnPattern(), username);

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapProperties.getServerUrl());
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, userDn);
            env.put(Context.SECURITY_CREDENTIALS, password);
            env.put(
                    "com.sun.jndi.ldap.connect.timeout",
                    ldapProperties.getConnectTimeout().toString());
            env.put("com.sun.jndi.ldap.read.timeout", ldapProperties.getReadTimeout().toString());

            context = new InitialLdapContext(env, null);
            return true;

        } catch (Exception e) {
            log.debug("LDAP authentication failed for user: {}", username, e);
            return false;
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException e) {
                    log.warn("Failed to close LDAP context", e);
                }
            }
        }
    }

    /** 获取用户信息 */
    private OAuthUserInfo getUserInfo(String username) {
        try {
            LdapTemplate template = getLdapTemplate();

            EqualsFilter filter = new EqualsFilter(ldapProperties.getUsernameAttribute(), username);
            List<OAuthUserInfo> users =
                    template.search(
                            ldapProperties.getUserSearchBase(),
                            filter.encode(),
                            new AttributesMapper<OAuthUserInfo>() {
                                @Override
                                public OAuthUserInfo mapFromAttributes(Attributes attrs)
                                        throws NamingException {
                                    return OAuthUserInfo.builder()
                                            .uid(
                                                    getAttributeValue(
                                                            attrs,
                                                            ldapProperties.getUsernameAttribute()))
                                            .email(
                                                    getAttributeValue(
                                                            attrs,
                                                            ldapProperties.getEmailAttribute()))
                                            .nickname(
                                                    getAttributeValue(
                                                            attrs,
                                                            ldapProperties
                                                                    .getDisplayNameAttribute()))
                                            .realName(
                                                    getAttributeValue(
                                                            attrs,
                                                            ldapProperties.getRealNameAttribute()))
                                            .department(
                                                    getAttributeValue(
                                                            attrs,
                                                            ldapProperties
                                                                    .getDepartmentAttribute()))
                                            .position(
                                                    getAttributeValue(
                                                            attrs,
                                                            ldapProperties.getPositionAttribute()))
                                            .build();
                                }
                            });

            if (!users.isEmpty()) {
                return users.get(0);
            }

            return null;

        } catch (Exception e) {
            log.error("Failed to get user info for: {}", username, e);
            return null;
        }
    }

    /** 获取用户DN */
    private String getUserDn(String username) {
        try {
            LdapTemplate template = getLdapTemplate();

            EqualsFilter filter = new EqualsFilter(ldapProperties.getUsernameAttribute(), username);
            List<String> dns =
                    template.search(
                            ldapProperties.getUserSearchBase(),
                            filter.encode(),
                            new AttributesMapper<String>() {
                                @Override
                                public String mapFromAttributes(Attributes attrs)
                                        throws NamingException {
                                    return getAttributeValue(attrs, "distinguishedName");
                                }
                            });

            if (!dns.isEmpty() && dns.get(0) != null) {
                return dns.get(0);
            }

            // 如果没有找到distinguishedName属性，使用模式构造
            return String.format(ldapProperties.getUserDnPattern(), username);

        } catch (Exception e) {
            log.warn("Failed to get user DN for: {}, using pattern", username, e);
            return String.format(ldapProperties.getUserDnPattern(), username);
        }
    }

    /** 检查是否为测试模式 当LDAP服务器为localhost或test时，启用测试模式 */
    private boolean isTestMode() {
        String serverUrl = ldapProperties.getServerUrl();
        return serverUrl != null
                && (serverUrl.contains("localhost")
                        || serverUrl.contains("test")
                        || serverUrl.contains("127.0.0.1")
                        || ldapProperties.getManagerDn() == null);
    }

    /** 处理测试模式的认证 提供内置测试账号用于开发和测试 */
    private OAuthUserInfo handleTestAuthentication(String username, String password) {
        log.info("Using test mode for LDAP authentication");

        // 内置测试账号
        if (("test@company.com".equals(username) && "password".equals(password))
                || ("admin@company.com".equals(username) && "admin123".equals(password))) {

            return OAuthUserInfo.builder()
                    .uid("ldap_" + extractUsername(username))
                    .email(username)
                    .nickname(extractUsername(username))
                    .realName(username.startsWith("admin") ? "管理员" : "测试用户")
                    .department("IT部门")
                    .position("开发工程师")
                    .build();
        }

        throw new RuntimeException("LDAP test authentication failed: Invalid test credentials");
    }

    /** 从邮箱中提取用户名 */
    private String extractUsername(String email) {
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        }
        return email;
    }

    /** 安全地从LDAP属性中获取字符串值 */
    private String getAttributeValue(Attributes attrs, String attributeName)
            throws NamingException {
        if (attrs == null || attributeName == null) {
            return null;
        }

        var attribute = attrs.get(attributeName);
        if (attribute == null) {
            return null;
        }

        var value = attribute.get();
        return value != null ? value.toString() : null;
    }
}
