package com.example.equipment.controller;

import com.example.equipment.dto.SearchRequest;
import com.example.equipment.entity.SearchHistory;
import com.example.equipment.enums.DifyAgentType;
import com.example.equipment.service.DifyService;
import com.example.equipment.service.SearchHistoryService;
import com.example.equipment.utils.JwtUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/search")
public class SearchSseController {

    @Resource
    private DifyService difyService;

    @Resource
    private SearchHistoryService searchHistoryService;

    @Resource
    private JwtUtils jwtUtils;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * SSE流式检索接口（POST + JSON Body，避免URL编码导致文件路径被破坏）
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSearch(
            @RequestBody SearchRequest request,
            @RequestHeader("Authorization") String token) {

        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));

        String agentType = request.getAgentType() != null ? request.getAgentType() : "zdjc";

        // 创建SSE发射器，超时时间60秒
        SseEmitter emitter = new SseEmitter(60000L);

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时: userId={}", userId);
            emitter.complete();
        });

        emitter.onCompletion(() -> {
            log.info("SSE连接完成: userId={}", userId);
        });

        emitter.onError(e -> {
            log.error("SSE连接错误: userId={}", userId, e);
        });

        // 异步处理检索请求
        executorService.execute(() -> {
            StringBuilder fullAnswer = new StringBuilder();

            try {
                difyService.streamSearch(request, userId, new DifyService.StreamCallback() {
                    @Override
                    public void onMessage(String content) {
                        fullAnswer.append(content);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(content));
                        } catch (IOException e) {
                            log.error("发送SSE消息失败", e);
                        }
                    }

                    @Override
                    public void onComplete(String answer) {
                        // 保存检索历史（成功）
                        Long historyId = saveHistory(userId, request, answer, true, null);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data("{\"answer\":" + toJsonString(answer) + ",\"historyId\":" + historyId + "}"));
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("发送SSE完成消息失败", e);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // 保存检索历史（失败）
                        saveHistory(userId, request, fullAnswer.toString(), false, error);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data(error));
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("发送SSE错误消息失败", e);
                        }
                    }
                }, DifyAgentType.fromCode(agentType));

            } catch (Exception e) {
                log.error("SSE检索失败", e);
                // 保存检索历史（失败）
                saveHistory(userId, request, fullAnswer.toString(), false, e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("检索失败: " + e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    log.error("发送SSE错误消息失败", ex);
                }
            }
        });

        return emitter;
    }

    /**
     * 保存检索历史，返回记录ID
     */
    private Long saveHistory(Long userId, SearchRequest request, String resultAnswer,
                             boolean success, String errorMessage) {
        try {
            SearchHistory history = new SearchHistory();
            history.setUserId(userId);
            history.setQueryText(request.getQuery());
            history.setEquipModel(request.getEquipModel());

            // 保存文件URL和类型
            List<String> urls = request.getFileUrls();
            List<String> types = request.getFileTypes();
            if (urls != null && !urls.isEmpty()) {
                // 第一个文件的URL存入imageUrl（兼容旧字段）
                history.setImageUrl(urls.get(0));
                // 所有文件URL存入imageUrls（JSON数组）
                history.setImageUrls(urls.stream()
                        .map(url -> "\"" + url + "\"")
                        .collect(Collectors.joining(",", "[", "]")));
            }
            if (types != null && !types.isEmpty()) {
                // 文件类型存入fileTypes（JSON数组）
                history.setFileTypes(types.stream()
                        .map(type -> "\"" + type + "\"")
                        .collect(Collectors.joining(",", "[", "]")));
            }
            // 保存原始文件名
            List<String> names = request.getFileNames();
            if (names != null && !names.isEmpty()) {
                history.setFileNames(names.stream()
                        .map(name -> "\"" + name.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                        .collect(Collectors.joining(",", "[", "]")));
            }

            history.setResultAnswer(resultAnswer);
            history.setSearchSuccess(success);
            history.setErrorMessage(errorMessage);
            searchHistoryService.save(history);
            return history.getId();
        } catch (Exception e) {
            log.warn("保存检索历史失败", e);
            return null;
        }
    }

    /**
     * 将字符串转为JSON安全格式
     */
    private String toJsonString(String str) {
        if (str == null) return "null";
        return "\"" + str
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
