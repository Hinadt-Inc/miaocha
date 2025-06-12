package com.hinadt.miaocha.common.util;

import com.hinadt.miaocha.application.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** 用户上下文工具类 用于获取当前登录用户信息 */
@Component
public class UserContextUtil {

    private static final String DEFAULT_SYSTEM_USER = "system";
    private static final String DEFAULT_ANONYMOUS_USER = "anonymous";

    private static UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        UserContextUtil.userService = userService;
    }

    /**
     * 获取当前登录用户的邮箱
     *
     * @return 用户邮箱，如果未登录则返回"anonymous"，如果是系统操作则返回"system"
     */
    public static String getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return DEFAULT_ANONYMOUS_USER;
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof String) {
                String uid = (String) principal;
                // 直接根据uid获取用户邮箱（高效查询）
                if (userService != null) {
                    return userService.getUserEmailByUid(uid);
                }
            }

            return DEFAULT_ANONYMOUS_USER;
        } catch (Exception e) {
            // 如果获取用户信息失败，返回默认值
            return DEFAULT_ANONYMOUS_USER;
        }
    }

    /**
     * 获取当前用户邮箱，如果未登录则返回指定的默认邮箱
     *
     * @param defaultUser 默认用户邮箱
     * @return 用户邮箱
     */
    public static String getCurrentUserEmail(String defaultUser) {
        String email = getCurrentUserEmail();
        return DEFAULT_ANONYMOUS_USER.equals(email) ? defaultUser : email;
    }

    /**
     * 检查当前是否有已登录的用户
     *
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null
                    && authentication.isAuthenticated()
                    && !DEFAULT_ANONYMOUS_USER.equals(authentication.getName());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取系统用户邮箱 用于系统自动操作
     *
     * @return 系统用户邮箱
     */
    public static String getSystemUser() {
        return DEFAULT_SYSTEM_USER;
    }
}
