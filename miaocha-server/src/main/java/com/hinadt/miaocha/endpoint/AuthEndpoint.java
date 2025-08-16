package com.hinadt.miaocha.endpoint;

import com.hinadt.miaocha.application.service.UserService;
import com.hinadt.miaocha.domain.dto.ApiResponse;
import com.hinadt.miaocha.domain.dto.auth.LoginRequestDTO;
import com.hinadt.miaocha.domain.dto.auth.LoginResponseDTO;
import com.hinadt.miaocha.domain.dto.auth.RefreshTokenRequestDTO;
import com.hinadt.miaocha.spi.AuthenticationProvider;
import com.hinadt.miaocha.spi.LdapProvider;
import com.hinadt.miaocha.spi.OAuthProvider;
import com.hinadt.miaocha.spi.model.OAuthProviderInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 认证控制器 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "提供用户登录认证相关功能")
public class AuthEndpoint {

    private final UserService userService;

    /**
     * 统一用户登录接口 支持系统默认登录和SPI提供者登录（如LDAP）
     *
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用邮箱和密码进行登录认证，支持系统认证和第三方认证提供者，返回JWT令牌")
    public ApiResponse<LoginResponseDTO> login(
            @Parameter(description = "登录请求信息", required = true) @Valid @RequestBody
                    LoginRequestDTO loginRequest) {

        String providerId = loginRequest.getProviderId();

        if (providerId == null || providerId.equals("system")) {
            // 传统系统登录
            LoginResponseDTO response = userService.login(loginRequest);
            return ApiResponse.success(response);
        } else {
            // SPI提供者登录
            LoginResponseDTO response = userService.loginWithProvider(loginRequest);
            return ApiResponse.success(response);
        }
    }

    /**
     * 刷新令牌
     *
     * @param refreshTokenRequest 刷新令牌请求
     * @return 登录响应
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌和刷新令牌。每次刷新都会生成新的刷新令牌，旧的刷新令牌将不再有效。")
    public ApiResponse<LoginResponseDTO> refreshToken(
            @Parameter(description = "刷新令牌请求信息", required = true) @Valid @RequestBody
                    RefreshTokenRequestDTO refreshTokenRequest) {
        LoginResponseDTO response = userService.refreshToken(refreshTokenRequest);
        return ApiResponse.success(response);
    }

    /**
     * 获取支持的认证提供者列表 包括OAuth和LDAP等所有认证方式
     *
     * @return 支持的提供者信息列表
     */
    @GetMapping("/providers")
    @Operation(summary = "获取支持的认证提供者", description = "返回系统支持的所有认证提供者详细信息列表，包括OAuth和LDAP等")
    public ApiResponse<List<OAuthProviderInfo>> getAuthProviders() {
        List<OAuthProviderInfo> providers = new ArrayList<>();

        // 加载OAuth提供者
        ServiceLoader<OAuthProvider> oauthLoader = ServiceLoader.load(OAuthProvider.class);
        StreamSupport.stream(oauthLoader.spliterator(), false)
                .filter(AuthenticationProvider::isAvailable)
                .map(AuthenticationProvider::getProviderInfo)
                .forEach(providers::add);

        // 加载LDAP提供者
        ServiceLoader<LdapProvider> ldapLoader = ServiceLoader.load(LdapProvider.class);
        StreamSupport.stream(ldapLoader.spliterator(), false)
                .filter(AuthenticationProvider::isAvailable)
                .map(AuthenticationProvider::getProviderInfo)
                .forEach(providers::add);

        return ApiResponse.success(
                providers.stream()
                        .sorted(Comparator.comparingInt(OAuthProviderInfo::getSortOrder))
                        .collect(Collectors.toList()));
    }

    /**
     * OAuth 回调处理
     *
     * @param providerId 提供者ID
     * @param code 授权码
     * @param redirectUri 重定向URI
     * @return 登录响应
     */
    @GetMapping("/oauth/callback")
    @Operation(summary = "OAuth 回调", description = "处理 OAuth 回调，验证授权码并返回 JWT 令牌")
    public ApiResponse<LoginResponseDTO> oauthCallback(
            @RequestParam("provider") String providerId,
            @RequestParam("code") String code,
            @RequestParam("redirect_uri") String redirectUri) {
        LoginResponseDTO response = userService.oauthLogin(providerId, code, redirectUri);
        return ApiResponse.success(response);
    }
}
