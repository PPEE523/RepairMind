package com.example.equipment.controller;

import com.example.equipment.dto.Result;
import com.example.equipment.util.VideoFrameExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    @Value("${max-file-size-mb:10}")
    private Integer maxFileSizeMb;

    @Value("${upload.max-video-size-mb:50}")
    private Integer maxVideoSizeMb;

    /**
     * 上传文件（图片或视频）。视频上传后自动提取关键帧为图片
     */
    @PostMapping
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        // 检查文件是否为空
        if (file.isEmpty()) {
            return Result.error(400, "请选择要上传的文件");
        }

        // 获取文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 检查文件类型
        String lowerExtension = extension.toLowerCase();
        if (!isAllowedFileType(lowerExtension)) {
            return Result.error(400, "不支持的文件类型");
        }

        // 检查文件大小
        boolean isVideo = isVideoFile(lowerExtension);
        long maxSize = isVideo ? maxVideoSizeMb * 1024L * 1024 : maxFileSizeMb * 1024L * 1024;
        if (file.getSize() > maxSize) {
            String sizeLimit = isVideo ? maxVideoSizeMb + "MB" : maxFileSizeMb + "MB";
            return Result.error(400, "文件大小不能超过" + sizeLimit);
        }

        // 生成新的文件名
        String newFileName = UUID.randomUUID().toString() + extension;

        // 按日期创建目录
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path basePath = Paths.get(uploadDir).toAbsolutePath();
        Path dirPath = basePath.resolve(dateDir);

        // 保存文件
        try {
            // 判断存储路径是否存在，不存在则新建
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Path destFile = dirPath.resolve(newFileName);
            file.transferTo(destFile.toFile());

            // 如果是视频文件，提取关键帧
            if (isVideo) {
                List<String> frameUrls = VideoFrameExtractor.extractFrames(
                        destFile.toAbsolutePath().toString(), uploadDir);

                if (frameUrls.isEmpty()) {
                    log.error("视频截帧失败，无有效帧: {}", destFile);
                    return Result.error(500, "视频截帧失败");
                }

                // 返回帧图片URL列表
                Map<String, Object> result = new HashMap<>();
                result.put("urls", frameUrls);
                List<String> names = new ArrayList<>();
                for (int i = 0; i < frameUrls.size(); i++) {
                    names.add("frame_" + (i + 1) + ".jpg");
                }
                result.put("names", names);
                result.put("size", file.getSize());
                result.put("fileType", "image");
                result.put("frameCount", frameUrls.size());

                log.info("视频上传并截帧成功: video={}, frames={}", newFileName, frameUrls.size());
                return Result.success(result);
            }

            // 返回文件访问路径（图片）
            String fileUrl = "/uploads/" + dateDir + "/" + newFileName;

            Map<String, Object> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("name", originalFilename);
            result.put("size", String.valueOf(file.getSize()));
            result.put("fileType", "image");

            log.info("图片上传成功: {}", fileUrl);
            return Result.success(result);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error(500, "文件上传失败");
        }
    }

    /**
     * 检查文件类型是否允许
     */
    private boolean isAllowedFileType(String extension) {
        // 图片
        if (".jpg".equals(extension) || ".jpeg".equals(extension) ||
                ".png".equals(extension) || ".gif".equals(extension) ||
                ".bmp".equals(extension)) {
            return true;
        }
        // 视频
        if (".mp4".equals(extension) || ".avi".equals(extension) ||
                ".mov".equals(extension) || ".mkv".equals(extension) ||
                ".webm".equals(extension)) {
            return true;
        }
        // 文档
        if (".pdf".equals(extension) || ".doc".equals(extension) ||
                ".docx".equals(extension) || ".xls".equals(extension) ||
                ".xlsx".equals(extension) || ".txt".equals(extension)) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否为视频文件
     */
    private boolean isVideoFile(String extension) {
        return ".mp4".equals(extension) || ".avi".equals(extension) ||
                ".mov".equals(extension) || ".mkv".equals(extension) ||
                ".webm".equals(extension);
    }
}
