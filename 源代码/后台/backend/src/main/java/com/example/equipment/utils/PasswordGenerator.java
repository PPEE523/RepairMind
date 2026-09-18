package com.example.equipment.utils;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "Admin@123456";
        String encodedPassword = encoder.encode(rawPassword);

        System.out.println("原始密码: " + rawPassword);
        System.out.println("加密后的密码: " + encodedPassword);
        System.out.println("\n请在数据库中执行以下SQL:");
        System.out.println("UPDATE sys_user SET password = '" + encodedPassword + "' WHERE username = 'admin';");

        boolean matches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("\n验证结果: " + matches);
    }
}

