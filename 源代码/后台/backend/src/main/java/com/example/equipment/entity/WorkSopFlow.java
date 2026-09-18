package com.example.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("work_sop_flow")
public class WorkSopFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流程名称 */
    private String name;

    /** 来源文档ID（Dify知识库文档） */
    private String sourceDocumentId;

    /** 来源文档名称 */
    private String sourceDocumentName;

    /** 来源知识库ID */
    private String datasetId;

    /** 步骤JSON数据 */
    private String stepJson;

    /** 总步骤数 */
    private Integer totalSteps;

    /** 状态: active-启用/archived-归档 */
    private String status;

    /** 创建用户ID */
    private Long createUser;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
