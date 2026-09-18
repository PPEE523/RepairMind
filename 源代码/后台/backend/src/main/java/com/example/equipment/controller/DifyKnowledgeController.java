package com.example.equipment.controller;

import com.example.equipment.dto.Result;
import com.example.equipment.service.DifyKnowledgeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dify/knowledge")
public class DifyKnowledgeController {

    @Resource
    private DifyKnowledgeService difyKnowledgeService;

    /**
     * 获取知识库列表（支持按模块 scope 过滤）
     * @param scope 模块标识，不传默认 knowledge-management
     */
    @GetMapping("/datasets")
    public Result<List<Map<String, Object>>> getDatasets(
            @RequestParam(defaultValue = "knowledge-management") String scope) {
        List<Map<String, Object>> datasets = difyKnowledgeService.getDatasets(scope);
        return Result.success(datasets);
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/datasets/{datasetId}")
    public Result<Map<String, Object>> getDatasetDetail(@PathVariable String datasetId) {
        Map<String, Object> dataset = difyKnowledgeService.getDatasetDetail(datasetId);
        return Result.success(dataset);
    }

    /**
     * 更新知识库信息（名称、描述）
     */
    @PatchMapping("/datasets/{datasetId}")
    public Result<Map<String, Object>> updateDataset(
            @PathVariable String datasetId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Map<String, Object> result = difyKnowledgeService.updateDataset(datasetId, name, description);
        return Result.success(result);
    }

    /**
     * 获取知识库文档列表
     */
    @GetMapping("/datasets/{datasetId}/documents")
    public Result<Map<String, Object>> getDocuments(
            @PathVariable String datasetId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = difyKnowledgeService.getDocuments(datasetId, page, limit);
        return Result.success(result);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/datasets/{datasetId}/documents/{documentId}")
    public Result<Map<String, Object>> getDocumentDetail(
            @PathVariable String datasetId,
            @PathVariable String documentId) {
        Map<String, Object> document = difyKnowledgeService.getDocumentDetail(datasetId, documentId);
        return Result.success(document);
    }

    /**
     * 获取文档分段列表
     */
    @GetMapping("/datasets/{datasetId}/documents/{documentId}/segments")
    public Result<Map<String, Object>> getDocumentSegments(
            @PathVariable String datasetId,
            @PathVariable String documentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = difyKnowledgeService.getDocumentSegments(datasetId, documentId, page, limit);
        return Result.success(result);
    }

    /**
     * 上传文档到知识库
     */
    @PostMapping("/datasets/{datasetId}/documents/upload")
    public Result<Map<String, Object>> uploadDocument(
            @PathVariable String datasetId,
            @RequestParam("file") MultipartFile file) {
        
        // 检查文件大小 (15MB)
        if (file.getSize() > 15 * 1024 * 1024) {
            return Result.error(400, "文件大小不能超过15MB");
        }
        
        // 检查文件格式
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            List<String> allowedExts = List.of(
                "docx", "pdf", "properties", "mdx", "csv", "html", 
                "markdown", "xlsx", "vtt", "xls", "htm", "txt", "md"
            );
            if (!allowedExts.contains(ext)) {
                return Result.error(400, "不支持的文件格式，支持: " + String.join(", ", allowedExts));
            }
        }
        
        Map<String, Object> result = difyKnowledgeService.uploadDocument(datasetId, file);
        return Result.success(result);
    }

    /**
     * 批量上传文档到知识库
     */
    @PostMapping("/datasets/{datasetId}/documents/batch-upload")
    public Result<List<Map<String, Object>>> batchUploadDocuments(
            @PathVariable String datasetId,
            @RequestParam("files") MultipartFile[] files) {
        
        // 检查文件数量
        if (files.length > 5) {
            return Result.error(400, "每批最多上传5个文件");
        }
        
        List<Map<String, Object>> results = new ArrayList<>();
        List<String> allowedExts = List.of(
            "docx", "pdf", "properties", "mdx", "csv", "html", 
            "markdown", "xlsx", "vtt", "xls", "htm", "txt", "md"
        );
        
        for (MultipartFile file : files) {
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", file.getOriginalFilename());
            
            // 检查文件大小
            if (file.getSize() > 15 * 1024 * 1024) {
                result.put("success", false);
                result.put("message", "文件大小超过15MB限制");
                results.add(result);
                continue;
            }
            
            // 检查文件格式
            String filename = file.getOriginalFilename();
            if (filename != null) {
                String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
                if (!allowedExts.contains(ext)) {
                    result.put("success", false);
                    result.put("message", "不支持的文件格式");
                    results.add(result);
                    continue;
                }
            }
            
            try {
                Map<String, Object> uploadResult = difyKnowledgeService.uploadDocument(datasetId, file);
                result.put("success", true);
                result.put("message", "上传成功");
                result.put("data", uploadResult);
            } catch (Exception e) {
                result.put("success", false);
                result.put("message", e.getMessage());
            }
            results.add(result);
        }
        
        return Result.success(results);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/datasets/{datasetId}/documents/{documentId}")
    public Result<Void> deleteDocument(
            @PathVariable String datasetId,
            @PathVariable String documentId) {
        difyKnowledgeService.deleteDocument(datasetId, documentId);
        return Result.success();
    }

    /**
     * 知识库检索测试
     */
    @PostMapping("/datasets/{datasetId}/retrieve")
    public Result<Map<String, Object>> retrieve(
            @PathVariable String datasetId,
            @RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        Integer topK = request.containsKey("top_k") ? (Integer) request.get("top_k") : 5;
        Map<String, Object> result = difyKnowledgeService.retrieve(datasetId, query, topK);
        return Result.success(result);
    }

    /**
     * 获取文档下载链接
     */
    @GetMapping("/datasets/{datasetId}/documents/{documentId}/download")
    public Result<String> getDownloadUrl(
            @PathVariable String datasetId,
            @PathVariable String documentId) {
        try {
            // 调用 Dify API 获取签名下载链接
            String downloadUrl = difyKnowledgeService.getDocumentDownloadUrl(datasetId, documentId);
            
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                return Result.error(400, "获取下载链接失败");
            }
            
            // 返回签名 URL，让前端直接访问下载
            return Result.success(downloadUrl);
                    
        } catch (Exception e) {
            log.error("获取下载链接失败", e);
            return Result.error(500, "获取下载链接失败");
        }
    }

    /**
     * 获取文件预览签名 URL
     */
    @GetMapping("/files/preview")
    public void getFilePreview(@RequestParam String path, HttpServletResponse servletResponse) {
        try {
            // 构建 Dify 文件预览 URL
            String difyFileUrl = difyKnowledgeService.getDifyBaseUrl() + path;
            
            log.info("代理访问 Dify 文件: {}", difyFileUrl);
            
            // 使用 RestTemplate 代理请求
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            
            // 添加认证
            String apiKey = difyKnowledgeService.getDatasetApiKey();
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + apiKey);
            }
            
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            try {
                org.springframework.http.ResponseEntity<byte[]> response = restTemplate.exchange(
                    difyFileUrl,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    byte[].class
                );
                
                // 设置响应头
                if (response.getHeaders().getContentType() != null) {
                    servletResponse.setContentType(response.getHeaders().getContentType().toString());
                }
                servletResponse.setContentLength(response.getBody() != null ? response.getBody().length : 0);
                servletResponse.setHeader("Cache-Control", "public, max-age=3600");
                
                // 写入响应体
                if (response.getBody() != null) {
                    servletResponse.getOutputStream().write(response.getBody());
                }
            } catch (Exception e) {
                // 如果代理失败，返回 404
                log.warn("代理访问文件失败: {}", e.getMessage());
                servletResponse.setStatus(404);
            }
                    
        } catch (Exception e) {
            log.error("获取预览链接失败", e);
            servletResponse.setStatus(500);
        }
    }

    // ==================== 分段管理接口 ====================

    /**
     * 更新文档分段
     */
    @PutMapping("/datasets/{datasetId}/documents/{documentId}/segments/{segmentId}")
    public Result<Map<String, Object>> updateSegment(
            @PathVariable String datasetId,
            @PathVariable String documentId,
            @PathVariable String segmentId,
            @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        String answer = (String) body.get("answer");
        List<String> keywords = body.containsKey("keywords") ? (List<String>) body.get("keywords") : null;
        Boolean enabled = body.containsKey("enabled") ? (Boolean) body.get("enabled") : null;
        Map<String, Object> result = difyKnowledgeService.updateSegment(datasetId, documentId, segmentId, content, answer, keywords, enabled);
        return Result.success(result);
    }

    /**
     * 删除文档分段
     */
    @DeleteMapping("/datasets/{datasetId}/documents/{documentId}/segments/{segmentId}")
    public Result<Void> deleteSegment(
            @PathVariable String datasetId,
            @PathVariable String documentId,
            @PathVariable String segmentId) {
        difyKnowledgeService.deleteSegment(datasetId, documentId, segmentId);
        return Result.success();
    }

    /**
     * 创建文档分段
     */
    @PostMapping("/datasets/{datasetId}/documents/{documentId}/segments")
    public Result<List<Map<String, Object>>> createSegment(
            @PathVariable String datasetId,
            @PathVariable String documentId,
            @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        String answer = (String) body.get("answer");
        List<Map<String, Object>> result = difyKnowledgeService.createSegment(datasetId, documentId, content, answer);
        return Result.success(result);
    }

    /**
     * 获取子分段列表
     */
    @GetMapping("/datasets/{datasetId}/documents/{documentId}/segments/{segmentId}/child-chunks")
    public Result<Map<String, Object>> getChildChunks(
            @PathVariable String datasetId,
            @PathVariable String documentId,
            @PathVariable String segmentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        Map<String, Object> result = difyKnowledgeService.getChildChunks(datasetId, documentId, segmentId, page, limit);
        return Result.success(result);
    }

    /**
     * 创建子分段
     */
    @PostMapping("/datasets/{datasetId}/documents/{documentId}/segments/{segmentId}/child-chunks")
    public Result<Map<String, Object>> createChildChunk(
            @PathVariable String datasetId,
            @PathVariable String documentId,
            @PathVariable String segmentId,
            @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        Map<String, Object> result = difyKnowledgeService.createChildChunk(datasetId, documentId, segmentId, content);
        return Result.success(result);
    }

    /**
     * 更新子分段
     */
    @PutMapping("/datasets/{datasetId}/documents/{documentId}/segments/{segmentId}/child-chunks/{childChunkId}")
    public Result<Map<String, Object>> updateChildChunk(
            @PathVariable String datasetId,
            @PathVariable String documentId,
            @PathVariable String segmentId,
            @PathVariable String childChunkId,
            @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        Map<String, Object> result = difyKnowledgeService.updateChildChunk(datasetId, documentId, segmentId, childChunkId, content);
        return Result.success(result);
    }

    /**
     * 删除子分段
     */
    @DeleteMapping("/datasets/{datasetId}/documents/{documentId}/segments/{segmentId}/child-chunks/{childChunkId}")
    public Result<Void> deleteChildChunk(
            @PathVariable String datasetId,
            @PathVariable String documentId,
            @PathVariable String segmentId,
            @PathVariable String childChunkId) {
        difyKnowledgeService.deleteChildChunk(datasetId, documentId, segmentId, childChunkId);
        return Result.success();
    }
}
