package com.example.equipment.utils;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "Admin@123456";
        String encodedPassword = encoder.encode(rawPassword);

        boolean matches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("密码生成完成，校验结果: " + matches);
    }
}
