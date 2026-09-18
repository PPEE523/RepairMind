package com.example.equipment.service;

import com.example.equipment.dto.ChatMessageVO;
import com.example.equipment.dto.SearchRequest;
import com.example.equipment.dto.SearchResponse;
import com.example.equipment.entity.RepairKnowledge;
import com.example.equipment.enums.DifyAgentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface DifyService {

    SearchResponse search(SearchRequest request, Long userId);

    SearchResponse search(SearchRequest request, Long userId, DifyAgentType agentType);

    void streamSearch(SearchRequest request, Long userId, StreamCallback callback);

    void streamSearch(SearchRequest request, Long userId, StreamCallback callback, DifyAgentType agentType);

    String uploadToDataset(RepairKnowledge knowledge);

    String uploadToDataset(RepairKnowledge knowledge, String datasetId);

    /**
     * 检测案例相似度（仅检索案例审核白名单知识库）
     * @return Map 包含 similarity(最高分), topDocuments(匹配文档列表)
     */
    Map<String, Object> checkSimilarity(String content, String equipModel);

    /**
     * 获取知识库列表
     */
    List<Map<String, Object>> getDatasets();

    /**
     * 获取知识库列表（按模块过滤）
     * @param scope 模块标识（对应配置中的 visible-ids key）
     */
    List<Map<String, Object>> getDatasets(String scope);

    String generateSopFromDocument(String documentName, Long userId);

    void testSopStep(String stepJson, Integer stepIndex, String userInput,
                     String imageUrl, Long userId, StreamCallback callback);

    String testSopStepSync(String stepJson, Integer stepIndex, String userInput,
                           String imageUrl, Long userId);

    /**
     * 删除Dify远端对话
     * @param conversationId 对话ID
     * @param apiKey 应用API Key
     * @param userId 用户标识
     */
    void deleteConversation(String conversationId, String apiKey, String userId);

    /**
     * 获取Dify远端对话消息列表
     * @param conversationId 对话ID
     * @param apiKey 应用API Key
     * @param userId 用户标识
     * @return 消息列表（含引用资源）
     */
    List<ChatMessageVO> getConversationMessages(String conversationId, String apiKey, String userId);

    /**
     * 语音转文字
     * @param audioFile 音频文件
     * @param userId 用户ID
     * @return 转写后的文字
     */
    String audioToText(MultipartFile audioFile, Long userId);

    /**
     * 总结对话为案例 - 调用 Dify Workflow /v1/workflows/run
     * @param conversationContent 拼接后的对话内容（Q/A 格式）
     * @param userId 用户ID
     * @return 结构化案例数据 { title, equipModel, symptom, solution }
     */
    Map<String, Object> summarizeConversationToCase(String conversationContent, Long userId);

    interface StreamCallback {
        void onMessage(String content);
        void onComplete(String fullAnswer);
        void onError(String error);
    }
}
