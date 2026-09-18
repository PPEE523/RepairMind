package com.example.equipment.service.impl;

import com.example.equipment.config.DifyDatasetProperties;
import com.example.equipment.config.DifyStatusProperties;
import com.example.equipment.dto.ChatMessageVO;
import com.example.equipment.dto.SearchRequest;
import com.example.equipment.dto.SearchResponse;
import com.example.equipment.entity.RepairKnowledge;
import com.example.equipment.enums.DifyAgentType;
import com.example.equipment.exception.BusinessException;
import com.example.equipment.service.DifyKnowledgeService;
import com.example.equipment.service.DifyService;
import com.example.equipment.util.LocalMultipartFile;
import io.github.guoshiqiufeng.dify.chat.DifyChat;
import io.github.guoshiqiufeng.dify.chat.dto.request.ChatMessageSendRequest;
import io.github.guoshiqiufeng.dify.chat.dto.request.FileUploadRequest;
import io.github.guoshiqiufeng.dify.chat.dto.response.ChatMessageSendCompletionResponse;
import io.github.guoshiqiufeng.dify.chat.dto.response.ChatMessageSendResponse;
import io.github.guoshiqiufeng.dify.chat.dto.response.FileUploadResponse;
import io.github.guoshiqiufeng.dify.core.pojo.DifyFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "dify.mock.enabled", havingValue = "false", matchIfMissing = true)
public class DifyServiceImpl implements DifyService {

    @Resource
    private DifyChat difyChat;

    @Value("${dify.url}")
    private String difyUrl;

    @Value("${dify.app.api-key:${dify.dataset.api-key}}")
    private String appApiKey;

    @Value("${dify.app.sop-api-key:${dify.app.api-key}}")
    private String sopApiKey;

    @Value("${dify.app.test-api-key:${dify.app.api-key}}")
    private String testApiKey;

    @Value("${dify.app.summary-api-key:${dify.app.api-key}}")
    private String summaryApiKey;

    @Value("${dify.dataset.api-key}")
    private String datasetApiKey;

    @Value("${dify.dataset.id}")
    private String datasetId;

    @Value("${dify.dataset.case-id:${dify.dataset.id}}")
    private String caseDatasetId;

    @Resource
    private DifyDatasetProperties difyDatasetProperties;

    @Resource
    private DifyStatusProperties difyStatusProperties;

    @Resource
    private DifyKnowledgeService difyKnowledgeService;

    @Value("${dify.sop-dataset-id:}")
    private String sopDatasetId;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public DifyServiceImpl() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    @Value("${server.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public SearchResponse search(SearchRequest request, Long userId) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", request.getQuery());
            requestBody.put("response_mode", "blocking");
            requestBody.put("user", String.valueOf(userId));

            if (request.getEquipModel() != null && !request.getEquipModel().isEmpty()) {
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("equip_model", request.getEquipModel());
                requestBody.put("inputs", inputs);
            }

            // 调用Dify Chat API
            String responseStr = webClient.post()
                    .uri(difyUrl + "/v1/chat-messages")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + appApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 解析响应
            JsonNode responseJson = objectMapper.readTree(responseStr);
            String answer = responseJson.has("answer") ? responseJson.get("answer").asText() : "";
            String conversationId = responseJson.has("conversation_id") ? responseJson.get("conversation_id").asText() : "";

            // 解析检索资源
            List<SearchResponse.RetrieverResource> resources = new ArrayList<>();
            if (responseJson.has("metadata") && responseJson.get("metadata").has("retriever_resources")) {
                JsonNode resourcesNode = responseJson.get("metadata").get("retriever_resources");
                for (JsonNode resource : resourcesNode) {
                    resources.add(SearchResponse.RetrieverResource.builder()
                            .datasetId(resource.has("dataset_id") ? resource.get("dataset_id").asText() : "")
                            .datasetName(resource.has("dataset_name") ? resource.get("dataset_name").asText() : "")
                            .documentId(resource.has("document_id") ? resource.get("document_id").asText() : "")
                            .documentName(resource.has("document_name") ? resource.get("document_name").asText() : "")
                            .content(resource.has("content") ? resource.get("content").asText() : "")
                            .score(resource.has("score") ? resource.get("score").asDouble() : 0.0)
                            .segmentId(resource.has("segment_id") ? resource.get("segment_id").asText() : "")
                            .build());
                }
            }

            return SearchResponse.builder()
                    .answer(answer)
                    .resources(resources)
                    .conversationId(conversationId)
                    .build();

        } catch (Exception e) {
            log.error("调用Dify API失败", e);
            throw new BusinessException("检索失败，请稍后重试: " + e.getMessage());
        }
    }

    @Override
    public String uploadToDataset(RepairKnowledge knowledge) {
        try {
            // 构建文档内容
            String documentContent = buildDocumentContent(knowledge);

            // 使用 Dify SDK 的 create-by-text API 上传（自动处理 doc_form、分块规则等参数）
            String documentId = difyKnowledgeService.uploadTextToDataset(caseDatasetId, knowledge.getTitle(), documentContent);

            // 审核通过后：将案例图片作为独立文件上传到同一知识库，尽力而为实现多模态索引
            // 单张失败不影响文本文档；Dify若不支持图片格式会在此捕获异常并跳过
            try {
                uploadCaseImagesToDataset(knowledge, caseDatasetId);
            } catch (Exception imgEx) {
                log.warn("案例图片多模态上传整体失败（不影响文本文档）: knowledgeId={}, error={}",
                        knowledge.getId(), imgEx.getMessage());
            }

            return documentId;

        } catch (Exception e) {
            log.error("上传文档到Dify知识库失败", e);
            throw new BusinessException("上传文档失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> checkSimilarity(String content, String equipModel) {
        Map<String, Object> result = new HashMap<>();
        result.put("similarity", 0.0);
        result.put("topDocuments", new ArrayList<>());

        try {
            // 从白名单获取案例审核知识库ID，不直接使用明文ID
            String targetDatasetId = getCaseAuditDatasetId();

            // 构建检索请求
            // 不传 retrieval_model，让Dify使用知识库默认检索设置（混合检索等）
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", content);

            log.debug("相似度检测请求体: {}", objectMapper.writeValueAsString(requestBody));

            log.info("相似度检测 目标知识库: {}  query: {}", targetDatasetId,
                    content.substring(0, Math.min(80, content.length())));

            // 调用Dify检索API
            String responseStr = webClient.post()
                    .uri(difyUrl + "/v1/datasets/" + targetDatasetId + "/retrieve")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + datasetApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(body -> {
                                log.error("Dify检索返回错误: status={}, body={}", clientResponse.statusCode(), body);
                                return reactor.core.publisher.Mono.error(
                                        new RuntimeException("Dify检索错误 [" + clientResponse.statusCode() + "]: " + body));
                            }))
                    .bodyToMono(String.class)
                    .block();

            // 解析响应获取所有匹配记录
            JsonNode responseJson = objectMapper.readTree(responseStr);
            JsonNode recordsNode = responseJson.has("records") ? responseJson.get("records") : null;

            log.info("Dify检索响应: records={}, queryResult={}",
                    recordsNode != null ? recordsNode.size() : 0,
                    responseJson.has("query") ? responseJson.get("query").get("content") : "N/A");

            if (recordsNode != null && recordsNode.isArray() && recordsNode.size() > 0) {
                double topScore = 0.0;
                List<Map<String, Object>> documents = new ArrayList<>();

                for (JsonNode record : recordsNode) {
                    double score = record.has("score") ? record.get("score").asDouble() : 0.0;
                    if (score > topScore) {
                        topScore = score;
                    }

                    Map<String, Object> doc = new HashMap<>();
                    doc.put("score", Math.round(score * 10000.0) / 10000.0); // 保留4位小数

                    // 提取文档信息（从 segment.document 中获取）
                    JsonNode segment = record.has("segment") ? record.get("segment") : null;
                    if (segment != null) {
                        JsonNode document = segment.has("document") ? segment.get("document") : null;
                        if (document != null) {
                            doc.put("documentId", document.has("id") ? document.get("id").asText() : "");
                            doc.put("documentName", document.has("name") ? document.get("name").asText() : "");
                        }
                        doc.put("segmentContent", segment.has("content") ? segment.get("content").asText() : "");
                    }
                    documents.add(doc);
                }

                // 按 score 降序排列
                documents.sort((a, b) -> Double.compare((double) b.get("score"), (double) a.get("score")));

                result.put("similarity", Math.round(topScore * 10000.0) / 10000.0);
                result.put("topDocuments", documents);

                log.info("相似度检测完成: score={}, matchedDocs={}", topScore, documents.size());
            }

        } catch (Exception e) {
            log.warn("相似度检测失败，返回默认值: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 从配置白名单获取案例审核专用知识库ID
     * 不依赖明文硬编码的 dataset.id / case-id，统一走 visible-ids 白名单
     */
    private String getCaseAuditDatasetId() {
        Map<String, String> visibleIds = difyDatasetProperties.getVisibleIds();
        if (visibleIds != null) {
            String casesStr = visibleIds.get("case-audit");
            if (casesStr != null && !casesStr.isBlank()) {
                String[] ids = casesStr.split(",");
                String id = ids[0].trim();
                log.debug("从白名单获取 case-audit 知识库ID: {}", id);
                return id;
            }
        }
        // 兜底：白名单未配置时使用 caseDatasetId
        log.warn("未配置 case-audit 白名单，回退到 caseDatasetId: {}", caseDatasetId);
        return caseDatasetId;
    }

    @Override
    public void streamSearch(SearchRequest request, Long userId, StreamCallback callback) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", request.getQuery());
            requestBody.put("response_mode", "streaming");
            requestBody.put("user", String.valueOf(userId));

            if (request.getEquipModel() != null && !request.getEquipModel().isEmpty()) {
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("equip_model", request.getEquipModel());
                requestBody.put("inputs", inputs);
            }

            // 调用Dify Chat API (流式)
            webClient.post()
                    .uri(difyUrl + "/v1/chat-messages")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + appApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .subscribe(
                            line -> {
                                // 解析SSE数据
                                if (line.startsWith("data:")) {
                                    String data = line.substring(5).trim();
                                    try {
                                        JsonNode node = objectMapper.readTree(data);
                                        String event = node.has("event") ? node.get("event").asText() : "";
                                        
                                        if ("message".equals(event)) {
                                            String answer = node.has("answer") ? node.get("answer").asText() : "";
                                            callback.onMessage(answer);
                                        } else if ("message_end".equals(event)) {
                                            String fullAnswer = node.has("metadata") ? 
                                                    node.get("metadata").toString() : "";
                                            callback.onComplete(fullAnswer);
                                        }
                                    } catch (Exception e) {
                                        log.warn("解析SSE数据失败: {}", data);
                                    }
                                }
                            },
                            error -> {
                                log.error("SSE流式请求失败", error);
                                callback.onError("请求失败: " + error.getMessage());
                            },
                            () -> {
                                callback.onComplete("");
                            }
                    );

        } catch (Exception e) {
            log.error("调用Dify流式API失败", e);
            callback.onError("检索失败: " + e.getMessage());
        }
    }

    @Override
    public String generateSopFromDocument(String documentName, Long userId) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", "文档名称：" + documentName);
            requestBody.put("response_mode", "blocking");
            requestBody.put("user", String.valueOf(userId));
            requestBody.put("inputs", new HashMap<>());

            log.info("调用Dify SOP Chatflow(blocking): url={}", difyUrl + "/v1/chat-messages");

            StringBuilder fullAnswer = new StringBuilder();

            String responseStr = webClient.post()
                    .uri(difyUrl + "/v1/chat-messages")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + sopApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> 
                        clientResponse.bodyToMono(String.class).flatMap(body -> {
                            log.error("Dify返回错误: status={}, body={}", clientResponse.statusCode(), body);
                            return reactor.core.publisher.Mono.error(
                                new RuntimeException("Dify API错误 [" + clientResponse.statusCode() + "]: " + body));
                        }))
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseJson = objectMapper.readTree(responseStr);
            log.info("Dify blocking响应: answer长度={}, 响应前200字={}", 
                    responseJson.has("answer") ? responseJson.get("answer").asText().length() : 0,
                    responseStr.length() > 200 ? responseStr.substring(0, 200) : responseStr);
            String answer = responseJson.has("answer") ? responseJson.get("answer").asText() : "";
            log.info("SOP生成完成: documentName={}, answerLength={}", documentName, answer.length());
            return answer;

        } catch (Exception e) {
            log.error("调用Dify SOP Agent失败", e);
            throw new BusinessException("SOP流程生成失败: " + e.getMessage());
        }
    }

    @Override
    public void testSopStep(String stepJson, Integer stepIndex, String userInput,
                            String imageUrl, Long userId, StreamCallback callback) {
        // 防止 onComplete/onError 被多次调用
        final java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean(false);
        try {
            String query = buildSopTestQuery(stepJson, stepIndex, userInput, imageUrl);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("response_mode", "streaming");
            requestBody.put("user", String.valueOf(userId));

            // Dify 要求 inputs 必须存在
            requestBody.put("inputs", new HashMap<>());

            // 上传图片到Dify，通过files参数传递（替代URL方式，解决Dify沙箱无法访问内网IP问题）
            if (imageUrl != null && !imageUrl.isEmpty()) {
                String uploadFileId = uploadFileToDify(imageUrl, testApiKey, String.valueOf(userId));
                if (uploadFileId != null) {
                    List<Map<String, Object>> files = new ArrayList<>();
                    Map<String, Object> fileObj = new HashMap<>();
                    fileObj.put("type", "image");
                    fileObj.put("transfer_method", "local_file");
                    fileObj.put("upload_file_id", uploadFileId);
                    files.add(fileObj);
                    requestBody.put("files", files);
                    log.info("SOP测试图片已上传至Dify: fileId={}", uploadFileId);
                } else {
                    log.warn("SOP测试图片上传Dify失败，将无图评估: imageUrl={}", imageUrl);
                }
            }

            StringBuilder fullAnswer = new StringBuilder();

            webClient.post()
                    .uri(difyUrl + "/v1/chat-messages")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + testApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .subscribe(
                            line -> {
                                String data = line;
                                if (line.startsWith("data:")) data = line.substring(5).trim();
                                if (data.isEmpty() || !data.startsWith("{")) return;
                                try {
                                    JsonNode node = objectMapper.readTree(data);
                                    String event = node.has("event") ? node.get("event").asText() : "";
                                    if ("message".equals(event) || "agent_message".equals(event)) {
                                        String answer = node.has("answer") ? node.get("answer").asText() : "";
                                        fullAnswer.append(answer);
                                        callback.onMessage(answer);
                                    } else if ("message_end".equals(event)) {
                                        log.info("SOP测试收到 message_end, 累积answer长度={}", fullAnswer.length());
                                        if (completed.compareAndSet(false, true)) {
                                            callback.onComplete(fullAnswer.toString());
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("解析SSE数据失败: {}", data);
                                }
                            },
                            error -> {
                                log.error("SOP测试SSE请求失败: {}", error.getMessage());
                                if (completed.compareAndSet(false, true)) {
                                    callback.onError("请求失败: " + error.getMessage());
                                }
                            },
                            () -> {
                                if (completed.compareAndSet(false, true)) {
                                    callback.onComplete(fullAnswer.toString());
                                }
                            }
                    );

        } catch (Exception e) {
            log.error("调用SOP测试Agent失败: {}", e.getMessage());
            if (completed.compareAndSet(false, true)) {
                callback.onError("SOP测试失败: " + e.getMessage());
            }
        }
    }

    @Override
    public String testSopStepSync(String stepJson, Integer stepIndex, String userInput,
                                  String imageUrl, Long userId) {
        try {
            String query = buildSopTestQuery(stepJson, stepIndex, userInput, imageUrl);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("response_mode", "blocking");
            requestBody.put("user", String.valueOf(userId));

            // Dify 要求 inputs 必须存在
            requestBody.put("inputs", new HashMap<>());

            // 上传图片到Dify，通过files参数传递（替代URL方式，解决Dify沙箱无法访问内网IP问题）
            if (imageUrl != null && !imageUrl.isEmpty()) {
                String uploadFileId = uploadFileToDify(imageUrl, testApiKey, String.valueOf(userId));
                if (uploadFileId != null) {
                    List<Map<String, Object>> files = new ArrayList<>();
                    Map<String, Object> fileObj = new HashMap<>();
                    fileObj.put("type", "image");
                    fileObj.put("transfer_method", "local_file");
                    fileObj.put("upload_file_id", uploadFileId);
                    files.add(fileObj);
                    requestBody.put("files", files);
                    log.info("SOP测试图片已上传至Dify: fileId={}", uploadFileId);
                } else {
                    log.warn("SOP测试图片上传Dify失败，将无图评估: imageUrl={}", imageUrl);
                }
            }

            String responseStr = webClient.post()
                    .uri(difyUrl + "/v1/chat-messages")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + testApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseJson = objectMapper.readTree(responseStr);
            return responseJson.has("answer") ? responseJson.get("answer").asText() : "";

        } catch (Exception e) {
            log.error("SOP测试同步调用失败", e);
            throw new BusinessException("SOP测试失败: " + e.getMessage());
        }
    }

    /**
     * 构建SOP测试提示词
     */
    private String buildSopTestQuery(String stepJson, Integer stepIndex, String userInput, String imageUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("当前步骤内容：\n");
        sb.append(stepJson);
        sb.append("\n\n第 ").append(stepIndex + 1).append(" 步，用户操作：").append(userInput != null ? userInput : "无文字描述");

        if (imageUrl != null && !imageUrl.isEmpty()) {
            // 图片通过Dify的files参数传递，不在此处输出URL（Dify沙箱无法访问内网地址）
            sb.append("\n\n【用户上传了现场操作图片，请结合图片内容评估操作是否符合验收标准】");
        }

        return sb.toString();
    }

    /**
     * 将相对路径图片URL转为完整可访问的URL
     */

    private String resolveImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return imageUrl;
        }
        // 如果已经是完整URL，直接返回
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        // 去除基地址末尾的斜杠，拼接路径
        String base = serverBaseUrl.endsWith("/") ? serverBaseUrl.substring(0, serverBaseUrl.length() - 1) : serverBaseUrl;
        return base + (imageUrl.startsWith("/") ? imageUrl : "/" + imageUrl);
    }

    /**
     * 解析SSE事件行
     */
    private void parseSseLine(String line, StreamCallback callback) {
        // SSE 格式可能是 "data:{...}" 或纯 JSON "{...}"
        String data = line;
        if (line.startsWith("data:")) {
            data = line.substring(5).trim();
        }
        if (data.isEmpty() || !data.startsWith("{")) return;
        try {
            JsonNode node = objectMapper.readTree(data);
            String event = node.has("event") ? node.get("event").asText() : "";

            if ("message".equals(event) || "agent_message".equals(event)) {
                String answer = node.has("answer") ? node.get("answer").asText() : "";
                callback.onMessage(answer);
            } else if ("message_end".equals(event)) {
                String metadata = node.has("metadata") ? node.get("metadata").toString() : "";
                callback.onComplete(metadata);
            }
        } catch (Exception e) {
            log.warn("解析SSE数据失败: {}", data);
        }
    }

    /**
     * 构建文档内容（优化MD格式，H1标题行嵌入型号关键词，利用Dify父子分段）
     */
    private String buildDocumentContent(RepairKnowledge knowledge) {
        StringBuilder sb = new StringBuilder();
        // H1标题行集中关键词：标题 + 型号，利用 \n 分隔符使整行为一个子块
        sb.append("# ").append(knowledge.getTitle())
          .append(" | 型号: ").append(knowledge.getEquipModel()).append("\n\n");
        // 故障描述
        String symptom = knowledge.getSymptom();
        if (symptom != null && !symptom.isEmpty()) {
            sb.append("## 故障描述\n").append(symptom).append("\n\n");
        } else if (knowledge.getContent() != null && !knowledge.getContent().isEmpty()) {
            sb.append("## 故障描述\n").append(knowledge.getContent()).append("\n\n");
        }
        // 解决方案
        String solution = knowledge.getSolution();
        if (solution != null && !solution.isEmpty()) {
            sb.append("## 解决方案\n").append(solution).append("\n\n");
        }
        // 故障图片：合并 imgUrl（单图，向后兼容）与 images（多图JSON数组）
        List<String> imageUrls = collectImageUrls(knowledge);
        if (!imageUrls.isEmpty()) {
            sb.append("## 故障图片\n");
            for (String url : imageUrls) {
                sb.append("![案例图片](").append(toAbsoluteUrl(url)).append(")\n");
            }
        }
        return sb.toString();
    }

    /**
     * 合并 imgUrl 与 images 字段，返回去重后的图片URL列表
     */
    private List<String> collectImageUrls(RepairKnowledge knowledge) {
        List<String> urls = new ArrayList<>();
        if (knowledge.getImgUrl() != null && !knowledge.getImgUrl().isEmpty()) {
            urls.add(knowledge.getImgUrl());
        }
        String imagesJson = knowledge.getImages();
        if (imagesJson != null && !imagesJson.isEmpty()) {
            try {
                JsonNode arr = objectMapper.readTree(imagesJson);
                if (arr.isArray()) {
                    for (JsonNode node : arr) {
                        String u = node.asText("");
                        if (!u.isEmpty()) {
                            urls.add(u);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析 images JSON 失败，跳过多图: {}", e.getMessage());
            }
        }
        return urls;
    }

    /**
     * 将相对路径图片URL转为绝对URL，便于Dify知识库检索后展示
     */
    private String toAbsoluteUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String base = serverBaseUrl != null ? serverBaseUrl.replaceAll("/+$", "") : "";
        return base + (url.startsWith("/") ? url : "/" + url);
    }

    /**
     * 审核通过后，将案例图片文件上传到 Dify 知识库，实现多模态索引。
     * 尽力而为：单张失败不影响文本文档与其他图片上传。
     */
    private void uploadCaseImagesToDataset(RepairKnowledge knowledge, String datasetId) {
        List<String> imageUrls = collectImageUrls(knowledge);
        if (imageUrls.isEmpty()) {
            return;
        }
        int success = 0;
        for (String url : imageUrls) {
            try {
                java.io.File localFile = resolveLocalImageFile(url);
                if (localFile == null || !localFile.exists()) {
                    log.warn("案例图片本地文件不存在，跳过上传到Dify: {}", url);
                    continue;
                }
                LocalMultipartFile mf = new LocalMultipartFile(localFile);
                Map<String, Object> res = difyKnowledgeService.uploadDocument(datasetId, mf);
                log.info("案例图片上传到Dify知识库成功: datasetId={}, file={}, result={}", datasetId, localFile.getName(), res);
                success++;
            } catch (Exception e) {
                log.warn("案例图片上传到Dify知识库失败（不影响文本文档）: url={}, error={}", url, e.getMessage());
            }
        }
        log.info("案例 {} 图片多模态上传完成: 共 {} 张, 成功 {} 张", knowledge.getId(), imageUrls.size(), success);
    }

    /**
     * 将图片访问URL解析为本地文件。URL形如 /uploads/knowledge/images/2026/06/30/xxx.png
     */
    private java.io.File resolveLocalImageFile(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        String relative = url;
        if (relative.startsWith("http://") || relative.startsWith("https://")) {
            int idx = relative.indexOf("://");
            if (idx > 0) {
                int pathStart = relative.indexOf("/", idx + 3);
                relative = pathStart >= 0 ? relative.substring(pathStart) : "";
            }
        }
        String prefix = "/uploads";
        if (relative.startsWith(prefix)) {
            relative = relative.substring(prefix.length());
        }
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        java.nio.file.Path base = java.nio.file.Paths.get(uploadDir).toAbsolutePath();
        return base.resolve(relative).toFile();
    }

     // ========== DifyAgentType 重载方法（兼容 master 分支代码）==========
    @Override
    public SearchResponse search(SearchRequest request, Long userId, DifyAgentType agentType) {
        try {
            String appApiKey = difyStatusProperties.getApiKey(agentType);
            ChatMessageSendRequest chatRequest = new ChatMessageSendRequest();
            chatRequest.setApiKey(appApiKey);
            chatRequest.setContent(request.getQuery());
            chatRequest.setUserId(String.valueOf(userId));

            // 添加设备型号到输入变量
            if (request.getEquipModel() != null && !request.getEquipModel().isEmpty()) {
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("equip_model", request.getEquipModel());
                chatRequest.setInputs(inputs);
            }

            // 添加文件（图片/视频）到请求中
            List<ChatMessageSendRequest.ChatMessageFile> files = buildChatMessageFiles(request, appApiKey, String.valueOf(userId));
            if (!files.isEmpty()) {
                chatRequest.setFiles(files);
            }

            ChatMessageSendResponse response = difyChat.send(chatRequest);

            // 构建响应
            List<SearchResponse.RetrieverResource> resources = new ArrayList<>();
            if (response.getMetadata() != null && response.getMetadata().getRetrieverResources() != null) {
                for (var resource : response.getMetadata().getRetrieverResources()) {
                    resources.add(SearchResponse.RetrieverResource.builder()
                            .datasetId(resource.getDatasetId())
                            .datasetName(resource.getDatasetName())
                            .documentId(resource.getDocumentId())
                            .documentName(resource.getDocumentName())
                            .content(resource.getContent())
                            .score(resource.getScore() != null ? resource.getScore().doubleValue() : 0.0)
                            .segmentId(resource.getSegmentId() != null ? resource.getSegmentId() : "")
                            .build());
                }
            }

            return SearchResponse.builder()
                    .answer(response.getAnswer())
                    .resources(resources)
                    .conversationId(response.getConversationId())
                    .build();

        } catch (Exception e) {
            log.error("调用Dify Chat API失败", e);
            throw new BusinessException("检索失败，请稍后重试: " + e.getMessage());
        }
    }
// ========== DifyAgentType 重载方法（兼容 master 分支代码）==========
    @Override
    public void streamSearch(SearchRequest request, Long userId, StreamCallback callback, DifyAgentType agentType) {
        try {
            String appApiKey = difyStatusProperties.getApiKey(agentType);
            ChatMessageSendRequest chatRequest = new ChatMessageSendRequest();
            chatRequest.setApiKey(appApiKey);
            chatRequest.setContent(request.getQuery());
            chatRequest.setUserId(String.valueOf(userId));

            if (request.getEquipModel() != null && !request.getEquipModel().isEmpty()) {
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("equip_model", request.getEquipModel());
                chatRequest.setInputs(inputs);
            }

            // 添加文件（图片/视频）到请求中
            List<ChatMessageSendRequest.ChatMessageFile> files = buildChatMessageFiles(request, appApiKey, String.valueOf(userId));
            if (!files.isEmpty()) {
                chatRequest.setFiles(files);
            }

            Flux<ChatMessageSendCompletionResponse> flux = difyChat.sendChatMessageStream(chatRequest);

            StringBuilder fullAnswer = new StringBuilder();
            flux.subscribe(
                    chunk -> {
                        if (chunk.getAnswer() != null) {
                            fullAnswer.append(chunk.getAnswer());
                            callback.onMessage(chunk.getAnswer());
                        }
                    },
                    error -> {
                        log.error("SSE流式请求失败", error);
                        callback.onError("请求失败: " + error.getMessage());
                    },
                    () -> {
                        callback.onComplete(fullAnswer.toString());
                    }
            );

        } catch (Exception e) {
            log.error("调用Dify流式API失败", e);
            callback.onError("检索失败: " + e.getMessage());
        }
    }

     @Override
    public List<Map<String, Object>> getDatasets() {
        try {
            String response = webClient.get()
                    .uri(difyUrl + "/v1/datasets?page=1&limit=100")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + datasetApiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Map<String, Object> result = objectMapper.readValue(response, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            return data != null ? data : List.of();
        } catch (Exception e) {
            log.error("获取知识库列表失败", e);
            throw new BusinessException("获取知识库列表失败");
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
    public String uploadToDataset(RepairKnowledge knowledge, String datasetId) {
        try {
            String documentContent = buildDocumentContent(knowledge);

            // 使用 Dify SDK 的 create-by-text API 上传（自动处理 doc_form、分块规则等参数）
            String documentId = difyKnowledgeService.uploadTextToDataset(datasetId, knowledge.getTitle(), documentContent);

            // 审核通过后：将案例图片作为独立文件上传到指定知识库，尽力而为实现多模态索引
            try {
                uploadCaseImagesToDataset(knowledge, datasetId);
            } catch (Exception imgEx) {
                log.warn("案例图片多模态上传整体失败（不影响文本文档）: knowledgeId={}, datasetId={}, error={}",
                        knowledge.getId(), datasetId, imgEx.getMessage());
            }

            return documentId;

        } catch (Exception e) {
            log.error("上传文档到指定知识库失败: datasetId={}", datasetId, e);
            throw new BusinessException("上传文档失败: " + e.getMessage());
        }
    }

    /**
     * 构建Dify聊天消息中的文件列表
     * 使用 local_file 方式：先将文件上传到Dify，获取文件ID，再在聊天消息中引用
     */
    private List<ChatMessageSendRequest.ChatMessageFile> buildChatMessageFiles(SearchRequest request, String apiKey, String userId) {
        List<ChatMessageSendRequest.ChatMessageFile> files = new ArrayList<>();
        if (request.getFileUrls() == null || request.getFileUrls().isEmpty()) {
            return files;
        }

        List<String> fileTypes = request.getFileTypes();
        for (int i = 0; i < request.getFileUrls().size(); i++) {
            String url = request.getFileUrls().get(i);
            String type = (fileTypes != null && i < fileTypes.size()) ? fileTypes.get(i) : "image";

            try {
                // 将本地文件上传到Dify，获取文件ID
                String uploadFileId = uploadFileToDify(url, apiKey, userId);
                if (uploadFileId != null) {
                    ChatMessageSendRequest.ChatMessageFile file = new ChatMessageSendRequest.ChatMessageFile();
                    file.setType(type);
                    file.setTransferMethod("local_file");
                    file.setUploadFileId(uploadFileId);
                    files.add(file);
                }
            } catch (Exception e) {
                log.warn("上传文件到Dify失败，跳过该文件: url={}, error={}", url, e.getMessage());
            }
        }
        return files;
    }

    /**
     * 将本地文件上传到Dify，返回文件ID
     */
    private String uploadFileToDify(String url, String apiKey, String userId) {
        try {
            // 将相对路径转为本地文件绝对路径
            java.nio.file.Path localFilePath = resolveLocalFilePath(url);
            if (localFilePath == null || !java.nio.file.Files.exists(localFilePath)) {
                log.warn("文件不存在: url={}, localPath={}", url, localFilePath);
                return null;
            }

            DifyFile difyFile = DifyFile.from(localFilePath);
            FileUploadRequest uploadRequest = new FileUploadRequest(difyFile);
            uploadRequest.setApiKey(apiKey);
            uploadRequest.setUserId(userId);

            FileUploadResponse uploadResponse = difyChat.fileUpload(uploadRequest);
            if (uploadResponse != null && uploadResponse.getId() != null) {
                log.info("文件上传到Dify成功: id={}, name={}", uploadResponse.getId(), uploadResponse.getName());
                return uploadResponse.getId();
            }
            return null;
        } catch (Exception e) {
            log.error("上传文件到Dify失败: url={}", url, e);
            return null;
        }
    }

    /**
     * 将URL路径转为本地文件路径
     */
    private java.nio.file.Path resolveLocalFilePath(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        // 去掉URL前缀，提取相对路径 /uploads/2026/06/09/xxx.png
        String relativePath = url;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            // 从绝对URL中提取路径部分
            try {
                java.net.URI uri = new java.net.URI(url);
                relativePath = uri.getPath();
            } catch (Exception e) {
                return null;
            }
        }
        // 去掉开头的 /uploads/ 前缀
        if (relativePath.startsWith("/uploads/")) {
            relativePath = relativePath.substring("/uploads/".length());
        }
        return java.nio.file.Paths.get(uploadDir, relativePath).toAbsolutePath();
    }

    // ========== 语音转文字 ==========

    /**
     * 语音转文字 - 使用 dify-spring-boot-starter SDK 的 audioToText 方法
     * SDK 会自动处理 multipart 请求的 Content-Type，无需手动设置
     */
    @Override
    public String audioToText(MultipartFile audioFile, Long userId) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException("音频文件不能为空");
        }

        String originalName = audioFile.getOriginalFilename();
        String contentType = audioFile.getContentType();
        long size = audioFile.getSize();
        log.info("语音转文字请求: fileName={}, contentType={}, size={}bytes, userId={}",
                originalName, contentType, size, userId);

        java.nio.file.Path tempFilePath = null;
        try {
            java.nio.file.Path tempDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "equipment-repair-audio");
            java.nio.file.Files.createDirectories(tempDir);
            String tempFileName = System.currentTimeMillis() + "_" + originalName;
            tempFilePath = tempDir.resolve(tempFileName);
            audioFile.transferTo(tempFilePath.toFile());

            DifyFile difyFile = DifyFile.from(tempFilePath);

            io.github.guoshiqiufeng.dify.chat.dto.request.AudioToTextRequest request =
                    new io.github.guoshiqiufeng.dify.chat.dto.request.AudioToTextRequest();
            request.setApiKey(appApiKey);
            request.setUserId(String.valueOf(userId));
            request.setFile(difyFile);

            log.info("调用 DifyChat.audioToText: apiKey={}, userId={}", appApiKey, userId);
            io.github.guoshiqiufeng.dify.chat.dto.response.DifyTextVO result = difyChat.audioToText(request);

            if (result == null || result.getText() == null) {
                throw new BusinessException("Dify返回空的转写结果");
            }

            String text = result.getText().trim();
            log.info("语音转文字成功: textLen={}", text.length());
            return text;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 Dify 语音转文字失败", e);
            String message = e.getMessage();
            if (message != null && message.contains("unsupported_audio_type")) {
                throw new BusinessException("不支持的音频格式，请使用 wav、mp3、m4a 等格式");
            }
            throw new BusinessException("语音转文字失败：" + message);
        } finally {
            if (tempFilePath != null) {
                try {
                    java.nio.file.Files.deleteIfExists(tempFilePath);
                } catch (IOException ignored) {
                }
            }
        }
    }

    // ========== 总结对话为案例 ==========

    @Override
    public Map<String, Object> summarizeConversationToCase(String conversationContent, Long userId) {
        if (conversationContent == null || conversationContent.trim().isEmpty()) {
            throw new BusinessException("对话内容不能为空");
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("conversation_content", conversationContent);
            requestBody.put("inputs", inputs);
            requestBody.put("user", String.valueOf(userId));
            requestBody.put("response_mode", "blocking");

            log.info("调用Dify Workflow 总结案例: contentLen={}, userId={}", conversationContent.length(), userId);

            String responseStr = webClient.post()
                    .uri(difyUrl + "/v1/workflows/run")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + summaryApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(body -> {
                                log.error("Dify Workflow返回错误: status={}, body={}", clientResponse.statusCode(), body);
                                return reactor.core.publisher.Mono.error(
                                        new RuntimeException("Dify Workflow错误 [" + clientResponse.statusCode() + "]: " + body));
                            }))
                    .bodyToMono(String.class)
                    .block();

            log.debug("Dify Workflow响应前300字: {}", responseStr.length() > 300 ? responseStr.substring(0, 300) : responseStr);

            JsonNode responseJson = objectMapper.readTree(responseStr);
            JsonNode dataNode = responseJson.path("data");
            JsonNode outputsNode = dataNode.path("outputs");
            JsonNode caseDataNode = outputsNode.path("case_data");

            // 如果 structured_output 返回了 case_data，优先使用
            if (!caseDataNode.isMissingNode() && !caseDataNode.isNull()) {
                Map<String, Object> result = objectMapper.convertValue(caseDataNode,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                log.info("案例总结完成: title={}", result.get("title"));
                return result;
            }

            // 降级：尝试从 text 字段解析 JSON
            String text = outputsNode.path("text").asText("");
            if (!text.isEmpty()) {
                JsonNode extractedJson = extractJsonFromText(text);
                if (extractedJson != null) {
                    Map<String, Object> result = objectMapper.convertValue(extractedJson,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                    log.info("案例总结完成(从text提取): title={}", result.get("title"));
                    return result;
                }
            }

            throw new BusinessException("Dify返回数据中未找到案例数据");

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用Dify Workflow总结案例失败", e);
            throw new BusinessException("总结失败: " + e.getMessage());
        }
    }

    /**
     * 从 LLM 文本输出中容错提取 JSON
     * 支持 ```json ... ``` 代码块和裸 { ... } 对象
     */
    private JsonNode extractJsonFromText(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            // 尝试匹配 ```json ... ``` 代码块
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```");
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                return objectMapper.readTree(m.group(1));
            }
        } catch (Exception ignored) {}
        try {
            // 尝试匹配第一个 { ... } 对象
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return objectMapper.readTree(text.substring(start, end + 1));
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ========== AI智能问答 - 会话管理方法 ==========

    /**
     * 删除Dify远端对话
     * 调用 Dify REST API: DELETE /v1/conversations/{conversationId}
     */
    @Override
    public void deleteConversation(String conversationId, String apiKey, String userId) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("user", userId);

            webClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(difyUrl + "/v1/conversations/" + conversationId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Dify远端对话已删除: conversationId={}", conversationId);
        } catch (Exception e) {
            log.error("删除Dify远端对话失败: conversationId={}", conversationId, e);
            throw new RuntimeException("删除Dify远端对话失败: " + e.getMessage());
        }
    }

    /**
     * 获取Dify远端对话消息列表
     * 调用 Dify REST API: GET /v1/messages?conversation_id=xxx&user=xxx
     */
    @Override
    public List<ChatMessageVO> getConversationMessages(String conversationId, String apiKey, String userId) {
        try {
            String responseStr = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(difyUrl + "/v1/messages")
                            .queryParam("conversation_id", conversationId)
                            .queryParam("user", userId)
                            .queryParam("limit", 100)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseJson = objectMapper.readTree(responseStr);
            JsonNode dataNode = responseJson.has("data") ? responseJson.get("data") : null;

            List<ChatMessageVO> messages = new ArrayList<>();
            if (dataNode != null && dataNode.isArray()) {
                for (JsonNode msgNode : dataNode) {
                    ChatMessageVO vo = new ChatMessageVO();
                    vo.setMessageId(msgNode.has("id") ? msgNode.get("id").asText() : "");
                    vo.setConversationId(conversationId);
                    vo.setUserQuestion(msgNode.has("query") ? msgNode.get("query").asText() : "");
                    vo.setAnswerContent(msgNode.has("answer") ? msgNode.get("answer").asText() : "");

                    // 解析引用资源
                    List<ChatMessageVO.RetrieverResource> resources = new ArrayList<>();
                    if (msgNode.has("retriever_resources") && msgNode.get("retriever_resources").isArray()) {
                        for (JsonNode resNode : msgNode.get("retriever_resources")) {
                            ChatMessageVO.RetrieverResource resource = new ChatMessageVO.RetrieverResource();
                            resource.setDatasetId(resNode.has("dataset_id") ? resNode.get("dataset_id").asText() : "");
                            resource.setDatasetName(resNode.has("dataset_name") ? resNode.get("dataset_name").asText() : "");
                            resource.setDocumentId(resNode.has("document_id") ? resNode.get("document_id").asText() : "");
                            resource.setDocumentName(resNode.has("document_name") ? resNode.get("document_name").asText() : "");
                            resource.setContent(resNode.has("content") ? resNode.get("content").asText() : "");
                            resource.setScore(resNode.has("score") ? resNode.get("score").asDouble() : 0.0);
                            resource.setSegmentId(resNode.has("segment_id") ? resNode.get("segment_id").asText() : "");
                            resources.add(resource);
                        }
                    }
                    vo.setResources(resources);
                    messages.add(vo);
                }
            }

            return messages;
        } catch (Exception e) {
            log.error("获取Dify远端对话消息失败: conversationId={}", conversationId, e);
            throw new RuntimeException("获取Dify远端对话消息失败: " + e.getMessage());
        }
    }
}
