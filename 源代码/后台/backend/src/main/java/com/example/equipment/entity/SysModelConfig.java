package com.example.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_model_config")
public class SysModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String difyBaseUrl;

    private String difyAppApiKey;

    private String difyDatasetApiKey;

    private String difyDatasetId;

    private Integer enable;

    private LocalDateTime createTime;
}
