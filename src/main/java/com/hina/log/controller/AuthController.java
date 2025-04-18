package com.hina.log.controller;

import com.hina.log.dto.ApiResponse;
import com.hina.log.dto.auth.LoginRequestDTO;
import com.hina.log.dto.auth.LoginResponseDTO;
import com.hina.log.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "提供用户登录认证相关功能")
public class AuthController {

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
            @Parameter(description = "登录请求信息", required = true) @Valid @RequestBody LoginRequestDTO loginRequest) {
        LoginResponseDTO response = userService.login(loginRequest);
        return ApiResponse.success(response);
    }
}