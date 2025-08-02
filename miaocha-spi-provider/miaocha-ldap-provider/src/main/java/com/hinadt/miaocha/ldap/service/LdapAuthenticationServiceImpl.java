package com.hinadt.miaocha.ldap.service;

import com.hinadt.miaocha.ldap.config.LdapProperties;
import com.hinadt.miaocha.spi.LdapAuthProvider;
import com.hinadt.miaocha.spi.model.LdapProviderInfo;
import com.hinadt.miaocha.spi.model.LdapUserInfo;
import java.util.List;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapUtils;

/** LDAP认证提供者实现 */
@Slf4j
public class LdapAuthenticationServiceImpl implements LdapAuthProvider {

    private final LdapProperties ldapProperties;
    private final LdapTemplate ldapTemplate;

    public LdapAuthenticationServiceImpl() {
        this(new LdapProperties());
    }

    public LdapAuthenticationServiceImpl(LdapProperties ldapProperties) {
        this.ldapProperties = ldapProperties;
        this.ldapTemplate = createLdapTemplate();
    }

    @Override
    public LdapProviderInfo getProviderInfo() {
        return LdapProviderInfo.builder()
                .providerId("ldap")
                .displayName("LDAP认证")
                .description("通过LDAP服务器进行用户身份认证")
                .version("1.0.0")
                .providerType("ldap")
                .sortOrder(100)
                .enabled(ldapProperties.isEnabled())
                .serverUrl(ldapProperties.getUrl())
                .baseDn(ldapProperties.getBaseDn())
                .supportUserSync(true)
                .connectionStatus(isAvailable() ? "connected" : "disconnected")
                .lastConnectionTest(System.currentTimeMillis())
                .build();
    }

    @Override
    public LdapUserInfo authenticate(String loginIdentifier, String password) {
        if (!isAvailable()) {
            log.warn("LDAP service is not available");
            return null;
        }

        try {
            log.debug("Attempting LDAP authentication for user: {}", loginIdentifier);

            // 1. 先搜索用户
            LdapUserInfo user = searchUser(loginIdentifier);
            if (user == null) {
                log.debug("User not found in LDAP: {}", loginIdentifier);
                return null;
            }

            // 2. 验证密码
            if (authenticateUser(user.getDn(), password)) {
                log.info("LDAP authentication successful for user: {}", loginIdentifier);
                return user;
            } else {
                log.debug("LDAP authentication failed for user: {}", loginIdentifier);
                return null;
            }

        } catch (Exception e) {
            log.error("LDAP authentication error for user: {}", loginIdentifier, e);
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return ldapProperties.isEnabled() && testLdapConnection();
    }

    /** 搜索用户 */
    private LdapUserInfo searchUser(String loginIdentifier) {
        try {
            // 构造搜索过滤器
            String searchFilter =
                    ldapProperties.getUserSearchFilter().replace("{0}", loginIdentifier);

            // 如果输入的是邮箱，也要在邮箱字段中搜索
            String combinedFilter;
            if (loginIdentifier.contains("@")) {
                combinedFilter =
                        String.format(
                                "(|%s(%s=%s))",
                                searchFilter, ldapProperties.getEmailAttribute(), loginIdentifier);
            } else {
                combinedFilter = searchFilter;
            }

            log.debug("LDAP search filter: {}", combinedFilter);

            String searchBase = ldapProperties.getUserDn() + "," + ldapProperties.getBaseDn();

            List<LdapUserInfo> users =
                    ldapTemplate.search(
                            searchBase,
                            combinedFilter,
                            SearchControls.SUBTREE_SCOPE,
                            new LdapUserContextMapper());

            return users.isEmpty() ? null : users.get(0);

        } catch (Exception e) {
            log.error("Error searching LDAP user: {}", loginIdentifier, e);
            return null;
        }
    }

    /** 验证用户密码 */
    private boolean authenticateUser(String userDn, String password) {
        try {
            LdapContextSource contextSource = (LdapContextSource) ldapTemplate.getContextSource();
            LdapContext context = (LdapContext) contextSource.getContext(userDn, password);
            LdapUtils.closeContext(context);
            return true;
        } catch (Exception e) {
            log.debug("Password authentication failed for DN: {}", userDn);
            return false;
        }
    }

    /** 测试LDAP连接 */
    private boolean testLdapConnection() {
        try {
            ldapTemplate.lookup("");
            return true;
        } catch (Exception e) {
            log.warn("LDAP connection test failed", e);
            return false;
        }
    }

    /** 创建LDAP模板 */
    private LdapTemplate createLdapTemplate() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapProperties.getUrl());
        contextSource.setBase(ldapProperties.getBaseDn());
        contextSource.setUserDn(ldapProperties.getManagerDn());
        contextSource.setPassword(ldapProperties.getManagerPassword());

        try {
            contextSource.afterPropertiesSet();
        } catch (Exception e) {
            log.error("Failed to initialize LDAP context source", e);
        }

        return new LdapTemplate(contextSource);
    }

    /** LDAP用户上下文映射器 */
    private class LdapUserContextMapper extends AbstractContextMapper<LdapUserInfo> {
        @Override
        protected LdapUserInfo doMapFromContext(DirContextOperations ctx) {
            return LdapUserInfo.builder()
                    .dn(ctx.getNameInNamespace())
                    .uid(getAttributeValue(ctx, "uid"))
                    .email(getAttributeValue(ctx, ldapProperties.getEmailAttribute()))
                    .nickname(getAttributeValue(ctx, ldapProperties.getNicknameAttribute()))
                    .realName(getAttributeValue(ctx, ldapProperties.getRealNameAttribute()))
                    .department(getAttributeValue(ctx, ldapProperties.getDepartmentAttribute()))
                    .position(getAttributeValue(ctx, ldapProperties.getPositionAttribute()))
                    .organizationalUnit(
                            getAttributeValue(ctx, ldapProperties.getOrganizationalUnitAttribute()))
                    .build();
        }

        private String getAttributeValue(DirContextOperations ctx, String attributeName) {
            try {
                return ctx.getStringAttribute(attributeName);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
