package com.example.equipment.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI修正统计 - 对话反馈详情VO
 * 用于展示chat_feedback关联的完整对话+文档+分段信息
 */
@Data
public class ChatFeedbackVO {

    /** chat_feedback.id */
    private Long id;

    /** chat_history.id */
    private Long chatHistoryId;

    /** 反馈状态：pending/correct/incorrect */
    private String feedbackStatus;

    /** 反馈时间 */
    private LocalDateTime feedbackTime;

    /** 用户ID */
    private Long userId;

    // ===== 来自chat_history =====

    /** 用户问题 */
    private String userQuestion;

    /** AI回答内容 */
    private String answerContent;

    /** 设备型号 */
    private String deviceModel;

    /** 对话时间 */
    private LocalDateTime chatCreateTime;

    // ===== 命中的文档资源列表 =====

    /** 命中文档资源列表 */
    private List<DocumentResourceVO> documentResources;

    /**
     * 文档资源VO
     */
    @Data
    public static class DocumentResourceVO {

        /** chat_document_resource.id */
        private Long id;

        /** 文档ID */
        private String documentId;

        /** 文档名称 */
        private String documentName;

        /** 知识库ID */
        private String datasetId;

        /** 知识库名称 */
        private String datasetName;

        /** 命中分段列表 */
        private List<SegmentVO> segments;
    }

    /**
     * 分段VO
     */
    @Data
    public static class SegmentVO {

        /** chat_segment.id */
        private Long id;

        /** 分段ID */
        private String segmentId;

        /** 分段内容 */
        private String content;

        /** 分段命中得分 */
        private Double score;
    }
}
