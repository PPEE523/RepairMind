package com.example.equipment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数据传输加密配置
 * 默认关闭，生产环境开启
 */
@Data
@Component
@ConfigurationProperties(prefix = "crypto")
public class CryptoProperties {

    /**
     * 是否启用加密传输
     */
    private boolean enabled = false;

    /**
     * AES密钥（32字符 = 256位）
     */
    private String secretKey = "EquipmentRepair2026SecretKey!@#$";

    /**
     * 需要排除的URL路径（不加密）
     */
    private String[] excludePaths = {
            "/api/auth/**",
            "/api/upload/**",
            "/api/dify/knowledge/datasets/*/documents/upload",
            "/api/dify/knowledge/datasets/*/documents/*/download",
            "/api/search/**",
    };
}
