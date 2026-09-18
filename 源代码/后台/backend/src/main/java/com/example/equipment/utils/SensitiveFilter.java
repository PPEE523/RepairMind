package com.example.equipment.utils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 敏感信息过滤工具类
 */
public class SensitiveFilter {

    /**
     * 敏感词列表
     */
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "密码", "口令", "token", "secret", "key", "credential",
            "身份证", "手机号", "电话号码", "银行卡", "账号密码"
    );

    /**
     * 手机号正则
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    /**
     * 身份证号正则
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[\\dXx]");

    /**
     * 银行卡号正则
     */
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\d{16,19}");

    /**
     * 过滤敏感信息
     * @param content 原始内容
     * @return 过滤后的内容
     */
    public static String filter(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String result = content;

        // 过滤敏感词
        for (String word : SENSITIVE_WORDS) {
            result = result.replaceAll("(?i)" + word, "***");
        }

        // 过滤手机号
        result = PHONE_PATTERN.matcher(result).replaceAll("1**********");

        // 过滤身份证号
        result = ID_CARD_PATTERN.matcher(result).replaceAll("******************");

        // 过滤银行卡号
        result = BANK_CARD_PATTERN.matcher(result).replaceAll("****************");

        return result;
    }

    /**
     * 检查内容是否包含敏感信息
     * @param content 内容
     * @return 是否包含敏感信息
     */
    public static boolean containsSensitive(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        // 检查敏感词
        for (String word : SENSITIVE_WORDS) {
            if (content.toLowerCase().contains(word.toLowerCase())) {
                return true;
            }
        }

        // 检查手机号
        if (PHONE_PATTERN.matcher(content).find()) {
            return true;
        }

        // 检查身份证号
        if (ID_CARD_PATTERN.matcher(content).find()) {
            return true;
        }

        // 检查银行卡号
        if (BANK_CARD_PATTERN.matcher(content).find()) {
            return true;
        }

        return false;
    }
}