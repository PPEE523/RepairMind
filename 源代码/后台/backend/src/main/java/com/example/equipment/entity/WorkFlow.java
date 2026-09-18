package com.example.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("work_flow")
public class WorkFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String equipModel;

    private String repairLevel;

    private String stepJson;

    private Integer version;

    private Long createUser;

    private LocalDateTime createTime;
}
