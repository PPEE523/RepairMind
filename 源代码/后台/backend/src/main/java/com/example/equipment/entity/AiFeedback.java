package com.example.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_feedback")
public class AiFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long searchHistoryId;

    private String originalAnswer;

    private Integer rating;

    private String correctedAnswer;

    private String feedbackType;

    private LocalDateTime createTime;
}
