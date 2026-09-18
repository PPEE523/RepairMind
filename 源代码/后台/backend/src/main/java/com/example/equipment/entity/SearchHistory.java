package com.example.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("search_history")
public class SearchHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

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
