package com.example.equipment.dto;

import lombok.Data;

/**
 * AI智能问答 - 反馈请求DTO
 * 通过chatHistoryId与chat_history表关联
 */
@Data
public class ChatFeedbackRequest {

    /** 关联chat_history.id */
    private Long chatHistoryId;

    /** 反馈状态：correct / incorrect */
    private String feedbackStatus;
}
