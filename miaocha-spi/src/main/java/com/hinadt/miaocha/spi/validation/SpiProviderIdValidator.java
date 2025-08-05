package com.hinadt.miaocha.spi.validation;

import com.hinadt.miaocha.spi.LdapAuthProvider;
import com.hinadt.miaocha.spi.OAuthProvider;
import java.util.*;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpiProviderIdValidator {

    public void validateProviderIds() {

        log.info("开始验证 SPI 提供者 ID 的唯一性...");
        Set<String> allProviderIds = new HashSet<>();
        List<String> duplicateIds = new ArrayList<>();

        validateOAuthProviders(allProviderIds, duplicateIds);
        validateLdapProviders(allProviderIds, duplicateIds);

        if (!duplicateIds.isEmpty()) {
            String errorMessage =
                    String.format(
                            "发现重复的提供者 ID: %s。所有提供者的 providerId 必须唯一，请修改重复的提供者配置。", duplicateIds);
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        log.info("SPI 提供者 ID 唯一性验证完成，共注册 {} 个提供者", allProviderIds.size());
    }

    private void validateOAuthProviders(Set<String> allProviderIds, List<String> duplicateIds) {
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
    }

    private void validateLdapProviders(Set<String> allProviderIds, List<String> duplicateIds) {
        ServiceLoader<LdapAuthProvider> ldapLoader = ServiceLoader.load(LdapAuthProvider.class);
        StreamSupport.stream(ldapLoader.spliterator(), false)
                .filter(LdapAuthProvider::isAvailable)
                .forEach(
                        provider -> {
                            String providerId = provider.getProviderInfo().getProviderId();
                            if (!allProviderIds.add(providerId)) {
                                duplicateIds.add(providerId);
                                log.error("发现重复的 LDAP 认证提供者 ID: {}", providerId);
                            } else {
                                log.info(
                                        "已注册 LDAP 认证提供者: {} ({})",
                                        provider.getProviderInfo().getDisplayName(),
                                        providerId);
                            }
                        });
    }
}
