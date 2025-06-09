package com.hinadt.miaocha.endpoint;

import com.hinadt.miaocha.application.service.UserService;
import com.hinadt.miaocha.common.annotation.CurrentUser;
import com.hinadt.miaocha.domain.dto.ApiResponse;
import com.hinadt.miaocha.domain.dto.user.AdminUpdatePasswordDTO;
import com.hinadt.miaocha.domain.dto.user.UpdatePasswordDTO;
import com.hinadt.miaocha.domain.dto.user.UserCreateDTO;
import com.hinadt.miaocha.domain.dto.user.UserDTO;
import com.hinadt.miaocha.domain.dto.user.UserUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 用户管理控制器 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "提供用户的增删改查等管理功能")
public class UserEndpoint {

    private final UserService userService;

    /**
     * 获取当前用户信息
     *
     * @param currentUser 当前登录用户
     * @return 用户详情
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public ApiResponse<UserDTO> getCurrentUser(@CurrentUser UserDTO currentUser) {
        return ApiResponse.success(currentUser);
    }

    /**
     * 获取所有用户
     *
     * @return 用户列表
     */
    @GetMapping
    @Operation(summary = "获取所有用户", description = "获取系统中所有用户的列表信息")
    public ApiResponse<List<UserDTO>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsers());
    }

    /**
     * 获取用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户的详细信息")
    public ApiResponse<UserDTO> getUserById(
            @Parameter(description = "用户ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    /**
     * 创建用户
     *
     * @param userCreateDTO 用户创建DTO
     * @return 创建的用户
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "创建一个新用户")
    public ApiResponse<UserDTO> createUser(
            @Parameter(description = "用户创建信息", required = true) @Valid @RequestBody
                    UserCreateDTO userCreateDTO) {
        return ApiResponse.success(userService.createUser(userCreateDTO));
    }

    /**
     * 更新用户
     *
     * @param userUpdateDTO 用户更新DTO
     * @return 更新的用户
     */
    @PutMapping
    @Operation(summary = "更新用户", description = "根据用户ID更新用户信息")
    public ApiResponse<UserDTO> updateUser(
            @Parameter(description = "用户更新信息", required = true) @Valid @RequestBody
                    UserUpdateDTO userUpdateDTO) {
        return ApiResponse.success(userService.updateUser(userUpdateDTO));
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据用户ID删除用户")
    public ApiResponse<Void> deleteUser(
            @Parameter(description = "用户ID", required = true) @PathVariable("id") Long id) {
        userService.deleteUser(id);
        return ApiResponse.success();
    }

    /**
     * 修改用户密码（管理员操作）
     *
     * @param id 用户ID
     * @param adminUpdatePasswordDTO 密码更新DTO
     * @return 结果
     */
    @PutMapping("/{id}/password")
    @Operation(summary = "修改用户密码", description = "管理员根据用户ID修改用户密码")
    public ApiResponse<Void> updatePassword(
            @Parameter(description = "用户ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "密码更新信息", required = true) @Valid @RequestBody
                    AdminUpdatePasswordDTO adminUpdatePasswordDTO) {
        userService.updatePasswordByAdmin(id, adminUpdatePasswordDTO);
        return ApiResponse.success();
    }

    /**
     * 修改自己的密码
     *
     * @param currentUser 当前登录用户
     * @param updatePasswordDTO 密码更新DTO
     * @return 结果
     */
    @PutMapping("/password")
    @Operation(summary = "修改自己的密码", description = "用户修改自己的密码，需要验证旧密码")
    public ApiResponse<Void> updateOwnPassword(
            @CurrentUser UserDTO currentUser,
            @Parameter(description = "密码更新信息", required = true) @Valid @RequestBody
                    UpdatePasswordDTO updatePasswordDTO) {
        userService.updateOwnPassword(currentUser.getId(), updatePasswordDTO);
        return ApiResponse.success();
    }
}
