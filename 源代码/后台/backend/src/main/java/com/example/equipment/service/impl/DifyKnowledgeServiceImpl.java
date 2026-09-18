package com.example.equipment.service.impl;

import com.example.equipment.config.DifyDatasetProperties;
import com.example.equipment.exception.BusinessException;
import com.example.equipment.service.DifyKnowledgeService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import io.github.guoshiqiufeng.dify.core.pojo.DifyPageResult;
import io.github.guoshiqiufeng.dify.dataset.DifyDataset;
import io.github.guoshiqiufeng.dify.dataset.dto.request.*;
import io.github.guoshiqiufeng.dify.dataset.dto.response.*;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DifyKnowledgeServiceImpl implements DifyKnowledgeService {

    @Resource
    private DifyDataset difyDataset;
    
    @Value("${dify.url:}")
    private String difyBaseUrl;
    
    @Value("${dify.dataset.api-key:}")
    private String datasetApiKey;

    @Resource
    private DifyDatasetProperties difyDatasetProperties;

    @Override
    public String getDifyBaseUrl() {
        return difyBaseUrl;
    }
    
    @Override
    public String getDatasetApiKey() {
        return datasetApiKey;
    }

    @Override
    public List<Map<String, Object>> getDatasets() {
        try {
            DatasetPageRequest request = new DatasetPageRequest();
            request.setPage(1);
            request.setLimit(100);
            
            DifyPageResult<DatasetResponse> result = difyDataset.page(request);
            List<Map<String, Object>> datasets = new ArrayList<>();
            
            if (result != null && result.getData() != null) {
                for (DatasetResponse dataset : result.getData()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", dataset.getId());
                    map.put("name", dataset.getName());
                    map.put("description", dataset.getDescription());
                    map.put("document_count", dataset.getDocumentCount());
                    map.put("word_count", dataset.getWordCount());
                    map.put("created_at", dataset.getCreatedAt());
                    datasets.add(map);
                }
            }
            
            return datasets;
        } catch (Exception e) {
            log.error("获取知识库列表失败", e);
            throw new BusinessException("获取知识库列表失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getDatasets(String scope) {
        List<Map<String, Object>> allDatasets = getDatasets();
        Map<String, String> visibleIds = difyDatasetProperties.getVisibleIds();
        // 无白名单配置时不做过滤
        if (visibleIds == null || visibleIds.isEmpty()) {
            log.debug("无 visible-ids 配置，返回全部 {} 个知识库", allDatasets.size());
            return allDatasets;
        }
        // 根据 scope 获取对应的白名单 ID 列表
        String allowedIdsStr = visibleIds.get(scope);
        if (allowedIdsStr == null || allowedIdsStr.isBlank()) {
            log.debug("scope={} 未配置白名单，返回全部 {} 个知识库", scope, allDatasets.size());
            return allDatasets;
        }
        Set<String> idSet = new HashSet<>(Arrays.asList(allowedIdsStr.split(",")));
        List<Map<String, Object>> filtered = allDatasets.stream()
                .filter(d -> {
                    Object id = d.get("id");
                    return id != null && idSet.contains(id.toString());
                })
                .collect(Collectors.toList());
        log.info("scope={} 白名单过滤：{} 个 → {} 个，ID: {}", scope, allDatasets.size(), filtered.size(), allowedIdsStr);
        return filtered;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDatasetDetail(String datasetId) {
        try {
            // 直接调用 Dify API 获取知识库详情（包含 doc_form 等字段）
            String difyUrl = difyBaseUrl + "/v1/datasets/" + datasetId;

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + datasetApiKey);

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(
                difyUrl, org.springframework.http.HttpMethod.GET, entity, Map.class);

            return response.getBody() != null ? response.getBody() : new HashMap<>();
        } catch (Exception e) {
            log.error("获取知识库详情失败", e);
            throw new BusinessException("获取知识库详情失败: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> updateDataset(String datasetId, String name, String description) {
        try {
            // Dify API 更新知识库使用 PATCH 方法，用 WebClient 支持
            String difyUrl = difyBaseUrl + "/v1/datasets/" + datasetId;

            Map<String, Object> requestBody = new HashMap<>();
            if (name != null && !name.isEmpty()) {
                requestBody.put("name", name);
            }
            if (description != null && !description.isEmpty()) {
                requestBody.put("description", description);
            }

            // 使用 WebClient 发送 PATCH 请求
            org.springframework.web.reactive.function.client.WebClient webClient = 
                org.springframework.web.reactive.function.client.WebClient.create(difyBaseUrl);

            Map<String, Object> result = webClient.patch()
                .uri("/v1/datasets/" + datasetId)
                .header("Authorization", "Bearer " + datasetApiKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            return result != null ? result : new HashMap<>();
        } catch (Exception e) {
            log.error("更新知识库失败", e);
            throw new BusinessException("更新知识库失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getDocuments(String datasetId, Integer page, Integer limit) {
        try {
            DatasetPageDocumentRequest request = new DatasetPageDocumentRequest();
            request.setDatasetId(datasetId);
            request.setPage(page);
            request.setLimit(limit);
            
            DifyPageResult<DocumentInfo> result = difyDataset.pageDocument(request);
            Map<String, Object> response = new HashMap<>();
            
            if (result != null) {
                List<Map<String, Object>> docs = new ArrayList<>();
                if (result.getData() != null) {
                    for (DocumentInfo doc : result.getData()) {
                        Map<String, Object> docMap = new HashMap<>();
                        docMap.put("id", doc.getId());
                        docMap.put("name", doc.getName());
                        docMap.put("word_count", doc.getWordCount());
                        docMap.put("tokens", doc.getTokens());
                        docMap.put("indexing_status", doc.getIndexingStatus());
                        docMap.put("display_status", doc.getDisplayStatus());
                        docMap.put("hit_count", doc.getHitCount());
                        docMap.put("enabled", doc.getEnabled());
                        docMap.put("created_at", doc.getCreatedAt());
                        docs.add(docMap);
                    }
                }
                response.put("data", docs);
                response.put("total", result.getTotal());
                response.put("page", result.getPage());
                response.put("limit", result.getLimit());
            }
            
            return response;
        } catch (Exception e) {
            log.error("获取文档列表失败", e);
            throw new BusinessException("获取文档列表失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getDocumentDetail(String datasetId, String documentId) {
        try {
            // SDK没有直接获取单个文档的方法，从列表中查找
            DatasetPageDocumentRequest request = new DatasetPageDocumentRequest();
            request.setDatasetId(datasetId);
            request.setPage(1);
            request.setLimit(100);
            
            DifyPageResult<DocumentInfo> result = difyDataset.pageDocument(request);
            if (result != null && result.getData() != null) {
                for (DocumentInfo doc : result.getData()) {
                    if (doc.getId().equals(documentId)) {
                        Map<String, Object> docMap = new HashMap<>();
                        docMap.put("id", doc.getId());
                        docMap.put("name", doc.getName());
                        docMap.put("word_count", doc.getWordCount());
                        docMap.put("tokens", doc.getTokens());
                        docMap.put("indexing_status", doc.getIndexingStatus());
                        return docMap;
                    }
                }
            }
            throw new BusinessException("文档不存在");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取文档详情失败", e);
            throw new BusinessException("获取文档详情失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getDocumentSegments(String datasetId, String documentId, Integer page, Integer limit) {
        try {
            SegmentPageRequest request = new SegmentPageRequest();
            request.setDatasetId(datasetId);
            request.setDocumentId(documentId);
            request.setPage(page);
            request.setLimit(limit);
            
            SegmentResponse result = difyDataset.pageSegment(request);
            Map<String, Object> response = new HashMap<>();
            
            if (result != null) {
                List<Map<String, Object>> segments = new ArrayList<>();
                if (result.getData() != null) {
                    for (SegmentData seg : result.getData()) {
                        Map<String, Object> segMap = new HashMap<>();
                        segMap.put("id", seg.getId());
                        segMap.put("content", seg.getContent());
                        segMap.put("word_count", seg.getWordCount());
                        segMap.put("hit_count", seg.getHitCount());
                        segMap.put("enabled", seg.getEnabled());
                        segments.add(segMap);
                    }
                }
                response.put("data", segments);
                response.put("total", result.getTotal());
            }
            
            return response;
        } catch (Exception e) {
            log.error("获取文档分段失败，返回空结果", e);
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("data", new ArrayList<>());
            emptyResult.put("total", 0);
            return emptyResult;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadDocument(String datasetId, MultipartFile file) {
        try {
            // 先获取知识库详情，获取 doc_form 和 indexing_technique
            Map<String, Object> datasetDetail = getDatasetDetail(datasetId);
            String docForm = (String) datasetDetail.get("doc_form");
            String indexingTechnique = (String) datasetDetail.get("indexing_technique");
            log.info("知识库 {} 上传文档 - doc_form: {}, indexing_technique: {}", datasetId, docForm, indexingTechnique);

            // 构建 data JSON 参数（Dify API 要求通过 data 表单字段传递）
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("indexing_technique", indexingTechnique != null ? indexingTechnique : "high_quality");
            dataMap.put("doc_form", docForm != null ? docForm : "text_model");
            dataMap.put("doc_language", "Chinese");

            // process_rule 根据 doc_form 设置：hierarchical_model 使用 hierarchical 模式，其他使用 automatic
            if ("hierarchical_model".equals(docForm)) {
                // 父子分段模式 - 需使用 hierarchical 规则
                Map<String, Object> rules = new HashMap<>();
                rules.put("parent_mode", "paragraph");
                // 预处理规则（Dify hierarchical 模式必填）
                List<Map<String, Object>> preProcessingRules = new ArrayList<>();
                Map<String, Object> rule1 = new HashMap<>();
                rule1.put("id", "remove_extra_spaces");
                rule1.put("enabled", true);
                preProcessingRules.add(rule1);
                Map<String, Object> rule2 = new HashMap<>();
                rule2.put("id", "remove_urls_emails");
                rule2.put("enabled", true);
                preProcessingRules.add(rule2);
                rules.put("pre_processing_rules", preProcessingRules);
                Map<String, Object> segmentation = new HashMap<>();
                segmentation.put("separator", "\n");
                segmentation.put("max_tokens", 500);
                rules.put("segmentation", segmentation);
                Map<String, Object> subchunkSegmentation = new HashMap<>();
                subchunkSegmentation.put("separator", "\n");
                subchunkSegmentation.put("max_tokens", 500);
                subchunkSegmentation.put("chunk_overlap", 50);
                rules.put("subchunk_segmentation", subchunkSegmentation);

                Map<String, Object> processRule = new HashMap<>();
                processRule.put("mode", "hierarchical");
                processRule.put("rules", rules);
                dataMap.put("process_rule", processRule);
            } else {
                dataMap.put("process_rule", Map.of("mode", "automatic"));
            }

            String dataJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(dataMap);
            log.info("上传数据参数: {}", dataJson);

            String difyUrl = difyBaseUrl + "/v1/datasets/" + datasetId + "/document/create-by-file";

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer " + datasetApiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // file 部分：设置文件 MIME 类型
            String filename = file.getOriginalFilename();
            MediaType fileContentType = MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM);
            HttpHeaders filePartHeaders = new HttpHeaders();
            filePartHeaders.setContentType(fileContentType);
            filePartHeaders.set(HttpHeaders.CONTENT_DISPOSITION,
                    "form-data; name=\"file\"; filename=\"" + filename + "\"");

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(fileResource, filePartHeaders);
            body.add("file", fileEntity);

            // data 部分：设置 JSON Content-Type
            HttpHeaders dataPartHeaders = new HttpHeaders();
            dataPartHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> dataEntity = new HttpEntity<>(dataJson, dataPartHeaders);
            body.add("data", dataEntity);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(difyUrl, entity, Map.class);
            log.info("Dify 文件上传响应: {}", response.getBody());

            Map<String, Object> result = new HashMap<>();
            if (response.getBody() != null) {
                Map<String, Object> bodyMap = response.getBody();
                Map<String, Object> document = (Map<String, Object>) bodyMap.get("document");
                if (document != null) {
                    result.put("id", document.get("id"));
                    result.put("name", document.get("name"));
                    result.put("indexing_status", document.get("indexing_status"));
                    result.put("error", document.get("error")); // 记录可能的错误信息
                    // 如果索引状态有异常，记录日志
                    if ("error".equals(document.get("indexing_status")) && document.get("error") != null) {
                        log.error("Dify 文档索引失败: {}", document.get("error"));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.error("上传文档失败", e);
            throw new BusinessException("上传文档失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteDocument(String datasetId, String documentId) {
        try {
            difyDataset.deleteDocument(datasetId, documentId);
        } catch (Exception e) {
            log.error("删除文档失败", e);
            throw new BusinessException("删除文档失败: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> retrieve(String datasetId, String query, Integer topK) {
        try {
            // 绕过SDK，直接调用Dify API（SDK反序列化有问题）
            String difyUrl = difyBaseUrl + "/v1/datasets/" + datasetId + "/retrieve";

            // 构建请求体（匹配Dify官方API格式）
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            Map<String, Object> retrievalModel = new HashMap<>();
            retrievalModel.put("top_k", topK != null ? topK : 5);
            retrievalModel.put("score_threshold", 0.5);
            retrievalModel.put("score_threshold_enabled", false);
            requestBody.put("external_retrieval_model", retrievalModel);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + datasetApiKey);

            org.springframework.http.HttpEntity<Map<String, Object>> entity = 
                new org.springframework.http.HttpEntity<>(requestBody, headers);

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(difyUrl, entity, Map.class);

            Map<String, Object> result = new HashMap<>();
            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                // 提取query信息 - 修复类型转换问题
                Object queryObj = body.get("query");
                if (queryObj != null) {
                    if (queryObj instanceof Map) {
                        result.put("query", queryObj);
                    } else {
                        // 如果query是字符串，包装成Map
                        Map<String, Object> queryMap = new HashMap<>();
                        queryMap.put("content", queryObj.toString());
                        result.put("query", queryMap);
                    }
                }
                // 提取records并转换
                List<Map<String, Object>> srcRecords = (List<Map<String, Object>>) body.get("records");
                if (srcRecords != null) {
                    List<Map<String, Object>> records = new ArrayList<>();
                    for (Map<String, Object> record : srcRecords) {
                        Map<String, Object> recordMap = new HashMap<>();
                        recordMap.put("score", record.get("score"));
                        Object segmentObj = record.get("segment");
                        if (segmentObj != null) {
                            Map<String, Object> segment;
                            if (segmentObj instanceof Map) {
                                segment = (Map<String, Object>) segmentObj;
                            } else {
                                log.warn("segment不是Map类型，跳过该记录: {}", segmentObj.getClass().getName());
                                continue;
                            }
                            recordMap.put("content", segment.get("content"));
                            recordMap.put("keywords", segment.get("keywords"));
                            Object docObj = segment.get("document");
                            if (docObj != null) {
                                if (docObj instanceof Map) {
                                    Map<String, Object> docMap = new HashMap<>();
                                    Map<String, Object> document = (Map<String, Object>) docObj;
                                    docMap.put("name", document.get("name"));
                                    docMap.put("id", document.get("id"));
                                    recordMap.put("document", docMap);
                                } else {
                                    log.warn("document不是Map类型: {}", docObj.getClass().getName());
                                }
                            }
                        }
                        records.add(recordMap);
                    }
                    result.put("records", records);
                }
            }

            return result;
        } catch (Exception e) {
            log.error("知识库检索失败", e);
            throw new BusinessException("知识库检索失败: " + e.getMessage());
        }
    }

    @Override
    public String getDocumentDownloadUrl(String datasetId, String documentId) {
        try {
            // 调用 Dify 公开 API 获取下载链接
            String difyUrl = difyBaseUrl + "/v1/datasets/" + datasetId + "/documents/" + documentId + "/download";

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

            // 设置请求头，添加 Dataset API Key 认证
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + datasetApiKey);

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            // 发送请求
            org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.exchange(
                difyUrl,
                org.springframework.http.HttpMethod.GET,
                entity,
                java.util.Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("url")) {
                String url = (String) response.getBody().get("url");
                // 如果是相对路径，拼接完整的 URL
                if (url != null && url.startsWith("/")) {
                    url = difyBaseUrl + url;
                }
                log.info("获取文档下载链接成功");
                return url;
            }
            
            return null;
        } catch (Exception e) {
            log.warn("获取文档下载链接失败: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public String getFilePreviewUrl(String filePath) {
        try {
            // filePath 格式: /files/{file_id}/image-preview 或 /files/{file_id}/file-preview
            // 调用 Dify API 获取签名 URL
            String difyUrl = difyBaseUrl + "/v1" + filePath;

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

            // 设置请求头
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + datasetApiKey);

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            // 发送请求，获取重定向或签名 URL
            try {
                org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.exchange(
                    difyUrl,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    java.util.Map.class
                );
                
                if (response.getBody() != null && response.getBody().containsKey("url")) {
                    String url = (String) response.getBody().get("url");
                    if (url != null && url.startsWith("/")) {
                        url = difyBaseUrl + url;
                    }
                    return url;
                }
            } catch (Exception e) {
                // 如果 API 返回重定向，RestTemplate 会抛异常
                // 尝试直接返回拼接的 URL
                log.debug("获取签名 URL 失败，尝试直接访问: {}", e.getMessage());
            }
            
            // 返回原始路径（可能需要签名）
            return difyBaseUrl + filePath;
        } catch (Exception e) {
            log.warn("获取文件预览链接失败: {}", e.getMessage());
            return difyBaseUrl + filePath;
        }
    }

    @Override
    public boolean checkDocumentExists(String datasetId, String documentId) {
        try {
            String difyUrl = difyBaseUrl + "/v1/datasets/" + datasetId + "/documents/" + documentId;

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + datasetApiKey);

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

            restTemplate.exchange(difyUrl, org.springframework.http.HttpMethod.GET, entity, Map.class);
            return true;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            log.info("文档不存在: datasetId={}, documentId={}", datasetId, documentId);
            return false;
        } catch (Exception e) {
            log.warn("检查文档存在性失败: datasetId={}, documentId={}, error={}", datasetId, documentId, e.getMessage());
            return false;
        }
    }

    /**
     * 上传文本到知识库（用于案例审核通过后上传）
     * 先生成临时 .md 文件，再通过文件上传 API 上传（文件上传路径已验证可靠）
     */
    public String uploadTextToDataset(String datasetId, String name, String content) {
        java.io.File tempFile = null;
        try {
            // 先获取知识库详情，获取 doc_form 和 indexing_technique
            Map<String, Object> datasetDetail = getDatasetDetail(datasetId);
            String docForm = (String) datasetDetail.get("doc_form");
            String indexingTechnique = (String) datasetDetail.get("indexing_technique");
            log.info("知识库 {} 上传文本 - doc_form: {}, indexing_technique: {}", datasetId, docForm, indexingTechnique);

            // 构建 Markdown 格式内容（文件名用作文档标题）
            String mdContent = content;

            // 生成临时 .md 文件
            String safeName = name.replaceAll("[\\\\/:*?\"<>|]", "_");
            tempFile = java.io.File.createTempFile("knowledge_", "_" + safeName + ".md");
            java.nio.file.Files.writeString(tempFile.toPath(), mdContent, java.nio.charset.StandardCharsets.UTF_8);
            log.info("临时文件创建成功: {}", tempFile.getAbsolutePath());

            // 构建 data JSON 参数（与 uploadDocument 一致）
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("indexing_technique", indexingTechnique != null ? indexingTechnique : "high_quality");
            dataMap.put("doc_form", docForm != null ? docForm : "text_model");
            dataMap.put("doc_language", "Chinese");

            // process_rule 根据 doc_form 设置
            if ("hierarchical_model".equals(docForm)) {
                Map<String, Object> rules = new HashMap<>();
                rules.put("parent_mode", "paragraph");
                // 预处理规则（Dify hierarchical 模式必填）
                List<Map<String, Object>> preProcessingRules = new ArrayList<>();
                Map<String, Object> rule1 = new HashMap<>();
                rule1.put("id", "remove_extra_spaces");
                rule1.put("enabled", true);
                preProcessingRules.add(rule1);
                Map<String, Object> rule2 = new HashMap<>();
                rule2.put("id", "remove_urls_emails");
                rule2.put("enabled", true);
                preProcessingRules.add(rule2);
                rules.put("pre_processing_rules", preProcessingRules);
                Map<String, Object> segmentation = new HashMap<>();
                segmentation.put("separator", "\n");
                segmentation.put("max_tokens", 500);
                rules.put("segmentation", segmentation);
                Map<String, Object> subchunkSegmentation = new HashMap<>();
                subchunkSegmentation.put("separator", "\n");
                subchunkSegmentation.put("max_tokens", 500);
                subchunkSegmentation.put("chunk_overlap", 50);
                rules.put("subchunk_segmentation", subchunkSegmentation);

                Map<String, Object> processRule = new HashMap<>();
                processRule.put("mode", "hierarchical");
                processRule.put("rules", rules);
                dataMap.put("process_rule", processRule);
            } else {
                dataMap.put("process_rule", Map.of("mode", "automatic"));
            }

            String dataJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(dataMap);
            log.info("上传文件参数: {}", dataJson);

            String difyUrl = difyBaseUrl + "/v1/datasets/" + datasetId + "/document/create-by-file";

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer " + datasetApiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // file 部分：设置文件 MIME 类型
            String mdFileName = safeName + ".md";
            MediaType fileContentType = MediaTypeFactory.getMediaType(mdFileName).orElse(MediaType.TEXT_PLAIN);
            HttpHeaders filePartHeaders = new HttpHeaders();
            filePartHeaders.setContentType(fileContentType);
            filePartHeaders.set(HttpHeaders.CONTENT_DISPOSITION,
                    "form-data; name=\"file\"; filename=\"" + mdFileName + "\"");

            ByteArrayResource fileResource = new ByteArrayResource(java.nio.file.Files.readAllBytes(tempFile.toPath())) {
                @Override
                public String getFilename() {
                    return mdFileName;
                }
            };
            HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(fileResource, filePartHeaders);
            body.add("file", fileEntity);

            // data 部分：设置 JSON Content-Type
            HttpHeaders dataPartHeaders = new HttpHeaders();
            dataPartHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> dataEntity = new HttpEntity<>(dataJson, dataPartHeaders);
            body.add("data", dataEntity);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForEntity(difyUrl, entity, Map.class).getBody();

            if (response != null) {
                log.info("Dify 响应: {}", response);
                @SuppressWarnings("unchecked")
                Map<String, Object> document = (Map<String, Object>) response.get("document");
                if (document != null) {
                    String documentId = (String) document.get("id");
                    String indexingStatus = (String) document.get("indexing_status");
                    String errorMsg = (String) document.get("error");
                    log.info("文档上传成功: documentId={}, title={}, indexing_status={}, error={}",
                            documentId, name, indexingStatus, errorMsg);
                    if ("error".equals(indexingStatus) && errorMsg != null) {
                        log.error("Dify 文档索引失败: {}", errorMsg);
                    }
                    return documentId;
                }
            }
            log.warn("上传成功但响应中没有 document 信息, response: {}", response);
            return null;
        } catch (Exception e) {
            log.error("上传文本到知识库失败", e);
            throw new BusinessException("上传失败: " + e.getMessage());
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
                log.debug("临时文件已删除: {}", tempFile.getAbsolutePath());
            }
        }
    }

    @Override
    public Map<String, Object> updateSegment(String datasetId, String documentId, String segmentId,
                                              String content, String answer, List<String> keywords, Boolean enabled) {
        try {
            SegmentUpdateRequest request = new SegmentUpdateRequest();
            request.setDatasetId(datasetId);
            request.setDocumentId(documentId);
            request.setSegmentId(segmentId);

            SegmentUpdateParam param = new SegmentUpdateParam();
            if (content != null) param.setContent(content);
            if (answer != null) param.setAnswer(answer);
            if (keywords != null) param.setKeywords(keywords);
            if (enabled != null) param.setEnabled(enabled);
            request.setSegment(param);

            SegmentUpdateResponse result = difyDataset.updateSegment(request);
            Map<String, Object> response = new HashMap<>();
            if (result != null && result.getData() != null) {
                SegmentData seg = result.getData();
                response.put("id", seg.getId());
                response.put("content", seg.getContent());
                response.put("word_count", seg.getWordCount());
                response.put("hit_count", seg.getHitCount());
                response.put("enabled", seg.getEnabled());
            }
            return response;
        } catch (Exception e) {
            log.error("更新分段失败", e);
            throw new BusinessException("更新分段失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteSegment(String datasetId, String documentId, String segmentId) {
        try {
            difyDataset.deleteSegment(datasetId, documentId, segmentId);
        } catch (Exception e) {
            log.error("删除分段失败", e);
            throw new BusinessException("删除分段失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> createSegment(String datasetId, String documentId, String content, String answer) {
        try {
            SegmentCreateRequest request = new SegmentCreateRequest();
            request.setDatasetId(datasetId);
            request.setDocumentId(documentId);

            SegmentParam param = new SegmentParam();
            param.setContent(content);
            if (answer != null) param.setAnswer(answer);
            request.setSegments(List.of(param));

            SegmentResponse createResult = difyDataset.createSegment(request);
            List<Map<String, Object>> segments = new ArrayList<>();
            if (createResult != null && createResult.getData() != null) {
                for (SegmentData seg : createResult.getData()) {
                    Map<String, Object> segMap = new HashMap<>();
                    segMap.put("id", seg.getId());
                    segMap.put("content", seg.getContent());
                    segMap.put("word_count", seg.getWordCount());
                    segMap.put("hit_count", seg.getHitCount());
                    segMap.put("enabled", seg.getEnabled());
                    segments.add(segMap);
                }
            }
            return segments;
        } catch (Exception e) {
            log.error("创建分段失败", e);
            throw new BusinessException("创建分段失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getChildChunks(String datasetId, String documentId, String segmentId, Integer page, Integer limit) {
        try {
            SegmentChildChunkPageRequest request = new SegmentChildChunkPageRequest();
            request.setDatasetId(datasetId);
            request.setDocumentId(documentId);
            request.setSegmentId(segmentId);
            request.setPage(page != null ? page : 1);
            request.setLimit(limit != null ? limit : 20);

            DifyPageResult<SegmentChildChunkResponse> result = difyDataset.pageSegmentChildChunk(request);
            Map<String, Object> response = new HashMap<>();

            if (result != null) {
                List<Map<String, Object>> chunks = new ArrayList<>();
                if (result.getData() != null) {
                    for (SegmentChildChunkResponse chunk : result.getData()) {
                        Map<String, Object> chunkMap = new HashMap<>();
                        chunkMap.put("id", chunk.getId());
                        chunkMap.put("segment_id", chunk.getSegmentId());
                        chunkMap.put("content", chunk.getContent());
                        chunkMap.put("word_count", chunk.getWordCount());
                        chunkMap.put("tokens", chunk.getTokens());
                        chunkMap.put("status", chunk.getStatus());
                        chunks.add(chunkMap);
                    }
                }
                response.put("data", chunks);
                response.put("total", result.getTotal());
            }
            return response;
        } catch (Exception e) {
            log.error("获取子分段失败", e);
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("data", new ArrayList<>());
            emptyResult.put("total", 0);
            return emptyResult;
        }
    }

    @Override
    public Map<String, Object> createChildChunk(String datasetId, String documentId, String segmentId, String content) {
        try {
            SegmentChildChunkCreateRequest request = new SegmentChildChunkCreateRequest();
            request.setDatasetId(datasetId);
            request.setDocumentId(documentId);
            request.setSegmentId(segmentId);
            request.setContent(content);

            SegmentChildChunkCreateResponse result = difyDataset.createSegmentChildChunk(request);
            Map<String, Object> response = new HashMap<>();
            if (result != null && result.getData() != null) {
                SegmentChildChunkResponse chunk = result.getData();
                response.put("id", chunk.getId());
                response.put("segment_id", chunk.getSegmentId());
                response.put("content", chunk.getContent());
                response.put("word_count", chunk.getWordCount());
                response.put("status", chunk.getStatus());
            }
            return response;
        } catch (Exception e) {
            log.error("创建子分段失败", e);
            throw new BusinessException("创建子分段失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> updateChildChunk(String datasetId, String documentId, String segmentId,
                                                 String childChunkId, String content) {
        try {
            SegmentChildChunkUpdateRequest request = new SegmentChildChunkUpdateRequest();
            request.setDatasetId(datasetId);
            request.setDocumentId(documentId);
            request.setSegmentId(segmentId);
            request.setChildChunkId(childChunkId);
            request.setContent(content);

            SegmentChildChunkUpdateResponse result = difyDataset.updateSegmentChildChunk(request);
            Map<String, Object> response = new HashMap<>();
            if (result != null && result.getData() != null) {
                SegmentChildChunkResponse chunk = result.getData();
                response.put("id", chunk.getId());
                response.put("segment_id", chunk.getSegmentId());
                response.put("content", chunk.getContent());
                response.put("word_count", chunk.getWordCount());
                response.put("status", chunk.getStatus());
            }
            return response;
        } catch (Exception e) {
            log.error("更新子分段失败", e);
            throw new BusinessException("更新子分段失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteChildChunk(String datasetId, String documentId, String segmentId, String childChunkId) {
        try {
            SegmentChildChunkDeleteRequest request = new SegmentChildChunkDeleteRequest();
            request.setDatasetId(datasetId);
            request.setDocumentId(documentId);
            request.setSegmentId(segmentId);
            request.setChildChunkId(childChunkId);

            difyDataset.deleteSegmentChildChunk(request);
        } catch (Exception e) {
            log.error("删除子分段失败", e);
            throw new BusinessException("删除子分段失败: " + e.getMessage());
        }
    }
}
