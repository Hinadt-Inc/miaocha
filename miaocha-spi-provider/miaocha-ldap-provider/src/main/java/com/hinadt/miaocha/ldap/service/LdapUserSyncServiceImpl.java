package com.hinadt.miaocha.ldap.service;

import com.hinadt.miaocha.ldap.config.LdapProperties;
import com.hinadt.miaocha.spi.LdapUserSyncService;
import com.hinadt.miaocha.spi.model.LdapUserDTO;
import java.util.List;
import javax.naming.directory.SearchControls;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;

/** LDAP用户同步服务实现 */
@Slf4j
public class LdapUserSyncServiceImpl implements LdapUserSyncService {

    private final LdapProperties ldapProperties;
    private final LdapTemplate ldapTemplate;

    public LdapUserSyncServiceImpl() {
        this.ldapProperties = loadLdapProperties();
        this.ldapTemplate = createLdapTemplate();
    }

    @Override
    public List<LdapUserDTO> syncAllUsers() {
        if (!isAvailable()) {
            log.warn("LDAP service is not available for user sync");
            return List.of();
        }

        try {
            log.info("Starting LDAP user synchronization");

            String searchBase = ldapProperties.getUserDn() + "," + ldapProperties.getBaseDn();
            String filter = String.format("(objectClass=%s)", ldapProperties.getUserObjectClass());

            List<LdapUserDTO> users =
                    ldapTemplate.search(
                            searchBase,
                            filter,
                            SearchControls.SUBTREE_SCOPE,
                            new LdapUserContextMapper());

            log.info("LDAP user synchronization completed, found {} users", users.size());
            return users;

        } catch (Exception e) {
            log.error("Error during LDAP user synchronization", e);
            return List.of();
        }
    }

    @Override
    public List<LdapUserDTO> syncUsersByFilter(String filter) {
        if (!isAvailable()) {
            log.warn("LDAP service is not available for filtered user sync");
            return List.of();
        }

        try {
            log.info("Starting LDAP user synchronization with filter: {}", filter);

            String searchBase = ldapProperties.getUserDn() + "," + ldapProperties.getBaseDn();

            // 组合过滤器：用户对象类 AND 自定义过滤器
            AndFilter combinedFilter = new AndFilter();
            combinedFilter.and(
                    new EqualsFilter("objectClass", ldapProperties.getUserObjectClass()));

            // 解析自定义过滤器
            if (filter != null && !filter.isEmpty()) {
                // 这里简化处理，直接使用提供的过滤器
                String combinedFilterString =
                        String.format(
                                "(&(objectClass=%s)%s)",
                                ldapProperties.getUserObjectClass(), filter);

                List<LdapUserDTO> users =
                        ldapTemplate.search(
                                searchBase,
                                combinedFilterString,
                                SearchControls.SUBTREE_SCOPE,
                                new LdapUserContextMapper());

                log.info(
                        "LDAP filtered user synchronization completed, found {} users",
                        users.size());
                return users;
            } else {
                return syncAllUsers();
            }

        } catch (Exception e) {
            log.error("Error during LDAP filtered user synchronization", e);
            return List.of();
        }
    }

    @Override
    public LdapUserDTO getUserByIdentifier(String loginIdentifier) {
        if (!isAvailable()) {
            log.warn("LDAP service is not available for user lookup");
            return null;
        }

        try {
            log.debug("Looking up LDAP user: {}", loginIdentifier);

            String searchBase = ldapProperties.getUserDn() + "," + ldapProperties.getBaseDn();

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

            List<LdapUserDTO> users =
                    ldapTemplate.search(
                            searchBase,
                            combinedFilter,
                            SearchControls.SUBTREE_SCOPE,
                            new LdapUserContextMapper());

            if (users.isEmpty()) {
                log.debug("LDAP user not found: {}", loginIdentifier);
                return null;
            }

            return users.get(0);

        } catch (Exception e) {
            log.error("Error looking up LDAP user: {}", loginIdentifier, e);
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return ldapProperties.isEnabled() && testConnection();
    }

    @Override
    public boolean testConnection() {
        try {
            log.debug("Testing LDAP connection");
            ldapTemplate.lookup("");
            log.debug("LDAP connection test successful");
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
