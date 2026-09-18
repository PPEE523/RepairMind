package com.example.equipment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.equipment.dto.Result;
import com.example.equipment.dto.SearchHistoryQuery;
import com.example.equipment.dto.SearchHistoryVO;
import com.example.equipment.dto.SearchRequest;
import com.example.equipment.dto.SearchResponse;
import com.example.equipment.entity.SearchHistory;
import com.example.equipment.enums.DifyAgentType;
import com.example.equipment.security.SecurityUtils;
import com.example.equipment.service.DifyService;
import com.example.equipment.service.SearchHistoryService;
import com.example.equipment.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Resource
    private DifyService difyService;

    @Resource
    private SearchHistoryService searchHistoryService;

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    private static final String SEARCH_CACHE_PREFIX = "search:cache:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @PostMapping("/multi")
    public Result<SearchResponse> multiSearch(
            @RequestBody SearchRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));

        // 生成缓存key
        String cacheKey = generateCacheKey(request);

        // 尝试从缓存获取
        try {
            String cachedResult = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                log.info("从缓存获取检索结果: key={}", cacheKey);
                SearchResponse response = objectMapper.readValue(cachedResult, SearchResponse.class);
                return Result.success(response);
            }
        } catch (Exception e) {
            log.warn("读取缓存失败", e);
        }

        try {
            SearchResponse response = difyService.search(request, userId, DifyAgentType.fromCode(request.getAgentType()));

            // 保存到缓存
            try {
                String responseJson = objectMapper.writeValueAsString(response);
                stringRedisTemplate.opsForValue().set(cacheKey, responseJson, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                log.info("检索结果已缓存: key={}", cacheKey);
            } catch (Exception e) {
                log.warn("缓存检索结果失败", e);
            }

            // 保存检索历史（成功）
            saveHistory(userId, request, response.getAnswer(), response.getConversationId(), true, null);

            return Result.success(response);
        } catch (Exception e) {
            log.error("检索失败", e);
            // 保存检索历史（失败）
            saveHistory(userId, request, null, null, false, e.getMessage());
            return Result.error("检索失败: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public Result<Page<SearchHistoryVO>> getHistory(
            @RequestHeader("Authorization") String token,
            SearchHistoryQuery query) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        Page<SearchHistoryVO> page;
        if (SecurityUtils.isAdmin()) {
            page = searchHistoryService.getHistoryList(query);
        } else {
            page = searchHistoryService.getUserHistoryList(userId, query);
        }
        return Result.success(page);
    }

    @GetMapping("/history/{id}")
    public Result<SearchHistoryVO> getHistoryDetail(@PathVariable Long id) {
        SearchHistoryVO detail = searchHistoryService.getHistoryDetail(id);
        if (detail == null) {
            return Result.error(404, "记录不存在");
        }
        return Result.success(detail);
    }

    @DeleteMapping("/history/{id}")
    public Result<Void> deleteHistory(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        // 管理员可删除任意记录，普通用户只能删除自己的
        SearchHistory history = searchHistoryService.getById(id);
        if (history == null) {
            return Result.error(404, "记录不存在");
        }
        if (!SecurityUtils.isAdmin() && !history.getUserId().equals(userId)) {
            return Result.error(403, "无权删除该记录");
        }
        searchHistoryService.removeById(id);
        return Result.success();
    }

    @DeleteMapping("/history/batch")
    public Result<Void> batchDeleteHistory(
            @RequestBody List<Long> ids,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        if (SecurityUtils.isAdmin()) {
            // 管理员直接批量删除
            searchHistoryService.removeByIds(ids);
        } else {
            // 普通用户只能删除自己的
            searchHistoryService.batchDeleteHistory(userId, ids);
        }
        return Result.success();
    }

    /**
     * 保存检索历史
     */
    private void saveHistory(Long userId, SearchRequest request, String resultAnswer,
                             String conversationId, boolean success, String errorMessage) {
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
            history.setConversationId(conversationId);
            searchHistoryService.save(history);
        } catch (Exception e) {
            log.warn("保存检索历史失败", e);
        }
    }

    /**
     * 生成缓存key
     */
    private String generateCacheKey(SearchRequest request) {
        try {
            String fileUrlsStr = request.getFileUrls() != null ? String.join(",", request.getFileUrls()) : "";
            String keyContent = request.getQuery() + "|" +
                    (request.getEquipModel() != null ? request.getEquipModel() : "") + "|" +
                    fileUrlsStr;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(keyContent.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return SEARCH_CACHE_PREFIX + sb.toString();
        } catch (Exception e) {
            log.warn("生成缓存key失败", e);
            return SEARCH_CACHE_PREFIX + System.currentTimeMillis();
        }
    }
}
