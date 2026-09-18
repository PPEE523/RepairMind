package com.example.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("repair_knowledge")
public class RepairKnowledge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String symptom;

    private String solution;

    private String imgUrl;

    /**
     * 案例图片URL列表（JSON数组格式，最多5张）
     */
    private String images;

    private String equipModel;

    private Integer auditStatus;

    private Long uploadUserId;

    /**
     * 上传人昵称（非数据库字段，查询时动态填充）
     */
    @TableField(exist = false)
    private String uploadUserName;

    private Long auditUserId;

    private String auditRemark;

    private String documentId;

    private String datasetId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
