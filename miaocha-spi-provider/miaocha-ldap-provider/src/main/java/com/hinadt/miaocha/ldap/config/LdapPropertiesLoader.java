package com.hinadt.miaocha.ldap.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LdapPropertiesLoader {

    private static final String CONFIG_FILE = "application.properties";
    private static final String PREFIX = "miaocha.ldap.";

    public static LdapProperties loadFromClasspath() {
        LdapProperties ldapProperties = new LdapProperties();

        try (InputStream input =
                LdapPropertiesLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                Properties properties = new Properties();
                properties.load(input);

                loadProperty(
                        properties,
                        ldapProperties::setEnabled,
                        "enabled",
                        Boolean::parseBoolean,
                        false);
                loadProperty(
                        properties,
                        ldapProperties::setUrl,
                        "url",
                        String::valueOf,
                        "ldap://localhost:389");
                loadProperty(
                        properties,
                        ldapProperties::setBaseDn,
                        "base-dn",
                        String::valueOf,
                        "dc=example,dc=com");
                loadProperty(
                        properties,
                        ldapProperties::setUserDn,
                        "user-dn",
                        String::valueOf,
                        "ou=users");
                loadProperty(
                        properties,
                        ldapProperties::setManagerDn,
                        "manager-dn",
                        String::valueOf,
                        "cn=admin,dc=example,dc=com");
                loadProperty(
                        properties,
                        ldapProperties::setManagerPassword,
                        "manager-password",
                        String::valueOf,
                        "admin");
                loadProperty(
                        properties,
                        ldapProperties::setUserSearchFilter,
                        "user-search-filter",
                        String::valueOf,
                        "(uid={0})");
                loadProperty(
                        properties,
                        ldapProperties::setUserObjectClass,
                        "user-object-class",
                        String::valueOf,
                        "inetOrgPerson");
                loadProperty(
                        properties,
                        ldapProperties::setEmailAttribute,
                        "email-attribute",
                        String::valueOf,
                        "mail");
                loadProperty(
                        properties,
                        ldapProperties::setNicknameAttribute,
                        "nickname-attribute",
                        String::valueOf,
                        "cn");
                loadProperty(
                        properties,
                        ldapProperties::setRealNameAttribute,
                        "real-name-attribute",
                        String::valueOf,
                        "displayName");
                loadProperty(
                        properties,
                        ldapProperties::setDepartmentAttribute,
                        "department-attribute",
                        String::valueOf,
                        "department");
                loadProperty(
                        properties,
                        ldapProperties::setPositionAttribute,
                        "position-attribute",
                        String::valueOf,
                        "title");
                loadProperty(
                        properties,
                        ldapProperties::setOrganizationalUnitAttribute,
                        "organizational-unit-attribute",
                        String::valueOf,
                        "ou");
                loadProperty(
                        properties,
                        ldapProperties::setConnectTimeout,
                        "connect-timeout",
                        Integer::parseInt,
                        5000);
                loadProperty(
                        properties,
                        ldapProperties::setReadTimeout,
                        "read-timeout",
                        Integer::parseInt,
                        5000);

                log.info("已从配置文件加载 LDAP 配置");
            } else {
                log.warn("未找到配置文件 {}，使用默认配置", CONFIG_FILE);
            }
        } catch (IOException e) {
            log.warn("加载 LDAP 配置文件失败，使用默认配置", e);
        }

        return ldapProperties;
    }

    private static <T> void loadProperty(
            Properties properties,
            java.util.function.Consumer<T> setter,
            String key,
            java.util.function.Function<String, T> converter,
            T defaultValue) {
        String value = properties.getProperty(PREFIX + key);
        if (value != null && !value.trim().isEmpty()) {
            try {
                setter.accept(converter.apply(value.trim()));
            } catch (Exception e) {
                log.warn("配置项 {} 解析失败，使用默认值: {}", PREFIX + key, defaultValue, e);
                setter.accept(defaultValue);
            }
        } else {
            setter.accept(defaultValue);
        }
    }
}
