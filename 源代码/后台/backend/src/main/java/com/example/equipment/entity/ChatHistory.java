package com.example.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI智能问答 - 对话历史实体
 * 对应表：chat_history
 */
@Data
@TableName("chat_history")
public class ChatHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Dify对话ID */
    private String conversationId;

    /** 用户问题 */
    private String userQuestion;

    /** 设备型号 */
    private String deviceModel;

    /** AI回答内容 */
    private String answerContent;

    /** Dify消息ID（本轮） */
    private String messageId;

    /** 状态：success/fail */
    private String status;

    /** 当前对话轮次 */
    private Integer roundNum;

    /** 对话时间 */
    private LocalDateTime createTime;

    /** 用户ID（预留） */
    private Long userId;
}
