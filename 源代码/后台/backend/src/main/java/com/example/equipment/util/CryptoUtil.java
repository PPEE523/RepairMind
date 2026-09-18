package com.example.equipment.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-CBC 加解密工具
 * 加密格式：Base64(IV[16字节] + 密文)
 */
public class CryptoUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    private CryptoUtil() {}

    /**
     * AES-256-CBC 加密
     * @param plaintext 明文
     * @param secretKey 密钥（32字符）
     * @return Base64(IV + 密文)
     */
    public static String encrypt(String plaintext, String secretKey) {
        try {
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);

            // 生成随机IV
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // IV + 密文拼接后Base64编码
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * AES-256-CBC 解密
     * @param ciphertext Base64(IV + 密文)
     * @param secretKey 密钥（32字符）
     * @return 明文
     */
    public static String decrypt(String ciphertext, String secretKey) {
        try {
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);

            byte[] combined = Base64.getDecoder().decode(ciphertext);

            // 提取IV
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // 提取密文
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }
}
