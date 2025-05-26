package com.hina.log.endpoint;

import com.hina.log.application.service.UserService;
import com.hina.log.domain.dto.ApiResponse;
import com.hina.log.domain.dto.auth.LoginRequestDTO;
import com.hina.log.domain.dto.auth.LoginResponseDTO;
import com.hina.log.domain.dto.auth.RefreshTokenRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证控制器 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "提供用户登录认证相关功能")
public class AuthEndpoint {

    private final UserService userService;

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用邮箱和密码进行登录认证，返回JWT令牌")
    public ApiResponse<LoginResponseDTO> login(
            @Parameter(description = "登录请求信息", required = true) @Valid @RequestBody
                    LoginRequestDTO loginRequest) {
        LoginResponseDTO response = userService.login(loginRequest);
        return ApiResponse.success(response);
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
}
