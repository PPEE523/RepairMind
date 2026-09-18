package com.example.equipment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI智能问答 - 对话列表视图对象
 * 用于左侧对话历史列表展示
 */
@Data
public class ChatConversationVO {

    /** 对话ID */
    private String conversationId;

    /** 最新一条用户问题 */
    private String userQuestion;

    /** 设备型号 */
    private String deviceModel;

    /** 对话时间（最新一条） */
    private LocalDateTime createTime;

    /** 状态：success/fail */
    private String status;

    /** 对话轮次数 */
    private Integer roundCount;

    /** 用户ID */
    private Long userId;
}
