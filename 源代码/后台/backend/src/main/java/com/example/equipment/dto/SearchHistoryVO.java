package com.example.equipment.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchHistoryVO {

    private Long id;

    private Long userId;

    private String username;

    private String nickname;

    private String queryText;

    private String equipModel;

    private String imageUrl;

    private String imageUrls;

    private String fileTypes;

    private String fileNames;

    private String resultAnswer;

    private Boolean searchSuccess;

    private String errorMessage;

    private String conversationId;

    private LocalDateTime createTime;
}
