package com.example.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("work_execution")
public class WorkExecution {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workflowId;

    private Long userId;

    private Integer stepIndex;

    private String stepStatus;

    private LocalDateTime completedTime;

    private String remark;

    private String imageUrls;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
