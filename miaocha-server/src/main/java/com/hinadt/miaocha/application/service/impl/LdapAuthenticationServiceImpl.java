package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.LdapAuthenticationService;
import com.hinadt.miaocha.domain.dto.auth.LdapUserDTO;
import java.util.List;
import javax.naming.directory.Attributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Service;

/** LDAP认证服务实现类 */
@Slf4j
@Service
public class LdapAuthenticationServiceImpl implements LdapAuthenticationService {

    private final LdapTemplate ldapTemplate;

    @Value("${ldap.user-search-filter:(&(mail={0}))}")
    private String userSearchFilter;

    @Value("${ldap.user-search-base}")
    private String userSearchBase;

    @Value("${ldap.default-email-domain:}")
    private String defaultEmailDomain;

    public LdapAuthenticationServiceImpl(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    @Override
    public LdapUserDTO authenticate(String username, String password) {
        try {
            log.info("Starting LDAP authentication for user: {}", username);

            // 提取真实用户名（如果输入的是邮箱）
            String actualUsername = username;
            if (username.contains("@")) {
                actualUsername = username.substring(0, username.indexOf("@"));
                log.debug("Extracted username from email: {} -> {}", username, actualUsername);
            }

            // 直接使用LDAP Template进行认证
            boolean authenticated = false;

            try {
                // 优先使用邮箱认证（如果输入的是邮箱）
                if (username.contains("@")) {
                    log.debug("Attempting mail authentication for: {}", username);
                    authenticated =
                            ldapTemplate.authenticate(
                                    "", // 使用空base，LdapConfig中已设置base
                                    "(mail=" + username + ")",
                                    password);
                    log.debug("Mail authentication result: {}", authenticated);
                }

                // 如果邮箱认证失败，尝试sAMAccountName认证
                if (!authenticated) {
                    log.debug("Attempting sAMAccountName authentication for: {}", actualUsername);
                    authenticated =
                            ldapTemplate.authenticate(
                                    "", // 使用空base
                                    "(sAMAccountName=" + actualUsername + ")",
                                    password);
                    log.debug("sAMAccountName authentication result: {}", authenticated);
                }

            } catch (Exception e) {
                log.debug("LDAP authentication failed: {}", e.getMessage());
            }

            if (authenticated) {
                log.info("LDAP authentication successful for user: {}", username);

                // 认证成功后获取用户信息
                LdapUserDTO ldapUser = null;

                // 尝试获取详细用户信息
                if (username.contains("@")) {
                    ldapUser = getLdapUserByMail(username);
                }
                if (ldapUser == null) {
                    ldapUser = getLdapUserBySAM(actualUsername);
                }

                // 如果找不到详细信息，创建基本信息
                if (ldapUser == null) {
                    ldapUser =
                            LdapUserDTO.builder()
                                    .username(actualUsername)
                                    .email(
                                            username.contains("@")
                                                    ? username
                                                    : (defaultEmailDomain.isEmpty()
                                                            ? null
                                                            : actualUsername
                                                                    + "@"
                                                                    + defaultEmailDomain))
                                    .displayName(actualUsername)
                                    .build();
                }

                return ldapUser;
            } else {
                log.warn("LDAP authentication failed for user: {}", username);
                return null;
            }
        } catch (Exception e) {
            log.error("LDAP authentication error for user {}: {}", username, e.getMessage());
            return null;
        }
    }

    @Override
    public LdapUserDTO getLdapUser(String username) {
        // 这个方法现在主要用于向后兼容，实际使用专门的搜索方法
        if (username.contains("@")) {
            return getLdapUserByMail(username);
        } else {
            return getLdapUserBySAM(username);
        }
    }

    /** 根据sAMAccountName获取LDAP用户信息 */
    private LdapUserDTO getLdapUserBySAM(String samAccountName) {
        try {
            log.debug("Searching LDAP user by sAMAccountName: {}", samAccountName);

            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("sAMAccountName", samAccountName));
            String filterString = filter.encode();
            log.debug("Search filter: {}", filterString);

            List<LdapUserDTO> users =
                    ldapTemplate.search("", filterString, new LdapUserAttributesMapper());

            if (!users.isEmpty()) {
                LdapUserDTO user = users.get(0);
                log.debug("Found user: {}", user.getDisplayName());
                return user;
            }
        } catch (Exception e) {
            log.error(
                    "Error searching user by sAMAccountName {}: {}",
                    samAccountName,
                    e.getMessage());
        }
        return null;
    }

    /** 根据邮箱获取LDAP用户信息 */
    private LdapUserDTO getLdapUserByMail(String email) {
        try {
            log.debug("Searching LDAP user by mail: {}", email);

            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("mail", email));
            String filterString = filter.encode();
            log.debug("Search filter: {}", filterString);

            List<LdapUserDTO> users =
                    ldapTemplate.search("", filterString, new LdapUserAttributesMapper());

            if (!users.isEmpty()) {
                LdapUserDTO user = users.get(0);
                log.debug("Found user: {}", user.getDisplayName());
                return user;
            }
        } catch (Exception e) {
            log.error("Error searching user by mail {}: {}", email, e.getMessage());
        }
        return null;
    }

    /** LDAP用户属性映射器 */
    private static class LdapUserAttributesMapper implements AttributesMapper<LdapUserDTO> {
        @Override
        public LdapUserDTO mapFromAttributes(Attributes attributes)
                throws javax.naming.NamingException {
            LdapUserDTO user =
                    LdapUserDTO.builder()
                            .username(getAttributeValue(attributes, "sAMAccountName"))
                            .displayName(getAttributeValue(attributes, "displayName"))
                            .email(getAttributeValue(attributes, "mail"))
                            .surname(getAttributeValue(attributes, "sn"))
                            .givenName(getAttributeValue(attributes, "givenName"))
                            .department(getAttributeValue(attributes, "department"))
                            .title(getAttributeValue(attributes, "title"))
                            .phone(getAttributeValue(attributes, "telephoneNumber"))
                            .dn(getAttributeValue(attributes, "distinguishedName"))
                            .build();

            log.debug(
                    "Mapped LDAP user: username={}, email={}, displayName={}",
                    user.getUsername(),
                    user.getEmail(),
                    user.getDisplayName());

            return user;
        }

        private String getAttributeValue(Attributes attributes, String attributeName) {
            try {
                if (attributes.get(attributeName) != null
                        && attributes.get(attributeName).get() != null) {
                    return attributes.get(attributeName).get().toString();
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }
    }
}
