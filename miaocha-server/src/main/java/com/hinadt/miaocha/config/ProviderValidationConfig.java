package com.hinadt.miaocha.config;

import com.hinadt.miaocha.spi.LdapAuthProvider;
import com.hinadt.miaocha.spi.OAuthProvider;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 提供者验证配置
 *
 * <p>在应用启动时验证所有 SPI 提供者的 providerId 是否唯一
 */
@Slf4j
@Configuration
public class ProviderValidationConfig {

    @PostConstruct
    public void validateProviderIds() {
        log.info("开始验证提供者 ID 的唯一性...");

        Set<String> allProviderIds = new HashSet<>();
        List<String> duplicateIds = new ArrayList<>();

        // 检查 OAuth 提供者
        ServiceLoader<OAuthProvider> oauthLoader = ServiceLoader.load(OAuthProvider.class);
        StreamSupport.stream(oauthLoader.spliterator(), false)
                .filter(OAuthProvider::isAvailable)
                .forEach(
                        provider -> {
                            String providerId = provider.getProviderInfo().getProviderId();
                            if (!allProviderIds.add(providerId)) {
                                duplicateIds.add(providerId);
                                log.error("发现重复的 OAuth 提供者 ID: {}", providerId);
                            } else {
                                log.info(
                                        "已注册 OAuth 提供者: {} ({})",
                                        provider.getProviderInfo().getDisplayName(),
                                        providerId);
                            }
                        });

        // 检查 LDAP 提供者
        ServiceLoader<LdapAuthProvider> ldapLoader = ServiceLoader.load(LdapAuthProvider.class);
        StreamSupport.stream(ldapLoader.spliterator(), false)
                .filter(LdapAuthProvider::isAvailable)
                .forEach(
                        provider -> {
                            String providerId = provider.getProviderInfo().getProviderId();
                            if (!allProviderIds.add(providerId)) {
                                duplicateIds.add(providerId);
                                log.error("发现重复的 LDAP 提供者 ID: {}", providerId);
                            } else {
                                log.info(
                                        "已注册 LDAP 提供者: {} ({})",
                                        provider.getProviderInfo().getDisplayName(),
                                        providerId);
                            }
                        });

        // 如果发现重复的 Provider ID，抛出异常阻止应用启动
        if (!duplicateIds.isEmpty()) {
            String errorMessage =
                    String.format(
                            "发现重复的提供者 ID: %s。所有提供者的 providerId 必须唯一，请修改重复的提供者配置。", duplicateIds);
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        log.info("提供者 ID 唯一性验证完成，共注册 {} 个提供者", allProviderIds.size());
    }
}
