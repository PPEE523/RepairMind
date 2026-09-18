package com.example.equipment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.equipment.dto.AuditRequest;
import com.example.equipment.dto.KnowledgeUploadRequest;
import com.example.equipment.dto.Result;
import com.example.equipment.entity.RepairKnowledge;
import com.example.equipment.entity.SysUser;
import com.example.equipment.service.DifyService;
import com.example.equipment.service.RepairKnowledgeService;
import com.example.equipment.service.SysUserService;
import com.example.equipment.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @Resource
    private RepairKnowledgeService repairKnowledgeService;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private DifyService difyService;

    @Resource
    private JwtUtils jwtUtils;

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    /**
     * 检测案例相似度
     */
    @PostMapping("/check-similarity")
    public Result<Map<String, Object>> checkSimilarity(@RequestBody Map<String, String> request) {
        String model = request.get("equipModel");
        String symptom = request.get("symptom");
        // 兼容旧 content 字段
        if (symptom == null || symptom.isEmpty()) {
            symptom = request.get("content");
        }

        if (symptom == null || symptom.trim().isEmpty()) {
            return Result.error(400, "请输入案例内容");
        }

        // 构建查询：仅保留故障描述 + 型号，放弃标题和解决方案
        // Dify /retrieve API 对 query 限制 250 字符
        String modelPrefix = (model != null && !model.isEmpty())
                ? "型号: " + model + " | "
                : "";
        int maxSymptomLen = 250 - modelPrefix.length();
        String truncatedSymptom = symptom.length() > maxSymptomLen
                ? symptom.substring(0, maxSymptomLen)
                : symptom;
        String query = modelPrefix + truncatedSymptom;

        Map<String, Object> checkResult = difyService.checkSimilarity(query, model);
        double similarity = (double) checkResult.getOrDefault("similarity", 0.0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topDocuments = (List<Map<String, Object>>) checkResult
                .getOrDefault("topDocuments", List.of());

        Map<String, Object> result = new HashMap<>();
        result.put("similarity", similarity);

        // 三分级阈值：>80% 高相似 / >60% 部分相似 / ≤60% 无明显相似
        if (similarity > 0.8) {
            result.put("level", "high");
            result.put("isDuplicate", true);
            result.put("message", "检测到高度相似案例，请确认是否重复");
        } else if (similarity > 0.6) {
            result.put("level", "medium");
            result.put("isDuplicate", false);
            result.put("message", "检测到部分相似内容，建议人工比对");
        } else {
            result.put("level", "low");
            result.put("isDuplicate", false);
            result.put("message", "未检测到明显相似案例");
        }

        // 传递匹配到的文档列表（最多3条）
        result.put("topDocuments", topDocuments.size() > 3
                ? topDocuments.subList(0, 3) : topDocuments);

        return Result.success(result);
    }

    /**
     * 敏感信息检测
     */
    @PostMapping("/check-sensitive")
    public Result<Map<String, Object>> checkSensitive(@RequestBody Map<String, String> request) {
        String symptom = request.get("symptom");
        String solution = request.get("solution");
        String content = request.get("content");
        // 优先使用拆分字段，兼容旧 content 字段
        String combinedContent = content != null ? content
                : ((symptom != null ? symptom : "") + "\n" + (solution != null ? solution : ""));

        if (combinedContent.trim().isEmpty()) {
            return Result.error(400, "请输入内容");
        }

        List<String> sensitiveItems = new ArrayList<>();

        // 检测手机号
        if (combinedContent.matches(".*1[3-9]\\d{9}.*")) {
            sensitiveItems.add("手机号码");
        }

        // 检测工号
        if (combinedContent.matches(".*[A-Za-z]{2,3}\\d{4,6}.*")) {
            sensitiveItems.add("工号");
        }

        // 检测身份证号
        if (combinedContent.matches(".*\\d{17}[\\dXx].*")) {
            sensitiveItems.add("身份证号码");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("hasSensitive", !sensitiveItems.isEmpty());
        result.put("sensitiveItems", sensitiveItems);
        result.put("message", sensitiveItems.isEmpty() ? "未检测到敏感信息" :
                "检测到以下敏感信息: " + String.join("、", sensitiveItems));

        return Result.success(result);
    }

    @PostMapping("/upload")
    public Result<Void> upload(
            @Valid @RequestBody KnowledgeUploadRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));

        // 拼接完整内容
        String fullContent = (request.getSymptom() != null ? request.getSymptom() : "")
                + "\n---\n" + (request.getSolution() != null ? request.getSolution() : "");

        RepairKnowledge knowledge = new RepairKnowledge();
        knowledge.setTitle(request.getTitle());
        knowledge.setSymptom(request.getSymptom());
        knowledge.setSolution(request.getSolution());
        knowledge.setContent(fullContent);
        knowledge.setImgUrl(request.getImgUrl());
        knowledge.setImages(request.getImages());
        knowledge.setEquipModel(request.getEquipModel());
        knowledge.setUploadUserId(userId);
        knowledge.setAuditStatus(0); // 待审核
        repairKnowledgeService.save(knowledge);

        log.info("案例上传成功: id={}, title={}, images={}, imgUrl={}",
                knowledge.getId(), knowledge.getTitle(), knowledge.getImages(), knowledge.getImgUrl());
        return Result.success();
    }

    /**
     * 批量上传案例文件
     */
    @PostMapping("/batch-upload")
    public Result<List<Map<String, Object>>> batchUpload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("equipModel") String equipModel,
            @RequestHeader("Authorization") String token) {

        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));

        // 检查文件数量限制
        if (files.length > 10) {
            return Result.error(400, "单次最多上传10个文件");
        }

        List<Map<String, Object>> results = new ArrayList<>();

        for (MultipartFile file : files) {
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", file.getOriginalFilename());

            // 检查文件大小（50MB）
            if (file.getSize() > 50 * 1024 * 1024) {
                result.put("success", false);
                result.put("message", "文件大小超过50MB限制");
                results.add(result);
                continue;
            }

            try {
                // 保存文件
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }

                String fileName = UUID.randomUUID() + extension;
                String dateDir = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));

                java.nio.file.Path basePath = java.nio.file.Paths.get(uploadDir).toAbsolutePath();
                java.nio.file.Path dirPath = basePath.resolve("knowledge/" + dateDir);
                if (!java.nio.file.Files.exists(dirPath)) {
                    java.nio.file.Files.createDirectories(dirPath);
                }

                java.nio.file.Path destPath = dirPath.resolve(fileName);
                file.transferTo(destPath.toFile());

                // 创建知识案例记录
                RepairKnowledge knowledge = new RepairKnowledge();
                knowledge.setTitle(originalFilename);
                knowledge.setContent("文件上传: " + originalFilename);
                knowledge.setImgUrl("/uploads/knowledge/" + dateDir + "/" + fileName);
                knowledge.setEquipModel(equipModel);
                knowledge.setUploadUserId(userId);
                knowledge.setAuditStatus(0);
                repairKnowledgeService.save(knowledge);

                result.put("success", true);
                result.put("message", "上传成功");
                result.put("knowledgeId", knowledge.getId());

            } catch (IOException e) {
                log.error("文件上传失败", e);
                result.put("success", false);
                result.put("message", "上传失败");
            }

            results.add(result);
        }

        return Result.success(results);
    }

    /**
     * 案例图片上传接口
     * 接收单张图片，保存到 uploads/knowledge/images/ 目录，返回可访问的URL
     */
    @PostMapping("/upload-image")
    public Result<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {

        if (file.isEmpty()) {
            return Result.error(400, "图片不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "仅支持图片格式");
        }

        long maxImageSize = 10 * 1024 * 1024;
        if (file.getSize() > maxImageSize) {
            return Result.error(400, "图片大小不能超过10MB");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID() + extension;

            String dateDir = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            java.nio.file.Path basePath = java.nio.file.Paths.get(uploadDir).toAbsolutePath();
            java.nio.file.Path dirPath = basePath.resolve("knowledge/images/" + dateDir);
            if (!java.nio.file.Files.exists(dirPath)) {
                java.nio.file.Files.createDirectories(dirPath);
            }

            java.nio.file.Path filePath = dirPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            String imageUrl = "/uploads/knowledge/images/" + dateDir + "/" + fileName;
            log.info("案例图片上传成功: {}", imageUrl);

            Map<String, Object> result = new HashMap<>();
            result.put("url", imageUrl);
            result.put("fileName", originalFilename);
            return Result.success(result);
        } catch (Exception e) {
            log.error("案例图片上传失败", e);
            return Result.error(500, "图片上传失败：" + e.getMessage());
        }
    }

    /**
     * 语音转文字接口
     * 接收音频文件，调用 Dify /v1/audio-to-text 接口转写成文字
     * 用于案例上传时辅助填写故障描述/解决方案
     */
    @PostMapping("/audio-to-text")
    public Result<Map<String, Object>> audioToText(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        log.info("语音转文字请求: userId={}, fileName={}, size={}",
                userId, file.getOriginalFilename(), file.getSize());

        String text = difyService.audioToText(file, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("text", text);
        return Result.success(result);
    }

    /**
     * 总结对话为案例
     * 接收故障诊断对话内容，调用 Dify Workflow /v1/workflows/run 接口进行总结
     * 返回结构化案例数据 { title, equipModel, symptom, solution }
     */
    @PostMapping("/summarize-to-case")
    public Result<Map<String, Object>> summarizeToCase(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        String conversationContent = (String) request.get("conversationContent");

        if (conversationContent == null || conversationContent.trim().isEmpty()) {
            return Result.error(400, "对话内容不能为空");
        }

        log.info("总结对话为案例: userId={}, contentLen={}", userId, conversationContent.length());

        Map<String, Object> caseData = difyService.summarizeConversationToCase(conversationContent, userId);
        return Result.success(caseData);
    }

    @GetMapping("/list")
    public Result<Page<RepairKnowledge>> list(
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        // 获取用户角色
        SysUser user = sysUserService.getById(userId);
        Long roleId = user != null ? user.getRoleId() : null;
        Page<RepairKnowledge> page = repairKnowledgeService.getKnowledgeList(auditStatus, pageNum, pageSize, userId, roleId);
        return Result.success(page);
    }

    @GetMapping("/audit/list")
    public Result<Page<RepairKnowledge>> auditList(
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<RepairKnowledge> page = repairKnowledgeService.getAuditList(auditStatus, pageNum, pageSize);
        return Result.success(page);
    }

    @PostMapping("/audit")
    public Result<Void> audit(
            @Valid @RequestBody AuditRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        SysUser user = sysUserService.getById(userId);

        // 权限检查：只有管理员和资深检修工程师可以审核
        if (user == null || (user.getRoleId() != 1 && user.getRoleId() != 2)) {
            return Result.error(403, "权限不足，无法审核案例");
        }

        // 如果指定了知识库列表，则上传到指定知识库
        if (request.getDatasetIds() != null && !request.getDatasetIds().isEmpty()) {
            repairKnowledgeService.auditAndUpload(
                    request.getKnowledgeId(),
                    request.getAuditStatus(),
                    request.getRemark(),
                    request.getTitle(),
                    request.getContent(),
                    request.getSymptom(),
                    request.getSolution(),
                    request.getEquipModel(),
                    request.getDatasetIds()
            );
        } else {
            repairKnowledgeService.audit(
                    request.getKnowledgeId(),
                    request.getAuditStatus(),
                    request.getRemark(),
                    request.getTitle(),
                    request.getContent(),
                    request.getSymptom(),
                    request.getSolution(),
                    request.getEquipModel()
            );
        }
        return Result.success();
    }

    /**
     * 获取知识库列表（用于审核时选择，支持按模块 scope 过滤）
     * @param scope 模块标识，不传默认 case-audit
     */
    @GetMapping("/datasets")
    public Result<List<Map<String, Object>>> getDatasets(
            @RequestParam(defaultValue = "case-audit") String scope) {
        List<Map<String, Object>> datasets = difyService.getDatasets(scope);
        return Result.success(datasets);
    }

    /**
     * 批量检查案例的知识库文档是否存在
     */
    @PostMapping("/check-documents")
    public Result<Map<Long, Map<String, Object>>> checkDocuments(@RequestBody List<Long> knowledgeIds) {
        Map<Long, Map<String, Object>> result = repairKnowledgeService.checkDocuments(knowledgeIds);
        return Result.success(result);
    }

    /**
     * 删除案例（仅管理员可操作，用于清理知识库中已不存在的文档记录）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteKnowledge(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        SysUser user = sysUserService.getById(userId);
        if (user == null || user.getRoleId() != 1) {
            return Result.error(403, "权限不足，仅管理员可删除案例");
        }
        boolean removed = repairKnowledgeService.removeById(id);
        if (removed) {
            log.info("管理员 {} 删除了案例 id={}", userId, id);
            return Result.success();
        }
        return Result.error(404, "案例不存在");
    }

    /**
     * 根据 Dify 文档 ID 批量查询已审核案例（用户端案例卡片使用）
     */
    @PostMapping("/by-dify-doc")
    public Result<List<Map<String, Object>>> getCasesByDifyDocIds(
            @RequestBody Map<String, List<String>> body,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        List<String> docIds = body.get("documentIds");
        if (docIds == null || docIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<RepairKnowledge> cases = repairKnowledgeService.listByDocumentIds(docIds);

        List<Map<String, Object>> result = cases.stream().map(k -> {
            Map<String, Object> map = new HashMap<>();
            map.put("knowledgeId", k.getId());
            map.put("documentId", k.getDocumentId());
            map.put("title", k.getTitle());
            map.put("equipModel", k.getEquipModel());
            map.put("symptom", k.getSymptom());
            map.put("solution", k.getSolution());
            map.put("content", k.getContent());
            map.put("imgUrl", k.getImgUrl());
            map.put("images", k.getImages());
            map.put("createTime", k.getCreateTime());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        return Result.success(result);
    }
}
