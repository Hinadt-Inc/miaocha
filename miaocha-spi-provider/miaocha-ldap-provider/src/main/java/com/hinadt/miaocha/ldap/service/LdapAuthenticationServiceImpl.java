package com.hinadt.miaocha.ldap.service;

import com.hinadt.miaocha.ldap.config.LdapProperties;
import com.hinadt.miaocha.spi.LdapAuthenticationService;
import com.hinadt.miaocha.spi.model.LdapUserDTO;
import java.util.List;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapUtils;

/** LDAP认证服务实现 */
@Slf4j
public class LdapAuthenticationServiceImpl implements LdapAuthenticationService {

    private final LdapProperties ldapProperties;
    private final LdapTemplate ldapTemplate;

    public LdapAuthenticationServiceImpl() {
        this.ldapProperties = loadLdapProperties();
        this.ldapTemplate = createLdapTemplate();
    }

    @Override
    public LdapUserDTO authenticate(String loginIdentifier, String password) {
        if (!isAvailable()) {
            log.warn("LDAP service is not available");
            return null;
        }

        try {
            log.debug("Attempting LDAP authentication for user: {}", loginIdentifier);

            // 1. 先搜索用户
            LdapUserDTO user = searchUser(loginIdentifier);
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
    private LdapUserDTO searchUser(String loginIdentifier) {
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

            List<LdapUserDTO> users =
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

    /** 加载LDAP配置 */
    private LdapProperties loadLdapProperties() {
        // 这里应该从配置文件或环境变量中加载配置
        // 为了简化，这里使用默认配置
        LdapProperties properties = new LdapProperties();

        // 从系统属性中读取配置
        String enabled = System.getProperty("miaocha.ldap.enabled", "false");
        properties.setEnabled(Boolean.parseBoolean(enabled));

        String url = System.getProperty("miaocha.ldap.url", "ldap://localhost:389");
        properties.setUrl(url);

        String baseDn = System.getProperty("miaocha.ldap.base-dn", "dc=example,dc=com");
        properties.setBaseDn(baseDn);

        String userDn = System.getProperty("miaocha.ldap.user-dn", "ou=users");
        properties.setUserDn(userDn);

        String managerDn =
                System.getProperty("miaocha.ldap.manager-dn", "cn=admin,dc=example,dc=com");
        properties.setManagerDn(managerDn);

        String managerPassword = System.getProperty("miaocha.ldap.manager-password", "admin");
        properties.setManagerPassword(managerPassword);

        return properties;
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
    private class LdapUserContextMapper extends AbstractContextMapper<LdapUserDTO> {
        @Override
        protected LdapUserDTO doMapFromContext(DirContextOperations ctx) {
            return LdapUserDTO.builder()
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
