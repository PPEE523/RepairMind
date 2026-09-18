package com.example.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI智能问答 - 反馈实体
 * 对应表：chat_feedback
 * 通过chat_history_id与chat_history表关联
 */
@Data
@TableName("chat_feedback")
public class ChatFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联chat_history.id */
    private Long chatHistoryId;

    /** 反馈状态：correct/incorrect */
    private String feedbackStatus;

    /** 标注时间 */
    private LocalDateTime feedbackTime;

    /** 用户ID（预留） */
    private Long userId;
}
