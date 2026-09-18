package com.example.equipment.dto;

import lombok.Data;

import java.util.List;

/**
 * AI智能问答 - 对话请求DTO
 */
@Data
public class ChatRequest {

    /** 用户问题（必传） */
    private String query;

    /** 设备型号（非必传） */
    private String equipModel;

    /** Dify对话ID，为空则新建对话（非必传） */
    private String conversationId;

    /** 文件URL列表（非必传） */
    private List<String> fileUrls;

    /** 文件类型列表，与fileUrls一一对应（非必传） */
    private List<String> fileTypes;

    /** 原始文件名列表，与fileUrls一一对应（非必传） */
    private List<String> fileNames;


}
