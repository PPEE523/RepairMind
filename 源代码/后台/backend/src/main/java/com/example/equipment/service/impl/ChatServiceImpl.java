package com.example.equipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.equipment.config.ChatProperties;
import com.example.equipment.dto.ChatConversationVO;
import com.example.equipment.dto.ChatFeedbackRequest;
import com.example.equipment.dto.ChatMessageVO;
import com.example.equipment.dto.ChatRequest;
import com.example.equipment.entity.ChatDocumentResource;
import com.example.equipment.entity.ChatFeedback;
import com.example.equipment.entity.ChatFile;
import com.example.equipment.entity.ChatHistory;
import com.example.equipment.entity.ChatSegment;
import com.example.equipment.mapper.ChatDocumentResourceMapper;
import com.example.equipment.mapper.ChatFeedbackMapper;
import com.example.equipment.mapper.ChatFileMapper;
import com.example.equipment.mapper.ChatHistoryMapper;
import com.example.equipment.mapper.ChatSegmentMapper;
import com.example.equipment.service.ChatService;
import com.example.equipment.service.DifyService;
import io.github.guoshiqiufeng.dify.chat.DifyChat;
import io.github.guoshiqiufeng.dify.chat.dto.request.ChatMessageSendRequest;
import io.github.guoshiqiufeng.dify.chat.dto.request.FileUploadRequest;
import io.github.guoshiqiufeng.dify.chat.dto.response.ChatMessageSendCompletionResponse;
import io.github.guoshiqiufeng.dify.chat.dto.response.ChatMessageSendResponse;
import io.github.guoshiqiufeng.dify.chat.dto.response.FileUploadResponse;
import io.github.guoshiqiufeng.dify.core.pojo.DifyFile;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * AI智能问答 - 对话服务实现
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    private ChatHistoryMapper chatHistoryMapper;

    @Resource
    private ChatFeedbackMapper chatFeedbackMapper;

    @Resource
    private ChatDocumentResourceMapper chatDocumentResourceMapper;

    @Resource
    private ChatSegmentMapper chatSegmentMapper;

    @Resource
    private ChatFileMapper chatFileMapper;

    @Resource
    private DifyChat difyChat;

    @Resource
    private DifyService difyService;

    @Resource
    private ChatProperties chatProperties;

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    /** 文件上传线程池（并行上传多文件到Dify） */
    private final ExecutorService uploadExecutor = Executors.newFixedThreadPool(5);

    /** 复用管理员端已验证可用的 Dify App API Key */
    @Value("${chat.api-key:${dify.dataset.api-key}}")
    private String appApiKey;

    @Value("${dify.url}")
    private String difyBaseUrl;

    @Override
    public void sendChatMessage(ChatRequest request, Long userId, ChatStreamCallback callback) {
        try {
            String apiKey = appApiKey;

            // 1. 校验问题内容
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                callback.onError("请输入问题内容");
                return;
            }

            // 2. 如果有conversationId，校验轮次限制（maxRounds为0时不限制）
            if (request.getConversationId() != null && !request.getConversationId().isEmpty()
                    && chatProperties.getMaxRounds() > 0) {
                Long existRounds = chatHistoryMapper.selectCount(
                        new LambdaQueryWrapper<ChatHistory>()
                                .eq(ChatHistory::getConversationId, request.getConversationId())
                );
                if (existRounds >= chatProperties.getMaxRounds()) {
                    callback.onError("已达到最大对话轮数(" + chatProperties.getMaxRounds() + "轮)，请开始新对话");
                    return;
                }
            }

            // 3. 构建Dify请求
            ChatMessageSendRequest chatRequest = new ChatMessageSendRequest();
            chatRequest.setApiKey(apiKey);
            chatRequest.setContent(request.getQuery());
            chatRequest.setUserId(String.valueOf(userId));

            // 设置conversationId（多轮对话）
            if (request.getConversationId() != null && !request.getConversationId().isEmpty()) {
                chatRequest.setConversationId(request.getConversationId());
            }

            // 设置设备型号到输入变量
            if (request.getEquipModel() != null && !request.getEquipModel().isEmpty()) {
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("equip_model", request.getEquipModel());
                chatRequest.setInputs(inputs);
            }

            // 4. 添加文件（图片/视频）到请求中
            List<ChatMessageSendRequest.ChatMessageFile> files = buildChatMessageFiles(request, apiKey, String.valueOf(userId));
            if (!files.isEmpty()) {
                chatRequest.setFiles(files);
            }

            // 5. 调用Dify流式API
            Flux<ChatMessageSendCompletionResponse> flux = difyChat.sendChatMessageStream(chatRequest);

            // 用于收集完整回答和元数据
            StringBuilder fullAnswer = new StringBuilder();
            AtomicReference<String> conversationIdRef = new AtomicReference<>(request.getConversationId());
            AtomicReference<String> messageIdRef = new AtomicReference<>("");
            java.util.concurrent.ConcurrentHashMap<String, ChatMessageVO.RetrieverResource> metadataResourcesMap = new java.util.concurrent.ConcurrentHashMap<>();
            AtomicBoolean completed = new AtomicBoolean(false);

            flux.subscribe(
                    chunk -> {
                        try {
                            // 收集流式内容
                            if (chunk.getAnswer() != null) {
                                String answer = replaceFileUrls(chunk.getAnswer());
                                fullAnswer.append(answer);
                                callback.onMessage(answer);
                            }
                            // 收集conversationId
                            if (chunk.getConversationId() != null && !chunk.getConversationId().isEmpty()) {
                                conversationIdRef.set(chunk.getConversationId());
                            }
                            // 收集messageId
                            if (chunk.getMessageId() != null && !chunk.getMessageId().isEmpty()) {
                                messageIdRef.set(chunk.getMessageId());
                            }
                            // 从metadata中提取引用资源（message_end事件时metadata非空）
                            // 多个chunk可能分批返回不同资源，使用累加去重而非覆盖
                            if (chunk.getMetadata() != null && chunk.getMetadata().getRetrieverResources() != null
                                    && !chunk.getMetadata().getRetrieverResources().isEmpty()) {
                                for (ChatMessageSendResponse.RetrieverResources res : chunk.getMetadata().getRetrieverResources()) {
                                    ChatMessageVO.RetrieverResource resource = new ChatMessageVO.RetrieverResource();
                                    resource.setDatasetId(res.getDatasetId() != null ? res.getDatasetId() : "");
                                    resource.setDatasetName(res.getDatasetName() != null ? res.getDatasetName() : "");
                                    resource.setDocumentId(res.getDocumentId() != null ? res.getDocumentId() : "");
                                    resource.setDocumentName(res.getDocumentName() != null ? res.getDocumentName() : "");
                                    resource.setContent(res.getContent() != null ? res.getContent() : "");
                                    resource.setScore(res.getScore() != null ? res.getScore().doubleValue() : 0.0);
                                    resource.setSegmentId(res.getSegmentId() != null ? res.getSegmentId() : "");
                                    // 按 documentId + segmentId 去重累加，避免后续chunk覆盖前序资源
                                    String key = resource.getDocumentId() + "::" + resource.getSegmentId();
                                    metadataResourcesMap.putIfAbsent(key, resource);
                                }
                                System.out.println("从metadata中提取引用资源的结果是-------==================" + metadataResourcesMap);
                            }
                        } catch (Exception e) {
                            log.warn("处理流式chunk失败", e);
                        }
                    },
                    error -> {
                        log.error("SSE流式请求失败", error);
                        // 保存失败记录
                        saveChatHistory(userId, request, fullAnswer.toString(), conversationIdRef.get(),
                                messageIdRef.get(), "fail", null, null);
                        if (completed.compareAndSet(false, true)) {
                            callback.onError("请求失败: " + error.getMessage());
                        }
                    },
                    () -> {
                        // 流式完成，优先使用从metadata中提取的引用资源
                        List<ChatMessageVO.RetrieverResource> resources = new ArrayList<>(metadataResourcesMap.values());
                        String convId = conversationIdRef.get();
                        String msgId = messageIdRef.get();

                        // 如果metadata中未获取到resources，fallback到二次调用Dify消息接口
                        if (resources == null || resources.isEmpty()) {
                            try {
                                if (convId != null && !convId.isEmpty() && msgId != null && !msgId.isEmpty()) {
                                    List<ChatMessageVO> difyMessages = difyService.getConversationMessages(
                                            convId, appApiKey, String.valueOf(userId));
                                    for (ChatMessageVO msg : difyMessages) {
                                        if (msgId.equals(msg.getMessageId()) && msg.getResources() != null && !msg.getResources().isEmpty()) {
                                            resources = msg.getResources();
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("流式完成后获取引用资源失败: {}", e.getMessage());
                            }
                        }

                        if (resources == null) {
                            resources = Collections.emptyList();
                        }

                        // 保存成功记录（含文档资源和分段信息）
                        Long chatHistoryId = saveChatHistory(userId, request, fullAnswer.toString(), convId, msgId, "success", null, resources);

                        if (completed.compareAndSet(false, true)) {
                            callback.onComplete(fullAnswer.toString(), convId, msgId, resources, chatHistoryId);
                        }
                    }
            );

        } catch (Exception e) {
            log.error("发起提问失败", e);
            callback.onError("提问失败: " + e.getMessage());
        }
    }

    @Override
    public Page<ChatConversationVO> getConversationList(Long userId, boolean isAdmin, int pageNum, int pageSize) {
        // 查询所有对话记录
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            wrapper.eq(ChatHistory::getUserId, userId);
        }
        wrapper.orderByDesc(ChatHistory::getCreateTime);

        List<ChatHistory> allRecords = chatHistoryMapper.selectList(wrapper);

        // 按conversationId分组，取每组最新一条
        Map<String, ChatHistory> latestMap = new LinkedHashMap<>();
        for (ChatHistory record : allRecords) {
            String convId = record.getConversationId();
            if (convId != null && !convId.isEmpty()) {
                if (!latestMap.containsKey(convId)) {
                    latestMap.put(convId, record);
                }
            }
        }

        // 转换为VO并计算轮次
        List<ChatConversationVO> voList = new ArrayList<>();
        for (Map.Entry<String, ChatHistory> entry : latestMap.entrySet()) {
            ChatHistory latest = entry.getValue();
            ChatConversationVO vo = new ChatConversationVO();
            vo.setConversationId(entry.getKey());
            vo.setUserQuestion(latest.getUserQuestion());
            vo.setDeviceModel(latest.getDeviceModel());
            vo.setCreateTime(latest.getCreateTime());
            vo.setStatus(latest.getStatus());
            vo.setUserId(latest.getUserId());

            // 计算该对话的轮次数
            Long roundCount = chatHistoryMapper.selectCount(
                    new LambdaQueryWrapper<ChatHistory>()
                            .eq(ChatHistory::getConversationId, entry.getKey())
            );
            vo.setRoundCount(roundCount.intValue());

            voList.add(vo);
        }

        // 手动分页
        int total = voList.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        Page<ChatConversationVO> page = new Page<>(pageNum, pageSize, total);
        if (fromIndex < total) {
            page.setRecords(voList.subList(fromIndex, toIndex));
        } else {
            page.setRecords(Collections.emptyList());
        }

        return page;
    }

    @Override
    public List<ChatMessageVO> getConversationDetail(String conversationId) {
        // 查询本地数据库中该对话的所有记录
        List<ChatHistory> records = chatHistoryMapper.selectList(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getConversationId, conversationId)
                        .orderByAsc(ChatHistory::getCreateTime)
        );

        // 转换为VO
        return records.stream().map(record -> {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(record.getId());
            vo.setConversationId(record.getConversationId());
            vo.setUserQuestion(record.getUserQuestion());
            vo.setDeviceModel(record.getDeviceModel());
            vo.setAnswerContent(record.getAnswerContent());
            vo.setMessageId(record.getMessageId());
            vo.setStatus(record.getStatus());
            vo.setRoundNum(record.getRoundNum());
            vo.setCreateTime(record.getCreateTime());
            vo.setUserId(record.getUserId());

            // 查询反馈状态
            ChatFeedback feedback = chatFeedbackMapper.selectOne(
                    new LambdaQueryWrapper<ChatFeedback>()
                            .eq(ChatFeedback::getChatHistoryId, record.getId())
                            .last("LIMIT 1")
            );
            vo.setFeedbackStatus(feedback != null ? feedback.getFeedbackStatus() : "pending");

            // 从本地数据库查询文档资源
            List<ChatDocumentResource> docResources = chatDocumentResourceMapper.selectList(
                    new LambdaQueryWrapper<ChatDocumentResource>()
                            .eq(ChatDocumentResource::getChatHistoryId, record.getId())
            );

            List<ChatMessageVO.RetrieverResource> resources = new ArrayList<>();
            for (ChatDocumentResource docRes : docResources) {
                // 查询该文档下的分段信息
                List<ChatSegment> segments = chatSegmentMapper.selectList(
                        new LambdaQueryWrapper<ChatSegment>()
                                .eq(ChatSegment::getDocumentResourceId, docRes.getId())
                );

                if (segments.isEmpty()) {
                    // 无分段信息，直接用文档资源构建
                    ChatMessageVO.RetrieverResource resource = new ChatMessageVO.RetrieverResource();
                    resource.setDatasetId(docRes.getDatasetId());
                    resource.setDatasetName(docRes.getDatasetName());
                    resource.setDocumentId(docRes.getDocumentId());
                    resource.setDocumentName(docRes.getDocumentName());
                    resource.setScore(0.0);
                    resource.setSegmentId("");
                    resource.setContent("");
                    resources.add(resource);
                } else {
                    // 有分段信息，每个分段构建一条（score从segment取）
                    for (ChatSegment seg : segments) {
                        ChatMessageVO.RetrieverResource resource = new ChatMessageVO.RetrieverResource();
                        resource.setDatasetId(docRes.getDatasetId());
                        resource.setDatasetName(docRes.getDatasetName());
                        resource.setDocumentId(docRes.getDocumentId());
                        resource.setDocumentName(docRes.getDocumentName());
                        resource.setScore(seg.getScore() != null ? seg.getScore() : 0.0);
                        resource.setSegmentId(seg.getSegmentId());
                        resource.setContent(seg.getContent());
                        resources.add(resource);
                    }
                }
            }
            vo.setResources(resources);

            // 查询附件文件列表
            List<ChatFile> chatFiles = chatFileMapper.selectList(
                    new LambdaQueryWrapper<ChatFile>()
                            .eq(ChatFile::getChatHistoryId, record.getId())
                            .orderByAsc(ChatFile::getCreateTime)
            );
            List<ChatMessageVO.ChatFileVO> fileVOs = chatFiles.stream().map(cf -> {
                ChatMessageVO.ChatFileVO fileVO = new ChatMessageVO.ChatFileVO();
                fileVO.setId(cf.getId());
                fileVO.setFileName(cf.getFileName());
                fileVO.setFilePath(cf.getFilePath());
                fileVO.setFileSize(cf.getFileSize());
                fileVO.setFileType(cf.getFileType());
                fileVO.setCreateTime(cf.getCreateTime());
                return fileVO;
            }).collect(Collectors.toList());
            vo.setFiles(fileVOs);

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void deleteConversation(String conversationId, Long userId) {
        // 1. 查询该对话下所有chat_history的id
        List<ChatHistory> historyRecords = chatHistoryMapper.selectList(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getConversationId, conversationId)
                        .select(ChatHistory::getId)
        );
        List<Long> historyIds = historyRecords.stream().map(ChatHistory::getId).collect(Collectors.toList());

        // 2. 级联删除：chat_file -> chat_segment -> chat_document_resource -> chat_feedback -> chat_history
        if (!historyIds.isEmpty()) {
            // 删除附件文件记录
            chatFileMapper.delete(
                    new LambdaQueryWrapper<ChatFile>()
                            .in(ChatFile::getChatHistoryId, historyIds)
            );

            // 查询所有关联的文档资源id
            List<ChatDocumentResource> docResources = chatDocumentResourceMapper.selectList(
                    new LambdaQueryWrapper<ChatDocumentResource>()
                            .in(ChatDocumentResource::getChatHistoryId, historyIds)
                            .select(ChatDocumentResource::getId)
            );
            List<Long> docResourceIds = docResources.stream().map(ChatDocumentResource::getId).collect(Collectors.toList());

            // 删除分段信息
            if (!docResourceIds.isEmpty()) {
                chatSegmentMapper.delete(
                        new LambdaQueryWrapper<ChatSegment>()
                                .in(ChatSegment::getDocumentResourceId, docResourceIds)
                );
            }

            // 删除文档资源
            chatDocumentResourceMapper.delete(
                    new LambdaQueryWrapper<ChatDocumentResource>()
                            .in(ChatDocumentResource::getChatHistoryId, historyIds)
            );

            // 删除反馈记录
            chatFeedbackMapper.delete(
                    new LambdaQueryWrapper<ChatFeedback>()
                            .in(ChatFeedback::getChatHistoryId, historyIds)
            );
        }

        // 3. 删除本地chat_history记录
        chatHistoryMapper.delete(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getConversationId, conversationId)
        );

        // 4. 同步调用Dify接口删除远端对话记录
        try {
            difyService.deleteConversation(conversationId, appApiKey, String.valueOf(userId));
            log.info("已同步删除Dify远端对话: conversationId={}", conversationId);
        } catch (Exception e) {
            log.warn("删除Dify远端对话失败，本地记录已删除: conversationId={}, error={}", conversationId, e.getMessage());
        }
    }

    @Override
    public void submitFeedback(ChatFeedbackRequest request, Long userId) {
        // 校验chatHistoryId是否存在
        if (request.getChatHistoryId() == null) {
            throw new RuntimeException("对话记录ID不能为空");
        }

        // 校验对应的chat_history记录是否存在
        ChatHistory history = chatHistoryMapper.selectById(request.getChatHistoryId());
        if (history == null) {
            throw new RuntimeException("对话记录不存在");
        }

        // 查询是否已存在feedback记录（对话完成时已创建pending记录）
        ChatFeedback existingFeedback = chatFeedbackMapper.selectOne(
                new LambdaQueryWrapper<ChatFeedback>()
                        .eq(ChatFeedback::getChatHistoryId, request.getChatHistoryId())
                        .last("LIMIT 1")
        );

        if (existingFeedback != null) {
            // 更新已有记录
            existingFeedback.setFeedbackStatus(request.getFeedbackStatus());
            existingFeedback.setFeedbackTime(LocalDateTime.now());
            existingFeedback.setUserId(userId);
            chatFeedbackMapper.updateById(existingFeedback);
        } else {
            // 不存在则新建
            ChatFeedback feedback = new ChatFeedback();
            feedback.setChatHistoryId(request.getChatHistoryId());
            feedback.setFeedbackStatus(request.getFeedbackStatus());
            feedback.setFeedbackTime(LocalDateTime.now());
            feedback.setUserId(userId);
            chatFeedbackMapper.insert(feedback);
        }

        log.info("用户 {} 提交反馈: chatHistoryId={}, status={}",
                userId, request.getChatHistoryId(), request.getFeedbackStatus());
    }

    // ==================== 私有方法 ====================

    /**
     * 将 Dify 返回内容中的文件路径替换为完整 URL
     * ![xxx](/files/...) => ![xxx](http://192.168.110.107/files/...)
     * 签名参数原样保留，浏览器可直接访问
     */
    private String replaceFileUrls(String text) {
        if (text == null || !text.contains("/files/")) {
            return text;
        }
        return text.replaceAll("(!\\[[^\\]]*])\\(/files/", "$1(" + difyBaseUrl + "/files/");
    }

    /**
     * 保存对话历史记录（含文档资源和分段信息）
     * @return chat_history记录的id，保存失败返回null
     */
    private Long saveChatHistory(Long userId, ChatRequest request, String answerContent,
                                 String conversationId, String messageId, String status,
                                 String errorMessage, List<ChatMessageVO.RetrieverResource> resources) {
        try {
            ChatHistory history = new ChatHistory();
            history.setUserId(userId);
            history.setUserQuestion(request.getQuery());
            history.setDeviceModel(request.getEquipModel());
            history.setAnswerContent(answerContent);
            history.setConversationId(conversationId);
            history.setMessageId(messageId);
            history.setStatus(status);
            history.setCreateTime(LocalDateTime.now());

            // 计算轮次
            if (conversationId != null && !conversationId.isEmpty()) {
                Long existRounds = chatHistoryMapper.selectCount(
                        new LambdaQueryWrapper<ChatHistory>()
                                .eq(ChatHistory::getConversationId, conversationId)
                );
                history.setRoundNum(existRounds.intValue() + 1);
            } else {
                history.setRoundNum(1);
            }

            chatHistoryMapper.insert(history);
            log.info("保存对话历史: conversationId={}, round={}, id={}", conversationId, history.getRoundNum(), history.getId());

            // 保存附件文件记录
            if (request.getFileUrls() != null && !request.getFileUrls().isEmpty() && history.getId() != null) {
                List<String> fileTypes = request.getFileTypes();
                List<String> fileNames = request.getFileNames();
                for (int i = 0; i < request.getFileUrls().size(); i++) {
                    String url = request.getFileUrls().get(i);
                    String fileType = (fileTypes != null && i < fileTypes.size()) ? fileTypes.get(i) : "image";
                    // 优先使用前端传来的原始文件名，否则从URL中提取
                    String fileName;
                    if (fileNames != null && i < fileNames.size() && fileNames.get(i) != null && !fileNames.get(i).isEmpty()) {
                        fileName = fileNames.get(i);
                    } else {
                        fileName = url.substring(url.lastIndexOf("/") + 1);
                    }

                    ChatFile chatFile = new ChatFile();
                    chatFile.setChatHistoryId(history.getId());
                    chatFile.setFileName(fileName);
                    chatFile.setFilePath(url);
                    chatFile.setFileType(fileType);
                    chatFile.setCreateTime(LocalDateTime.now());
                    chatFileMapper.insert(chatFile);
                }
                log.info("保存附件文件记录: conversationId={}, fileCount={}", conversationId, request.getFileUrls().size());
            }

            // 持久化文档资源和分段信息，同时创建反馈记录（状态留空，等待用户主动反馈）
            if (resources != null && !resources.isEmpty() && history.getId() != null) {
                // 创建反馈记录（状态为空，未反馈）
                ChatFeedback feedback = new ChatFeedback();
                feedback.setChatHistoryId(history.getId());
                feedback.setFeedbackStatus("pending");
                feedback.setFeedbackTime(LocalDateTime.now());
                feedback.setUserId(userId);
                chatFeedbackMapper.insert(feedback);

                // 按documentId去重，同一文档只存一条记录
                Map<String, ChatMessageVO.RetrieverResource> docMap = new LinkedHashMap<>();
                Map<String, List<ChatMessageVO.RetrieverResource>> docSegmentsMap = new LinkedHashMap<>();
                for (ChatMessageVO.RetrieverResource res : resources) {
                    String docId = res.getDocumentId();
                    if (!docMap.containsKey(docId)) {
                        docMap.put(docId, res);
                        docSegmentsMap.put(docId, new ArrayList<>());
                    }
                    docSegmentsMap.get(docId).add(res);
                }

                for (Map.Entry<String, ChatMessageVO.RetrieverResource> entry : docMap.entrySet()) {
                    ChatMessageVO.RetrieverResource res = entry.getValue();
                    ChatDocumentResource docResource = new ChatDocumentResource();
                    docResource.setChatHistoryId(history.getId());
                    docResource.setDatasetId(res.getDatasetId());
                    docResource.setDatasetName(res.getDatasetName());
                    docResource.setDocumentId(res.getDocumentId());
                    docResource.setDocumentName(res.getDocumentName());
                    docResource.setCreateTime(LocalDateTime.now());
                    chatDocumentResourceMapper.insert(docResource);

                    // 保存该文档下的分段信息
                    List<ChatMessageVO.RetrieverResource> segments = docSegmentsMap.get(entry.getKey());
                    if (segments != null) {
                        for (ChatMessageVO.RetrieverResource seg : segments) {
                            if (seg.getSegmentId() != null && !seg.getSegmentId().isEmpty()) {
                                ChatSegment segment = new ChatSegment();
                                segment.setDocumentResourceId(docResource.getId());
                                segment.setSegmentId(seg.getSegmentId());
                                segment.setContent(seg.getContent());
                                segment.setScore(seg.getScore());
                                segment.setCreateTime(LocalDateTime.now());
                                chatSegmentMapper.insert(segment);
                            }
                        }
                    }
                }
                log.info("保存文档资源和分段信息: conversationId={}, docCount={}", conversationId, docMap.size());
            }
            return history.getId();
        } catch (Exception e) {
            log.warn("保存对话历史失败", e);
            return null;
        }
    }

    /**
     * 构建Dify聊天消息中的文件列表
     * 使用 local_file 方式：先将文件上传到Dify，获取文件ID，再在聊天消息中引用
     */
    private List<ChatMessageSendRequest.ChatMessageFile> buildChatMessageFiles(ChatRequest request, String apiKey, String userId) {
        if (request.getFileUrls() == null || request.getFileUrls().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> fileTypes = request.getFileTypes();
        int count = request.getFileUrls().size();

        // 并行上传所有文件到Dify
        List<CompletableFuture<ChatMessageSendRequest.ChatMessageFile>> futures = IntStream.range(0, count)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    String url = request.getFileUrls().get(i);
                    String type = (fileTypes != null && i < fileTypes.size()) ? fileTypes.get(i) : "image";
                    try {
                        String uploadFileId = uploadFileToDify(url, apiKey, userId);
                        if (uploadFileId != null) {
                            ChatMessageSendRequest.ChatMessageFile file = new ChatMessageSendRequest.ChatMessageFile();
                            String difyType = type;
                            if ("file".equals(type)) {
                                difyType = "document";
                            }
                            file.setType(difyType);
                            file.setTransferMethod("local_file");
                            file.setUploadFileId(uploadFileId);
                            return file;
                        }
                    } catch (Exception e) {
                        log.warn("上传文件到Dify失败，跳过该文件: url={}, error={}", url, e.getMessage());
                    }
                    return null;
                }, uploadExecutor))
                .collect(Collectors.toList());

        // 等待所有上传完成，过滤失败的
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 将本地文件上传到Dify，返回文件ID
     */
    private String uploadFileToDify(String url, String apiKey, String userId) {
        try {
            // 将相对路径转为本地文件绝对路径
            Path localFilePath = resolveLocalFilePath(url);
            if (localFilePath == null || !Files.exists(localFilePath)) {
                log.warn("文件不存在: url={}, localPath={}", url, localFilePath);
                return null;
            }

            DifyFile difyFile = DifyFile.from(localFilePath);
            FileUploadRequest uploadRequest = new FileUploadRequest(difyFile);
            uploadRequest.setApiKey(apiKey);
            uploadRequest.setUserId(userId);

            FileUploadResponse uploadResponse = difyChat.fileUpload(uploadRequest);
            if (uploadResponse != null && uploadResponse.getId() != null) {
                log.info("文件上传到Dify成功: fileId={}, url={}", uploadResponse.getId(), url);
                return uploadResponse.getId();
            }
            return null;
        } catch (Exception e) {
            log.warn("上传文件到Dify失败: url={}, error={}", url, e.getMessage());
            return null;
        }
    }

    /**
     * 将HTTP URL路径转为本地文件系统绝对路径
     * 支持新目录结构：/uploads/images/2026-06-12/xxx.jpg
     * 兼容旧目录结构：/uploads/2026/06/09/xxx.jpg
     */
    private Path resolveLocalFilePath(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        // 去掉 /uploads/ 前缀，拼接到 uploadDir
        String relativePath = url;
        if (url.startsWith("/uploads/")) {
            relativePath = url.substring("/uploads/".length());
        }
        return Paths.get(uploadDir).toAbsolutePath().resolve(relativePath);
    }
}
