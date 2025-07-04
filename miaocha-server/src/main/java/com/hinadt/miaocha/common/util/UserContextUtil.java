package com.hinadt.miaocha.common.util;

import com.hinadt.miaocha.domain.dto.user.UserDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** 用户上下文工具类 用于获取当前登录用户信息 */
@Component
public class UserContextUtil {

    private static final String DEFAULT_ANONYMOUS_USER = "anonymous";

    /**
     * 获取当前登录用户的邮箱
     *
     * @return 用户邮箱，如果未登录则返回"anonymous"，如果是系统操作则返回"system"
     */
    public static String getCurrentUserEmail() {
        try {
            UserDTO user = getCurrentUser();
            if (user != null) {
                return user.getEmail();
            }
            return DEFAULT_ANONYMOUS_USER;
        } catch (Exception e) {
            // 如果获取用户信息失败，返回默认值
            return DEFAULT_ANONYMOUS_USER;
        }
    }

    /**
     * 获取当前用户的完整信息
     *
     * @return 用户DTO，如果未认证则返回null
     */
    public static UserDTO getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof UserDTO) {
                return (UserDTO) principal;
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前用户的角色
     *
     * @return 用户角色，如果未认证则返回null
     */
    public static String getCurrentUserRole() {
        UserDTO user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }

    /**
     * 检查当前用户是否有指定角色
     *
     * @param role 角色名称
     * @return 是否有指定角色
     */
    public static boolean hasRole(String role) {
        String currentRole = getCurrentUserRole();
        return currentRole != null && currentRole.equals(role);
    }

    /**
     * 检查当前用户是否是管理员
     *
     * @return 是否是管理员
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("SUPER_ADMIN");
    }

    /**
     * 检查当前用户是否是超级管理员
     *
     * @return 是否是超级管理员
     */
    public static boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }
}
