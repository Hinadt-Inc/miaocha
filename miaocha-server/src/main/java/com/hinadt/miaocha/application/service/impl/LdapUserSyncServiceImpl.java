package com.hinadt.miaocha.application.service.impl;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.hinadt.miaocha.application.service.LdapUserSyncService;
import com.hinadt.miaocha.domain.dto.auth.LdapUserDTO;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.entity.enums.UserRole;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import java.util.ArrayList;
import java.util.List;
import javax.naming.directory.Attributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** LDAP用户同步服务实现 */
@Slf4j
@Service
public class LdapUserSyncServiceImpl implements LdapUserSyncService {

    @Autowired private LdapTemplate ldapTemplate;

    @Autowired private UserMapper userMapper;

    @Value("${ldap.enabled:false}")
    private boolean ldapEnabled;

    @Value("${ldap.user-search-base}")
    private String userSearchBase;

    @Value("${ldap.default-email-domain:}")
    private String defaultEmailDomain;

    @Override
    public List<LdapUserDTO> getAllLdapUsers() {
        if (!ldapEnabled) {
            log.warn("LDAP功能已禁用，跳过获取LDAP用户");
            return new ArrayList<>();
        }

        log.info("开始从LDAP获取所有用户...");

        List<LdapUserDTO> allUsers = new ArrayList<>();

        try {
            // 使用objectClass=person查询所有人员
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectClass", "person"));

            String filterString = filter.encode();
            log.debug("LDAP查询过滤器: {}", filterString);

            // 分页查询所有用户
            List<LdapUserDTO> users =
                    ldapTemplate.search("", filterString, new LdapUserAttributesMapper());

            // 过滤掉无效用户
            for (LdapUserDTO user : users) {
                if (isValidUser(user)) {
                    allUsers.add(user);
                }
            }

            log.info("从LDAP获取到 {} 个有效用户", allUsers.size());

        } catch (Exception e) {
            log.error("从LDAP获取用户失败: {}", e.getMessage(), e);
        }

        return allUsers;
    }

    @Override
    @Transactional
    public int syncUsersToLocal() {
        if (!ldapEnabled) {
            log.warn("LDAP功能已禁用，跳过用户同步");
            return 0;
        }

        log.info("开始同步LDAP用户到本地数据库...");

        List<LdapUserDTO> ldapUsers = getAllLdapUsers();
        int syncCount = 0;

        for (LdapUserDTO ldapUser : ldapUsers) {
            try {
                // 检查用户是否已存在（根据邮箱）
                User existingUser = userMapper.selectByEmail(ldapUser.getEmail());

                if (existingUser == null) {
                    // 创建新用户
                    User newUser = createUserFromLdap(ldapUser);
                    userMapper.insert(newUser);
                    syncCount++;
                    log.debug("创建新LDAP用户: {} ({})", ldapUser.getUsername(), ldapUser.getEmail());
                } else {
                    // 更新现有用户信息（除了密码和角色）
                    if (updateUserFromLdap(existingUser, ldapUser)) {
                        userMapper.update(existingUser);
                        log.debug(
                                "更新LDAP用户信息: {} ({})", ldapUser.getUsername(), ldapUser.getEmail());
                    }
                }

            } catch (Exception e) {
                log.error("同步用户失败: {} - {}", ldapUser.getUsername(), e.getMessage());
            }
        }

        log.info("LDAP用户同步完成，新增/更新 {} 个用户", syncCount);
        return syncCount;
    }

    @Override
    public String manualSync() {
        if (!ldapEnabled) {
            return "LDAP功能已禁用，无法执行同步";
        }

        try {
            int syncCount = syncUsersToLocal();
            return String.format("手动同步完成，处理了 %d 个用户", syncCount);
        } catch (Exception e) {
            log.error("手动同步失败: {}", e.getMessage(), e);
            return "手动同步失败: " + e.getMessage();
        }
    }

    /** 验证LDAP用户是否有效 */
    private boolean isValidUser(LdapUserDTO user) {
        if (user == null) {
            return false;
        }

        // 必须有用户名或邮箱
        if ((user.getUsername() == null || user.getUsername().trim().isEmpty())
                && (user.getEmail() == null || user.getEmail().trim().isEmpty())) {
            return false;
        }

        // 过滤掉系统账号
        if (user.getUsername() != null) {
            String username = user.getUsername().toLowerCase();
            if (username.contains("admin")
                    || username.contains("system")
                    || username.contains("service")
                    || username.startsWith("$")) {
                return false;
            }
        }

        return true;
    }

    /** 从LDAP用户信息创建本地用户 */
    private User createUserFromLdap(LdapUserDTO ldapUser) {
        User user = new User();
        user.setUid(NanoIdUtils.randomNanoId());
        user.setEmail(
                ldapUser.getEmail() != null
                        ? ldapUser.getEmail()
                        : (defaultEmailDomain.isEmpty()
                                ? null
                                : ldapUser.getUsername() + "@" + defaultEmailDomain));
        user.setNickname(
                ldapUser.getDisplayName() != null
                        ? ldapUser.getDisplayName()
                        : ldapUser.getUsername());
        user.setPassword("LDAP_USER"); // 标识为LDAP用户，密码留空
        user.setRole(UserRole.USER.name()); // 默认角色为普通用户
        user.setStatus(1); // 启用状态

        log.debug(
                "创建LDAP用户: username={}, email={}, nickname={}",
                ldapUser.getUsername(),
                user.getEmail(),
                user.getNickname());

        return user;
    }

    /** 使用LDAP信息更新本地用户 */
    private boolean updateUserFromLdap(User user, LdapUserDTO ldapUser) {
        boolean needUpdate = false;

        // 更新邮箱
        String newEmail =
                ldapUser.getEmail() != null
                        ? ldapUser.getEmail()
                        : (defaultEmailDomain.isEmpty()
                                ? null
                                : ldapUser.getUsername() + "@" + defaultEmailDomain);
        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            user.setEmail(newEmail);
            needUpdate = true;
        }

        // 更新昵称
        String newNickname =
                ldapUser.getDisplayName() != null
                        ? ldapUser.getDisplayName()
                        : ldapUser.getUsername();
        if (newNickname != null && !newNickname.equals(user.getNickname())) {
            user.setNickname(newNickname);
            needUpdate = true;
        }

        // 确保LDAP用户的密码标识正确
        if (!"LDAP_USER".equals(user.getPassword())) {
            user.setPassword("LDAP_USER");
            needUpdate = true;
        }

        return needUpdate;
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

            // 如果没有sAMAccountName，使用cn
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                user.setUsername(getAttributeValue(attributes, "cn"));
            }

            return user;
        }

        private String getAttributeValue(Attributes attributes, String attributeName) {
            try {
                if (attributes.get(attributeName) != null
                        && attributes.get(attributeName).get() != null) {
                    return attributes.get(attributeName).get().toString().trim();
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }
    }
}
