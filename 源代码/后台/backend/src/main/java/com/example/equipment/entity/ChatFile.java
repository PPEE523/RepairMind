package com.example.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI智能问答 - 对话附件文件实体
 * 对应表：chat_file
 */
@Data
@TableName("chat_file")
public class ChatFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联对话历史ID */
    private Long chatHistoryId;

    /** 文件名称 */
    private String fileName;

    /** 文件存储路径（相对路径，如 /uploads/images/2026-06-12/xxx.jpg） */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件类型：image/video/audio */
    private String fileType;

    /** Dify上传返回的文件ID */
    private String difyFileId;

    /** 上传时间 */
    private LocalDateTime createTime;
}
