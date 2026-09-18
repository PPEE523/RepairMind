package com.example.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI智能问答 - 分段信息实体
 * 对应表：chat_segment
 * 存储命中的分段ID，与chat_document_resource多对一关联
 */
@Data
@TableName("chat_segment")
public class ChatSegment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联chat_document_resource.id */
    private Long documentResourceId;

    /** 分段ID */
    private String segmentId;

    /** 分段内容 */
    private String content;

    /** 分段命中得分 */
    private Double score;

    /** 创建时间 */
    private LocalDateTime createTime;
}
