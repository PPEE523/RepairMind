package com.example.equipment.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security工具类
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户
     */
    public static UserContext getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserContext) {
            return (UserContext) principal;
        }

        return null;
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        UserContext user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        UserContext user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 判断是否为管理员
     */
    public static boolean isAdmin() {
        UserContext user = getCurrentUser();
        return user != null && user.getRoleId() != null && user.getRoleId() == 1;
    }
}
