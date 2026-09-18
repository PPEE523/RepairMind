package com.example.equipment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.equipment.dto.*;
import com.example.equipment.config.ChatProperties;
import com.example.equipment.security.SecurityUtils;
import com.example.equipment.service.ChatService;
import com.example.equipment.util.VideoFrameExtractor;
import com.example.equipment.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI智能问答 - 对话控制器
 * 路径前缀：/api/chat
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private ChatProperties chatProperties;

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 发起提问（SSE流式）
     * 支持附件上传 + 设备型号 + 多轮对话（conversationId）
     */
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendChatMessage(
            @RequestBody ChatRequest request,
            @RequestHeader("Authorization") String token) {

        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));

        // 创建SSE发射器，超时时间120秒（AI回答可能较慢）
        SseEmitter emitter = new SseEmitter(120000L);

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时: userId={}", userId);
            emitter.complete();
        });
        emitter.onCompletion(() -> log.info("SSE连接完成: userId={}", userId));
        emitter.onError(e -> log.error("SSE连接错误: userId={}", userId, e));

        // 异步处理对话请求（传递SecurityContext到异步线程）
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        executorService.execute(() -> {
            SecurityContextHolder.setContext(securityContext);
            try {
                chatService.sendChatMessage(request, userId, new ChatService.ChatStreamCallback() {
                    @Override
                    public void onMessage(String content) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(content));
                        } catch (IOException e) {
                            log.error("发送SSE消息失败", e);
                        }
                    }

                    @Override
                    public void onComplete(String answer, String conversationId, String messageId,
                                           List<ChatMessageVO.RetrieverResource> resources, Long chatHistoryId) {
                        try {
                            // 构建完成事件数据
                            Map<String, Object> doneData = new HashMap<>();
                            doneData.put("answer", answer);
                            doneData.put("conversationId", conversationId);
                            doneData.put("messageId", messageId);
                            doneData.put("resources", resources != null ? resources : Collections.emptyList());
                            doneData.put("chatHistoryId", chatHistoryId);

                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data(objectMapper.writeValueAsString(doneData)));
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("发送SSE完成消息失败", e);
                            try {
                                emitter.complete();
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    @Override
                    public void onError(String error) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data(error));
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("发送SSE错误消息失败", e);
                        }
                    }
                });
            } catch (Exception e) {
                log.error("对话请求处理失败", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("对话请求处理失败: " + e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    log.error("发送SSE错误消息失败", ex);
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        return emitter;
    }

    /**
     * 获取对话历史列表（分页）
     * 管理员看全部，普通用户看自己
     */
    @GetMapping("/conversations")
    public Result<Page<ChatConversationVO>> getConversationList(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {

        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        boolean isAdmin = SecurityUtils.isAdmin();

        Page<ChatConversationVO> page = chatService.getConversationList(userId, isAdmin, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 根据对话ID获取多轮详情
     */
    @GetMapping("/conversations/{conversationId}")
    public Result<List<ChatMessageVO>> getConversationDetail(
            @PathVariable String conversationId) {

        List<ChatMessageVO> messages = chatService.getConversationDetail(conversationId);
        return Result.success(messages);
    }

    /**
     * 删除对话（同步删除Dify远端记录）
     */
    @DeleteMapping("/conversations/{conversationId}")
    public Result<Void> deleteConversation(
            @PathVariable String conversationId,
            @RequestHeader("Authorization") String token) {

        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        chatService.deleteConversation(conversationId, userId);
        return Result.success();
    }

    /**
     * 提交回答标注（正确/不正确）
     * 仅当AI回答引用了知识库文档时才允许标注
     */
    @PostMapping("/feedback")
    public Result<Void> submitFeedback(
            @RequestBody ChatFeedbackRequest request,
            @RequestHeader("Authorization") String token) {

        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));

        try {
            chatService.submitFeedback(request, userId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 附件上传接口（含格式+大小校验）
     * 返回文件URL，供对话提问时引用
     * 文件按类型分目录存储：uploads/images/、uploads/videos/、uploads/audios/、uploads/files/
     * 每个目录下按日期(yyyy-MM-dd)存储
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        // 1. 校验文件是否为空
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 2. 校验文件格式
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return Result.error("文件名无效");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        List<String> allowedExtensions = chatProperties.getAllowedExtensions();
        if (!allowedExtensions.contains(extension)) {
            return Result.error("不支持的文件格式: " + extension + "，允许的格式: " + String.join(",", allowedExtensions));
        }

        // 3. 校验对应类型的上传开关
        String fileType = chatProperties.determineFileTypeWithCheck(extension);
        if (fileType == null) {
            String typeName = chatProperties.determineFileType(extension);
            String typeNameCn;
            switch (typeName) {
                case "image": typeNameCn = "图片"; break;
                case "video": typeNameCn = "视频"; break;
                case "audio": typeNameCn = "音频"; break;
                case "file": typeNameCn = "文件"; break;
                default: typeNameCn = "该类型"; break;
            }
            return Result.error(typeNameCn + "上传功能已关闭");
        }

        // 4. 校验文件大小
        long fileSize = file.getSize();
        long maxSizeBytes = chatProperties.getMaxFileSizeBytes();
        if (fileSize > maxSizeBytes) {
            return Result.error("文件大小超过限制: 最大" + chatProperties.getMaxFileSizeMb() + "MB");
        }

        // 5. 保存文件到本地（按类型分子目录，按日期存储）
        try {
            // 按类型确定子目录名
            String subDir;
            switch (fileType) {
                case "video": subDir = "videos"; break;
                case "audio": subDir = "audios"; break;
                case "file": subDir = "files"; break;
                default: subDir = "images"; break;
            }

            // 日期目录格式：yyyy-MM-dd
            String datePath = String.format("%tY-%<tm-%<td", new Date());
            Path basePath = Paths.get(uploadDir).toAbsolutePath();
            Path dirPath = basePath.resolve(subDir).resolve(datePath);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path filePath = dirPath.resolve(newFileName);
            file.transferTo(filePath.toFile());

            // 6. 如果是视频文件，自动提取关键帧
            boolean isVideo = "video".equals(fileType);
            if (isVideo) {
                List<String> frameUrls = VideoFrameExtractor.extractFrames(
                        filePath.toAbsolutePath().toString(), uploadDir);

                if (frameUrls.isEmpty()) {
                    log.error("视频截帧失败，无有效帧: {}", filePath);
                    return Result.error("视频截帧失败");
                }

                // 返回帧图片URL列表
                Map<String, Object> result = new HashMap<>();
                result.put("urls", frameUrls);
                List<String> names = new ArrayList<>();
                for (int i = 0; i < frameUrls.size(); i++) {
                    names.add("frame_" + (i + 1) + ".jpg");
                }
                result.put("names", names);
                result.put("size", fileSize);
                result.put("fileType", "image");
                result.put("frameCount", frameUrls.size());

                log.info("视频上传并截帧成功: video={}, frames={}", newFileName, frameUrls.size());
                return Result.success(result);
            }

            // 返回文件URL（非视频文件）
            String url = "/uploads/" + subDir + "/" + datePath + "/" + newFileName;
            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("name", originalFilename);
            result.put("size", fileSize);
            result.put("fileType", fileType);

            return Result.success(result);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取上传配置（供前端查询开关状态和允许格式）
     */
    @GetMapping("/upload-config")
    public Result<Map<String, Object>> getUploadConfig() {
        return Result.success(chatProperties.getUploadConfigMap());
    }
}
