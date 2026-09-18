package com.example.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI智能问答 - 文档资源实体
 * 对应表：chat_document_resource
 * 存储对话命中的文档信息，与chat_history多对一关联
 */
@Data
@TableName("chat_document_resource")
public class ChatDocumentResource {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联chat_history.id */
    private Long chatHistoryId;

    /** 知识库ID */
    private String datasetId;

    /** 知识库名称 */
    private String datasetName;

    /** 文档ID */
    private String documentId;

    /** 文档名称 */
    private String documentName;

    /** 创建时间 */
    private LocalDateTime createTime;
}
