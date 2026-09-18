package com.example.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("work_execution_main")
public class WorkExecutionMain {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workflowId;

    private Long userId;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer totalSteps;

    private Integer completedSteps;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
