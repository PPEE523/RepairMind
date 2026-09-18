package com.example.equipment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI智能问答 - 配置属性类
 * 对应 application.yml 中的 chat 配置节
 */
@Data
@Component
@ConfigurationProperties(prefix = "chat")
public class ChatProperties {

    /** Dify Agent ApiKey（由 application.yml 中 chat.api-key 配置） */
    private String apiKey;

    /** 单次对话最大轮数（0表示不限制） */
    private int maxRounds = 0;

    /** 单文件最大大小（MB） */
    private long maxFileSizeMb = 50;

    /** 允许的图片格式（逗号分隔） */
    private String allowedImageTypes = "jpg,jpeg,png,gif,webp,svg";

    /** 允许的视频格式（逗号分隔） */
    private String allowedVideoTypes = "mp4,mov,mpeg,webm";

    /** 允许的音频格式（逗号分隔） */
    private String allowedAudioTypes = "mp3,wav,aac,ogg,m4a,flac,webm,mpga";

    /** 允许的文件格式（逗号分隔） */
    private String allowedFileTypes = "txt,md,pdf,xls,xlsx,doc,docx";

    /** 图片上传开关 */
    private boolean enableImageUpload = true;

    /** 视频上传开关 */
    private boolean enableVideoUpload = true;

    /** 音频上传开关 */
    private boolean enableAudioUpload = true;

    /** 文件上传开关 */
    private boolean enableFileUpload = true;

    /**
     * 获取所有允许的文件扩展名列表（仅包含已开启类型的扩展名）
     */
    public List<String> getAllowedExtensions() {
        List<String> extensions = new ArrayList<>();
        if (enableImageUpload && allowedImageTypes != null && !allowedImageTypes.isEmpty()) {
            for (String ext : allowedImageTypes.split(",")) {
                extensions.add(ext.trim().toLowerCase());
            }
        }
        if (enableVideoUpload && allowedVideoTypes != null && !allowedVideoTypes.isEmpty()) {
            for (String ext : allowedVideoTypes.split(",")) {
                extensions.add(ext.trim().toLowerCase());
            }
        }
        if (enableAudioUpload && allowedAudioTypes != null && !allowedAudioTypes.isEmpty()) {
            for (String ext : allowedAudioTypes.split(",")) {
                extensions.add(ext.trim().toLowerCase());
            }
        }
        if (enableFileUpload && allowedFileTypes != null && !allowedFileTypes.isEmpty()) {
            for (String ext : allowedFileTypes.split(",")) {
                extensions.add(ext.trim().toLowerCase());
            }
        }
        return extensions;
    }

    /**
     * 获取最大文件大小（字节）
     */
    public long getMaxFileSizeBytes() {
        return maxFileSizeMb * 1024 * 1024;
    }

    /**
     * 根据文件扩展名判断文件类型
     * @param extension 文件扩展名（不含点号）
     * @return image / video / audio / file
     */
    public String determineFileType(String extension) {
        if (extension == null) return "file";
        String ext = extension.trim().toLowerCase();
        List<String> imageExts = parseExtensions(allowedImageTypes);
        if (imageExts.contains(ext)) return "image";
        List<String> videoExts = parseExtensions(allowedVideoTypes);
        if (videoExts.contains(ext)) return "video";
        List<String> audioExts = parseExtensions(allowedAudioTypes);
        if (audioExts.contains(ext)) return "audio";
        List<String> fileExts = parseExtensions(allowedFileTypes);
        if (fileExts.contains(ext)) return "file";
        return "file";
    }

    /**
     * 根据文件扩展名判断文件类型，同时检查对应开关是否开启
     * @param extension 文件扩展名（不含点号）
     * @return 文件类型，若对应开关关闭则返回 null
     */
    public String determineFileTypeWithCheck(String extension) {
        String fileType = determineFileType(extension);
        switch (fileType) {
            case "image": return enableImageUpload ? "image" : null;
            case "video": return enableVideoUpload ? "video" : null;
            case "audio": return enableAudioUpload ? "audio" : null;
            case "file": return enableFileUpload ? "file" : null;
            default: return null;
        }
    }

    /**
     * 获取上传配置信息（供前端查询）
     */
    public java.util.Map<String, Object> getUploadConfigMap() {
        java.util.Map<String, Object> config = new java.util.LinkedHashMap<>();
        config.put("enableImageUpload", enableImageUpload);
        config.put("enableVideoUpload", enableVideoUpload);
        config.put("enableAudioUpload", enableAudioUpload);
        config.put("enableFileUpload", enableFileUpload);
        config.put("allowedImageTypes", allowedImageTypes);
        config.put("allowedVideoTypes", allowedVideoTypes);
        config.put("allowedAudioTypes", allowedAudioTypes);
        config.put("allowedFileTypes", allowedFileTypes);
        config.put("maxFileSizeMb", maxFileSizeMb);
        return config;
    }

    private List<String> parseExtensions(String config) {
        if (config == null || config.isEmpty()) return List.of();
        return Arrays.stream(config.split(","))
                .map(s -> s.trim().toLowerCase())
                .collect(Collectors.toList());
    }
}
