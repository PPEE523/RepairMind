package com.example.equipment.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI智能问答 - 对话消息视图对象
 * 用于展示单轮对话的完整信息
 */
@Data
public class ChatMessageVO {

    /** 记录ID */
    private Long id;

    /** 对话ID */
    private String conversationId;

    /** 用户问题 */
    private String userQuestion;

    /** 设备型号 */
    private String deviceModel;

    /** AI回答内容 */
    private String answerContent;

    /** Dify消息ID */
    private String messageId;

    /** 状态：success/fail */
    private String status;

    /** 当前轮次 */
    private Integer roundNum;

    /** 对话时间 */
    private LocalDateTime createTime;

    /** 用户ID */
    private Long userId;

    /** 引用文档资源列表 */
    private List<RetrieverResource> resources;

    /** 反馈状态：pending/correct/incorrect */
    private String feedbackStatus;

    /** 附件文件列表 */
    private List<ChatFileVO> files;

    /**
     * 附件文件视图对象
     */
    @Data
    public static class ChatFileVO {

        /** 文件ID */
        private Long id;

        /** 文件名称 */
        private String fileName;

        /** 文件访问路径（HTTP URL） */
        private String filePath;

        /** 文件大小（字节） */
        private Long fileSize;

        /** 文件类型：image/video/audio */
        private String fileType;

        /** 上传时间 */
        private LocalDateTime createTime;
    }

    /**
     * 引用文档资源
     */
    @Data
    public static class RetrieverResource {

        /** 知识库ID */
        private String datasetId;

        /** 知识库名称 */
        private String datasetName;

        /** 文档ID */
        private String documentId;

        /** 文档名称 */
        private String documentName;

        /** 内容摘要 */
        private String content;

        /** 匹配度分数 */
        private Double score;

        /** 分段ID */
        private String segmentId;
    }
}
