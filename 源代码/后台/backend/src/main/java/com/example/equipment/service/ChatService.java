package com.example.equipment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.equipment.dto.ChatConversationVO;
import com.example.equipment.dto.ChatFeedbackRequest;
import com.example.equipment.dto.ChatMessageVO;
import com.example.equipment.dto.ChatRequest;

import java.util.List;

/**
 * AI智能问答 - 对话服务接口
 */
public interface ChatService {

    /**
     * SSE流式提问回调接口
     */
    interface ChatStreamCallback {
        /** 流式消息片段 */
        void onMessage(String content);
        /** 流式完成（含完整回答、conversationId、messageId、引用资源、chatHistoryId） */
        void onComplete(String answer, String conversationId, String messageId, List<ChatMessageVO.RetrieverResource> resources, Long chatHistoryId);
        /** 流式错误 */
        void onError(String error);
    }

    /**
     * 发起提问（SSE流式）
     *
     * @param request    对话请求
     * @param userId     用户ID
     * @param callback   流式回调
     */
    void sendChatMessage(ChatRequest request, Long userId, ChatStreamCallback callback);

    /**
     * 获取对话历史列表（分页）
     * 按conversationId分组，取每个对话的最新一条记录
     *
     * @param userId   用户ID
     * @param isAdmin  是否管理员
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 对话列表分页
     */
    Page<ChatConversationVO> getConversationList(Long userId, boolean isAdmin, int pageNum, int pageSize);

    /**
     * 根据对话ID获取多轮详情
     *
     * @param conversationId 对话ID
     * @return 消息列表
     */
    List<ChatMessageVO> getConversationDetail(String conversationId);

    /**
     * 删除对话（同步删除Dify远端记录）
     *
     * @param conversationId 对话ID
     * @param userId         用户ID
     */
    void deleteConversation(String conversationId, Long userId);

    /**
     * 提交回答标注
     *
     * @param request 反馈请求
     * @param userId  用户ID
     */
    void submitFeedback(ChatFeedbackRequest request, Long userId);
}
