package com.example.equipment.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface DifyKnowledgeService {

    /**
     * 获取 Dify 服务器地址
     */
    String getDifyBaseUrl();

    /**
     * 获取 Dataset API Key
     */
    String getDatasetApiKey();

    /**
     * 获取知识库列表
     */
    List<Map<String, Object>> getDatasets();

    /**
     * 获取知识库列表（按模块过滤）
     * @param scope 模块标识（对应配置中的 visible-ids key）
     */
    List<Map<String, Object>> getDatasets(String scope);

    /**
     * 获取知识库详情
     */
    Map<String, Object> getDatasetDetail(String datasetId);

    /**
     * 更新知识库信息（名称、描述等）
     */
    Map<String, Object> updateDataset(String datasetId, String name, String description);

    /**
     * 获取文档列表
     */
    Map<String, Object> getDocuments(String datasetId, Integer page, Integer limit);

    /**
     * 获取文档详情
     */
    Map<String, Object> getDocumentDetail(String datasetId, String documentId);

    /**
     * 获取文档分段
     */
    Map<String, Object> getDocumentSegments(String datasetId, String documentId, Integer page, Integer limit);

    /**
     * 上传文档
     */
    Map<String, Object> uploadDocument(String datasetId, MultipartFile file);

    /**
     * 删除文档
     */
    void deleteDocument(String datasetId, String documentId);

    /**
     * 知识库检索
     */
    Map<String, Object> retrieve(String datasetId, String query, Integer topK);

    /**
     * 获取文档下载链接
     */
    String getDocumentDownloadUrl(String datasetId, String documentId);
    
    /**
     * 获取文件预览链接（签名 URL）
     */
    String getFilePreviewUrl(String filePath);

    /**
     * 更新文档分段
     */
    Map<String, Object> updateSegment(String datasetId, String documentId, String segmentId,
                                       String content, String answer, List<String> keywords, Boolean enabled);

    /**
     * 删除文档分段
     */
    void deleteSegment(String datasetId, String documentId, String segmentId);

    /**
     * 创建文档分段
     */
    List<Map<String, Object>> createSegment(String datasetId, String documentId, String content, String answer);

    /**
     * 获取子分段列表
     */
    Map<String, Object> getChildChunks(String datasetId, String documentId, String segmentId, Integer page, Integer limit);

    /**
     * 创建子分段
     */
    Map<String, Object> createChildChunk(String datasetId, String documentId, String segmentId, String content);

    /**
     * 更新子分段
     */
    Map<String, Object> updateChildChunk(String datasetId, String documentId, String segmentId,
                                          String childChunkId, String content);

    /**
     * 删除子分段
     */
    void deleteChildChunk(String datasetId, String documentId, String segmentId, String childChunkId);

    /**
     * 上传文本到知识库（通过 SDK 的 create-by-text API）
     *
     * @param datasetId 知识库 ID
     * @param name      文档名称
     * @param content   文档文本内容
     * @return 文档 ID
     */
    String uploadTextToDataset(String datasetId, String name, String content);

    /**
     * 检查文档是否在知识库中存在
     *
     * @param datasetId   知识库 ID
     * @param documentId  文档 ID
     * @return true 存在, false 不存在
     */
    boolean checkDocumentExists(String datasetId, String documentId);
}
